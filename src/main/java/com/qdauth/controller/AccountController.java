package com.qdauth.controller;

import com.qdauth.dto.AccountResponse;
import com.qdauth.dto.RegisterRequest;
import com.qdauth.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
    return accountService.register(request);
  }

  @GetMapping("/me")
  public AccountResponse getAccount(@AuthenticationPrincipal String userId) {
    return accountService.getAccount(userId);
  }
}
