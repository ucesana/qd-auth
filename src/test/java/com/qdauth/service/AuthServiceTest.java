package com.qdauth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.qdauth.dto.LoginRequest;
import com.qdauth.dto.RefreshTokenRequest;
import com.qdauth.dto.TokensResponse;
import com.qdauth.model.User;
import com.qdauth.repository.RefreshTokenRepository;
import com.qdauth.repository.UserRepository;
import com.qdauth.util.TestKeyLoader;
import java.time.LocalDateTime;
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

  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private AuthService authService;

  private User testUser;

  @BeforeEach
  void setUp() throws Exception {
    passwordEncoder = new BCryptPasswordEncoder();
    jwtService = new JwtService(TestKeyLoader.loadPrivateKey(), TestKeyLoader.loadPublicKey());
    authService =
        new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);

    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setPassword(passwordEncoder.encode("password123"));
    testUser.setEnabled(true);
  }

  @Test
  void login_returnsTokenPairOnValidCredentials() throws Exception {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(refreshTokenRepository.save(any(com.qdauth.model.RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");

    TokensResponse response = authService.login(request);

    assertThat(response.getAccessToken()).isNotBlank();
    assertThat(response.getRefreshToken()).isNotBlank();
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getExpiresIn()).isEqualTo(900);
  }

  @Test
  void login_throwsOnUnknownEmail() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    LoginRequest request = new LoginRequest();
    request.setEmail("ghost@example.com");
    request.setPassword("password123");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void login_throwsOnWrongPassword() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("wrongpassword");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void login_throwsOnDisabledAccount() {
    testUser.setEnabled(false);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("disabled");
  }

  @Test
  void refresh_returnsNewTokenPairOnValidToken() throws Exception {
    String familyId = UUID.randomUUID().toString();
    String tokenId = UUID.randomUUID().toString();

    String refreshJwt = jwtService.issueRefreshToken(testUser.getId(), tokenId);

    com.qdauth.model.RefreshToken stored = new com.qdauth.model.RefreshToken();
    stored.setUser(testUser);
    stored.setFamilyId(familyId);
    stored.setConsumed(false);
    stored.setRevoked(false);
    stored.setExpiresAt(LocalDateTime.now().plusDays(7));

    when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));
    when(refreshTokenRepository.save(any(com.qdauth.model.RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshJwt);

    TokensResponse response = authService.refresh(request);

    assertThat(response.getAccessToken()).isNotBlank();
    assertThat(response.getRefreshToken()).isNotBlank();
    verify(refreshTokenRepository).save(argThat(com.qdauth.model.RefreshToken::isConsumed));
  }

  @Test
  void refresh_revokesEntireFamilyOnTokenReuse() throws Exception {
    String familyId = UUID.randomUUID().toString();
    String tokenId = UUID.randomUUID().toString();

    String refreshJwt = jwtService.issueRefreshToken(testUser.getId(), tokenId);

    com.qdauth.model.RefreshToken consumed = new com.qdauth.model.RefreshToken();
    consumed.setUser(testUser);
    consumed.setFamilyId(familyId);
    consumed.setConsumed(true);
    consumed.setRevoked(false);
    consumed.setExpiresAt(LocalDateTime.now().plusDays(7));

    when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(consumed));

    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshJwt);

    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("reuse detected");

    verify(refreshTokenRepository).revokeFamily(familyId);
  }

  @Test
  void refresh_throwsOnRevokedToken() throws Exception {
    String tokenId = UUID.randomUUID().toString();
    String refreshJwt = jwtService.issueRefreshToken(testUser.getId(), tokenId);

    com.qdauth.model.RefreshToken revoked = new com.qdauth.model.RefreshToken();
    revoked.setUser(testUser);
    revoked.setFamilyId(UUID.randomUUID().toString());
    revoked.setConsumed(false);
    revoked.setRevoked(true);
    revoked.setExpiresAt(LocalDateTime.now().plusDays(7));

    when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(revoked));

    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshJwt);

    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("revoked");
  }
}
