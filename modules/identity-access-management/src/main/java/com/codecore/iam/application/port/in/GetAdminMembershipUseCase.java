package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminMembershipView;
import com.codecore.iam.domain.valueobject.MembershipId;
import reactor.core.publisher.Mono;

public interface GetAdminMembershipUseCase {

    Mono<AdminMembershipView> execute(MembershipId membershipId);
}
