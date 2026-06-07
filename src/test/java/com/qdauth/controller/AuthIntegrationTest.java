package com.qdauth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.qdauth.BaseIntegrationTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Transactional
class AuthIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void registerTestUser() throws Exception {
    try {
      mockMvc.perform(
          post("/api/accounts/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  """
                  {"email":"auth@example.com","password":"password123"}
                  """));
    } catch (Exception ignored) {
      // User may already exist from a prior test in this run
    }
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
        .andExpect(jsonPath("$.expiresIn").value(900));
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
    String refreshToken = loginAndGetToken("refreshToken");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        """
                        {"refreshToken":"%s"}
                        """,
                        refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  @Test
  void refresh_returns401OnTokenReuse() throws Exception {
    String refreshToken = loginAndGetToken("refreshToken");

    String body =
        String.format(
            """
            {"refreshToken":"%s"}
            """,
            refreshToken);

    // First use — valid
    mockMvc
        .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    // Second use — reuse detected
    mockMvc
        .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.message").value("Refresh token reuse detected. All sessions revoked."));
  }

  @Test
  void refresh_returns401OnRevokedToken() throws Exception {
    String refreshToken = loginAndGetToken("refreshToken");

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
                            refreshToken)))
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
                        refreshToken)))
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
}
