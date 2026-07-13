package com.codecore.iam.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256TokenHasherTest {

    @Test
    void shouldGenerateOpaqueHexToken() {
        String token = Sha256TokenHasher.generateRawToken();
        assertThat(token).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void shouldHashDeterministically() {
        String raw = "test-token-value";
        assertThat(Sha256TokenHasher.hash(raw)).isEqualTo(Sha256TokenHasher.hash(raw));
        assertThat(Sha256TokenHasher.hash(raw)).hasSize(64);
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(() -> Sha256TokenHasher.hash("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
