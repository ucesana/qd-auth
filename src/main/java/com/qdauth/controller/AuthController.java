package com.qdauth.controller;

import com.qdauth.dto.LoginRequest;
import com.qdauth.dto.RefreshRequest;
import com.qdauth.dto.TokenResponse;
import com.qdauth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) throws Exception {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) throws Exception {
    return authService.refresh(request);
  }
}
