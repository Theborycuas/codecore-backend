package com.codecore.audit.application.port.in;

import com.codecore.audit.application.dto.AdminAuditView;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import reactor.core.publisher.Mono;

public interface GetAuditEntryUseCase {

    Mono<AdminAuditView> execute(AuditEntryId auditEntryId);
}
