package com.codecore.audit.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditPermissionCatalogTest {

    @Test
    void shouldExposeOnlyAuditRead() {
        assertThat(AuditPermissionCatalog.AUDIT_READ).isEqualTo("audit:read");
        assertThat(AuditPermissionCatalog.ALL).containsExactly(AuditPermissionCatalog.AUDIT_READ);
        assertThat(AuditPermissionCatalog.AUDIT_READ_ONLY).isEqualTo(AuditPermissionCatalog.ALL);
    }

    @Test
    void shouldNotExposeWriteOrDeletePermissions() {
        assertThat(AuditPermissionCatalog.ALL)
                .doesNotContain("audit:create", "audit:update", "audit:delete", "audit:append");
    }
}
