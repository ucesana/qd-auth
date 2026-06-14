package com.qdauth.dto;

public class Pulse {

    private String status;

    public Pulse() {
        this("OK");
    }

    public Pulse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
