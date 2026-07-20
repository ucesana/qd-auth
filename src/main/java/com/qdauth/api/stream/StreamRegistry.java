package com.qdauth.api.stream;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StreamRegistry {

  private final ConcurrentHashMap<String, StreamState> streams = new ConcurrentHashMap<>();

  public StreamState getOrCreate(String streamId) {
    return streams.computeIfAbsent(streamId, StreamState::new);
  }

  public StreamState get(String streamId) {
    return streams.get(streamId);
  }

  public void remove(String streamId) {
    streams.remove(streamId);
  }
}
