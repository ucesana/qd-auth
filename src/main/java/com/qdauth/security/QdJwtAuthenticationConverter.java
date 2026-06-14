package com.qdauth.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QdJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        final String type = jwt.getClaimAsString("type");
        if (!"access".equals(type)) {
            throw new InvalidBearerTokenException("Invalid token type.");
        }

        final List<String> roles = jwt.getClaimAsStringList("roles");
        final List<SimpleGrantedAuthority> authorities = roles == null
                ? Collections.emptyList()
                : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);
    }
}