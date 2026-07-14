package com.qdauth.api.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.qdauth.api.auth.dto.LoginRequest;
import com.qdauth.api.auth.dto.RefreshTokenRequest;
import com.qdauth.api.auth.dto.TokensResponse;
import com.qdauth.api.auth.entity.RefreshToken;
import com.qdauth.api.auth.entity.User;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import com.qdauth.util.TestKeyLoader;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private SessionRepository sessionRepository;

  private JwtService jwtService;
  private AuthService authService;

  private User testUser;

  private final String deviceId = "deviceId";
  private final String deviceName = "deviceName";

  @BeforeEach
  void setUp() throws Exception {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    jwtService = new JwtService(TestKeyLoader.loadPrivateKey(), TestKeyLoader.loadPublicKey());
    authService =
        new AuthService(
            userRepository, refreshTokenRepository, sessionRepository, passwordEncoder, jwtService);

    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setPassword(passwordEncoder.encode("password123"));
    testUser.setEnabled(true);
  }

  @Test
  void login_returnsTokenPairOnValidCredentials() throws Exception {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

    LoginRequest request = new LoginRequest("test@example.com", "password123");

    TokensResponse response = authService.login(request, deviceId, deviceName);

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(900);
  }

  @Test
  void login_throwsOnUnknownEmail() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    LoginRequest request = new LoginRequest("ghost@example.com", "password123");

    assertThatThrownBy(() -> authService.login(request, deviceId, deviceName))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void login_throwsOnWrongPassword() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    LoginRequest request = new LoginRequest("test@example.com", "wrong-password");

    assertThatThrownBy(() -> authService.login(request, deviceId, deviceName))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void login_throwsOnDisabledAccount() {
    testUser.setEnabled(false);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    LoginRequest request = new LoginRequest("test@example.com", "password123");

    assertThatThrownBy(() -> authService.login(request, deviceId, deviceName))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("disabled");
  }

  @Test
  void refresh_returnsNewTokenPairOnValidToken() throws Exception {
    String familyId = UUID.randomUUID().toString();
    String tokenId = UUID.randomUUID().toString();

    String refreshJwt = jwtService.issueRefreshToken(testUser.getId(), tokenId);

    RefreshToken stored = new RefreshToken();
    stored.setUser(testUser);
    stored.setFamilyId(familyId);
    stored.setConsumed(false);
    stored.setRevoked(false);
    stored.setExpiresAt(OffsetDateTime.now().plusDays(7).toInstant());

    when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

    RefreshTokenRequest request = new RefreshTokenRequest(refreshJwt);

    TokensResponse response = authService.refresh(request);

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    verify(refreshTokenRepository).save(argThat(RefreshToken::isConsumed));
  }

  @Test
  void refresh_revokesEntireFamilyOnTokenReuse() throws Exception {
    String familyId = UUID.randomUUID().toString();
    String tokenId = UUID.randomUUID().toString();

    String refreshJwt = jwtService.issueRefreshToken(testUser.getId(), tokenId);

    RefreshToken consumed = new RefreshToken();
    consumed.setUser(testUser);
    consumed.setFamilyId(familyId);
    consumed.setConsumed(true);
    consumed.setRevoked(false);
    consumed.setExpiresAt(OffsetDateTime.now().plusDays(7).toInstant());

    when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(consumed));

    RefreshTokenRequest request = new RefreshTokenRequest(refreshJwt);

    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("reuse detected");

    verify(refreshTokenRepository).revokeFamily(familyId);
  }

  @Test
  void refresh_throwsOnRevokedToken() throws Exception {
    String tokenId = UUID.randomUUID().toString();
    String refreshJwt = jwtService.issueRefreshToken(testUser.getId(), tokenId);

    RefreshToken revoked = new RefreshToken();
    revoked.setUser(testUser);
    revoked.setFamilyId(UUID.randomUUID().toString());
    revoked.setConsumed(false);
    revoked.setRevoked(true);
    revoked.setExpiresAt(OffsetDateTime.now().plusDays(7).toInstant());

    when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(revoked));

    RefreshTokenRequest request = new RefreshTokenRequest(refreshJwt);

    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("revoked");
  }
}
