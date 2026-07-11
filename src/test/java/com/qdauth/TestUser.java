package com.qdauth;

import com.qdauth.api.user.dto.UserResponse;
import jakarta.servlet.http.Cookie;

public record TestUser(UserResponse user, Cookie cookie) {}
