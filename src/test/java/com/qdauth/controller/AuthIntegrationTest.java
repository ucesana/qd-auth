package com.qdauth.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.qdauth.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;

class AuthIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void registerTestUser() throws Exception {
    mockMvc.perform(
                    post("/api/accounts/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                    {"email":"auth@example.com","password":"password123"}
                    """))
            .andExpect(status().isCreated());
  }

  private String loginAndGetToken(String field) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
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

  @Test
  void login_returns200WithTokenPair() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"auth@example.com","password":"password123"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900))
        .andExpect(cookiesValid());
  }

  @Test
  void login_returns401OnWrongPassword() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"auth@example.com","password":"wrongpassword"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials."));
  }

  @Test
  void login_returns401OnUnknownEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"ghost@example.com","password":"password123"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_returnsNewTokenPair() throws Exception {
    String refreshTokenRequest = loginAndGetToken("refreshToken");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                        {"refreshToken":"%s"}
                        """,
                        refreshTokenRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(cookiesValid());
  }

  @Test
  void refresh_returns401OnTokenReuse() throws Exception {
    String refreshTokenRequest = loginAndGetToken("refreshToken");

    String body =
        String.format(
            """
            {"refreshToken":"%s"}
            """,
            refreshTokenRequest);

    // First use — valid
    mockMvc
        .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(cookiesValid());

    // Second use — reuse detected
    mockMvc
        .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.message").value("Refresh token reuse detected. All sessions revoked."));
  }

  @Test
  void refresh_returns401OnRevokedToken() throws Exception {
    String refreshTokenRequest = loginAndGetToken("refreshToken");

    // Use the token once to get a new pair — original is now consumed
    String newTokensResponse =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            """
                            {"refreshToken":"%s"}
                            """,
                            refreshTokenRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Attempt reuse of the original — family should be revoked
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                        {"refreshToken":"%s"}
                        """,
                        refreshTokenRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_returns401WhenAccessTokenPresentedAsRefresh() throws Exception {
    String accessToken = loginAndGetToken("accessToken");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                        {"refreshToken":"%s"}
                        """,
                        accessToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid token type."));
  }

  @Test
  void logout_returns200() throws Exception {
    String refreshTokenRequest = loginAndGetToken("refreshToken");

    ResultActions result = mockMvc
            .perform(
                    post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    String.format(
                                            """
                                            {"refreshToken":"%s"}
                                            """,
                                            refreshTokenRequest)))
            .andExpect(status().isNoContent())
            .andExpect(cookiesInvalidated());
  }

  private static void assertInvalidatedCookie(
          String name,
          MvcResult result
  ) throws Exception {
    cookie().exists(name).match(result);
    cookie().value(name, "").match(result);
    cookie().httpOnly(name, true).match(result);
    cookie().secure(name, true).match(result);
    cookie().path(name, "/").match(result);
    cookie().maxAge(name, 0).match(result);
  }
  
  private static void assertValidAccessTokenCookie(MvcResult result) throws Exception {
    cookie().exists("access_token").match(result);
    cookie().value("access_token", not(blankOrNullString())).match(result);
    cookie().httpOnly("access_token", true).match(result);
    cookie().secure("access_token", true).match(result);
    cookie().path("access_token", "/").match(result);
    cookie().maxAge("access_token", 900).match(result);
  }

  private static void assertValidRefreshTokenCookie(MvcResult result) throws Exception {
    cookie().exists("refresh_token").match(result);
    cookie().value("refresh_token", not(blankOrNullString())).match(result);
    cookie().httpOnly("refresh_token", true).match(result);
    cookie().secure("refresh_token", true).match(result);
    cookie().path("refresh_token", "/api/auth").match(result);
    cookie().maxAge("refresh_token", 7 * 24 * 60 * 60).match(result);
  }

  public static ResultMatcher cookiesValid() {
    return result -> {
      assertValidAccessTokenCookie(result);
      assertValidRefreshTokenCookie(result);
    };
  }

  public static ResultMatcher cookiesInvalidated() {
    return result -> {
      assertInvalidatedCookie("access_token", result);
      assertInvalidatedCookie("refresh_token", result);
    };
  }
}
