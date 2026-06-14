// src/test/java/com/qdauth/BaseIntegrationTest.java

package com.qdauth;

import com.qdauth.repository.RefreshTokenRepository;
import com.qdauth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

  @SuppressWarnings("resource")
  static final MySQLContainer<?> mysql =
          new MySQLContainer<>("mysql:8.3")
                  .withDatabaseName("qdauth")
                  .withUsername("qdauth")
                  .withPassword("qdauthpassword")
                  .withInitScript("db/init/schema.sql")
                  .waitingFor(
                          Wait.forSuccessfulCommand(
                                  "mysqladmin ping -h localhost -u qdauth -pqdauthpassword"));

  static {
    mysql.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("SPRING_DATASOURCE_USERNAME", mysql::getUsername);
    registry.add("SPRING_DATASOURCE_PASSWORD", mysql::getPassword);
  }

  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void resetDatabase() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected jakarta.servlet.http.Cookie extractCookie(
          org.springframework.mock.web.MockHttpServletResponse response,
          String cookieName) {
    return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
            .filter(h -> h.startsWith(cookieName + "="))
            .map(h -> h.split(";")[0].split("=", 2))
            .map(parts -> new jakarta.servlet.http.Cookie(parts[0], parts[1]))
            .findFirst()
            .orElseThrow(() -> new AssertionError(cookieName + " cookie not found"));
  }
}