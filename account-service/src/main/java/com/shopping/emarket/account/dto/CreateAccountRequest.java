package com.shopping.emarket.account.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Valid AddressDto shippingAddress,
        @Valid AddressDto billingAddress
) {}
