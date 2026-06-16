package com.qdauth.api.accounts.dto;

import java.time.Instant;

public record SessionResponse(
    String familyId,
    UserResponse user,
    String deviceId,
    String deviceName,
    Instant createdAt,
    Instant lastUsedAt) {}
