package com.qdauth.api.channel.entity;

import com.qdauth.api.account.entity.Account;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "channel_subscriptions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_channel_subscription_channel_account",
            columnNames = {"channel_id", "account_id"}))
public class ChannelSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "char(36)", length = 36, updatable = false, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "channel_id", nullable = false, columnDefinition = "char(36)")
  private Channel channel;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false, columnDefinition = "char(36)")
  private Account account;

  @Column(nullable = false)
  private boolean banned = false;

  @Column(name = "ban_reason", length = 512)
  private String banReason;

  @Column(name = "banned_at")
  private LocalDateTime bannedAt;

  public String getId() {
    return id;
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public boolean isBanned() {
    return banned;
  }

  public void setBanned(boolean banned) {
    this.banned = banned;
  }

  public String getBanReason() {
    return banReason;
  }

  public void setBanReason(String banReason) {
    this.banReason = banReason;
  }

  public LocalDateTime getBannedAt() {
    return bannedAt;
  }

  public void setBannedAt(LocalDateTime bannedAt) {
    this.bannedAt = bannedAt;
  }
}
