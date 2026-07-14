package com.qdauth.api.channel.repository;

import com.qdauth.api.channel.entity.Channel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChannelRepository
    extends JpaRepository<Channel, String>, JpaSpecificationExecutor<Channel> {

  List<Channel> findByAccountId(String accountId);
}
