package com.qdauth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String rsaPrivateKey, String rsaPublicKey) {}
