package com.codecore.appointment.testsupport;

import com.codecore.appointment.configuration.AppointmentOpenApiConfiguration;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Appointment administration stack for FASE 18.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        AppointmentAdminIntegrationTestConfiguration.class,
        AppointmentOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class AppointmentAdministrationVerificationTestConfiguration {
}
