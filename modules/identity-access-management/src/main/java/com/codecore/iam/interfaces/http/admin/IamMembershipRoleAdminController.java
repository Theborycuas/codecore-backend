package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.command.ReplaceAdminMembershipRolesCommand;
import com.codecore.iam.application.port.in.GetAdminMembershipRolesUseCase;
import com.codecore.iam.application.port.in.ReplaceAdminMembershipRolesUseCase;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.interfaces.http.admin.dto.MembershipRoleResponse;
import com.codecore.iam.interfaces.http.admin.dto.ReplaceMembershipRolesRequest;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(IamAdminApiPaths.MEMBERSHIPS + "/{membershipId}/roles")
public class IamMembershipRoleAdminController {

    private final GetAdminMembershipRolesUseCase getAdminMembershipRolesUseCase;
    private final ReplaceAdminMembershipRolesUseCase replaceAdminMembershipRolesUseCase;

    public IamMembershipRoleAdminController(
            GetAdminMembershipRolesUseCase getAdminMembershipRolesUseCase,
            ReplaceAdminMembershipRolesUseCase replaceAdminMembershipRolesUseCase
    ) {
        this.getAdminMembershipRolesUseCase = getAdminMembershipRolesUseCase;
        this.replaceAdminMembershipRolesUseCase = replaceAdminMembershipRolesUseCase;
    }

    @GetMapping
    @RequiresPermission("membership:update")
    public Mono<List<MembershipRoleResponse>> listMembershipRoles(@PathVariable UUID membershipId) {
        return getAdminMembershipRolesUseCase
                .execute(new MembershipId(membershipId))
                .map(views -> views.stream().map(MembershipRoleResponse::from).toList());
    }

    @PutMapping
    @RequiresPermission("membership:update")
    public Mono<List<MembershipRoleResponse>> replaceMembershipRoles(
            @PathVariable UUID membershipId,
            @Valid @RequestBody ReplaceMembershipRolesRequest request
    ) {
        ReplaceAdminMembershipRolesCommand command = new ReplaceAdminMembershipRolesCommand(
                new MembershipId(membershipId),
                request.roleIds()
        );
        return replaceAdminMembershipRolesUseCase
                .execute(command)
                .map(views -> views.stream().map(MembershipRoleResponse::from).toList());
    }
}
