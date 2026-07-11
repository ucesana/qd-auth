package com.qdauth.api.channel.repository;

import com.qdauth.api.channel.model.Channel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, String> {

  List<Channel> findByAccountId(String accountId);
}
