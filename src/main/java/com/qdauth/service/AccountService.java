package com.qdauth.service;

import com.qdauth.dto.AccountResponse;
import com.qdauth.dto.RegisterRequest;
import com.qdauth.model.User;
import com.qdauth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AccountResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already registered.");
    }

    final User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    userRepository.save(user);

    return new AccountResponse(user.getId(), user.getEmail(), user.isEnabled());
  }

  public AccountResponse getAccount(String userId) {
    final User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("Account not found."));
    return new AccountResponse(user.getId(), user.getEmail(), user.isEnabled());
  }
}
