package com.qdauth.controller;

import com.qdauth.dto.AccountResponse;
import com.qdauth.dto.Pulse;
import com.qdauth.dto.RegistrationRequest;
import com.qdauth.dto.SessionResponse;
import com.qdauth.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

  @GetMapping("/me/sessions")
  public List<SessionResponse> getSessions(@AuthenticationPrincipal String userId) {
    return accountService.getSessions(userId);
  }

  @DeleteMapping("/me/sessions/{familyId}")
  public ResponseEntity<Void> revokeSession(
          @AuthenticationPrincipal String userId,
          @PathVariable String familyId) {
    accountService.revokeSession(userId, familyId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/health")
  public Pulse checkHealth() {
    log.info("### /api/accounts/health ###");
    return new Pulse();
  }


}
