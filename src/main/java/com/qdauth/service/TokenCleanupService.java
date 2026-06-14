package com.qdauth.service;

import com.qdauth.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Runs at 03:00 daily. Cron expression: second minute hour day month weekday.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpired(LocalDateTime.now());
    }
}