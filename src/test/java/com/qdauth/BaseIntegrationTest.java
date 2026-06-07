// src/test/java/com/qdauth/BaseIntegrationTest.java
package com.qdauth;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

  @SuppressWarnings("resource")
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.3")
          .withDatabaseName("qdauth")
          .withUsername("qdauth")
          .withPassword("qdauthpassword")
          .withInitScript("db/init/01_schema.sql")
          .waitingFor(
              Wait.forSuccessfulCommand("mysqladmin ping -h localhost -u qdauth -pqdauthpassword"));

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
}
