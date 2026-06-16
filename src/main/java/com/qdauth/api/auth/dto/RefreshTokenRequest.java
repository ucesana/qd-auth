package com.qdauth.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {

  @NotBlank private String refreshToken;

  public RefreshTokenRequest() {
    this("");
  }

  public RefreshTokenRequest(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
