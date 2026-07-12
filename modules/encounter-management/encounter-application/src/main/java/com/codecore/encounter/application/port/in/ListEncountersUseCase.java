package com.codecore.encounter.application.port.in;

import com.codecore.encounter.application.dto.AdminEncounterView;
import com.codecore.encounter.application.dto.PagedResult;
import com.codecore.encounter.application.query.EncounterListQuery;
import com.codecore.encounter.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListEncountersUseCase {

    Mono<PagedResult<AdminEncounterView>> execute(EncounterListQuery filter, PageQuery pageQuery);
}
