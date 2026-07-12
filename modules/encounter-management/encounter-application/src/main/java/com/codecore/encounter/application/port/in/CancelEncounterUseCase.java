package com.codecore.encounter.application.port.in;

import com.codecore.encounter.application.dto.AdminEncounterView;
import com.codecore.encounter.domain.valueobject.EncounterId;
import reactor.core.publisher.Mono;

public interface CancelEncounterUseCase {

    Mono<AdminEncounterView> cancel(EncounterId encounterId);
}
