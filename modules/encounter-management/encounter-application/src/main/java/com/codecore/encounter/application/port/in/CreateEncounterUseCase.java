package com.codecore.encounter.application.port.in;

import com.codecore.encounter.application.command.CreateEncounterCommand;
import com.codecore.encounter.application.dto.AdminEncounterView;
import reactor.core.publisher.Mono;

public interface CreateEncounterUseCase {

    Mono<AdminEncounterView> execute(CreateEncounterCommand command);
}
