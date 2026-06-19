package com.qdauth.api.accounts.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    String userId,
    String email,
    Boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> roles) {}
