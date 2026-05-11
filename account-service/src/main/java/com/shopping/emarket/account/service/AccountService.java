package com.shopping.emarket.account.service;

import com.shopping.emarket.account.domain.Address;
import com.shopping.emarket.account.domain.User;
import com.shopping.emarket.account.dto.AccountResponse;
import com.shopping.emarket.account.dto.AddressDto;
import com.shopping.emarket.account.dto.CreateAccountRequest;
import com.shopping.emarket.account.dto.UpdateAccountRequest;
import com.shopping.emarket.account.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public AccountResponse register(CreateAccountRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new DuplicateEmailException(req.email());
        }
        User user = new User(UUID.randomUUID(), req.email(), req.username(), encoder.encode(req.password()));
        user.updateProfile(null, toEntity(req.shippingAddress()), toEntity(req.billingAddress()));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(req.email());
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        return toResponse(users.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User " + id + " not found")));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " not found"));
    }

    @Transactional
    public AccountResponse update(UUID id, UpdateAccountRequest req) {
        User user = users.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User " + id + " not found"));
        user.updateProfile(req.username(), toEntity(req.shippingAddress()), toEntity(req.billingAddress()));
        return toResponse(user);
    }

    static Address toEntity(AddressDto dto) {
        if (dto == null) return null;
        return new Address(dto.line1(), dto.line2(), dto.city(), dto.region(), dto.postalCode(), dto.country());
    }

    static AddressDto toDto(Address a) {
        if (a == null) return null;
        return new AddressDto(a.getLine1(), a.getLine2(), a.getCity(), a.getRegion(), a.getPostalCode(), a.getCountry());
    }

    static AccountResponse toResponse(User u) {
        return new AccountResponse(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                toDto(u.getShippingAddress()),
                toDto(u.getBillingAddress()),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
