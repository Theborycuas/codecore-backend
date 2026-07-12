package com.codecore.encounter.configuration;

import com.codecore.appointment.contract.reference.AppointmentReferencePort;
import com.codecore.encounter.application.admin.EncounterAdministrationUseCaseImpl;
import com.codecore.encounter.application.port.in.CancelEncounterUseCase;
import com.codecore.encounter.application.port.in.CompleteEncounterUseCase;
import com.codecore.encounter.application.port.in.CreateEncounterUseCase;
import com.codecore.encounter.application.port.in.GetEncounterUseCase;
import com.codecore.encounter.application.port.in.ListEncountersUseCase;
import com.codecore.encounter.application.port.in.UpdateEncounterUseCase;
import com.codecore.encounter.application.port.out.EncounterAdminQueryRepository;
import com.codecore.encounter.application.port.out.EncounterQueryPort;
import com.codecore.encounter.application.port.out.EncounterRepository;
import com.codecore.encounter.application.port.out.TenantContextAccessor;
import com.codecore.encounter.infrastructure.adapters.IamTenantContextAccessor;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.organization.contract.reference.OfficeReferencePort;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.contract.reference.StaffAssignmentReferencePort;
import com.codecore.patient.contract.reference.PatientReferencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class EncounterAdministrationConfiguration {

    @Bean
    public TenantContextAccessor encounterTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public EncounterAdministrationUseCaseImpl encounterAdministrationUseCase(
            TenantContextAccessor encounterTenantContextAccessor,
            EncounterAdminQueryRepository encounterAdminQueryRepository,
            EncounterRepository encounterRepository,
            EncounterQueryPort encounterQueryPort,
            PatientReferencePort patientReferencePort,
            OrganizationReferencePort organizationReferencePort,
            OfficeReferencePort officeReferencePort,
            StaffAssignmentReferencePort staffAssignmentReferencePort,
            AppointmentReferencePort appointmentReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new EncounterAdministrationUseCaseImpl(
                encounterTenantContextAccessor,
                encounterAdminQueryRepository,
                encounterRepository,
                encounterQueryPort,
                patientReferencePort,
                organizationReferencePort,
                officeReferencePort,
                staffAssignmentReferencePort,
                appointmentReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListEncountersUseCase listEncountersUseCase(EncounterAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetEncounterUseCase getEncounterUseCase(EncounterAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateEncounterUseCase createEncounterUseCase(EncounterAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateEncounterUseCase updateEncounterUseCase(EncounterAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CancelEncounterUseCase cancelEncounterUseCase(EncounterAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CompleteEncounterUseCase completeEncounterUseCase(EncounterAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
