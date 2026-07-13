package com.codecore.access.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationPermissionCatalogTest {

    @Test
    void shouldExposeThreeInvitationLifecyclePermissions() {
        assertThat(InvitationPermissionCatalog.ALL).hasSize(3);
        assertThat(InvitationPermissionCatalog.INVITATION_READ_ONLY)
                .containsExactly(InvitationPermissionCatalog.INVITATION_READ);
    }

    @Test
    void shouldUseCreateReadRevokeNotResendOrUpdate() {
        assertThat(InvitationPermissionCatalog.ALL)
                .contains(InvitationPermissionCatalog.INVITATION_CREATE)
                .contains(InvitationPermissionCatalog.INVITATION_READ)
                .contains(InvitationPermissionCatalog.INVITATION_REVOKE)
                .doesNotContain("invitation:resend")
                .doesNotContain("invitation:update")
                .doesNotContain("invitation:delete")
                .doesNotContain("invitation:accept")
                .doesNotContain("invitation:expire");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(InvitationPermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("retail")
                        || code.contains("subscription")
                        || code.contains("seat")
        );
    }
}
