package com.qdauth.api.channel.repository;

import com.qdauth.api.channel.model.ChannelSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ChannelSubscriptionRepository extends JpaRepository<ChannelSubscription, String> {

  /**
   * Retrieves the subscription record linking a given account to a given channel, if one exists.
   * Used to validate subscription state prior to chat submission or duplicate-subscription checks.
   */
  Optional<ChannelSubscription> findByChannelIdAndAccountId(String channelId, String accountId);

  /**
   * Indicates whether an account already holds a subscription to the given channel. This
   * duplicates, at the application level, the uniqueness now enforced by the
   * uq_channel_subscription_channel_account constraint on (channel_id, account_id); it allows a
   * caller to check for an existing subscription without relying on catching a constraint violation
   * from the persistence provider.
   */
  boolean existsByChannelIdAndAccountId(String channelId, String accountId);

  List<ChannelSubscription> findByChannelId(String channelId);

  List<ChannelSubscription> findByAccountId(String accountId);

  /**
   * Bans the subscription associated with the given channel and account, recording the supplied
   * reason and the current timestamp. Returns the number of rows affected (0 if no matching,
   * unbanned subscription exists), allowing the caller to distinguish a no-op from a successful ban
   * without an additional read.
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE ChannelSubscription cs SET cs.banned = true, cs.banReason = :reason, "
          + "cs.bannedAt = CURRENT_TIMESTAMP "
          + "WHERE cs.channel.id = :channelId AND cs.account.id = :accountId AND cs.banned = false")
  int banAccountFromChannel(
      @Param("channelId") String channelId,
      @Param("accountId") String accountId,
      @Param("reason") String reason);
}
