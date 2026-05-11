package com.shopping.emarket.account.web;

import com.shopping.emarket.account.dto.TokenRequest;
import com.shopping.emarket.account.dto.TokenResponse;
import com.shopping.emarket.account.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest req) {
        return auth.issueToken(req.email(), req.password());
    }
}
