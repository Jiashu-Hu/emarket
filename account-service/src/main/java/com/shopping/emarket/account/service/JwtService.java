package com.shopping.emarket.account.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.shopping.emarket.account.security.JwtKeyProvider;
import com.shopping.emarket.account.security.JwtProperties;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final JwtKeyProvider keyProvider;
    private final JwtProperties props;

    public JwtService(JwtKeyProvider keyProvider, JwtProperties props) {
        this.keyProvider = keyProvider;
        this.props = props;
    }

    public SignedToken sign(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.ttl());
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(props.issuer())
                .audience(props.audience())
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp));
        extraClaims.forEach(builder::claim);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keyProvider.rsaKey().getKeyID())
                .build();
        SignedJWT jwt = new SignedJWT(header, builder.build());
        try {
            jwt.sign(new RSASSASigner(keyProvider.rsaKey().toPrivateKey()));
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
        return new SignedToken(jwt.serialize(), exp);
    }

    public JWTClaimsSet parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new RSASSAVerifier(keyProvider.rsaKey().toRSAPublicKey()))) {
                throw new IllegalArgumentException("JWT signature invalid");
            }
            return jwt.getJWTClaimsSet();
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("JWT parse/verify failed", e);
        }
    }

    public JWKSet jwks() {
        return new JWKSet(List.of(keyProvider.rsaKey().toPublicJWK()));
    }

    public record SignedToken(String token, Instant expiresAt) {}
}
