package com.codecore.access.interfaces.http.admin;

import com.codecore.access.application.command.CreateInvitationCommand;
import com.codecore.access.application.port.in.CreateInvitationUseCase;
import com.codecore.access.application.port.in.GetInvitationUseCase;
import com.codecore.access.application.port.in.ListInvitationsUseCase;
import com.codecore.access.application.port.in.RevokeInvitationUseCase;
import com.codecore.access.application.query.InvitationListQuery;
import com.codecore.access.application.query.PageQueryParser;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.interfaces.http.admin.dto.CreateInvitationRequest;
import com.codecore.access.interfaces.http.admin.dto.InvitationCreatedResponse;
import com.codecore.access.interfaces.http.admin.dto.InvitationResponse;
import com.codecore.access.interfaces.http.admin.dto.PagedInvitationResponse;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(InvitationAdminApiPaths.INVITATIONS)
@Tag(name = "Access Invitations", description = "Invitation administration (`invitation:*` permissions)")
public class InvitationAdminController {

    private final ListInvitationsUseCase listInvitationsUseCase;
    private final GetInvitationUseCase getInvitationUseCase;
    private final CreateInvitationUseCase createInvitationUseCase;
    private final RevokeInvitationUseCase revokeInvitationUseCase;

    public InvitationAdminController(
            ListInvitationsUseCase listInvitationsUseCase,
            GetInvitationUseCase getInvitationUseCase,
            CreateInvitationUseCase createInvitationUseCase,
            RevokeInvitationUseCase revokeInvitationUseCase
    ) {
        this.listInvitationsUseCase = listInvitationsUseCase;
        this.getInvitationUseCase = getInvitationUseCase;
        this.createInvitationUseCase = createInvitationUseCase;
        this.revokeInvitationUseCase = revokeInvitationUseCase;
    }

    @GetMapping
    @RequiresPermission("invitation:read")
    public Mono<PagedInvitationResponse> listInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        InvitationListQuery filter = InvitationListQuery.of(status);
        return listInvitationsUseCase
                .execute(filter, PageQueryParser.parseInvitationPageQuery(page, size, sort))
                .map(PagedInvitationResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("invitation:read")
    public Mono<InvitationResponse> getInvitation(@PathVariable UUID id) {
        return getInvitationUseCase.execute(new InvitationId(id)).map(InvitationResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("invitation:create")
    public Mono<InvitationCreatedResponse> createInvitation(@Valid @RequestBody CreateInvitationRequest request) {
        return createInvitationUseCase
                .execute(new CreateInvitationCommand(request.email(), request.roleCode(), null))
                .map(InvitationCreatedResponse::from);
    }

    @PostMapping("/{id}/revoke")
    @RequiresPermission("invitation:revoke")
    public Mono<InvitationResponse> revokeInvitation(@PathVariable UUID id) {
        return revokeInvitationUseCase.revoke(new InvitationId(id)).map(InvitationResponse::from);
    }
}
