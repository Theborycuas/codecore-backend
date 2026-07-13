package com.codecore.access.domain.model.invitation;

import com.codecore.access.domain.exception.InvalidDomainValueException;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NEGATIVE API surface guard — Invitation must stay intentionally small (ADR-019).
 * Documents compile-time / reflection assertions that forbidden concerns are absent.
 */
class InvitationNegativeApiTest {

    private static final Set<String> FORBIDDEN_METHODS = Set.of(
            "assignOffice",
            "assignOrganization",
            "createStaffAssignment",
            "attachSubscription",
            "reserveSeat",
            "startTrial",
            "setPlan",
            "setPassword",
            "resetPassword",
            "completePasswordReset",
            "sendEmail",
            "retryNotification",
            "addPatient",
            "linkEncounter",
            "addInvoice",
            "inviteOwner",
            "updateEmail",
            "updateRole",
            "unrevoke",
            "reactivate",
            "reopen",
            "delete"
    );

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "organizationId",
            "officeId",
            "planId",
            "subscriptionId"
    );

    @Test
    void shouldNotExposeForbiddenLifecycleOrCrossBcMethods() {
        Set<String> methodNames = Arrays.stream(Invitation.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methodNames).doesNotContainAnyElementsOf(FORBIDDEN_METHODS);
    }

    @Test
    void shouldNotDeclareOrganizationOfficePlanOrSubscriptionFields() {
        Set<String> fieldNames = Arrays.stream(Invitation.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fieldNames).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
    }

    @Test
    void shouldRejectOwnerRoleCode() {
        assertThatThrownBy(() -> InvitationRoleCode.of("OWNER"))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("OWNER");
    }
}
