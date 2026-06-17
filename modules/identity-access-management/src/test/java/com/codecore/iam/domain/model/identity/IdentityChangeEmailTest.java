package com.codecore.iam.domain.model.identity;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityChangeEmailTest {

    @Test
    void shouldChangeEmailWhenActive() {
        Identity identity = identity(IdentityStatus.ACTIVE);

        identity.changeEmail(EmailAddress.of("new@codecore.local"));

        assertThat(identity.email().value()).isEqualTo("new@codecore.local");
    }

    @Test
    void shouldRejectEmailChangeWhenLocked() {
        Identity identity = identity(IdentityStatus.LOCKED);

        assertThatThrownBy(() -> identity.changeEmail(EmailAddress.of("new@codecore.local")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void shouldRejectEmailChangeWhenDisabled() {
        Identity identity = identity(IdentityStatus.DISABLED);

        assertThatThrownBy(() -> identity.changeEmail(EmailAddress.of("new@codecore.local")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    private static Identity identity(IdentityStatus status) {
        Instant now = Instant.now();
        return new Identity(
                IdentityId.generate(),
                TenantId.generate(),
                EmailAddress.of("old@codecore.local"),
                status,
                null,
                null,
                now,
                now,
                0L
        );
    }
}
