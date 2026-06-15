package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamRolePersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(IamRolePersistenceTestConfiguration.class)
class R2dbcRoleRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldPersistFindAndCheckExistsByTenantAndCode() {
        TenantId tenantId = TenantId.generate();
        RoleCode code = RoleCode.of("VET");
        Instant now = Instant.now();
        Role role = Role.create(tenantId, code, RoleName.of("Veterinarian"), now);

        StepVerifier.create(roleRepository.save(role))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(role.id());
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.code()).isEqualTo(code);
                    assertThat(saved.status()).isEqualTo(RoleStatus.ACTIVE);
                })
                .verifyComplete();

        StepVerifier.create(roleRepository.existsByTenantIdAndCode(tenantId, code))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(roleRepository.findById(role.id()))
                .assertNext(found -> assertThat(found.name().value()).isEqualTo("Veterinarian"))
                .verifyComplete();

        StepVerifier.create(roleRepository.findByTenantIdAndCode(tenantId, code))
                .assertNext(found -> assertThat(found.id()).isEqualTo(role.id()))
                .verifyComplete();

        role.deactivate();
        StepVerifier.create(roleRepository.save(role))
                .assertNext(updated -> assertThat(updated.status()).isEqualTo(RoleStatus.INACTIVE))
                .verifyComplete();
    }

    @Test
    void shouldAllowSameCodeInDifferentTenants() {
        RoleCode code = RoleCode.of("ADMIN");
        Instant now = Instant.now();

        Role tenantARole = Role.create(
                TenantId.generate(),
                code,
                RoleName.of("Tenant A Admin"),
                now
        );
        Role tenantBRole = Role.create(
                TenantId.generate(),
                code,
                RoleName.of("Tenant B Admin"),
                now
        );

        StepVerifier.create(roleRepository.save(tenantARole))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(roleRepository.save(tenantBRole))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateCodeWithinSameTenant() {
        TenantId tenantId = TenantId.generate();
        RoleCode code = RoleCode.of("RECEPTIONIST");
        Instant now = Instant.now();

        StepVerifier.create(roleRepository.save(
                Role.create(tenantId, code, RoleName.of("Front Desk"), now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(roleRepository.save(
                Role.create(tenantId, code, RoleName.of("Duplicate"), now)))
                .expectError(DuplicateKeyException.class)
                .verify();
    }
}
