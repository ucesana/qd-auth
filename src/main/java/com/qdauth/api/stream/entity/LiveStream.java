package com.qdauth.api.stream.entity;

import com.qdauth.api.channel.entity.Channel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "live_streams")
public class LiveStream {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "char(36)", length = 36, updatable = false, nullable = false)
  private String id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 2048)
  private String description = "";

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "channel_id", nullable = false, columnDefinition = "char(36)")
  private Channel channel;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "stopped_at")
  private LocalDateTime stoppedAt;

  @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<LiveStreamChat> chats = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

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

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getStoppedAt() {
    return stoppedAt;
  }

  public void setStoppedAt(LocalDateTime stoppedAt) {
    this.stoppedAt = stoppedAt;
  }

  public Set<LiveStreamChat> getChats() {
    return chats;
  }

  public void setChats(Set<LiveStreamChat> chats) {
    this.chats = chats;
  }
}
