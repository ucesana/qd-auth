package com.qdauth.api.channel.service;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.channel.entity.Channel;
import com.qdauth.api.channel.entity.ChannelSubscription;
import com.qdauth.api.channel.repository.ChannelRepository;
import com.qdauth.api.channel.repository.ChannelSubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelSubscriptionService {

  private final ChannelSubscriptionRepository channelSubscriptionRepository;
  private final ChannelRepository channelRepository;
  private final AccountRepository accountRepository;

  public ChannelSubscriptionService(
      ChannelSubscriptionRepository channelSubscriptionRepository,
      ChannelRepository channelRepository,
      AccountRepository accountRepository) {
    this.channelSubscriptionRepository = channelSubscriptionRepository;
    this.channelRepository = channelRepository;
    this.accountRepository = accountRepository;
  }

  /**
   * Creates a subscription linking the given account to the given channel. The application-level
   * existence check is performed first to provide a clear error in the common case; the operation
   * is additionally guarded by the uq_channel_subscription_channel_account database constraint, so
   * a concurrent duplicate request is rejected at the persistence layer even if it passes the
   * initial check (see the race-condition note under point 3 of the accompanying explanation).
   */
  @Transactional
  public ChannelSubscription subscribe(String channelId, String accountId) {
    Channel channel =
        channelRepository
            .findById(channelId)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + channelId));

    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

    if (channelSubscriptionRepository.existsByChannelIdAndAccountId(channelId, accountId)) {
      throw new IllegalStateException(
          "Account " + accountId + " is already subscribed to channel " + channelId);
    }

    ChannelSubscription subscription = new ChannelSubscription();
    subscription.setChannel(channel);
    subscription.setAccount(account);

    try {
      return channelSubscriptionRepository.save(subscription);
    } catch (DataIntegrityViolationException e) {
      throw new IllegalStateException(
          "Account " + accountId + " is already subscribed to channel " + channelId, e);
    }
  }

  public Optional<ChannelSubscription> getSubscription(String channelId, String accountId) {
    return channelSubscriptionRepository.findByChannelIdAndAccountId(channelId, accountId);
  }

  /**
   * Bans the given account from the given channel, recording the supplied reason. Throws if no
   * unbanned subscription exists for the pair, which covers both "never subscribed" and "already
   * banned" cases.
   */
  @Transactional
  public void banAccount(String channelId, String accountId, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("A ban reason is required");
    }
    int updated = channelSubscriptionRepository.banAccountFromChannel(channelId, accountId, reason);
    if (updated == 0) {
      throw new IllegalStateException(
          "No active subscription found for channel "
              + channelId
              + " and account "
              + accountId
              + ", or the account is already banned");
    }
  }

  public List<ChannelSubscription> listSubscriptionsByAccountId(String accountId) {
    return channelSubscriptionRepository.findByAccountId(accountId);
  }

  public List<ChannelSubscription> listSubscriptionsByChannelId(String channelId) {
    return channelSubscriptionRepository.findByChannelId(channelId);
  }
}
