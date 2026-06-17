package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.command.CreateAdminMembershipCommand;
import com.codecore.iam.application.command.UpdateAdminMembershipCommand;
import com.codecore.iam.application.port.in.CreateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.GetAdminMembershipUseCase;
import com.codecore.iam.application.port.in.ListAdminMembershipsUseCase;
import com.codecore.iam.application.port.in.UpdateAdminMembershipUseCase;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.interfaces.http.admin.dto.CreateMembershipRequest;
import com.codecore.iam.interfaces.http.admin.dto.MembershipResponse;
import com.codecore.iam.interfaces.http.admin.dto.PagedMembershipResponse;
import com.codecore.iam.interfaces.http.admin.dto.UpdateMembershipRequest;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(IamAdminApiPaths.MEMBERSHIPS)
public class IamMembershipAdminController {

    private final ListAdminMembershipsUseCase listAdminMembershipsUseCase;
    private final GetAdminMembershipUseCase getAdminMembershipUseCase;
    private final CreateAdminMembershipUseCase createAdminMembershipUseCase;
    private final UpdateAdminMembershipUseCase updateAdminMembershipUseCase;
    private final DeactivateAdminMembershipUseCase deactivateAdminMembershipUseCase;

    public IamMembershipAdminController(
            ListAdminMembershipsUseCase listAdminMembershipsUseCase,
            GetAdminMembershipUseCase getAdminMembershipUseCase,
            CreateAdminMembershipUseCase createAdminMembershipUseCase,
            UpdateAdminMembershipUseCase updateAdminMembershipUseCase,
            DeactivateAdminMembershipUseCase deactivateAdminMembershipUseCase
    ) {
        this.listAdminMembershipsUseCase = listAdminMembershipsUseCase;
        this.getAdminMembershipUseCase = getAdminMembershipUseCase;
        this.createAdminMembershipUseCase = createAdminMembershipUseCase;
        this.updateAdminMembershipUseCase = updateAdminMembershipUseCase;
        this.deactivateAdminMembershipUseCase = deactivateAdminMembershipUseCase;
    }

    @GetMapping
    @RequiresPermission("membership:read")
    public Mono<PagedMembershipResponse> listMemberships(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return listAdminMembershipsUseCase
                .execute(PageQueryParser.parseMembershipPageQuery(page, size, sort))
                .map(PagedMembershipResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("membership:read")
    public Mono<MembershipResponse> getMembership(@PathVariable UUID id) {
        return getAdminMembershipUseCase.execute(new MembershipId(id)).map(MembershipResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("membership:create")
    public Mono<MembershipResponse> createMembership(@Valid @RequestBody CreateMembershipRequest request) {
        CreateAdminMembershipCommand command = new CreateAdminMembershipCommand(
                request.identityId(),
                request.email(),
                request.password()
        );
        return createAdminMembershipUseCase.execute(command).map(MembershipResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("membership:update")
    public Mono<MembershipResponse> updateMembership(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMembershipRequest request
    ) {
        UpdateAdminMembershipCommand command = new UpdateAdminMembershipCommand(
                new MembershipId(id),
                request.status()
        );
        return updateAdminMembershipUseCase.execute(command).map(MembershipResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("membership:delete")
    public Mono<Void> deactivateMembership(@PathVariable UUID id) {
        return deactivateAdminMembershipUseCase.deactivate(new MembershipId(id));
    }
}
