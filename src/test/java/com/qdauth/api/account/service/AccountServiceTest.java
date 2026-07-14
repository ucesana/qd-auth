package com.qdauth.api.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.auth.entity.User;
import com.qdauth.api.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private UserRepository userRepository;

  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountService(accountRepository, userRepository);
  }

  @Test
  void create_successfullyCreatesAccount() {
    final String userId = "userId";
    final String userName = "userName";

    final User userMock = mock(User.class);
    when(userMock.getId()).thenReturn(userId);
    when(userRepository.findById(any())).thenReturn(Optional.of(userMock));

    when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

    Account account = accountService.createAccount(userId, userName);

    assertThat(account).isNotNull();
    assertThat(account.getName()).isEqualTo(userName);
    assertThat(account.getUser()).isNotNull();
    assertThat(account.getUser().getId()).isEqualTo(userId);
  }

  @Test
  void create_successfullyCreatesAccountNameIsNull() {
    final String userId = "userId";
    final String email = "abc123@company.com";

    final User userMock = mock(User.class);
    when(userMock.getId()).thenReturn(userId);
    when(userMock.getEmail()).thenReturn(email);
    when(userRepository.findById(any())).thenReturn(Optional.of(userMock));

    when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

    Account account = accountService.createAccount(userId, null);

    assertThat(account).isNotNull();
    assertThat(account.getName()).isEqualTo("abc123");
    assertThat(account.getUser()).isNotNull();
    assertThat(account.getUser().getId()).isEqualTo(userId);
  }

  @Test
  void create_successfullyCreatesAccountNameIsBlank() {
    final String userId = "userId";
    final String email = "abc123@company.com";

    final User userMock = mock(User.class);
    when(userMock.getId()).thenReturn(userId);
    when(userMock.getEmail()).thenReturn(email);
    when(userRepository.findById(any())).thenReturn(Optional.of(userMock));

    when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

    Account account = accountService.createAccount(userId, "");

    assertThat(account).isNotNull();
    assertThat(account.getName()).isEqualTo("abc123");
    assertThat(account.getUser()).isNotNull();
    assertThat(account.getUser().getId()).isEqualTo(userId);
  }

  @Test
  void create_throwsEntityNotFoundException() {
    assertThatThrownBy(() -> accountService.createAccount("userId", "userName"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void getAccount_successfullyReturnsAccount() {
    final String accountId = "accountId";

    Account account = new Account();
    account.setName("My Account");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    Account result = accountService.getAccount(accountId);

    assertThat(result).isSameAs(account);
    assertThat(result.getName()).isEqualTo("My Account");

    verify(accountRepository).findById(accountId);
  }

  @Test
  void getAccount_throwsEntityNotFoundException() {
    final String accountId = "missingAccount";

    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.getAccount(accountId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Account not found: " + accountId);

    verify(accountRepository).findById(accountId);
  }

  @Test
  void listAccountsForUser_returnsAccounts() {
    final String userId = "userId";

    Account account1 = new Account();
    account1.setName("Account 1");

    when(accountRepository.getByUserId(userId)).thenReturn(Optional.of(account1));

    Account account = accountService.getAccountForUser(userId);

    assertThat(account).isEqualTo(account1);

    verify(accountRepository).getByUserId(userId);
  }

  @Test
  void updateAccountName_updatesAndReturnsUpdatedAccount() {
    final String accountId = "accountId";
    final String newName = "Updated Name";

    Account updatedAccount = new Account();
    updatedAccount.setName(newName);

    when(accountRepository.findById(any())).thenReturn(Optional.of(updatedAccount));

    Account result = accountService.updateAccountName(accountId, newName);

    assertThat(result).isSameAs(updatedAccount);
    assertThat(result.getName()).isEqualTo(newName);

    verify(accountRepository).findById(accountId);
  }

  @Test
  void updateAccountName_throwsEntityNotFoundExceptionWhenAccountDoesNotExist() {
    final String accountId = "missingAccount";
    final String newName = "Updated Name";

    assertThatThrownBy(() -> accountService.updateAccountName(accountId, newName))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Account not found: " + accountId);

    verify(accountRepository).findById(accountId);
  }
}
