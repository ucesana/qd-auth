package com.qdauth.api.accounts.dto;

import java.time.Instant;

public class SessionResponse {

  private String familyId;
  private UserResponse user;
  private String deviceId;
  private String deviceName;
  Instant createdAt;
  Instant lastUsedAt;

  public String getFamilyId() {
    return familyId;
  }

  public void setFamilyId(String familyId) {
    this.familyId = familyId;
  }

  public UserResponse getUser() {
    return user;
  }

  public void setUser(UserResponse user) {
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

  public void setLastUsedAt(Instant lastUsedA) {
    this.lastUsedAt = lastUsedA;
  }
}
