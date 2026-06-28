package com.codecore.organization.application.admin;

import com.codecore.organization.application.command.CreateOrganizationCommand;
import com.codecore.organization.application.dto.AdminOrganizationView;
import com.codecore.organization.application.port.out.OfficeRepository;
import com.codecore.organization.application.port.out.OrganizationAdminQueryRepository;
import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.application.port.out.TenantContextAccessor;
import com.codecore.organization.domain.exception.OrganizationAlreadyExistsException;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAdministrationUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.generate();
    private static final Instant NOW = Instant.parse("2026-06-22T12:00:00Z");

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private OrganizationAdminQueryRepository organizationAdminQueryRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationQueryPort organizationQueryPort;
    @Mock
    private OfficeRepository officeRepository;
    @Mock
    private TransactionalOperator transactionalOperator;

    private OrganizationAdministrationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new OrganizationAdministrationUseCaseImpl(
                tenantContextAccessor,
                organizationAdminQueryRepository,
                organizationRepository,
                organizationQueryPort,
                officeRepository,
                transactionalOperator
        );
        org.mockito.Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateOrganizationWhenCodeIsUnique() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(TENANT_ID));
        when(organizationRepository.existsByTenantIdAndCode(TENANT_ID, OrganizationCode.of("DENTAL_NORTE")))
                .thenReturn(Mono.just(false));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(new CreateOrganizationCommand("DENTAL_NORTE", "Dental Norte")))
                .expectNextMatches(view -> view.code().equals("DENTAL_NORTE"))
                .verifyComplete();

        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void shouldRejectDuplicateOrganizationCode() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(TENANT_ID));
        when(organizationRepository.existsByTenantIdAndCode(TENANT_ID, OrganizationCode.of("DENTAL_NORTE")))
                .thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new CreateOrganizationCommand("DENTAL_NORTE", "Dental Norte")))
                .expectError(OrganizationAlreadyExistsException.class)
                .verify();
    }
}
