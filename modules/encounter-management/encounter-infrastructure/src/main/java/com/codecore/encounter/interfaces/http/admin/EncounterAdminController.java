package com.codecore.encounter.interfaces.http.admin;

import com.codecore.encounter.application.command.CreateEncounterCommand;
import com.codecore.encounter.application.command.UpdateEncounterCommand;
import com.codecore.encounter.application.port.in.CancelEncounterUseCase;
import com.codecore.encounter.application.port.in.CompleteEncounterUseCase;
import com.codecore.encounter.application.port.in.CreateEncounterUseCase;
import com.codecore.encounter.application.port.in.GetEncounterUseCase;
import com.codecore.encounter.application.port.in.ListEncountersUseCase;
import com.codecore.encounter.application.port.in.UpdateEncounterUseCase;
import com.codecore.encounter.application.query.EncounterListQuery;
import com.codecore.encounter.application.query.PageQueryParser;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.interfaces.http.admin.dto.CompleteEncounterRequest;
import com.codecore.encounter.interfaces.http.admin.dto.CreateEncounterRequest;
import com.codecore.encounter.interfaces.http.admin.dto.EncounterResponse;
import com.codecore.encounter.interfaces.http.admin.dto.PagedEncounterResponse;
import com.codecore.encounter.interfaces.http.admin.dto.UpdateEncounterRequest;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(EncounterAdminApiPaths.ENCOUNTERS)
@Tag(name = "Encounters", description = "Encounter records administration (`encounter:*` permissions)")
public class EncounterAdminController {

    private final ListEncountersUseCase listEncountersUseCase;
    private final GetEncounterUseCase getEncounterUseCase;
    private final CreateEncounterUseCase createEncounterUseCase;
    private final UpdateEncounterUseCase updateEncounterUseCase;
    private final CancelEncounterUseCase cancelEncounterUseCase;
    private final CompleteEncounterUseCase completeEncounterUseCase;

    public EncounterAdminController(
            ListEncountersUseCase listEncountersUseCase,
            GetEncounterUseCase getEncounterUseCase,
            CreateEncounterUseCase createEncounterUseCase,
            UpdateEncounterUseCase updateEncounterUseCase,
            CancelEncounterUseCase cancelEncounterUseCase,
            CompleteEncounterUseCase completeEncounterUseCase
    ) {
        this.listEncountersUseCase = listEncountersUseCase;
        this.getEncounterUseCase = getEncounterUseCase;
        this.createEncounterUseCase = createEncounterUseCase;
        this.updateEncounterUseCase = updateEncounterUseCase;
        this.cancelEncounterUseCase = cancelEncounterUseCase;
        this.completeEncounterUseCase = completeEncounterUseCase;
    }

    @GetMapping
    @RequiresPermission("encounter:read")
    public Mono<PagedEncounterResponse> listEncounters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt,desc") String sort,
            @RequestParam(defaultValue = "IN_PROGRESS") String status,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID staffAssignmentId,
            @RequestParam(required = false) UUID officeId,
            @RequestParam(required = false) UUID appointmentId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        EncounterListQuery filter = EncounterListQuery.of(
                status,
                organizationId,
                patientId,
                staffAssignmentId,
                officeId,
                appointmentId,
                from,
                to
        );
        return listEncountersUseCase
                .execute(filter, PageQueryParser.parseEncounterPageQuery(page, size, sort))
                .map(PagedEncounterResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("encounter:read")
    public Mono<EncounterResponse> getEncounter(@PathVariable UUID id) {
        return getEncounterUseCase.execute(new EncounterId(id)).map(EncounterResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("encounter:create")
    public Mono<EncounterResponse> createEncounter(@Valid @RequestBody CreateEncounterRequest request) {
        return createEncounterUseCase.execute(toCreateCommand(request)).map(EncounterResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("encounter:update")
    public Mono<EncounterResponse> updateEncounter(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEncounterRequest request
    ) {
        return updateEncounterUseCase.execute(toUpdateCommand(id, request)).map(EncounterResponse::from);
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("encounter:cancel")
    public Mono<EncounterResponse> cancelEncounter(@PathVariable UUID id) {
        return cancelEncounterUseCase.cancel(new EncounterId(id)).map(EncounterResponse::from);
    }

    @PostMapping("/{id}/complete")
    @RequiresPermission("encounter:update")
    public Mono<EncounterResponse> completeEncounter(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteEncounterRequest request
    ) {
        Instant endedAt = request == null ? null : request.endedAt();
        return completeEncounterUseCase.complete(new EncounterId(id), endedAt).map(EncounterResponse::from);
    }

    private static CreateEncounterCommand toCreateCommand(CreateEncounterRequest request) {
        return new CreateEncounterCommand(
                request.patientId(),
                request.staffAssignmentId(),
                request.organizationId(),
                request.officeId(),
                request.appointmentId(),
                request.startedAt(),
                request.endedAt()
        );
    }

    private static UpdateEncounterCommand toUpdateCommand(UUID id, UpdateEncounterRequest request) {
        return new UpdateEncounterCommand(
                new EncounterId(id),
                request.patientId(),
                request.staffAssignmentId(),
                request.organizationId(),
                request.officeId(),
                request.appointmentId(),
                request.startedAt(),
                request.endedAt()
        );
    }
}
