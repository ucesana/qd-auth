package com.qdauth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.qdauth.api.account.entity.Account;
import com.qdauth.api.account.repository.AccountRepository;
import com.qdauth.api.account.service.AccountService;
import com.qdauth.api.auth.dto.LoginRequest;
import com.qdauth.api.auth.dto.TokensResponse;
import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import com.qdauth.api.auth.repository.UserRepository;
import com.qdauth.api.auth.service.AuthService;
import com.qdauth.api.channel.repository.ChannelRepository;
import com.qdauth.api.channel.repository.ChannelSubscriptionRepository;
import com.qdauth.api.channel.service.ChannelService;
import com.qdauth.api.stream.repository.LiveStreamChatRepository;
import com.qdauth.api.stream.repository.LiveStreamRepository;
import com.qdauth.api.user.dto.UserCreateRequest;
import com.qdauth.api.user.dto.UserResponse;
import com.qdauth.api.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseControllerTest {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected UserService userService;
  @Autowired protected AuthService authService;
  @Autowired protected AccountService accountService;

  protected String deviceId = "deviceId";
  protected String deviceName = "deviceName";

  @SuppressWarnings("resource")
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.3")
          .withDatabaseName("qdauth")
          .withUsername("qdauth")
          .withPassword("qdauthpassword")
          .withInitScript("db/init/schema.sql")
          .waitingFor(
              Wait.forSuccessfulCommand("mysqladmin ping -h localhost -u qdauth -pqdauthpassword"));

  static {
    mysql.start();
  }

  @Autowired private ChannelService channelService;
  @Autowired private ChannelRepository channelRepository;
  @Autowired private ChannelSubscriptionRepository channelSubscriptionRepository;
  @Autowired private LiveStreamRepository liveStreamRepository;
  @Autowired private LiveStreamChatRepository liveStreamChatRepository;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("SPRING_DATASOURCE_USERNAME", mysql::getUsername);
    registry.add("SPRING_DATASOURCE_PASSWORD", mysql::getPassword);
  }

  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private AccountRepository accountRepository;

  @BeforeEach
  void resetDatabase() {
    liveStreamChatRepository.deleteAll();
    liveStreamRepository.deleteAll();
    channelSubscriptionRepository.deleteAll();
    channelRepository.deleteAll();
    accountRepository.deleteAll();
    sessionRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected jakarta.servlet.http.Cookie extractCookie(
      org.springframework.mock.web.MockHttpServletResponse response, String cookieName) {
    return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
        .filter(h -> h.startsWith(cookieName + "="))
        .map(h -> h.split(";")[0].split("=", 2))
        .map(parts -> new jakarta.servlet.http.Cookie(parts[0], parts[1]))
        .findFirst()
        .orElseThrow(() -> new AssertionError(cookieName + " cookie not found"));
  }

  protected String loginAndGetToken(String field) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Device-Id", deviceId)
                    .header("User-Agent", deviceName)
                    .content(
                        """
                        {"email":"auth@example.com","password":"password123"}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return com.jayway.jsonpath.JsonPath.read(response, "$." + field);
  }

  protected TestUser createUserAndLogin(String email) throws Exception {
    UserResponse user = userService.register(new UserCreateRequest(email, "password123"));
    TokensResponse tokenPair =
        authService.login(new LoginRequest(email, "password123"), deviceId, deviceName);
    Cookie cookie = new Cookie("access_token", tokenPair.accessToken());
    return new TestUser(user, cookie);
  }

  protected TestUserAccount createUserAndAccountAndLogin(String email) throws Exception {
    UserResponse user = userService.register(new UserCreateRequest(email, "password123"));
    Account account = accountService.createAccount(user.id(), "Test Account");
    TokensResponse tokenPair =
        authService.login(new LoginRequest(email, "password123"), deviceId, deviceName);
    Cookie cookie = new Cookie("access_token", tokenPair.accessToken());
    return new TestUserAccount(user, account, cookie);
  }
}
