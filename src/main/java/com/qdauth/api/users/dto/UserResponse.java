package com.qdauth.api.users.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    String id,
    String email,
    Boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> roles) {}
