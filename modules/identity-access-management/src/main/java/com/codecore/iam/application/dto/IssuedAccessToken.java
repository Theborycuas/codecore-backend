package com.codecore.iam.application.dto;

/**
 * Token material returned by {@link com.codecore.iam.application.port.out.TokenProvider}.
 */
public record IssuedAccessToken(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
