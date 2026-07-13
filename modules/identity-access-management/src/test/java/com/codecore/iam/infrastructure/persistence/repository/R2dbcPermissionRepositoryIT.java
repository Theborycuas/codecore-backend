package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamPermissionPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(IamPermissionPersistenceTestConfiguration.class)
class R2dbcPermissionRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void shouldPersistFindAndCheckExistsByCode() {
        PermissionCode code = PermissionCode.of("test-fixture:update");
        Instant now = Instant.now();
        Permission permission = Permission.create(code, "Update users", now);

        StepVerifier.create(permissionRepository.save(permission))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(permission.id());
                    assertThat(saved.code()).isEqualTo(code);
                    assertThat(saved.description()).isEqualTo("Update users");
                })
                .verifyComplete();

        StepVerifier.create(permissionRepository.existsByCode(code))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(permissionRepository.findById(permission.id()))
                .assertNext(found -> assertThat(found.code()).isEqualTo(code))
                .verifyComplete();

        StepVerifier.create(permissionRepository.findByCode(code))
                .assertNext(found -> assertThat(found.id()).isEqualTo(permission.id()))
                .verifyComplete();

        permission.updateDescription("Updated description");
        StepVerifier.create(permissionRepository.save(permission))
                .assertNext(updated -> assertThat(updated.description()).isEqualTo("Updated description"))
                .verifyComplete();
    }

    @Test
    void shouldEnforceGloballyUniqueCode() {
        PermissionCode code = PermissionCode.of("test-fixture:delete");
        Instant now = Instant.now();

        StepVerifier.create(permissionRepository.save(
                Permission.create(code, "Delete users", now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(permissionRepository.save(
                Permission.create(code, "Duplicate", now)))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldPersistSystemPermission() {
        Permission permission = Permission.createSystemPermission(
                PermissionCode.of("test-fixture:create"),
                "Create test fixtures",
                Instant.now()
        );

        StepVerifier.create(permissionRepository.save(permission))
                .assertNext(saved -> assertThat(saved.systemPermission()).isTrue())
                .verifyComplete();

        StepVerifier.create(permissionRepository.findByCode(PermissionCode.of("test-fixture:create")))
                .assertNext(found -> assertThat(found.systemPermission()).isTrue())
                .verifyComplete();
    }
}
