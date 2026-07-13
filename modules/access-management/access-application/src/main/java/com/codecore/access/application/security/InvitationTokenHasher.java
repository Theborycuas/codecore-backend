package com.codecore.access.application.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 hashing for opaque invitation tokens. Store only the hex digest; never the raw token.
 * Local Access copy of the IAM token hasher (avoids coupling application to IAM infrastructure).
 */
public final class InvitationTokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RAW_TOKEN_BYTES = 32;

    private InvitationTokenHasher() {
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
