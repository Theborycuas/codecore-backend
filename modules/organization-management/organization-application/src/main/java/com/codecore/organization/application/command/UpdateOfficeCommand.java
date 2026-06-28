package com.codecore.organization.application.command;

import com.codecore.organization.domain.valueobject.OfficeId;

public record UpdateOfficeCommand(OfficeId officeId, String name) {
}
