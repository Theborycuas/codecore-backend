package com.codecore.organization.application.command;

import com.codecore.organization.domain.valueobject.OrganizationId;

import java.util.UUID;

public record CreateOfficeCommand(UUID organizationId, String code, String name) {
}
