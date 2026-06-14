package com.qdauth.components;

import com.qdauth.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import java.util.Arrays;

/**
 * Resolves the Bearer token from the "access_token" HttpOnly cookie.
 *
 * BearerTokenResolver is the extension point Spring Security provides for
 * changing where the JWT is extracted from. The default implementation reads
 * the Authorization header; this implementation reads a named cookie instead.
 */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> JwtService.ACCESS_TOKEN.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
