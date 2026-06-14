package com.qdauth.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  public static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900; // 15 minutes
  public static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;
  public static final long REFRESH_TOKEN_EXPIRY_SECONDS = REFRESH_TOKEN_EXPIRY_DAYS * 24 * 60 * 60;

  public static final String ACCESS_TOKEN = "access_token";
  public static final String REFRESH_TOKEN = "refresh_token";

  private final RSAPrivateKey privateKey;
  private final RSAPublicKey publicKey;

  public JwtService(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  public String issueAccessToken(String subject, List<String> roles) throws JOSEException {
    final Instant now = Instant.now();
    final JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .issuer("qd-auth")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(ACCESS_TOKEN_EXPIRY_SECONDS)))
            .jwtID(UUID.randomUUID().toString())
            .claim("roles", roles)
            .claim("type", "access")
            .build();

    return sign(claims);
  }

  public String issueRefreshToken(String subject, String tokenId) throws JOSEException {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .issuer("qd-auth")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS)))
            .jwtID(tokenId)
            .claim("type", "refresh")
            .build();

    return sign(claims);
  }

  public JWTClaimsSet verify(String token) throws Exception {
    final SignedJWT jwt = SignedJWT.parse(token);
    final JWSVerifier verifier = new RSASSAVerifier(publicKey);

    if (!jwt.verify(verifier)) {
      throw new SecurityException("Invalid JWT signature.");
    }

    final JWTClaimsSet claims = jwt.getJWTClaimsSet();

    if (claims.getExpirationTime().before(new Date())) {
      throw new SecurityException("JWT has expired.");
    }

    return claims;
  }

  private String sign(JWTClaimsSet claims) throws JOSEException {
    final SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("qd-auth-key").build(), claims);
    jwt.sign(new RSASSASigner(privateKey));
    return jwt.serialize();
  }
}
