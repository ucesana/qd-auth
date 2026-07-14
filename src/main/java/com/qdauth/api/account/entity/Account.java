package com.qdauth.api.account.entity;

import com.qdauth.api.auth.entity.User;
import com.qdauth.api.channel.entity.Channel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "accounts")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "char(36)", length = 36, updatable = false, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, columnDefinition = "char(36)")
  @NotNull
  private User user;

  @Column(nullable = false, length = 150)
  private String name;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Channel> channels = new HashSet<>();

  public String getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<Channel> getChannels() {
    return channels;
  }

  public void setChannels(Set<Channel> channels) {
    this.channels = channels;
  }

  @Override
  public String toString() {
    return "Account{" + "id='" + id + '\'' + ", user=" + user + ", name='" + name + '\'' + '}';
  }
}
