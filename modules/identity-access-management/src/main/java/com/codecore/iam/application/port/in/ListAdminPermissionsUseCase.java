package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminPermissionView;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListAdminPermissionsUseCase {

    Mono<PagedResult<AdminPermissionView>> execute(PageQuery pageQuery);
}
