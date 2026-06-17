package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.ReplaceAdminMembershipRolesCommand;
import com.codecore.iam.application.dto.AdminMembershipRoleView;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReplaceAdminMembershipRolesUseCase {

    Mono<List<AdminMembershipRoleView>> execute(ReplaceAdminMembershipRolesCommand command);
}
