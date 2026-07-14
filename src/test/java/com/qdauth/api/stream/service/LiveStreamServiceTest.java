package com.qdauth.api.stream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.repository.ChannelRepository;
import com.qdauth.api.stream.entity.LiveStream;
import com.qdauth.api.stream.repository.LiveStreamRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiveStreamServiceTest {

  @Mock private LiveStreamRepository liveStreamRepository;
  @Mock private ChannelRepository channelRepository;

  private LiveStreamService liveStreamService;

  @BeforeEach
  void setUp() {
    liveStreamService = new LiveStreamService(liveStreamRepository, channelRepository);
  }

  @Test
  void createStream_successfullyCreatesStream() {
    final String channelId = "channelId";
    final String name = "Live Event";
    final String description = "Event description";

    Channel channel = new Channel();

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
    when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(i -> i.getArgument(0));

    LiveStream stream = liveStreamService.createStream(channelId, name, description);

    assertThat(stream).isNotNull();
    assertThat(stream.getChannel()).isSameAs(channel);
    assertThat(stream.getName()).isEqualTo(name);
    assertThat(stream.getDescription()).isEqualTo(description);

    verify(channelRepository).findById(channelId);
    verify(liveStreamRepository).save(any(LiveStream.class));
  }

  @Test
  void createStream_successfullyCreatesStreamWithNullDescription() {
    final String channelId = "channelId";
    final String name = "Live Event";

    Channel channel = new Channel();

    when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
    when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(i -> i.getArgument(0));

    LiveStream stream = liveStreamService.createStream(channelId, name, null);

    assertThat(stream).isNotNull();
    assertThat(stream.getChannel()).isSameAs(channel);
    assertThat(stream.getName()).isEqualTo(name);
    assertThat(stream.getDescription()).isEmpty();

    verify(channelRepository).findById(channelId);
    verify(liveStreamRepository).save(any(LiveStream.class));
  }

  @Test
  void createStream_throwsWhenChannelDoesNotExist() {
    final String channelId = "missingChannel";

    when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liveStreamService.createStream(channelId, "Stream", "Description"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Channel not found: " + channelId);

    verify(channelRepository).findById(channelId);
  }

  @Test
  void startStream_successfullyStartsStream() {
    final String streamId = "streamId";

    when(liveStreamRepository.startStream(streamId)).thenReturn(1);

    liveStreamService.startStream(streamId);

    verify(liveStreamRepository).startStream(streamId);
  }

  @Test
  void startStream_throwsWhenStreamCannotBeStarted() {
    final String streamId = "streamId";

    when(liveStreamRepository.startStream(streamId)).thenReturn(0);

    assertThatThrownBy(() -> liveStreamService.startStream(streamId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("does not exist or has already been started");

    verify(liveStreamRepository).startStream(streamId);
  }

  @Test
  void stopStream_successfullyStopsStream() {
    final String streamId = "streamId";

    when(liveStreamRepository.stopStream(streamId)).thenReturn(1);

    liveStreamService.stopStream(streamId);

    verify(liveStreamRepository).stopStream(streamId);
  }

  @Test
  void stopStream_throwsWhenStreamCannotBeStopped() {
    final String streamId = "streamId";

    when(liveStreamRepository.stopStream(streamId)).thenReturn(0);

    assertThatThrownBy(() -> liveStreamService.stopStream(streamId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("does not exist, was never started, or has already been stopped");

    verify(liveStreamRepository).stopStream(streamId);
  }

  @Test
  void getStream_successfullyReturnsStream() {
    final String streamId = "streamId";

    LiveStream stream = new LiveStream();
    stream.setName("Live Event");

    when(liveStreamRepository.findById(streamId)).thenReturn(Optional.of(stream));

    LiveStream result = liveStreamService.getStream(streamId);

    assertThat(result).isSameAs(stream);
    assertThat(result.getName()).isEqualTo("Live Event");

    verify(liveStreamRepository).findById(streamId);
  }

  @Test
  void getStream_throwsWhenStreamDoesNotExist() {
    final String streamId = "missingStream";

    when(liveStreamRepository.findById(streamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liveStreamService.getStream(streamId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Stream not found: " + streamId);

    verify(liveStreamRepository).findById(streamId);
  }

  @Test
  void listStreamsForChannel_returnsStreams() {
    final String channelId = "channelId";

    LiveStream stream1 = new LiveStream();
    stream1.setName("Stream 1");

    LiveStream stream2 = new LiveStream();
    stream2.setName("Stream 2");

    when(liveStreamRepository.findByChannelId(channelId)).thenReturn(List.of(stream1, stream2));

    List<LiveStream> result = liveStreamService.listStreamsForChannel(channelId);

    assertThat(result).hasSize(2).containsExactly(stream1, stream2);

    verify(liveStreamRepository).findByChannelId(channelId);
  }
}
