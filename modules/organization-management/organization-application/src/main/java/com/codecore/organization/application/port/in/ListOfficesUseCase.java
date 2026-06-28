package com.codecore.organization.application.port.in;

import com.codecore.organization.application.dto.AdminOfficeView;
import com.codecore.organization.application.dto.PagedResult;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.StructureListFilter;
import com.codecore.organization.domain.valueobject.OrganizationId;
import reactor.core.publisher.Mono;

public interface ListOfficesUseCase {

    Mono<PagedResult<AdminOfficeView>> execute(
            OrganizationId organizationId,
            StructureListFilter filter,
            PageQuery pageQuery
    );
}
