package com.codecore.organization.application.command;

import com.codecore.organization.domain.valueobject.OrganizationId;

public record UpdateOrganizationCommand(OrganizationId organizationId, String name) {
}
