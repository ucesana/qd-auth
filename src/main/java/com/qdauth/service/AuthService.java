package com.qdauth.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.qdauth.dto.LoginRequest;
import com.qdauth.dto.RefreshRequest;
import com.qdauth.dto.TokenResponse;
import com.qdauth.model.RefreshToken;
import com.qdauth.model.User;
import com.qdauth.repository.RefreshTokenRepository;
import com.qdauth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900;
  private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public TokenResponse login(LoginRequest request) throws Exception {
    final User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new SecurityException("Invalid credentials."));

    if (!user.isEnabled()) {
      throw new SecurityException("Account is disabled.");
    }

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new SecurityException("Invalid credentials.");
    }

    return issueTokenPair(user, UUID.randomUUID().toString());
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest request) throws Exception {
    final JWTClaimsSet claims = jwtService.verify(request.getRefreshToken());

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

    if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new SecurityException("Refresh token has expired.");
    }

    // Consume the current token
    stored.setConsumed(true);
    refreshTokenRepository.save(stored);

    final User user = stored.getUser();

    if (!user.isEnabled()) {
      throw new SecurityException("Account is disabled.");
    }

    // Issue new token pair in the same family
    return issueTokenPair(user, stored.getFamilyId());
  }

  private TokenResponse issueTokenPair(User user, String familyId) throws Exception {
    final List<String> roles =
        user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList());

    final String accessToken = jwtService.issueAccessToken(user.getId(), roles);

    // Persist the refresh token record first to obtain its ID
    final RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setFamilyId(familyId);
    refreshToken.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS));
    refreshTokenRepository.save(refreshToken);

    final String refreshTokenJwt = jwtService.issueRefreshToken(user.getId(), refreshToken.getId());

    return new TokenResponse(accessToken, refreshTokenJwt, ACCESS_TOKEN_EXPIRY_SECONDS);
  }
}
