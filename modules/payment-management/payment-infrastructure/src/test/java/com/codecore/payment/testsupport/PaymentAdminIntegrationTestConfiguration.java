package com.codecore.payment.testsupport;

import com.codecore.billing.configuration.BillingModuleConfiguration;
import com.codecore.billing.infrastructure.adapters.R2dbcInvoiceReferenceAdapter;
import com.codecore.billing.infrastructure.persistence.repository.R2dbcInvoiceAdminQueryRepository;
import com.codecore.billing.infrastructure.persistence.repository.R2dbcInvoiceRepository;
import com.codecore.billing.interfaces.http.InvoiceHttpExceptionHandler;
import com.codecore.billing.interfaces.http.admin.InvoiceAdminController;
import com.codecore.encounter.infrastructure.adapters.R2dbcEncounterReferenceAdapter;
import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.iam.configuration.IamAdministrationConfiguration;
import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamAuthorizationConfiguration;
import com.codecore.iam.configuration.IamBootstrapConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcAuthorizationQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.CreateTenantController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.security.AuthenticatedPrincipalAuthorizationManager;
import com.codecore.iam.interfaces.http.security.AuthorizationContextWebFilter;
import com.codecore.iam.interfaces.http.security.JwtAuthenticationWebFilter;
import com.codecore.iam.interfaces.http.security.RequiresPermissionAspect;
import com.codecore.inventory.configuration.InventoryModuleConfiguration;
import com.codecore.inventory.infrastructure.adapters.R2dbcItemReferenceAdapter;
import com.codecore.inventory.infrastructure.persistence.repository.R2dbcItemAdminQueryRepository;
import com.codecore.inventory.infrastructure.persistence.repository.R2dbcItemRepository;
import com.codecore.inventory.interfaces.http.ItemHttpExceptionHandler;
import com.codecore.inventory.interfaces.http.admin.ItemAdminController;
import com.codecore.organization.configuration.OrganizationModuleConfiguration;
import com.codecore.organization.infrastructure.adapters.R2dbcMembershipReferenceAdapter;
import com.codecore.organization.infrastructure.adapters.R2dbcOfficeReferenceAdapter;
import com.codecore.organization.infrastructure.adapters.R2dbcOrganizationReferenceAdapter;
import com.codecore.organization.infrastructure.adapters.R2dbcStaffAssignmentReferenceAdapter;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOfficeAdminQueryRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOfficeRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOrganizationAdminQueryRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOrganizationRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcStaffAssignmentAdminQueryRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcStaffAssignmentRepository;
import com.codecore.organization.interfaces.http.OrgHttpExceptionHandler;
import com.codecore.organization.interfaces.http.admin.OfficeAdminController;
import com.codecore.organization.interfaces.http.admin.OrganizationAdminController;
import com.codecore.organization.interfaces.http.admin.StaffAssignmentAdminController;
import com.codecore.patient.configuration.PatientModuleConfiguration;
import com.codecore.patient.infrastructure.adapters.R2dbcPatientReferenceAdapter;
import com.codecore.patient.infrastructure.persistence.repository.R2dbcPatientAdminQueryRepository;
import com.codecore.patient.infrastructure.persistence.repository.R2dbcPatientRepository;
import com.codecore.patient.interfaces.http.PatientHttpExceptionHandler;
import com.codecore.patient.interfaces.http.admin.PatientAdminController;
import com.codecore.payment.configuration.PaymentModuleConfiguration;
import com.codecore.payment.infrastructure.adapters.R2dbcPaymentReferenceAdapter;
import com.codecore.payment.infrastructure.persistence.repository.R2dbcPaymentAdminQueryRepository;
import com.codecore.payment.infrastructure.persistence.repository.R2dbcPaymentRepository;
import com.codecore.payment.interfaces.http.PaymentHttpExceptionHandler;
import com.codecore.payment.interfaces.http.admin.PaymentAdminController;
import com.codecore.platform.r2dbc.PlatformR2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Payment administration stack for FASE 22.7 verification (E2E HTTP). Mirrors
 * {@code InvoiceAdminIntegrationTestConfiguration} plus the Payment slice — brings in just
 * enough of Billing (+ Org/Patient/Item/Encounter reference) to create and issue an Invoice
 * that Payments can then settle against (ADR-018).
 */
@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamBootstrapConfiguration.class,
        IamAdministrationConfiguration.class,
        IamAuthenticationConfiguration.class,
        IamAuthorizationConfiguration.class,
        PlatformR2dbcAutoConfiguration.class,
        OrganizationModuleConfiguration.class,
        PatientModuleConfiguration.class,
        InventoryModuleConfiguration.class,
        BillingModuleConfiguration.class,
        PaymentModuleConfiguration.class,
        R2dbcOrganizationRepository.class,
        R2dbcOrganizationAdminQueryRepository.class,
        R2dbcOfficeRepository.class,
        R2dbcOfficeAdminQueryRepository.class,
        R2dbcStaffAssignmentRepository.class,
        R2dbcStaffAssignmentAdminQueryRepository.class,
        R2dbcMembershipReferenceAdapter.class,
        R2dbcOrganizationReferenceAdapter.class,
        R2dbcOfficeReferenceAdapter.class,
        R2dbcStaffAssignmentReferenceAdapter.class,
        R2dbcPatientRepository.class,
        R2dbcPatientAdminQueryRepository.class,
        R2dbcPatientReferenceAdapter.class,
        R2dbcItemRepository.class,
        R2dbcItemAdminQueryRepository.class,
        R2dbcItemReferenceAdapter.class,
        R2dbcEncounterReferenceAdapter.class,
        R2dbcInvoiceRepository.class,
        R2dbcInvoiceAdminQueryRepository.class,
        R2dbcInvoiceReferenceAdapter.class,
        R2dbcPaymentRepository.class,
        R2dbcPaymentAdminQueryRepository.class,
        R2dbcPaymentReferenceAdapter.class,
        R2dbcIdentityRepository.class,
        R2dbcIdentityAdminQueryRepository.class,
        R2dbcMembershipAdminQueryRepository.class,
        R2dbcRoleAdminQueryRepository.class,
        R2dbcPermissionAdminQueryRepository.class,
        R2dbcTenantRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcRoleRepository.class,
        R2dbcPermissionRepository.class,
        R2dbcRolePermissionRepository.class,
        R2dbcRolePermissionAdminQueryRepository.class,
        R2dbcMembershipRoleRepository.class,
        R2dbcMembershipRoleAdminQueryRepository.class,
        R2dbcAuthorizationQueryRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class,
        JwtTokenValidator.class,
        JwtAuthenticationWebFilter.class,
        AuthorizationContextWebFilter.class,
        RequiresPermissionAspect.class,
        ReactorAuthorizationContextAccessor.class,
        AuthenticatedPrincipalAuthorizationManager.class,
        AuthenticationController.class,
        CreateTenantController.class,
        OrganizationAdminController.class,
        OfficeAdminController.class,
        StaffAssignmentAdminController.class,
        PatientAdminController.class,
        ItemAdminController.class,
        InvoiceAdminController.class,
        PaymentAdminController.class,
        IamHttpExceptionHandler.class,
        OrgHttpExceptionHandler.class,
        PatientHttpExceptionHandler.class,
        ItemHttpExceptionHandler.class,
        InvoiceHttpExceptionHandler.class,
        PaymentHttpExceptionHandler.class
})
public class PaymentAdminIntegrationTestConfiguration {
}
