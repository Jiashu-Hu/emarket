package com.shopping.emarket.account.service;

import com.shopping.emarket.account.domain.User;
import com.shopping.emarket.account.dto.TokenResponse;
import com.shopping.emarket.account.repo.UserRepository;
import com.shopping.emarket.account.security.JwtProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final JwtProperties jwtProps;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, JwtProperties jwtProps) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
    }

    public TokenResponse issueToken(String email, String rawPassword) {
        Optional<User> maybe = users.findByEmail(email);
        if (maybe.isEmpty() || !encoder.matches(rawPassword, maybe.get().getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = maybe.get();
        JwtService.SignedToken signed = jwt.sign(user.getId().toString(), Map.of("email", user.getEmail()));
        long expiresIn = signed.expiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        return new TokenResponse(signed.token(), "Bearer", expiresIn);
    }
}
