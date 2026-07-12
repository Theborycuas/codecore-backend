package com.codecore.encounter.interfaces.http.admin.dto;

import java.time.Instant;

public record CompleteEncounterRequest(
        Instant endedAt
) {
}
