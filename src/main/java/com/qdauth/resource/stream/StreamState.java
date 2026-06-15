package com.qdauth.resource.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Centralised, thread-safe state shared between the ingest and consume handlers.
 *
 * Thread-safety model:
 *   - Consumer registration/removal uses a ConcurrentHashMap-backed Set.
 *   - EBML header and chunk buffer mutations are guarded by a ReadWriteLock.
 *     Reads (on consumer join) take the read lock; writes (on ingest) take the write lock.
 *   - Individual WebSocketSession.sendMessage() calls are synchronised per-session
 *     because the JSR-356 spec does not guarantee concurrent send safety.
 */
@Component
public class StreamState {

    private static final Logger log = LoggerFactory.getLogger(StreamState.class);

    static final int BUFFER_SIZE = 5;

    // EBML header marker: 0x1A 0x45 0xDF 0xA3
    private static final byte[] EBML_MAGIC = {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3};

    // Matroska Cluster element ID: 0x1F 0x43 0xB6 0x75
    static final byte[] CLUSTER_ID = {0x1f, 0x43, (byte) 0xb6, 0x75};

    private final Set<WebSocketSession> consumers = ConcurrentHashMap.newKeySet();

    // Guarded by lock
    private byte[] header = null;
    private final Deque<byte[]> chunkBuffer = new ArrayDeque<>();
    private boolean foundFirstCluster = false;

    private String mimeType = null;

    private boolean firstChunkSent = false;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void setMimeType(String mimeType) {
        lock.writeLock().lock();
        try { this.mimeType = mimeType; }
        finally { lock.writeLock().unlock(); }
    }

    // -------------------------------------------------------------------------
    // State reset
    // -------------------------------------------------------------------------

    public void reset() {
        lock.writeLock().lock();
        try {
            header = null;
            chunkBuffer.clear();
            foundFirstCluster = false;
            mimeType = null;
            firstChunkSent = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    // EBML / cluster detection helpers
    // -------------------------------------------------------------------------

    public static boolean isEBMLHeader(byte[] data) {
        if (data.length < 4) return false;
        return (data[0] & 0xFF) == 0x1a
                && (data[1] & 0xFF) == 0x45
                && (data[2] & 0xFF) == 0xdf
                && (data[3] & 0xFF) == 0xa3;
    }

    /**
     * Returns the index of the first occurrence of {@code needle} in {@code haystack},
     * or -1 if not found.
     */
    public static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static byte[] subarray(byte[] src, int from, int to) {
        byte[] result = new byte[to - from];
        System.arraycopy(src, from, result, 0, to - from);
        return result;
    }

    // -------------------------------------------------------------------------
    // Ingest-side processing
    // -------------------------------------------------------------------------

    /**
     * Process a raw binary chunk arriving from the producer.
     *
     * @return the media chunk to forward to consumers, or null if it should be
     *         withheld (still accumulating EBML header).
     */
    public byte[] processIncomingChunk(byte[] chunk) {
        lock.writeLock().lock();
        try {
            if (!foundFirstCluster) {
                if (!isEBMLHeader(chunk) && header == null) {
                    // Waiting for the stream to begin with an EBML header.
                    return null;
                }

                header = (header == null) ? chunk : concat(header, chunk);

                int clusterIndex = indexOf(header, CLUSTER_ID);
                if (clusterIndex == -1) {
                    log.debug("Accumulating EBML header, current size: {}", header.length);
                    return null;
                }

                byte[] actualHeader = subarray(header, 0, clusterIndex);
                byte[] firstMediaChunk = subarray(header, clusterIndex, header.length);
                header = actualHeader;
                foundFirstCluster = true;
                log.info("Split header: {} bytes. First media chunk: {} bytes",
                        header.length, firstMediaChunk.length);
                chunk = firstMediaChunk;
            }

            // Maintain rolling buffer; each entry must begin at a Cluster boundary.
            if (indexOf(chunk, CLUSTER_ID) == 0) {
                chunkBuffer.addLast(chunk);
            } else if (!chunkBuffer.isEmpty()) {
                byte[] last = chunkBuffer.removeLast();
                chunkBuffer.addLast(concat(last, chunk));
            }

            if (chunkBuffer.size() > BUFFER_SIZE) {
                chunkBuffer.removeFirst();
            }

            if (!firstChunkSent) {
                firstChunkSent = true;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }

            return chunk;

        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Consumer registration
    // -------------------------------------------------------------------------

    /**
     * Sends the EBML init segment and all buffered clusters to a newly joined consumer,
     * then registers it for live chunks.
     */
    public void registerConsumer(WebSocketSession session) throws IOException {
        lock.readLock().lock();
        try {
            log.info("registerConsumer: mimeType={}, hasHeader={}, bufferSize={}",
                    mimeType, header != null, chunkBuffer.size());
            if (mimeType != null) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(
                            "{\"type\":\"stream_start\",\"mimeType\":\"" + mimeType + "\"}"
                    ));
                }
            }
            if (header != null) {
                send(session, header);
                for (byte[] chunk : chunkBuffer) send(session, chunk);
            }
        } finally {
            lock.readLock().unlock();
        }
        consumers.add(session);
    }

    public void removeConsumer(WebSocketSession session) {
        consumers.remove(session);
    }

    // -------------------------------------------------------------------------
    // Broadcast
    // -------------------------------------------------------------------------

    public void broadcastChunk(byte[] chunk) {
        for (WebSocketSession c : consumers) {
            if (c.isOpen()) {
                try {
                    send(c, chunk);
                } catch (IOException e) {
                    log.warn("Failed to send chunk to consumer {}: {}", c.getId(), e.getMessage());
                }
            }
        }
    }

    public void broadcastText(String json) {
        for (WebSocketSession c : consumers) {
            if (c.isOpen()) {
                try {
                    synchronized (c) {
                        c.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    log.warn("Failed to send text to consumer {}: {}", c.getId(), e.getMessage());
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void send(WebSocketSession session, byte[] data) throws IOException {
        synchronized (session) {
            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(data)));
        }
    }
}