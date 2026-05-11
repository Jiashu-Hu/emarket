package com.shopping.emarket.account.service;

import com.shopping.emarket.account.domain.User;
import com.shopping.emarket.account.repo.UserRepository;
import com.shopping.emarket.account.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository repo;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AuthService service;

    @BeforeEach
    void setup() {
        repo = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        jwt = mock(JwtService.class);
        JwtProperties props = new JwtProperties("issuer", "aud", "kid", "", "", Duration.ofHours(1));
        service = new AuthService(repo, encoder, jwt, props);
    }

    @Test
    void validCredentialsReturnBearerToken() {
        User u = new User(UUID.randomUUID(), "a@b.c", "alice", encoder.encode("Passw0rd!"));
        when(repo.findByEmail("a@b.c")).thenReturn(Optional.of(u));
        when(jwt.sign(anyString(), any())).thenReturn(
                new JwtService.SignedToken("jwt-token", Instant.now().plusSeconds(3600)));

        var resp = service.issueToken("a@b.c", "Passw0rd!");

        assertThat(resp.accessToken()).isEqualTo("jwt-token");
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        assertThat(resp.expiresIn()).isGreaterThan(0);
    }

    @Test
    void unknownEmailThrowsBadCredentials() {
        when(repo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueToken("missing@example.com", "anything"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void wrongPasswordThrowsBadCredentials() {
        User u = new User(UUID.randomUUID(), "a@b.c", "alice", encoder.encode("Passw0rd!"));
        when(repo.findByEmail("a@b.c")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.issueToken("a@b.c", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
