package com.shopping.emarket.account.dto;

public record AddressDto(
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String country
) {}
