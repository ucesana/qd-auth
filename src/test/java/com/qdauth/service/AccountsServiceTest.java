package com.qdauth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.qdauth.api.accounts.dto.AccountResponse;
import com.qdauth.api.accounts.dto.RegistrationRequest;
import com.qdauth.api.accounts.dto.SessionResponse;
import com.qdauth.api.accounts.service.AccountsService;
import com.qdauth.api.auth.model.Session;
import com.qdauth.api.auth.model.User;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

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

  @Test
  void getCurrentSession_successfullyGetsCurrentSession() {
    String userId = "userId";
    String familyId = "familyId";

    User user = mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(user.getEmail()).thenReturn("test@example.com");
    when(user.isEnabled()).thenReturn(true);

    Session session = new Session();
    session.setUser(user);
    session.setFamilyId(familyId);

    when(sessionRepository.findById("familyId")).thenReturn(Optional.of(session));

    final SessionResponse response = accountsService.getCurrentSession(userId, familyId);

    assertThat(response).isNotNull();
    assertThat(response.user()).isNotNull();
    assertThat(response.user().email()).isEqualTo("test@example.com");
    assertThat(response.user().enabled()).isTrue();
    assertThat(response.familyId()).isEqualTo(familyId);
  }

  @Test
  void getCurrentSession_throwsNotFound() {
    assertThatThrownBy(() -> accountsService.getCurrentSession("userId", "familyId"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              assertThat(((ResponseStatusException) ex).getStatusCode())
                  .isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  void getCurrentSession_throwsForbidden() {
    String userId = "forbidden-userId";
    String familyId = "forbidden-familyId";

    User user = mock(User.class);
    when(user.getId()).thenReturn("userId");

    Session session = new Session();
    session.setUser(user);
    session.setFamilyId(familyId);

    when(sessionRepository.findById(familyId)).thenReturn(Optional.of(session));

    assertThatThrownBy(() -> accountsService.getCurrentSession(userId, familyId))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              assertThat(((ResponseStatusException) ex).getStatusCode())
                  .isEqualTo(HttpStatus.FORBIDDEN);
            });
  }

  @Test
  void getCurrentSession_SuccessfulGetSessions() {
    when(sessionRepository.findByUserId("userId"))
        .thenReturn(List.of(new Session(), new Session()));

    List<SessionResponse> sessions = accountsService.getSessions("userId");

    assertThat(sessions).hasSize(2);
  }

  @Test
  void getCurrentSession_revokeSession() {
    String userId = "userId";
    String familyId = "familyId";

    User user = mock(User.class);
    when(user.getId()).thenReturn(userId);

    Session session = new Session();
    session.setUser(user);
    session.setFamilyId(familyId);

    when(sessionRepository.findById(familyId)).thenReturn(Optional.of(session));

    accountsService.revokeSession(userId, familyId);

    verify(refreshTokenRepository).revokeFamily(familyId);
    verify(sessionRepository).deleteByFamilyId(familyId);
  }

  @Test
  void invokeSession_throwsNotFound() {
    String userId = "userId";
    String familyId = "familyId";

    assertThatThrownBy(() -> accountsService.revokeSession(userId, familyId))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException e = (ResponseStatusException) ex;
              assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  void invokeSession_throwsForbidden() {
    String userId = "notFound-userId";
    String familyId = "notFound-familyId";

    User user = mock(User.class);
    when(user.getId()).thenReturn("userId");

    Session session = new Session();
    session.setUser(user);
    session.setFamilyId(familyId);

    when(sessionRepository.findById(familyId)).thenReturn(Optional.of(session));

    assertThatThrownBy(() -> accountsService.revokeSession(userId, familyId))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException e = (ResponseStatusException) ex;
              assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            });
  }
}
