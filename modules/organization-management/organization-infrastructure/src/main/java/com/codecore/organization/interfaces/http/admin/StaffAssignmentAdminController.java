package com.codecore.organization.interfaces.http.admin;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import com.codecore.organization.application.command.CreateStaffAssignmentCommand;
import com.codecore.organization.application.command.UpdateStaffAssignmentCommand;
import com.codecore.organization.application.port.in.CreateStaffAssignmentUseCase;
import com.codecore.organization.application.port.in.DeleteStaffAssignmentUseCase;
import com.codecore.organization.application.port.in.GetStaffAssignmentUseCase;
import com.codecore.organization.application.port.in.ListStaffAssignmentsUseCase;
import com.codecore.organization.application.port.in.UpdateStaffAssignmentUseCase;
import com.codecore.organization.application.query.PageQueryParser;
import com.codecore.organization.application.query.StaffAssignmentListFilter;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.interfaces.http.admin.dto.CreateStaffAssignmentRequest;
import com.codecore.organization.interfaces.http.admin.dto.PagedStaffAssignmentResponse;
import com.codecore.organization.interfaces.http.admin.dto.StaffAssignmentResponse;
import com.codecore.organization.interfaces.http.admin.dto.UpdateStaffAssignmentRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(OrgAdminApiPaths.STAFF_ASSIGNMENTS)
@Tag(name = "Staff Assignments", description = "Staff assignment administration (`staff-assignment:*` permissions)")
public class StaffAssignmentAdminController {

    private final ListStaffAssignmentsUseCase listStaffAssignmentsUseCase;
    private final GetStaffAssignmentUseCase getStaffAssignmentUseCase;
    private final CreateStaffAssignmentUseCase createStaffAssignmentUseCase;
    private final UpdateStaffAssignmentUseCase updateStaffAssignmentUseCase;
    private final DeleteStaffAssignmentUseCase deleteStaffAssignmentUseCase;

    public StaffAssignmentAdminController(
            ListStaffAssignmentsUseCase listStaffAssignmentsUseCase,
            GetStaffAssignmentUseCase getStaffAssignmentUseCase,
            CreateStaffAssignmentUseCase createStaffAssignmentUseCase,
            UpdateStaffAssignmentUseCase updateStaffAssignmentUseCase,
            DeleteStaffAssignmentUseCase deleteStaffAssignmentUseCase
    ) {
        this.listStaffAssignmentsUseCase = listStaffAssignmentsUseCase;
        this.getStaffAssignmentUseCase = getStaffAssignmentUseCase;
        this.createStaffAssignmentUseCase = createStaffAssignmentUseCase;
        this.updateStaffAssignmentUseCase = updateStaffAssignmentUseCase;
        this.deleteStaffAssignmentUseCase = deleteStaffAssignmentUseCase;
    }

    @GetMapping
    @RequiresPermission("staff-assignment:read")
    public Mono<PagedStaffAssignmentResponse> listStaffAssignments(
            @RequestParam(required = false) UUID membershipId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID officeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        StaffAssignmentListFilter filter = StaffAssignmentListFilter.of(
                membershipId != null ? new MembershipId(membershipId) : null,
                organizationId != null ? new OrganizationId(organizationId) : null,
                officeId != null ? new OfficeId(officeId) : null
        );
        return listStaffAssignmentsUseCase
                .execute(filter, PageQueryParser.parseStaffAssignmentPageQuery(page, size, sort))
                .map(PagedStaffAssignmentResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("staff-assignment:read")
    public Mono<StaffAssignmentResponse> getStaffAssignment(@PathVariable UUID id) {
        return getStaffAssignmentUseCase.execute(new StaffAssignmentId(id)).map(StaffAssignmentResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("staff-assignment:create")
    public Mono<StaffAssignmentResponse> createStaffAssignment(
            @Valid @RequestBody CreateStaffAssignmentRequest request
    ) {
        CreateStaffAssignmentCommand command = new CreateStaffAssignmentCommand(
                request.membershipId(),
                request.organizationId(),
                request.officeId()
        );
        return createStaffAssignmentUseCase.execute(command).map(StaffAssignmentResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("staff-assignment:update")
    public Mono<StaffAssignmentResponse> updateStaffAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffAssignmentRequest request
    ) {
        UpdateStaffAssignmentCommand command = new UpdateStaffAssignmentCommand(
                new StaffAssignmentId(id),
                request.organizationId(),
                request.officeId()
        );
        return updateStaffAssignmentUseCase.execute(command).map(StaffAssignmentResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("staff-assignment:delete")
    public Mono<Void> deleteStaffAssignment(@PathVariable UUID id) {
        return deleteStaffAssignmentUseCase.delete(new StaffAssignmentId(id));
    }
}
