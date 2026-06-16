package com.qdauth.api.auth.service;

import com.qdauth.api.auth.repository.RefreshTokenRepository;
import com.qdauth.api.auth.repository.SessionRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenCleanupService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final SessionRepository sessionRepository;

  public TokenCleanupService(
      RefreshTokenRepository refreshTokenRepository, SessionRepository sessionRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.sessionRepository = sessionRepository;
  }

  // Runs at 03:00 daily. Cron expression: second minute hour day month weekday.
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void deleteExpiredTokens() {
    LocalDateTime now = LocalDateTime.now();
    refreshTokenRepository.deleteExpired(now);
    sessionRepository.deleteOrphaned(now);
  }
}
