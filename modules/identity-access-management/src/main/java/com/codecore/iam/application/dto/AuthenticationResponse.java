package com.codecore.iam.application.dto;

/**
 * Successful authentication response including issued access token (no refresh token in PASO 11.0).
 */
public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
