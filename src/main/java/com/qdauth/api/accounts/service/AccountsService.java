package com.qdauth.api.accounts.service;

import com.qdauth.api.accounts.dto.AccountResponse;
import com.qdauth.api.accounts.dto.RegistrationRequest;
import com.qdauth.api.accounts.dto.SessionResponse;
import com.qdauth.api.accounts.dto.UserResponse;
import com.qdauth.api.auth.model.Role;
import com.qdauth.api.auth.model.Session;
import com.qdauth.api.auth.model.User;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountsService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;

  public AccountsService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      SessionRepository sessionRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.sessionRepository = sessionRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AccountResponse register(RegistrationRequest registration) {
    if (userRepository.existsByEmail(registration.email())) {
      throw new IllegalArgumentException("Email already registered.");
    }

    final User user = new User();
    user.setEmail(registration.email());
    user.setPassword(passwordEncoder.encode(registration.password()));

    userRepository.save(user);

    return new AccountResponse(user.getId(), user.getEmail(), user.isEnabled());
  }

  public AccountResponse getAccount(String userId) {
    final User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("Account not found."));
    return new AccountResponse(user.getId(), user.getEmail(), user.isEnabled());
  }

  @Transactional
  public void revokeSession(String userId, String familyId) {
    Session session =
        sessionRepository
            .findById(familyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!session.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    refreshTokenRepository.revokeFamily(familyId);
    sessionRepository.deleteByFamilyId(familyId);
  }

  public List<SessionResponse> getSessions(String userId) {
    List<Session> sessions = this.sessionRepository.findByUserId(userId);
    return Optional.ofNullable(sessions).orElse(List.of()).stream().map(this::toDto).toList();
  }

  public SessionResponse getCurrentSession(String userId, String familyId) {
    Session session =
        sessionRepository
            .findById(familyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!session.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    return toDto(session);
  }

  private SessionResponse toDto(Session session) {
    return new SessionResponse(
        session.getFamilyId(),
        toDto(session.getUser()),
        session.getDeviceId(),
        session.getDeviceName(),
        session.getCreatedAt(),
        session.getLastUsedAt());
  }

  private UserResponse toDto(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        Optional.ofNullable(user.getRoles()).orElse(Set.of()).stream().map(Role::getName).toList());
  }
}
