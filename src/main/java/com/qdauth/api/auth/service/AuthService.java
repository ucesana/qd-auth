package com.qdauth.api.auth.service;

import static com.qdauth.api.auth.service.JwtService.ACCESS_TOKEN_EXPIRY_SECONDS;
import static com.qdauth.api.auth.service.JwtService.REFRESH_TOKEN_EXPIRY_DAYS;

import com.nimbusds.jwt.JWTClaimsSet;
import com.qdauth.api.auth.dto.LoginRequest;
import com.qdauth.api.auth.dto.RefreshTokenRequest;
import com.qdauth.api.auth.dto.TokensResponse;
import com.qdauth.api.auth.model.RefreshToken;
import com.qdauth.api.auth.model.Role;
import com.qdauth.api.auth.model.Session;
import com.qdauth.api.auth.model.User;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      SessionRepository sessionRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.sessionRepository = sessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public TokensResponse login(LoginRequest request, String deviceId, String deviceName)
      throws Exception {
    final User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new SecurityException("Invalid credentials."));

    if (!user.isEnabled()) {
      throw new SecurityException("Account is disabled.");
    }

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new SecurityException("Invalid credentials.");
    }

    // Invalidate any existing sessions for this device
    List<Session> staleSessions = sessionRepository.findByUserIdAndDeviceId(user.getId(), deviceId);
    if (!staleSessions.isEmpty()) {
      staleSessions.forEach(s -> refreshTokenRepository.deleteByFamilyId(s.getFamilyId()));
      sessionRepository.deleteAll(staleSessions);
    }

    final String familyId = UUID.randomUUID().toString();

    final Session session = new Session();
    session.setFamilyId(familyId);
    session.setUser(user);
    session.setDeviceId(deviceId);
    session.setDeviceName(deviceName);
    final Instant now = Instant.now();
    session.setCreatedAt(now);
    session.setLastUsedAt(now);
    sessionRepository.save(session);

    return issueTokenPair(user, familyId);
  }

  @Transactional
  public TokensResponse refresh(RefreshTokenRequest request) throws Exception {
    final JWTClaimsSet claims = jwtService.verify(request.refreshToken());

    final String type = (String) claims.getClaim("type");
    if (!"refresh".equals(type)) {
      throw new SecurityException("Invalid token type.");
    }

    final String tokenId = claims.getJWTID();

    final RefreshToken stored =
        refreshTokenRepository
            .findById(tokenId)
            .orElseThrow(() -> new SecurityException("Invalid refresh token."));

    // Reuse detected — revoke entire family immediately
    if (stored.isConsumed()) {
      refreshTokenRepository.revokeFamily(stored.getFamilyId());
      throw new SecurityException("Refresh token reuse detected. All sessions revoked.");
    }

    if (stored.isRevoked()) {
      throw new SecurityException("Refresh token has been revoked.");
    }

    if (stored.getExpiresAt().isBefore(Instant.now())) {
      throw new SecurityException("Refresh token has expired.");
    }

    // Consume the current token
    stored.setConsumed(true);
    refreshTokenRepository.save(stored);

    final User user = stored.getUser();

    if (!user.isEnabled()) {
      throw new SecurityException("Account is disabled.");
    }

    // Update session activity timestamp
    sessionRepository
        .findById(stored.getFamilyId())
        .ifPresent(
            (Session session) -> {
              session.setLastUsedAt(Instant.now());
              sessionRepository.save(session);
            });

    // Issue new token pair in the same family
    return issueTokenPair(user, stored.getFamilyId());
  }

  private TokensResponse issueTokenPair(User user, String familyId) throws Exception {
    final List<String> roles =
        user.getRoles().stream().map(Role::getName).collect(Collectors.toList());

    final String accessToken = jwtService.issueAccessToken(user.getId(), roles, familyId);

    // Persist the refresh token record first to get its ID
    final RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setFamilyId(familyId);
    refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS).toInstant());
    refreshTokenRepository.save(refreshToken);

    final String refreshTokenJwt = jwtService.issueRefreshToken(user.getId(), refreshToken.getId());

    return new TokensResponse(accessToken, refreshTokenJwt, ACCESS_TOKEN_EXPIRY_SECONDS);
  }

  @Transactional
  public void logout(@Valid RefreshTokenRequest request) {
    final JWTClaimsSet claims;
    try {
      claims = jwtService.verify(request.refreshToken());
    } catch (Exception e) {
      // The system treats logout as a no-op. The controller clears the client cookie regardless,
      // so no security gap occurs.
      return;
    }

    final String tokenId = claims.getJWTID();

    refreshTokenRepository
        .findById(tokenId)
        .ifPresent(
            token -> {
              refreshTokenRepository.revokeFamily(token.getFamilyId());
              sessionRepository.deleteByFamilyId(token.getFamilyId());
            });
  }
}
