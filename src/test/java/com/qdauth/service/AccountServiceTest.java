package com.qdauth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.qdauth.dto.AccountResponse;
import com.qdauth.dto.RegistrationRequest;
import com.qdauth.model.User;
import com.qdauth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private UserRepository userRepository;

  private PasswordEncoder passwordEncoder;
  private AccountService accountService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    accountService = new AccountService(userRepository, passwordEncoder);
  }

  @Test
  void register_successfullyCreatesAccount() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    RegistrationRequest request = new RegistrationRequest();
    request.setEmail("new@example.com");
    request.setPassword("password123");

    AccountResponse response = accountService.register(request);

    assertThat(response.getEmail()).isEqualTo("new@example.com");
    assertThat(response.isEnabled()).isTrue();
    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_throwsOnDuplicateEmail() {
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    RegistrationRequest request = new RegistrationRequest();
    request.setEmail("existing@example.com");
    request.setPassword("password123");

    assertThatThrownBy(() -> accountService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already registered");
  }

  @Test
  void register_storesEncodedPassword() {
    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    RegistrationRequest request = new RegistrationRequest();
    request.setEmail("new@example.com");
    request.setPassword("plaintext");

    accountService.register(request);

    verify(userRepository)
        .save(argThat(user -> passwordEncoder.matches("plaintext", user.getPassword())));
  }

  @Test
  void getAccount_returnsAccountForValidId() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setEnabled(true);

    when(userRepository.findById("some-uuid")).thenReturn(Optional.of(user));

    AccountResponse response = accountService.getAccount("some-uuid");

    assertThat(response.getEmail()).isEqualTo("test@example.com");
    assertThat(response.isEnabled()).isTrue();
  }

  @Test
  void getAccount_throwsWhenUserNotFound() {
    when(userRepository.findById("missing-uuid")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.getAccount("missing-uuid"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not found");
  }
}
