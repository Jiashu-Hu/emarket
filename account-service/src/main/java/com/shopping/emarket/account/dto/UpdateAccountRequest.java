package com.shopping.emarket.account.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(min = 3, max = 100) String username,
        @Valid AddressDto shippingAddress,
        @Valid AddressDto billingAddress
) {}
