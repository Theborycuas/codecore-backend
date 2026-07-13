package com.codecore.access.application.port.in;

import com.codecore.access.application.dto.AdminInvitationView;
import com.codecore.access.application.dto.PagedResult;
import com.codecore.access.application.query.InvitationListQuery;
import com.codecore.access.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListInvitationsUseCase {

    Mono<PagedResult<AdminInvitationView>> execute(InvitationListQuery filter, PageQuery pageQuery);
}
