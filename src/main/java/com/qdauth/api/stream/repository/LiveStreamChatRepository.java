package com.qdauth.api.stream.repository;

import com.qdauth.api.stream.entity.LiveStreamChat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveStreamChatRepository extends JpaRepository<LiveStreamChat, String> {

  List<LiveStreamChat> findByStreamId(String streamId);

  List<LiveStreamChat> findByChannelSubscriptionId(String channelSubscriptionId);
}
