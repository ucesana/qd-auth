package com.qdauth.api.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class ConsumeHandler extends AbstractWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(ConsumeHandler.class);

  private final StreamRegistry registry;

  public ConsumeHandler(StreamRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    String streamId = getStreamId(session);
    StreamState stream = registry.get(streamId);

    if (stream == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Stream does not exist"));
      return;
    }

    stream.registerConsumer(session);

    log.info("Consumer connected: stream={}, session={}", streamId, session.getId());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

    String streamId = getStreamId(session);
    StreamState stream = registry.get(streamId);

    if (stream != null) {
      stream.removeConsumer(session);
    }
  }

  private String getStreamId(WebSocketSession session) {
    return (String) session.getAttributes().get("streamId");
  }
}
