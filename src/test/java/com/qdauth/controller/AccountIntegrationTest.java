package com.qdauth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.qdauth.BaseIntegrationTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Transactional
class AccountIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void register_returns201WithAccountResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"integration@example.com","password":"password123"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("integration@example.com"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void register_returns409OnDuplicateEmail() throws Exception {
    String body =
        """
        {"email":"duplicate@example.com","password":"password123"}
        """;

    mockMvc
        .perform(
            post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered."));
  }

  @Test
  void register_returns400OnInvalidEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"not-an-email","password":"password123"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").isNotEmpty());
  }

  @Test
  void register_returns400OnShortPassword() throws Exception {
    mockMvc
        .perform(
            post("/api/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"valid@example.com","password":"short"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").isNotEmpty());
  }

  @Test
  void getMe_returns200WithValidToken() throws Exception {
    // Register
    mockMvc
        .perform(
            post("/api/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"me@example.com","password":"password123"}
                    """))
        .andExpect(status().isCreated());

    // Login
    String loginResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"me@example.com","password":"password123"}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");

    // Access protected endpoint
    mockMvc
        .perform(get("/api/accounts/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("me@example.com"));
  }

  @Test
  void getMe_returns401WithoutToken() throws Exception {
    mockMvc.perform(get("/api/accounts/me")).andExpect(status().isUnauthorized());
  }
}
