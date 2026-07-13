package com.codecore.iam.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 hashing for opaque reset tokens. Store only the hex digest; never the raw token.
 */
public final class Sha256TokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RAW_TOKEN_BYTES = 32;

    private Sha256TokenHasher() {
    }

    public static String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hash(String rawToken) {
        Objects.requireNonNull(rawToken, "rawToken");
        if (rawToken.isBlank()) {
            throw new IllegalArgumentException("Raw token must not be blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
