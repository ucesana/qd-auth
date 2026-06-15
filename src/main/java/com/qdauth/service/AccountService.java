package com.qdauth.service;

import com.qdauth.dto.AccountResponse;
import com.qdauth.dto.RegistrationRequest;
import com.qdauth.dto.SessionResponse;
import com.qdauth.model.Session;
import com.qdauth.model.User;
import com.qdauth.repository.RefreshTokenRepository;
import com.qdauth.repository.SessionRepository;
import com.qdauth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;

  public AccountService(
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
    if (userRepository.existsByEmail(registration.getEmail())) {
      throw new IllegalArgumentException("Email already registered.");
    }

    final User user = new User();
    user.setEmail(registration.getEmail());
    user.setPassword(passwordEncoder.encode(registration.getPassword()));

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

  public void revokeSession(String userId, String familyId) {
    Session session = sessionRepository.findById(familyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!session.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    refreshTokenRepository.revokeFamily(familyId);
    sessionRepository.deleteByFamilyId(familyId);
  }

  public List<SessionResponse> getSessions(String userId) {
    List<Session> sessions = this.sessionRepository.findByUserId(userId);
    return Optional.ofNullable(sessions).orElse(List.of()).stream().map(this::toSession).toList();
  }

  private SessionResponse toSession(Session session) {
    SessionResponse sessionResponse = new SessionResponse();
    sessionResponse.setFamilyId(session.getFamilyId());
    sessionResponse.setUser(session.getUser());
    sessionResponse.setDeviceName(session.getDeviceName());
    sessionResponse.setCreatedAt(session.getCreatedAt());
    sessionResponse.setLastUsedAt(session.getLastUsedAt());
    return sessionResponse;
  }
}
