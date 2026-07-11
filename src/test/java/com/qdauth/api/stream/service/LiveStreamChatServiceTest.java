package com.qdauth.api.stream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.qdauth.api.channel.model.Channel;
import com.qdauth.api.channel.model.ChannelSubscription;
import com.qdauth.api.channel.repository.ChannelSubscriptionRepository;
import com.qdauth.api.stream.model.LiveStream;
import com.qdauth.api.stream.model.LiveStreamChat;
import com.qdauth.api.stream.repository.LiveStreamChatRepository;
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
class LiveStreamChatServiceTest {

  @Mock private LiveStreamChatRepository liveStreamChatRepository;
  @Mock private LiveStreamRepository liveStreamRepository;
  @Mock private ChannelSubscriptionRepository channelSubscriptionRepository;

  private LiveStreamChatService liveStreamChatService;

  @BeforeEach
  void setUp() {
    liveStreamChatService =
        new LiveStreamChatService(
            liveStreamChatRepository, liveStreamRepository, channelSubscriptionRepository);
  }

  @Test
  void postMessage_successfullyCreatesChatMessage() {
    final String streamId = "streamId";
    final String accountId = "accountId";
    final String message = "Hello world";
    final String channelId = "channelId";

    Channel channel = mock(Channel.class);
    when(channel.getId()).thenReturn(channelId);

    LiveStream stream = new LiveStream();
    stream.setChannel(channel);

    ChannelSubscription subscription = new ChannelSubscription();

    when(liveStreamRepository.findById(streamId)).thenReturn(Optional.of(stream));
    when(channelSubscriptionRepository.findByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(Optional.of(subscription));
    when(liveStreamChatRepository.save(any(LiveStreamChat.class)))
        .thenAnswer(i -> i.getArgument(0));

    LiveStreamChat result = liveStreamChatService.postMessage(streamId, accountId, message);

    assertThat(result).isNotNull();
    assertThat(result.getStream()).isSameAs(stream);
    assertThat(result.getChannelSubscription()).isSameAs(subscription);
    assertThat(result.getMessage()).isEqualTo(message);

    verify(liveStreamRepository).findById(streamId);
    verify(channelSubscriptionRepository).findByChannelIdAndAccountId(channelId, accountId);
    verify(liveStreamChatRepository).save(any(LiveStreamChat.class));
  }

  @Test
  void postMessage_throwsWhenStreamDoesNotExist() {
    final String streamId = "missingStream";

    when(liveStreamRepository.findById(streamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liveStreamChatService.postMessage(streamId, "accountId", "message"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Stream not found: " + streamId);

    verify(liveStreamRepository).findById(streamId);
  }

  @Test
  void postMessage_throwsWhenAccountIsNotSubscribed() {
    final String streamId = "streamId";
    final String accountId = "accountId";
    final String channelId = "channelId";

    Channel channel = mock(Channel.class);
    when(channel.getId()).thenReturn(channelId);

    LiveStream stream = new LiveStream();
    stream.setChannel(channel);

    when(liveStreamRepository.findById(streamId)).thenReturn(Optional.of(stream));
    when(channelSubscriptionRepository.findByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> liveStreamChatService.postMessage(streamId, accountId, "message"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("is not subscribed");

    verify(channelSubscriptionRepository).findByChannelIdAndAccountId(channelId, accountId);
  }

  @Test
  void postMessage_throwsWhenAccountIsBanned() {
    final String streamId = "streamId";
    final String accountId = "accountId";
    final String channelId = "channelId";

    Channel channel = mock(Channel.class);
    when(channel.getId()).thenReturn(channelId);

    LiveStream stream = new LiveStream();
    stream.setChannel(channel);

    ChannelSubscription subscription = new ChannelSubscription();
    subscription.setBanned(true);

    when(liveStreamRepository.findById(streamId)).thenReturn(Optional.of(stream));
    when(channelSubscriptionRepository.findByChannelIdAndAccountId(channelId, accountId))
        .thenReturn(Optional.of(subscription));

    assertThatThrownBy(() -> liveStreamChatService.postMessage(streamId, accountId, "message"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("is banned");

    verify(channelSubscriptionRepository).findByChannelIdAndAccountId(channelId, accountId);
  }

  @Test
  void listMessagesForStream_returnsMessages() {
    final String streamId = "streamId";

    LiveStreamChat chat1 = new LiveStreamChat();
    chat1.setMessage("Hello");

    LiveStreamChat chat2 = new LiveStreamChat();
    chat2.setMessage("World");

    when(liveStreamChatRepository.findByStreamId(streamId)).thenReturn(List.of(chat1, chat2));

    List<LiveStreamChat> result = liveStreamChatService.listMessagesForStream(streamId);

    assertThat(result).hasSize(2).containsExactly(chat1, chat2);

    verify(liveStreamChatRepository).findByStreamId(streamId);
  }
}
