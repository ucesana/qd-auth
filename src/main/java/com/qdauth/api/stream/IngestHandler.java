package com.qdauth.api.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class IngestHandler extends AbstractWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(IngestHandler.class);

  private final StreamRegistry registry;

  public IngestHandler(StreamRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    String streamId = getStreamId(session);
    StreamState stream = registry.getOrCreate(streamId);

    stream.attachProducer(session);

    log.info("Producer connected: stream={}, session={}", streamId, session.getId());
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {

    String streamId = getStreamId(session);
    StreamState stream = registry.get(streamId);

    if (stream == null) {
      return;
    }

    byte[] chunk = new byte[message.getPayloadLength()];
    message.getPayload().get(chunk);

    stream.ingest(chunk);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

    String streamId = getStreamId(session);
    StreamState stream = registry.get(streamId);

    if (stream != null) {
      stream.detachProducer(session);
    }
  }

  private String getStreamId(WebSocketSession session) {
    return (String) session.getAttributes().get("streamId");
  }
}
