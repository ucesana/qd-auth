// src/main/java/com/qdauth/config/KeyConfig.java
package com.qdauth.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class KeyConfig {

  @Value("${RSA_PRIVATE_KEY:}")
  private String privateKeyEnv;

  @Value("${RSA_PUBLIC_KEY:}")
  private String publicKeyEnv;

  @Bean
  public RSAPrivateKey rsaPrivateKey() throws Exception {
    if (!privateKeyEnv.isBlank()) {
      final byte[] decoded = Base64.getDecoder().decode(privateKeyEnv.replaceAll("\\s", ""));
      final String pem = new String(decoded);
      return parsePrivateKey(pem);
    }
    final String pem = readClasspath("keys/private.pem");
    return parsePrivateKey(pem);
  }

  @Bean
  public RSAPublicKey rsaPublicKey() throws Exception {
    if (!publicKeyEnv.isBlank()) {
      final byte[] decoded = Base64.getDecoder().decode(publicKeyEnv.replaceAll("\\s", ""));
      final String pem = new String(decoded);
      return parsePublicKey(pem);
    }
    final String pem = readClasspath("keys/public.pem");
    return parsePublicKey(pem);
  }

  private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
    final String stripped =
        pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    final byte[] decoded = Base64.getDecoder().decode(stripped);
    final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
    return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  private RSAPublicKey parsePublicKey(String pem) throws Exception {
    final String stripped =
        pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
    final byte[] decoded = Base64.getDecoder().decode(stripped);
    final X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
  }

  private String readClasspath(String path) throws Exception {
    return new String(new ClassPathResource(path).getInputStream().readAllBytes());
  }

  @Bean
  public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey) {
    return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
  }
}
