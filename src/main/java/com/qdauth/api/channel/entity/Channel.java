package com.qdauth.api.channel.entity;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.stream.entity.LiveStream;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "channels")
public class Channel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "char(36)", length = 36, updatable = false, nullable = false)
  private String id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 2048)
  private String description = "";

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false, columnDefinition = "char(36)")
  private Account account;

  @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ChannelSubscription> subscriptions = new HashSet<>();

  @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<LiveStream> liveStreams = new HashSet<>();

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public Set<ChannelSubscription> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(Set<ChannelSubscription> subscriptions) {
    this.subscriptions = subscriptions;
  }

  public Set<LiveStream> getLiveStreams() {
    return liveStreams;
  }

  public void setLiveStreams(Set<LiveStream> liveStreams) {
    this.liveStreams = liveStreams;
  }
}
