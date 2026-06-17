package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminUserView;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListAdminUsersUseCase {

    Mono<PagedResult<AdminUserView>> execute(PageQuery pageQuery);
}
