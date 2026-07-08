package com.qdauth.api.users.service;

import com.qdauth.api.auth.model.Role;
import com.qdauth.api.auth.model.Session;
import com.qdauth.api.auth.model.User;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import com.qdauth.api.users.dto.SessionResponse;
import com.qdauth.api.users.dto.UserCreateRequest;
import com.qdauth.api.users.dto.UserResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsersService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;

  public UsersService(
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
  public UserResponse register(UserCreateRequest registration) {
    if (userRepository.existsByEmail(registration.email())) {
      throw new IllegalArgumentException("Email already registered.");
    }

    final User user = new User();
    user.setEmail(registration.email());
    user.setPassword(passwordEncoder.encode(registration.password()));

    final User userSaved = userRepository.save(user);

    return toDto(userSaved);
  }

  public UserResponse getUser(String userId) {
    final User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("Account not found."));
    return toDto(user);
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

  public List<SessionResponse> getSessions(String userId) {
    List<Session> sessions = this.sessionRepository.findByUserId(userId);
    return Optional.ofNullable(sessions).orElse(List.of()).stream().map(this::toDto).toList();
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

  private SessionResponse toDto(Session session) {
    if (session == null) {
      return null;
    }
    return new SessionResponse(
        session.getFamilyId(),
        toDto(session.getUser()),
        session.getDeviceId(),
        session.getDeviceName(),
        session.getCreatedAt(),
        session.getLastUsedAt());
  }

  private UserResponse toDto(User user) {
    if (user == null) {
      return null;
    }
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.isEnabled(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        Optional.ofNullable(user.getRoles()).orElse(Set.of()).stream().map(Role::getName).toList());
  }
}
