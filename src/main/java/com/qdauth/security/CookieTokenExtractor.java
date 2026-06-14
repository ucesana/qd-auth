package com.qdauth.security;

import com.qdauth.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;

public final class CookieTokenExtractor {

    public static final String NO_REFRESH_TOKEN = "No refresh token";

    private CookieTokenExtractor() {}

    public static String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, NO_REFRESH_TOKEN);
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> JwtService.REFRESH_TOKEN.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, NO_REFRESH_TOKEN));
    }
}