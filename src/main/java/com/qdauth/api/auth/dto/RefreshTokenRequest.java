package com.qdauth.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {

  public RefreshTokenRequest() {
    this("");
  }
}
