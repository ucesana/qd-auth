package com.qdauth.api.accounts.controller;

import com.qdauth.api.accounts.dto.AccountResponse;
import com.qdauth.api.accounts.dto.RegistrationRequest;
import com.qdauth.api.accounts.dto.SessionResponse;
import com.qdauth.api.accounts.service.AccountsService;
import com.qdauth.api.auth.security.QdPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountsController {

  private static final Logger log = LoggerFactory.getLogger(AccountsController.class);

  private final AccountsService accountsService;

  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @RequestMapping(
      path = "/register",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse register(@Valid @RequestBody RegistrationRequest registrationRequest) {
    return accountsService.register(registrationRequest);
  }

  @GetMapping("/me")
  public AccountResponse getAccount(@AuthenticationPrincipal QdPrincipal principal) {
    return accountsService.getAccount(principal.userId());
  }

  @GetMapping("/me/sessions")
  public List<SessionResponse> getSessions(@AuthenticationPrincipal QdPrincipal principal) {
    return accountsService.getSessions(principal.userId());
  }

  @GetMapping("/me/sessions/current")
  public SessionResponse getCurrentSession(@AuthenticationPrincipal QdPrincipal principal) {
    return accountsService.getCurrentSession(principal.userId(), principal.familyId());
  }

  @DeleteMapping("/me/sessions/{familyId}")
  public ResponseEntity<Void> revokeSession(
      @AuthenticationPrincipal QdPrincipal principal, @PathVariable String familyId) {
    accountsService.revokeSession(principal.userId(), familyId);
    return ResponseEntity.noContent().build();
  }
}
