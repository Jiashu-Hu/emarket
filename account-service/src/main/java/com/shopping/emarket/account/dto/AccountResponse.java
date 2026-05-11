package com.shopping.emarket.account.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        String username,
        AddressDto shippingAddress,
        AddressDto billingAddress,
        Instant createdAt,
        Instant updatedAt
) {}
