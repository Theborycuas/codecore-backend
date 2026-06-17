package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminMembershipRoleView;
import com.codecore.iam.domain.valueobject.MembershipId;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetAdminMembershipRolesUseCase {

    Mono<List<AdminMembershipRoleView>> execute(MembershipId membershipId);
}
