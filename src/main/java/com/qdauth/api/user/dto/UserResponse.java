package com.qdauth.api.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    String id,
    String email,
    Boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> roles) {}
