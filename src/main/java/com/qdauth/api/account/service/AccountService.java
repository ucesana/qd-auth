package com.qdauth.api.account.service;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.auth.entity.User;
import com.qdauth.api.auth.repository.UserRepository;
import com.qdauth.api.auth.security.QdPrincipal;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Account createAccount(String userId, String name) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    Account account = new Account();
    account.setUser(user);
    account.setName(useNameOrCreateName(name, user));
    return accountRepository.save(account);
  }

  private String useNameOrCreateName(String name, User user) {
    if (StringUtils.isBlank(name)) {
      return user.getEmail().substring(0, user.getEmail().indexOf("@"));
    }
    return name;
  }

  public Account getAccount(String accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
  }

  public Account getAccountForUser(QdPrincipal principal) {
    return getAccountForUser(principal.userId());
  }

  public Account getAccountForUser(String userId) {
    return accountRepository
        .getByUserId(userId)
        .orElseThrow(
            () -> new EntityNotFoundException("Account not found for user userId = " + userId));
  }

  public List<Account> listAccountsForUser(String userId) {
    return accountRepository.findByUserId(userId);
  }

  @Transactional
  public Account updateAccountName(String id, String name) {
    final Account account = getAccount(id);
    account.setName(name);
    return account;
  }
}
