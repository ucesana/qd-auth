package com.qdauth.api.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "sessions")
public class Session {

  @Id
  @Column(
      name = "family_id",
      columnDefinition = "char(36)",
      updatable = false,
      nullable = false,
      length = 36)
  private String familyId;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Size(max = 255)
  @Column(name = "device_id")
  private String deviceId;

  @Size(max = 255)
  @Column(name = "device_name")
  private String deviceName;

  @NotNull
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @NotNull
  @Column(name = "last_used_at", nullable = false)
  private Instant lastUsedAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    this.lastUsedAt = Instant.now();
  }

  public String getFamilyId() {
    return familyId;
  }

  public void setFamilyId(String familyId) {
    this.familyId = familyId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
