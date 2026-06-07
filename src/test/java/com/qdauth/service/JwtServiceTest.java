package com.qdauth.service;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jwt.JWTClaimsSet;
import com.qdauth.util.TestKeyLoader;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() throws Exception {
    jwtService = new JwtService(TestKeyLoader.loadPrivateKey(), TestKeyLoader.loadPublicKey());
  }

  @Test
  void issueAccessToken_containsExpectedClaims() throws Exception {
    String userId = "user-uuid-123";
    List<String> roles = List.of("ROLE_USER");

    String token = jwtService.issueAccessToken(userId, roles);
    JWTClaimsSet claims = jwtService.verify(token);

    assertThat(claims.getSubject()).isEqualTo(userId);
    assertThat(claims.getIssuer()).isEqualTo("qd-auth");
    assertThat(claims.getStringListClaim("roles")).containsExactly("ROLE_USER");
    assertThat(claims.getStringClaim("type")).isEqualTo("access");
    assertThat(claims.getExpirationTime()).isNotNull();
    assertThat(claims.getJWTID()).isNotNull();
  }

  @Test
  void issueRefreshToken_containsExpectedClaims() throws Exception {
    String userId = "user-uuid-123";
    String tokenId = "token-uuid-456";

    String token = jwtService.issueRefreshToken(userId, tokenId);
    JWTClaimsSet claims = jwtService.verify(token);

    assertThat(claims.getSubject()).isEqualTo(userId);
    assertThat(claims.getStringClaim("type")).isEqualTo("refresh");
    assertThat(claims.getJWTID()).isEqualTo(tokenId);
  }

  @Test
  void verify_throwsOnTamperedToken() throws Exception {
    String token = jwtService.issueAccessToken("user-uuid-123", List.of());
    String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

    assertThatThrownBy(() -> jwtService.verify(tampered)).isInstanceOf(Exception.class);
  }

  @Test
  void verify_throwsOnExpiredToken() throws Exception {
    // An expired token is constructed directly using Nimbus
    com.nimbusds.jose.JWSHeader header =
        new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256).build();

    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject("user-uuid-123")
            .expirationTime(java.util.Date.from(java.time.Instant.now().minusSeconds(60)))
            .claim("type", "access")
            .build();

    com.nimbusds.jwt.SignedJWT jwt = new com.nimbusds.jwt.SignedJWT(header, claims);
    jwt.sign(new com.nimbusds.jose.crypto.RSASSASigner(TestKeyLoader.loadPrivateKey()));

    assertThatThrownBy(() -> jwtService.verify(jwt.serialize()))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("expired");
  }
}
