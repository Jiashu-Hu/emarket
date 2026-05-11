package com.shopping.emarket.account.web;

import com.shopping.emarket.account.dto.AccountResponse;
import com.shopping.emarket.account.dto.CreateAccountRequest;
import com.shopping.emarket.account.dto.UpdateAccountRequest;
import com.shopping.emarket.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.register(req));
    }

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal Jwt principal) {
        return accounts.findById(UUID.fromString(principal.getSubject()));
    }

    @PutMapping("/me")
    public AccountResponse updateMe(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody UpdateAccountRequest req) {
        return accounts.update(UUID.fromString(principal.getSubject()), req);
    }
}
