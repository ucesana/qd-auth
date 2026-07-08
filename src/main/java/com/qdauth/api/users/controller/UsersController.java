package com.qdauth.api.users.controller;

import com.qdauth.api.auth.security.QdPrincipal;
import com.qdauth.api.users.dto.SessionResponse;
import com.qdauth.api.users.dto.UserCreateRequest;
import com.qdauth.api.users.dto.UserResponse;
import com.qdauth.api.users.service.UsersService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UsersController {

  private final UsersService usersService;

  public UsersController(UsersService usersService) {
    this.usersService = usersService;
  }

  @RequestMapping(
      path = "/create",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    return usersService.register(userCreateRequest);
  }

  @GetMapping("/me")
  public UserResponse getUser(@AuthenticationPrincipal QdPrincipal principal) {
    return usersService.getUser(principal.userId());
  }

  @GetMapping("/me/sessions")
  public List<SessionResponse> getSessions(@AuthenticationPrincipal QdPrincipal principal) {
    return usersService.getSessions(principal.userId());
  }

  @GetMapping("/me/sessions/current")
  public SessionResponse getCurrentSession(@AuthenticationPrincipal QdPrincipal principal) {
    return usersService.getCurrentSession(principal.userId(), principal.familyId());
  }

  @DeleteMapping("/me/sessions/{familyId}")
  public ResponseEntity<Void> revokeSession(
      @AuthenticationPrincipal QdPrincipal principal, @PathVariable String familyId) {
    usersService.revokeSession(principal.userId(), familyId);
    return ResponseEntity.noContent().build();
  }
}
