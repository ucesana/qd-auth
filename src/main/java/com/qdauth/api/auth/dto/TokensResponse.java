package com.qdauth.api.auth.dto;

public record TokensResponse(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {

  public TokensResponse(String accessToken, String refreshToken, long expiresIn) {
    this(accessToken, refreshToken, "Bearer", expiresIn);
  }
}
