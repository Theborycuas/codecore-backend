package com.codecore.audit.contract.reference;

import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class AuditReferencePortContractTest {

    @Test
    void contractSurfaceShouldAcceptDomainIds() {
        AuditReferencePort port = (id, tenantId) -> Mono.just(true);
        assertThat(port.existsByIdAndTenant(AuditEntryId.generate(), TenantId.generate()).block()).isTrue();
    }
}
