package com.qdauth.api.account.controller;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.service.AccountService;
import com.qdauth.api.auth.security.AccessGuard;
import com.qdauth.api.auth.security.QdPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;
  private final AccessGuard accessGuard;

  public AccountController(AccountService accountService, AccessGuard accessGuard) {
    this.accountService = accountService;
    this.accessGuard = accessGuard;
  }

  /** Create an account for the user. */
  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(
      @RequestBody CreateAccountRequest request, @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.createAccount(principal.userId(), request.name());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AccountController.AccountResponse.from(account));
  }

  /** List the accounts owned by the user. */
  @GetMapping("/me")
  public List<AccountResponse> getMyAccounts(@AuthenticationPrincipal QdPrincipal principal) {
    return accountService.listAccountsForUser(principal.userId()).stream()
        .map(AccountResponse::from)
        .toList();
  }

  /** Get an account owned by the user. */
  @GetMapping("/{id}")
  public AccountResponse getAccount(
      @PathVariable String id, @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.getAccount(id);
    accessGuard.requireAccountOwner(principal, account);
    return AccountResponse.from(account);
  }

  /** Update the name of an account owned by the user. */
  @PatchMapping("/{id}")
  public AccountResponse updateName(
      @PathVariable String id,
      @RequestBody UpdateAccountNameRequest request,
      @AuthenticationPrincipal QdPrincipal principal) {
    Account account = accountService.getAccount(id);
    accessGuard.requireAccountOwner(principal, account);
    return AccountResponse.from(accountService.updateAccountName(id, request.name()));
  }

  public record CreateAccountRequest(String name) {}

  public record UpdateAccountNameRequest(String name) {}

  public record AccountResponse(String id, String userId, String name) {
    public static AccountResponse from(Account account) {
      return new AccountResponse(account.getId(), account.getUser().getId(), account.getName());
    }
  }
}
