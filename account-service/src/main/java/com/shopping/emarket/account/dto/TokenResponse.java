package com.shopping.emarket.account.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
