package com.qdauth.dto;

public class TokensResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType = "Bearer";
  private long expiresIn;

  public TokensResponse(String accessToken, String refreshToken, long expiresIn) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public long getExpiresIn() {
    return expiresIn;
  }
}
