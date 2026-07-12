package com.codecore.appointment.configuration;

import com.codecore.appointment.application.admin.AppointmentAdministrationUseCaseImpl;
import com.codecore.appointment.application.port.in.CancelAppointmentUseCase;
import com.codecore.appointment.application.port.in.CompleteAppointmentUseCase;
import com.codecore.appointment.application.port.in.CreateAppointmentUseCase;
import com.codecore.appointment.application.port.in.GetAppointmentUseCase;
import com.codecore.appointment.application.port.in.ListAppointmentsUseCase;
import com.codecore.appointment.application.port.in.UpdateAppointmentUseCase;
import com.codecore.appointment.application.port.out.AppointmentAdminQueryRepository;
import com.codecore.appointment.application.port.out.AppointmentQueryPort;
import com.codecore.appointment.application.port.out.AppointmentRepository;
import com.codecore.appointment.application.port.out.TenantContextAccessor;
import com.codecore.appointment.infrastructure.adapters.IamTenantContextAccessor;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.organization.contract.reference.OfficeReferencePort;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.contract.reference.StaffAssignmentReferencePort;
import com.codecore.patient.contract.reference.PatientReferencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class AppointmentAdministrationConfiguration {

    @Bean
    public TenantContextAccessor appointmentTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public AppointmentAdministrationUseCaseImpl appointmentAdministrationUseCase(
            TenantContextAccessor appointmentTenantContextAccessor,
            AppointmentAdminQueryRepository appointmentAdminQueryRepository,
            AppointmentRepository appointmentRepository,
            AppointmentQueryPort appointmentQueryPort,
            PatientReferencePort patientReferencePort,
            OrganizationReferencePort organizationReferencePort,
            OfficeReferencePort officeReferencePort,
            StaffAssignmentReferencePort staffAssignmentReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new AppointmentAdministrationUseCaseImpl(
                appointmentTenantContextAccessor,
                appointmentAdminQueryRepository,
                appointmentRepository,
                appointmentQueryPort,
                patientReferencePort,
                organizationReferencePort,
                officeReferencePort,
                staffAssignmentReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListAppointmentsUseCase listAppointmentsUseCase(AppointmentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetAppointmentUseCase getAppointmentUseCase(AppointmentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateAppointmentUseCase createAppointmentUseCase(AppointmentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateAppointmentUseCase updateAppointmentUseCase(AppointmentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CancelAppointmentUseCase cancelAppointmentUseCase(AppointmentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CompleteAppointmentUseCase completeAppointmentUseCase(AppointmentAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
