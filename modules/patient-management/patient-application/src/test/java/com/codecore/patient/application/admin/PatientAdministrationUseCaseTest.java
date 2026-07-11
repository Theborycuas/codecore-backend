package com.codecore.patient.application.admin;

import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.patient.application.command.CreatePatientCommand;
import com.codecore.patient.application.dto.AdminPatientView;
import com.codecore.patient.application.port.out.PatientAdminQueryRepository;
import com.codecore.patient.application.port.out.PatientQueryPort;
import com.codecore.patient.application.port.out.PatientRepository;
import com.codecore.patient.application.port.out.TenantContextAccessor;
import com.codecore.patient.domain.exception.PrimaryOrganizationNotFoundException;
import com.codecore.patient.domain.valueobject.PatientDisplayName;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAdministrationUseCaseTest {

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private PatientAdminQueryRepository patientAdminQueryRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientQueryPort patientQueryPort;
    @Mock
    private OrganizationReferencePort organizationReferencePort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private PatientAdministrationUseCaseImpl useCase;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        useCase = new PatientAdministrationUseCaseImpl(
                tenantContextAccessor,
                patientAdminQueryRepository,
                patientRepository,
                patientQueryPort,
                organizationReferencePort,
                transactionalOperator
        );
        tenantId = TenantId.generate();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreatePatientWithoutPrimaryOrganization() {
        when(patientRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreatePatientCommand command = new CreatePatientCommand(
                "María García",
                null,
                null,
                null,
                null,
                List.of()
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    org.assertj.core.api.Assertions.assertThat(view.displayName()).isEqualTo("María García");
                    org.assertj.core.api.Assertions.assertThat(view.primaryOrganizationId()).isNull();
                    org.assertj.core.api.Assertions.assertThat(view.tenantId()).isEqualTo(tenantId);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectInactivePrimaryOrganization() {
        UUID orgId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(OrganizationId.class), any()))
                .thenReturn(Mono.just(false));

        CreatePatientCommand command = new CreatePatientCommand(
                "Buddy",
                null,
                null,
                null,
                orgId,
                List.of()
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(PrimaryOrganizationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldCreatePatientWithActivePrimaryOrganization() {
        UUID orgId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(OrganizationId.class), any()))
                .thenReturn(Mono.just(true));
        when(patientRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreatePatientCommand command = new CreatePatientCommand(
                PatientDisplayName.of("Ana").value(),
                "ana@example.com",
                null,
                null,
                orgId,
                List.of(new CreatePatientCommand.ExternalIdentifierInput("MRN", "P-1"))
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    org.assertj.core.api.Assertions.assertThat(view.primaryOrganizationId().value()).isEqualTo(orgId);
                    org.assertj.core.api.Assertions.assertThat(view.externalIdentifiers()).hasSize(1);
                })
                .verifyComplete();
    }
}
