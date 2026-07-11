package com.codecore.patient.configuration;

import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.patient.application.admin.PatientAdministrationUseCaseImpl;
import com.codecore.patient.application.port.in.ActivatePatientUseCase;
import com.codecore.patient.application.port.in.ArchivePatientUseCase;
import com.codecore.patient.application.port.in.CreatePatientUseCase;
import com.codecore.patient.application.port.in.GetPatientUseCase;
import com.codecore.patient.application.port.in.ListPatientsUseCase;
import com.codecore.patient.application.port.in.UpdatePatientUseCase;
import com.codecore.patient.application.port.out.PatientAdminQueryRepository;
import com.codecore.patient.application.port.out.PatientQueryPort;
import com.codecore.patient.application.port.out.PatientRepository;
import com.codecore.patient.application.port.out.TenantContextAccessor;
import com.codecore.patient.infrastructure.adapters.IamTenantContextAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class PatientAdministrationConfiguration {

    @Bean
    public TenantContextAccessor patientTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public PatientAdministrationUseCaseImpl patientAdministrationUseCase(
            TenantContextAccessor patientTenantContextAccessor,
            PatientAdminQueryRepository patientAdminQueryRepository,
            PatientRepository patientRepository,
            PatientQueryPort patientQueryPort,
            OrganizationReferencePort organizationReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new PatientAdministrationUseCaseImpl(
                patientTenantContextAccessor,
                patientAdminQueryRepository,
                patientRepository,
                patientQueryPort,
                organizationReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListPatientsUseCase listPatientsUseCase(PatientAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetPatientUseCase getPatientUseCase(PatientAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreatePatientUseCase createPatientUseCase(PatientAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdatePatientUseCase updatePatientUseCase(PatientAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ArchivePatientUseCase archivePatientUseCase(PatientAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ActivatePatientUseCase activatePatientUseCase(PatientAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
