package com.shopping.emarket.account.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.shopping.emarket.account.security.JwtKeyProvider;
import com.shopping.emarket.account.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setup() {
        JwtProperties props = new JwtProperties(
                "http://account-service:8081",
                "emarket",
                "emarket-dev-1",
                "classpath:dev-keys/private_key.pem",
                "classpath:dev-keys/public_key.pem",
                Duration.ofHours(1));
        JwtKeyProvider provider = new JwtKeyProvider(props, new DefaultResourceLoader());
        service = new JwtService(provider, props);
    }

    @Test
    void signThenParseRoundtripPreservesClaims() throws Exception {
        String userId = UUID.randomUUID().toString();
        JwtService.SignedToken signed = service.sign(userId, Map.of("email", "a@b.c"));

        JWTClaimsSet claims = service.parse(signed.token());

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.getIssuer()).isEqualTo("http://account-service:8081");
        assertThat(claims.getAudience()).containsExactly("emarket");
        assertThat(claims.getStringClaim("email")).isEqualTo("a@b.c");
        assertThat(claims.getExpirationTime()).isAfter(claims.getIssueTime());
    }

    @Test
    void parseRejectsTamperedToken() {
        JwtService.SignedToken signed = service.sign("anyone", Map.of());
        String tampered = signed.token().substring(0, signed.token().length() - 4) + "AAAA";
        assertThatThrownBy(() -> service.parse(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jwksExposesOnlyPublicMaterial() {
        var jwks = service.jwks().toJSONObject();
        var keys = (java.util.List<Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        Map<String, Object> key = keys.get(0);
        assertThat(key).containsKey("n").containsKey("e").containsEntry("kid", "emarket-dev-1");
        assertThat(key).doesNotContainKeys("d", "p", "q", "dp", "dq", "qi");
    }
}
