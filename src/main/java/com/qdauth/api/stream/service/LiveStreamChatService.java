package com.qdauth.api.stream.service;

import com.qdauth.api.channel.model.Channel;
import com.qdauth.api.channel.model.ChannelSubscription;
import com.qdauth.api.channel.repository.ChannelSubscriptionRepository;
import com.qdauth.api.stream.model.LiveStream;
import com.qdauth.api.stream.model.LiveStreamChat;
import com.qdauth.api.stream.repository.LiveStreamChatRepository;
import com.qdauth.api.stream.repository.LiveStreamRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiveStreamChatService {

  private final LiveStreamChatRepository liveStreamChatRepository;
  private final LiveStreamRepository liveStreamRepository;
  private final ChannelSubscriptionRepository channelSubscriptionRepository;

  public LiveStreamChatService(
      LiveStreamChatRepository liveStreamChatRepository,
      LiveStreamRepository liveStreamRepository,
      ChannelSubscriptionRepository channelSubscriptionRepository) {
    this.liveStreamChatRepository = liveStreamChatRepository;
    this.liveStreamRepository = liveStreamRepository;
    this.channelSubscriptionRepository = channelSubscriptionRepository;
  }

  /**
   * Posts a chat message to the given stream on behalf of the given account. Requires that the
   * account hold a non-banned subscription to the stream's channel; otherwise the message is
   * rejected without being persisted.
   */
  @Transactional
  public LiveStreamChat postMessage(String streamId, String accountId, String message) {
    LiveStream stream =
        liveStreamRepository
            .findById(streamId)
            .orElseThrow(() -> new EntityNotFoundException("Stream not found: " + streamId));
    Channel channel = stream.getChannel();

    ChannelSubscription subscription =
        channelSubscriptionRepository
            .findByChannelIdAndAccountId(channel.getId(), accountId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Account "
                            + accountId
                            + " is not subscribed to channel "
                            + channel.getId()));

    if (subscription.isBanned()) {
      throw new IllegalStateException(
          "Account " + accountId + " is banned from channel " + channel.getId());
    }

    LiveStreamChat chat = new LiveStreamChat();
    chat.setStream(stream);
    chat.setChannelSubscription(subscription);
    chat.setMessage(message);
    return liveStreamChatRepository.save(chat);
  }

  public List<LiveStreamChat> listMessagesForStream(String streamId) {
    return liveStreamChatRepository.findByStreamId(streamId);
  }
}
