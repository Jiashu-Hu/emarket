package com.shopping.emarket.account.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "emarket.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        String keyId,
        String privateKeyPath,
        String publicKeyPath,
        Duration ttl
) {}
