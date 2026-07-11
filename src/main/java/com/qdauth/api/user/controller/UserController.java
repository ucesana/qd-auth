package com.qdauth.api.user.controller;

import com.qdauth.api.auth.security.QdPrincipal;
import com.qdauth.api.user.dto.SessionResponse;
import com.qdauth.api.user.dto.UserCreateRequest;
import com.qdauth.api.user.dto.UserResponse;
import com.qdauth.api.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    return userService.register(userCreateRequest);
  }

  @GetMapping("/me")
  public UserResponse getUser(@AuthenticationPrincipal QdPrincipal principal) {
    return userService.getUser(principal.userId());
  }

  @GetMapping("/me/sessions")
  public List<SessionResponse> getSessions(@AuthenticationPrincipal QdPrincipal principal) {
    return userService.getSessions(principal.userId());
  }

  @GetMapping("/me/sessions/current")
  public SessionResponse getCurrentSession(@AuthenticationPrincipal QdPrincipal principal) {
    return userService.getCurrentSession(principal.userId(), principal.familyId());
  }

  @DeleteMapping("/me/sessions/{familyId}")
  public ResponseEntity<Void> revokeSession(
      @AuthenticationPrincipal QdPrincipal principal, @PathVariable String familyId) {
    userService.revokeSession(principal.userId(), familyId);
    return ResponseEntity.noContent().build();
  }
}
