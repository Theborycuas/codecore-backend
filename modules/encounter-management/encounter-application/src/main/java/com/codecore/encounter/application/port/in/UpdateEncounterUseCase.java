package com.codecore.encounter.application.port.in;

import com.codecore.encounter.application.command.UpdateEncounterCommand;
import com.codecore.encounter.application.dto.AdminEncounterView;
import reactor.core.publisher.Mono;

public interface UpdateEncounterUseCase {

    Mono<AdminEncounterView> execute(UpdateEncounterCommand command);
}
