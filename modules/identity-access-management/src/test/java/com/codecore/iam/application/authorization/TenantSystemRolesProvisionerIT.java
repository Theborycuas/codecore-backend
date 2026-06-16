package com.codecore.iam.application.authorization;

import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.configuration.IamAuthorizationConfiguration;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamAuthorizationPersistenceTestConfiguration;
import com.codecore.iam.testsupport.IamR2dbcTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
        IamAuthorizationPersistenceTestConfiguration.class,
        IamR2dbcTestConfiguration.class,
        IamAuthorizationConfiguration.class
})
class TenantSystemRolesProvisionerIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private TenantSystemRolesProvisioner tenantSystemRolesProvisioner;

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void shouldProvisionSystemRolesIdempotentlyForTenant() {
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(tenantSystemRolesProvisioner.provisionForTenant(tenantId))
                .verifyComplete();
        StepVerifier.create(tenantSystemRolesProvisioner.provisionForTenant(tenantId))
                .verifyComplete();

        StepVerifier.create(countRolesForTenant(tenantId))
                .assertNext(count -> assertThat(count).isEqualTo(5))
                .verifyComplete();

        StepVerifier.create(countRolePermissionsForTenantRole(tenantId, SystemRoleTemplate.OWNER.code()))
                .assertNext(count -> assertThat(count).isEqualTo(IamPermissionCatalog.ALL.size()))
                .verifyComplete();

        StepVerifier.create(countRolePermissionsForTenantRole(tenantId, SystemRoleTemplate.ADMIN.code()))
                .assertNext(count -> assertThat(count).isEqualTo(SystemRoleTemplate.ADMIN.permissions().size()))
                .verifyComplete();
    }

    private reactor.core.publisher.Mono<Long> countRolesForTenant(TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) FROM iam.role
                        WHERE tenant_id = :tenantId AND system_role = TRUE
                        """)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one();
    }

    private reactor.core.publisher.Mono<Long> countRolePermissionsForTenantRole(
            TenantId tenantId,
            RoleCode roleCode
    ) {
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM iam.role_permission rp
                        INNER JOIN iam.role r ON r.role_id = rp.role_id
                        WHERE r.tenant_id = :tenantId AND r.code = :roleCode
                        """)
                .bind("tenantId", tenantId.value())
                .bind("roleCode", roleCode.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one();
    }
}
