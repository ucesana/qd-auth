package com.qdauth.config;

import com.qdauth.components.CookieBearerTokenResolver;
import com.qdauth.security.QdJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.filters.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  public static final String CLIENT_HOST = "http://localhost:5173";

  private CookieBearerTokenResolver cookieBearerTokenResolver;

  public SecurityConfig(CookieBearerTokenResolver cookieBearerTokenResolver) {
    this.cookieBearerTokenResolver = cookieBearerTokenResolver;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
                session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(
            auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/api/accounts/register",
                        "/api/auth/login",
                        "/actuator/health").permitAll()
                    .requestMatchers(
                            "/api/auth/refresh",
                            "/api/auth/logout",
                            "/api/streams/live/**").authenticated()
                    .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(cookieBearerTokenResolver)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new QdJwtAuthenticationConverter())))

        .exceptionHandling(
                ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> response
                                        .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
        );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOriginPatterns(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(Duration.ofMinutes(5L));
    return new UrlBasedCorsConfigurationSource() {{
      registerCorsConfiguration("/**", config);
    }};
  }
}
