package com.qdauth.api.stream.service;

import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.repository.ChannelRepository;
import com.qdauth.api.stream.entity.LiveStream;
import com.qdauth.api.stream.repository.LiveStreamRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiveStreamService {

  private final LiveStreamRepository liveStreamRepository;
  private final ChannelRepository channelRepository;

  public LiveStreamService(
      LiveStreamRepository liveStreamRepository, ChannelRepository channelRepository) {
    this.liveStreamRepository = liveStreamRepository;
    this.channelRepository = channelRepository;
  }

  @Transactional
  public LiveStream createStream(String channelId, String name, String description) {
    Channel channel =
        channelRepository
            .findById(channelId)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + channelId));
    LiveStream stream = new LiveStream();
    stream.setChannel(channel);
    stream.setName(name);
    stream.setDescription(description == null ? "" : description);
    return liveStreamRepository.save(stream);
  }

  /**
   * Marks the given stream as started. Throws if the stream does not exist or has already been
   * started, since {@link LiveStreamRepository#startStream} reports 0 affected rows in either case.
   */
  @Transactional
  public void startStream(String streamId) {
    int updated = liveStreamRepository.startStream(streamId);
    if (updated == 0) {
      throw new IllegalStateException(
          "Stream " + streamId + " does not exist or has already been started");
    }
  }

  /**
   * Marks the given stream as stopped. Throws if the stream does not exist, was never started, or
   * has already been stopped.
   */
  @Transactional
  public void stopStream(String streamId) {
    int updated = liveStreamRepository.stopStream(streamId);
    if (updated == 0) {
      throw new IllegalStateException(
          "Stream " + streamId + " does not exist, was never started, or has already been stopped");
    }
  }

  public LiveStream getStream(String streamId) {
    return liveStreamRepository
        .findById(streamId)
        .orElseThrow(() -> new EntityNotFoundException("Stream not found: " + streamId));
  }

  public List<LiveStream> browseStreams() {
    return liveStreamRepository.findLiveStreamsByStartedAtIsNotNullAndStoppedAtIsNull();
  }

  public List<LiveStream> listStreamsForChannel(String channelId) {
    return liveStreamRepository.findByChannelId(channelId);
  }
}
