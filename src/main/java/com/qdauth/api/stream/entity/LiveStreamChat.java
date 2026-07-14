package com.qdauth.api.stream.entity;

import com.qdauth.api.channel.entity.ChannelSubscription;
import jakarta.persistence.*;

@Entity
@Table(name = "live_stream_chats")
public class LiveStreamChat {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "char(36)", length = 36, updatable = false, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stream_id", nullable = false, columnDefinition = "char(36)")
  private LiveStream stream;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "channel_subscription_id", nullable = false, columnDefinition = "char(36)")
  private ChannelSubscription channelSubscription;

  @Column(nullable = false, length = 256)
  private String message = "";

  public String getId() {
    return id;
  }

  public LiveStream getStream() {
    return stream;
  }

  public void setStream(LiveStream stream) {
    this.stream = stream;
  }

  public ChannelSubscription getChannelSubscription() {
    return channelSubscription;
  }

  public void setChannelSubscription(ChannelSubscription channelSubscription) {
    this.channelSubscription = channelSubscription;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
