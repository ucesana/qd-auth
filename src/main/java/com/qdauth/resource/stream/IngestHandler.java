package com.qdauth.resource.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Handles the /ingest endpoint.
 *
 * The Node.js original attempts JSON.parse on every binary frame and silently
 * ignores parse failures. This implementation mirrors that behaviour: text
 * frames are parsed as JSON control messages; binary frames are treated as
 * media data. Spring's WebSocket layer distinguishes them at the protocol
 * level, so no try/catch parse attempt on binary data is required.
 */
public class IngestHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(IngestHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final StreamState state;

    public IngestHandler(StreamState state) {
        this.state = state;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Producer connected: {}", session.getId());
        // Do not reset or broadcast here — wait for stream_start
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            if ("stream_start".equals(node.path("type").asText())) {
                String mimeType = node.path("mimeType").asText(null);
                log.info("stream_start received, mimeType: {}", mimeType);
                state.reset(); // clears header, buffer, mimeType
                if (mimeType != null) {
                    state.setMimeType(mimeType);
                }
                // Now broadcast reconnect — consumers will immediately receive
                // stream_start+mimeType from registerConsumer on their next join,
                // but existing consumers need to be told to reset their SourceBuffer
                state.broadcastText("{\"type\":\"reconnect\",\"mimeType\":\"" + mimeType + "\"}");
            }
        } catch (Exception e) {
            log.debug("Non-JSON text frame from producer: {}", e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] chunk = new byte[message.getPayload().remaining()];
        message.getPayload().get(chunk);

        byte[] toForward = state.processIncomingChunk(chunk);
        if (toForward != null) {
            state.broadcastChunk(toForward);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Producer disconnected: {}", status);
    }
}
