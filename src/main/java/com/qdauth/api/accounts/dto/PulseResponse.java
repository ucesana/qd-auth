package com.qdauth.api.accounts.dto;

public class PulseResponse {

  private String status;

  public PulseResponse() {
    this("OK");
  }

  public PulseResponse(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
