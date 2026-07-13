package com.codecore.audit.application.port.in;

import com.codecore.audit.application.dto.AdminAuditView;
import com.codecore.audit.application.dto.PagedResult;
import com.codecore.audit.application.query.AuditListQuery;
import com.codecore.audit.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListAuditEntriesUseCase {

    Mono<PagedResult<AdminAuditView>> execute(AuditListQuery filter, PageQuery pageQuery);
}
