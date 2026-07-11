package com.qdauth.api.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.qdauth.BaseControllerTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

class UsersControllerTest extends BaseControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void create_returns201WithUserResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
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
  void create_returns409OnDuplicateEmail() throws Exception {
    String body =
        """
        {"email":"duplicate@example.com","password":"password123"}
        """;

    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered."));
  }

  @Test
  void create_returns400OnInvalidEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"not-an-email","password":"password123"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").isNotEmpty());
  }

  @Test
  void create_returns400OnShortPassword() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
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
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                              {"email":"me@example.com","password":"password123"}
                            """))
        .andExpect(status().isCreated());

    // Login — capture the Set-Cookie header
    String setCookieHeader =
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
            .getHeader(HttpHeaders.SET_COOKIE);

    // Extract the cookie value from the Set-Cookie header
    // Header format: "access_token=<jwt>; Path=/; Max-Age=900; HttpOnly; Secure; SameSite=Strict"
    String accessTokenCookie =
        Arrays.stream(setCookieHeader.split(";"))
            .map(String::trim)
            .filter(part -> part.startsWith("access_token="))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("access_token cookie not found in Set-Cookie header"));

    String cookieName = accessTokenCookie.split("=", 2)[0];
    String cookieValue = accessTokenCookie.split("=", 2)[1];

    // Access protected endpoint — send the cookie as the browser would
    mockMvc
        .perform(
            get("/api/users/me").cookie(new jakarta.servlet.http.Cookie(cookieName, cookieValue)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("me@example.com"));
  }

  @Test
  void getMe_returns401WithoutToken() throws Exception {
    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void preflight_returnsCorsHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/users")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }
}
