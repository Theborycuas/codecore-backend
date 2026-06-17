package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminRoleView;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListAdminRolesUseCase {

    Mono<PagedResult<AdminRoleView>> execute(PageQuery pageQuery);
}
