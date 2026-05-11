package com.shopping.emarket.account.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtKeyProvider {

    private final RSAKey rsaKey;

    public JwtKeyProvider(JwtProperties props, ResourceLoader resourceLoader) {
        RSAPublicKey pub = readPublicKey(resourceLoader, props.publicKeyPath());
        RSAPrivateKey priv = readPrivateKey(resourceLoader, props.privateKeyPath());
        this.rsaKey = new RSAKey.Builder(pub).privateKey(priv).keyID(props.keyId()).build();
    }

    public RSAKey rsaKey() {
        return rsaKey;
    }

    private static RSAPublicKey readPublicKey(ResourceLoader loader, String path) {
        byte[] der = readPemBody(loader, path, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key at " + path, e);
        }
    }

    private static RSAPrivateKey readPrivateKey(ResourceLoader loader, String path) {
        byte[] der = readPemBody(loader, path, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA private key at " + path, e);
        }
    }

    private static byte[] readPemBody(ResourceLoader loader, String path, String expectedLabel) {
        try (InputStream in = loader.getResource(path).getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String body = pem.replaceAll("-----BEGIN " + expectedLabel + "-----", "")
                    .replaceAll("-----END " + expectedLabel + "-----", "")
                    .replaceAll("\\s+", "");
            return Base64.getDecoder().decode(body);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read PEM at " + path, e);
        }
    }
}
