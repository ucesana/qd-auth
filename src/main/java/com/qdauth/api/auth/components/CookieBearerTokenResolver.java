package com.qdauth.api.auth.components;

import com.qdauth.api.auth.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the Bearer token from the "access_token" HttpOnly cookie.
 *
 * <p>BearerTokenResolver is the extension point Spring Security provides for changing where the JWT
 * is extracted from. The default implementation reads the Authorization header; this implementation
 * reads a named cookie instead.
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
