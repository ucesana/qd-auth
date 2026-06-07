package com.qdauth.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.qdauth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String header = request.getHeader("Authorization");

    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String token = header.substring(7);

    try {
      final JWTClaimsSet claims = jwtService.verify(token);

      final String type = (String) claims.getClaim("type");
      if (!"access".equals(type)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token type.");
        return;
      }

      final List<String> roles = claims.getStringListClaim("roles");
      final List<SimpleGrantedAuthority> authorities =
          roles == null
              ? List.of()
              : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

      // Principal is set to claims.getSubject(), which is the user's UUID
      final UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (final Exception ex) {
      SecurityContextHolder.clearContext();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token.");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
