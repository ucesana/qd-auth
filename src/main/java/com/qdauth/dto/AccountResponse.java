package com.qdauth.dto;

public class AccountResponse {

  private String id;
  private String email;
  private boolean enabled;

  public AccountResponse(String id, String email, boolean enabled) {
    this.id = id;
    this.email = email;
    this.enabled = enabled;
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
