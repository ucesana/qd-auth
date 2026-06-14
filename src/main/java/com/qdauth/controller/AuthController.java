package com.qdauth.controller;

import com.qdauth.dto.LoginRequest;
import com.qdauth.dto.RefreshTokenRequest;
import com.qdauth.dto.TokensResponse;
import com.qdauth.security.CookieTokenExtractor;
import com.qdauth.service.AuthService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

import static com.qdauth.service.JwtService.*;
import static com.qdauth.service.JwtService.REFRESH_TOKEN_EXPIRY_DAYS;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public TokensResponse login(
          @Valid @RequestBody LoginRequest request,
          HttpServletResponse response
  ) throws Exception {
    TokensResponse tokens = authService.login(request);
    setTokenCookies(response, tokens);
    return tokens;
  }

  @PostMapping("/refresh")
  public TokensResponse refresh(
          @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
          HttpServletRequest request,
          HttpServletResponse response) throws Exception {
    RefreshTokenRequest resolved = resolveRefreshRequest(refreshTokenRequest, request);
    TokensResponse tokens = authService.refresh(resolved);
    setTokenCookies(response, tokens);
    return tokens;
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
          @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
          HttpServletRequest request,
          HttpServletResponse response) {
    authService.logout(resolveRefreshRequest(refreshTokenRequest, request));
    clearTokenCookies(response);
    return ResponseEntity.noContent().build();
  }

  private RefreshTokenRequest resolveRefreshRequest(RefreshTokenRequest body, HttpServletRequest request) {
    if (body != null && !StringUtils.isBlank(body.getRefreshToken())) {
      return body;
    }
    return new RefreshTokenRequest(CookieTokenExtractor.extractRefreshToken(request));
  }

  private void setTokenCookies(HttpServletResponse response, TokensResponse tokens) {
    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie(tokens).toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens).toString());
  }

  private ResponseCookie accessTokenCookie(TokensResponse tokens) {
    return ResponseCookie.from(ACCESS_TOKEN, tokens.getAccessToken())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(Duration.ofSeconds(ACCESS_TOKEN_EXPIRY_SECONDS))
            .sameSite("Strict")
            .build();
  }

  private ResponseCookie refreshTokenCookie(TokensResponse tokens) {
    return ResponseCookie.from(REFRESH_TOKEN, tokens.getRefreshToken())
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .maxAge(Duration.ofDays(REFRESH_TOKEN_EXPIRY_DAYS))
            .sameSite("Strict")
            .build();
  }

  private void clearTokenCookies(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie("access_token").toString());
    response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie("refresh_token").toString());
  }

  private ResponseCookie expiredCookie(String name) {
    return ResponseCookie.from(name, "").httpOnly(true).secure(true).path("/").maxAge(0).build();
  }
}
