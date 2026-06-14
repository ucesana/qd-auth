package com.qdauth.controller;

import com.qdauth.dto.AccountResponse;
import com.qdauth.dto.Pulse;
import com.qdauth.dto.RegistrationRequest;
import com.qdauth.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private static final Logger log = LoggerFactory.getLogger(AccountController.class);

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @RequestMapping(path = "/register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse register(@Valid @RequestBody RegistrationRequest reqistration) {
    return accountService.register(reqistration);
  }

  @GetMapping("/me")
  public AccountResponse getAccount(@AuthenticationPrincipal String userId) {
    log.info("userId", userId);
    return accountService.getAccount(userId);
  }

  @GetMapping("/health")
  public Pulse checkHealth() {
    log.info("### /api/accounts/health ###");
    return new Pulse();
  }
}
