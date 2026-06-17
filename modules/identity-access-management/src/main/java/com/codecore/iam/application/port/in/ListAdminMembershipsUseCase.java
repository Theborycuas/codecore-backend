package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminMembershipView;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListAdminMembershipsUseCase {

    Mono<PagedResult<AdminMembershipView>> execute(PageQuery pageQuery);
}
