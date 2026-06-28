package com.codecore.organization.application.port.in;

import com.codecore.organization.application.dto.AdminOrganizationView;
import com.codecore.organization.application.dto.PagedResult;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.StructureListFilter;
import reactor.core.publisher.Mono;

public interface ListOrganizationsUseCase {

    Mono<PagedResult<AdminOrganizationView>> execute(StructureListFilter filter, PageQuery pageQuery);
}
