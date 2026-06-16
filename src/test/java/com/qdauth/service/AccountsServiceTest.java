package com.qdauth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.qdauth.api.accounts.dto.AccountResponse;
import com.qdauth.api.accounts.dto.RegistrationRequest;
import com.qdauth.api.accounts.service.AccountsService;
import com.qdauth.api.auth.model.User;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountsServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private SessionRepository sessionRepository;

  private PasswordEncoder passwordEncoder;
  private AccountsService accountsService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    accountsService =
        new AccountsService(
            userRepository, refreshTokenRepository, sessionRepository, passwordEncoder);
  }

  @Test
  void register_successfullyCreatesAccount() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    RegistrationRequest request = new RegistrationRequest("new@example.com", "password123");

    AccountResponse response = accountsService.register(request);

    assertThat(response.email()).isEqualTo("new@example.com");
    assertThat(response.enabled()).isTrue();
    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_throwsOnDuplicateEmail() {
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    RegistrationRequest request = new RegistrationRequest("existing@example.com", "password123");

    assertThatThrownBy(() -> accountsService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already registered");
  }

  @Test
  void register_storesEncodedPassword() {
    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    RegistrationRequest request = new RegistrationRequest("new@example.com", "plaintext");

    accountsService.register(request);

    verify(userRepository)
        .save(argThat(user -> passwordEncoder.matches("plaintext", user.getPassword())));
  }

  @Test
  void getAccount_returnsAccountForValidId() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setEnabled(true);

    when(userRepository.findById("some-uuid")).thenReturn(Optional.of(user));

    AccountResponse response = accountsService.getAccount("some-uuid");

    assertThat(response.email()).isEqualTo("test@example.com");
    assertThat(response.enabled()).isTrue();
  }

  @Test
  void getAccount_throwsWhenUserNotFound() {
    when(userRepository.findById("missing-uuid")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountsService.getAccount("missing-uuid"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not found");
  }
}
