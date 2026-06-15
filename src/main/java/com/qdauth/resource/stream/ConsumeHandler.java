package com.qdauth.resource.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class ConsumeHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsumeHandler.class);

    private final StreamState state;

    public ConsumeHandler(StreamState state) {
        this.state = state;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Consumer connected: {}", session.getId());
        state.registerConsumer(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Consumer disconnected: {}", session.getId());
        state.removeConsumer(session);
    }
}
