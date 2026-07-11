package com.qdauth.api.account.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qdauth.BaseControllerTest;
import com.qdauth.TestUser;
import com.qdauth.TestUserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AccountControllerTest extends BaseControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void createMyAccount_returnsAccount() throws Exception {
    TestUser user = createUserAndLogin("accounts@example.com");

    mockMvc
        .perform(
            post("/api/accounts")
                .cookie(user.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"username"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(user.user().id()))
        .andExpect(jsonPath("$.name").value("username"))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void getMyAccounts_returnsAuthenticatedUsersAccounts() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("accounts@example.com");

    mockMvc
        .perform(get("/api/accounts/me").cookie(user.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(user.account().getId()))
        .andExpect(jsonPath("$[0].userId").value(user.user().id()))
        .andExpect(jsonPath("$[0].name").value("Test Account"));
  }

  @Test
  void getMyAccounts_returns401WithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/accounts/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void getAccount_returnsAccountForOwner() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("owner@example.com");

    mockMvc
        .perform(get("/api/accounts/{id}", user.account().getId()).cookie(user.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.account().getId()))
        .andExpect(jsonPath("$.userId").value(user.user().id()))
        .andExpect(jsonPath("$.name").value("Test Account"));
  }

  @Test
  void getAccount_returns403ForAnotherUsersAccount() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount other = createUserAndAccountAndLogin("other@example.com");

    mockMvc
        .perform(get("/api/accounts/{id}", owner.account().getId()).cookie(other.cookie()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAccount_returns404ForUnknownAccount() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("owner@example.com");

    mockMvc
        .perform(get("/api/accounts/does-not-exist").cookie(user.cookie()))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateName_updatesAccount() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("update@example.com");

    mockMvc
        .perform(
            patch("/api/accounts/{id}", user.account().getId())
                .cookie(user.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Updated Account"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Account"));

    mockMvc
        .perform(get("/api/accounts/{id}", user.account().getId()).cookie(user.cookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Account"));
  }

  @Test
  void updateName_returns403ForAnotherUsersAccount() throws Exception {
    TestUserAccount owner = createUserAndAccountAndLogin("owner@example.com");
    TestUserAccount other = createUserAndAccountAndLogin("other@example.com");

    mockMvc
        .perform(
            patch("/api/accounts/{id}", owner.account().getId())
                .cookie(other.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Hacked"}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateName_returns404ForUnknownAccount() throws Exception {
    TestUserAccount user = createUserAndAccountAndLogin("owner@example.com");

    mockMvc
        .perform(
            patch("/api/accounts/does-not-exist")
                .cookie(user.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Updated"}
                    """))
        .andExpect(status().isNotFound());
  }
}
