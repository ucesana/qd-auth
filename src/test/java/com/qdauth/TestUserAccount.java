package com.qdauth;

import com.qdauth.api.account.model.Account;
import com.qdauth.api.user.dto.UserResponse;
import jakarta.servlet.http.Cookie;

public record TestUserAccount(UserResponse user, Account account, Cookie cookie) {}
