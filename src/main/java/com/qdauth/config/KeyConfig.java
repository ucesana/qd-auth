package com.qdauth.config;

import com.qdauth.properties.JwtProperties;
import io.micrometer.common.util.StringUtils;
import jakarta.el.PropertyNotFoundException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class KeyConfig {

  private final JwtProperties jwtProperties;

  public KeyConfig(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Get the RSA private key used to sign JWT tokens. **Warning**: never commit keys to version
   * control!
   *
   * @return the RSA private key from Spring properties if it exists, otherwise attempts to return the key
   *     located in the `keys/` directory.
   * @throws Exception if decoding the key fails, an error occurs while reading the key, or the key does not exist.
   */
  @Bean
  public RSAPrivateKey rsaPrivateKey() throws Exception {
    final String privateKeyProperty = this.jwtProperties.rsaPrivateKey();
    if (!StringUtils.isBlank(privateKeyProperty)) {
      final byte[] decoded = Base64.getDecoder().decode(privateKeyProperty.replaceAll("\\s", ""));
      final String pem = new String(decoded);
      return parsePrivateKey(pem);
    }
    final String pem = readClasspath("keys/private.pem");
    if (StringUtils.isBlank(pem)) {
      throw new PropertyNotFoundException("Spring property JWT_RSA_PRIVATE_KEY not found.");
    }
    return parsePrivateKey(pem);
  }

  /**
   * Get the RSA public key used to verify JWT tokens. **Warning**: never commit keys to version
   * control!
   *
   * @return the RSA public key from Spring property file if it exists, otherwise attempts to return the key
   *     located in the `keys/` directory.
   * @throws Exception if decoding the key fails, an error occurs while reading the key, or the key does not exist.
   */
  @Bean
  public RSAPublicKey rsaPublicKey() throws Exception {
    final String publicKeyProperty = this.jwtProperties.rsaPublicKey();
    if (!StringUtils.isBlank(publicKeyProperty)) {
      final byte[] decoded = Base64.getDecoder().decode(publicKeyProperty.replaceAll("\\s", ""));
      final String pem = new String(decoded);
      return parsePublicKey(pem);
    }
    final String pem = readClasspath("keys/public.pem");
    if (StringUtils.isBlank(pem)) {
      throw new PropertyNotFoundException("Spring property JWT_RSA_PUBLIC_KEY not found.");
    }
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
