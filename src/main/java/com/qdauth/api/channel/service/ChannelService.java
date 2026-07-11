package com.qdauth.api.channel.service;

import com.qdauth.api.account.model.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.channel.model.Channel;
import com.qdauth.api.channel.repository.ChannelRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelService {

  private final ChannelRepository channelRepository;
  private final AccountRepository accountRepository;

  public ChannelService(ChannelRepository channelRepository, AccountRepository accountRepository) {
    this.channelRepository = channelRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public Channel createChannel(String accountId, String name, String description) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
    Channel channel = new Channel();
    channel.setAccount(account);
    channel.setName(name);
    channel.setDescription(description == null ? "" : description);
    return channelRepository.save(channel);
  }

  public Channel getChannel(String channelId) {
    return channelRepository
        .findById(channelId)
        .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + channelId));
  }

  public List<Channel> listChannelsForAccount(String accountId) {
    return channelRepository.findByAccountId(accountId);
  }

  public List<Channel> listAll() {
    return this.channelRepository.findAll();
  }
}
