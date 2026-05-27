package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.SessionId;

import java.util.Optional;

public record AuthenticationResultDto(
        AuthenticationOutcome outcome,
        Optional<IdentityId> identityId,
        Optional<SessionId> sessionId
) {
}
