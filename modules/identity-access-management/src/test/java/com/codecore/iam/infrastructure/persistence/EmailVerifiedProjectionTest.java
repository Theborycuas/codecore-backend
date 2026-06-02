package com.codecore.iam.infrastructure.persistence;

import com.codecore.iam.domain.valueobject.IdentityStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerifiedProjectionTest {

    @Test
    void shouldMatchStatusPendingVerification() {
        assertThat(EmailVerifiedProjection.fromStatus(IdentityStatus.PENDING_VERIFICATION)).isFalse();
        assertThat(EmailVerifiedProjection.fromStatus(IdentityStatus.ACTIVE)).isTrue();
        assertThat(EmailVerifiedProjection.fromStatus(IdentityStatus.LOCKED)).isTrue();
        assertThat(EmailVerifiedProjection.fromStatus(IdentityStatus.DISABLED)).isTrue();
    }
}
