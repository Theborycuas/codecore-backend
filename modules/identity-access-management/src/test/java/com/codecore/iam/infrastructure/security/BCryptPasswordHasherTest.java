package com.codecore.iam.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void shouldHashAndMatchPassword() {
        String raw = "ValidPass1!";

        String hash = hasher.hash(raw);

        assertThat(hash).isNotBlank();
        assertThat(hash).isNotEqualTo(raw);
        assertThat(hasher.matches(raw, hash)).isTrue();
        assertThat(hasher.matches("WrongPass1!", hash)).isFalse();
    }

    @Test
    void shouldProduceDifferentHashesForSamePassword() {
        String raw = "ValidPass1!";

        String first = hasher.hash(raw);
        String second = hasher.hash(raw);

        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches(raw, first)).isTrue();
        assertThat(hasher.matches(raw, second)).isTrue();
    }
}
