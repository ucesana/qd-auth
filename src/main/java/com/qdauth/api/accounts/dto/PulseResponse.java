package com.qdauth.api.accounts.dto;

public record PulseResponse(String status) {

  public PulseResponse() {
    this("OK");
  }
}
