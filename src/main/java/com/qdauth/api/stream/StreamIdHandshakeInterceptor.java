package com.qdauth.api.stream;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class StreamIdHandshakeInterceptor implements HandshakeInterceptor {
  private static final Logger log = LoggerFactory.getLogger(StreamIdHandshakeInterceptor.class);

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    String path = request.getURI().getPath();

    log.info("path = {}", path);

    String[] parts = path.split("/");

    // /api/livestreams/{id}/ingest
    //  0      1          2     3
    if (parts.length >= 4) {
      String streamId = parts[3];
      if (!"undefined".equals(streamId) && !"null".equals(streamId)) {
        attributes.put("streamId", streamId);
        log.info("Extracted streamId = {}", streamId);
        return true;
      }
    }

    return false;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
