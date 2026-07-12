package com.codecore.encounter.application.port.in;

import com.codecore.encounter.application.dto.AdminEncounterView;
import com.codecore.encounter.domain.valueobject.EncounterId;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface CompleteEncounterUseCase {

    /**
     * @param endedAt optional; resolution order: body → persisted endedAt → Instant.now()
     */
    Mono<AdminEncounterView> complete(EncounterId encounterId, Instant endedAt);
}
