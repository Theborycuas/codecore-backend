package com.codecore.patient.interfaces.http.admin;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import com.codecore.patient.application.command.CreatePatientCommand;
import com.codecore.patient.application.command.UpdatePatientCommand;
import com.codecore.patient.application.port.in.ActivatePatientUseCase;
import com.codecore.patient.application.port.in.ArchivePatientUseCase;
import com.codecore.patient.application.port.in.CreatePatientUseCase;
import com.codecore.patient.application.port.in.GetPatientUseCase;
import com.codecore.patient.application.port.in.ListPatientsUseCase;
import com.codecore.patient.application.port.in.UpdatePatientUseCase;
import com.codecore.patient.application.query.PageQueryParser;
import com.codecore.patient.application.query.PatientListQuery;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.interfaces.http.admin.dto.CreatePatientRequest;
import com.codecore.patient.interfaces.http.admin.dto.ExternalIdentifierRequest;
import com.codecore.patient.interfaces.http.admin.dto.PagedPatientResponse;
import com.codecore.patient.interfaces.http.admin.dto.PatientResponse;
import com.codecore.patient.interfaces.http.admin.dto.UpdatePatientRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(PatientAdminApiPaths.PATIENTS)
@Tag(name = "Patients", description = "Patient clinical registry administration (`patient:*` permissions)")
public class PatientAdminController {

    private final ListPatientsUseCase listPatientsUseCase;
    private final GetPatientUseCase getPatientUseCase;
    private final CreatePatientUseCase createPatientUseCase;
    private final UpdatePatientUseCase updatePatientUseCase;
    private final ArchivePatientUseCase archivePatientUseCase;
    private final ActivatePatientUseCase activatePatientUseCase;

    public PatientAdminController(
            ListPatientsUseCase listPatientsUseCase,
            GetPatientUseCase getPatientUseCase,
            CreatePatientUseCase createPatientUseCase,
            UpdatePatientUseCase updatePatientUseCase,
            ArchivePatientUseCase archivePatientUseCase,
            ActivatePatientUseCase activatePatientUseCase
    ) {
        this.listPatientsUseCase = listPatientsUseCase;
        this.getPatientUseCase = getPatientUseCase;
        this.createPatientUseCase = createPatientUseCase;
        this.updatePatientUseCase = updatePatientUseCase;
        this.archivePatientUseCase = archivePatientUseCase;
        this.activatePatientUseCase = activatePatientUseCase;
    }

    @GetMapping
    @RequiresPermission("patient:read")
    public Mono<PagedPatientResponse> listPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID primaryOrganizationId,
            @RequestParam(required = false) String externalIdentifierType,
            @RequestParam(required = false) String externalIdentifierValue
    ) {
        PatientListQuery filter = PatientListQuery.of(
                status,
                q,
                primaryOrganizationId,
                externalIdentifierType,
                externalIdentifierValue
        );
        return listPatientsUseCase
                .execute(filter, PageQueryParser.parsePatientPageQuery(page, size, sort))
                .map(PagedPatientResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("patient:read")
    public Mono<PatientResponse> getPatient(@PathVariable UUID id) {
        return getPatientUseCase.execute(new PatientId(id)).map(PatientResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("patient:create")
    public Mono<PatientResponse> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        return createPatientUseCase.execute(toCreateCommand(request)).map(PatientResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("patient:update")
    public Mono<PatientResponse> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        return updatePatientUseCase.execute(toUpdateCommand(id, request)).map(PatientResponse::from);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("patient:archive")
    public Mono<PatientResponse> archivePatient(@PathVariable UUID id) {
        return archivePatientUseCase.archive(new PatientId(id)).map(PatientResponse::from);
    }

    @PostMapping("/{id}/activate")
    @RequiresPermission("patient:update")
    public Mono<PatientResponse> activatePatient(@PathVariable UUID id) {
        return activatePatientUseCase.activate(new PatientId(id)).map(PatientResponse::from);
    }

    private static CreatePatientCommand toCreateCommand(CreatePatientRequest request) {
        return new CreatePatientCommand(
                request.displayName(),
                request.contactEmail(),
                request.contactPhone(),
                request.dateOfBirth(),
                request.primaryOrganizationId(),
                toExternalInputs(request.externalIdentifiers())
        );
    }

    private static UpdatePatientCommand toUpdateCommand(UUID id, UpdatePatientRequest request) {
        return new UpdatePatientCommand(
                new PatientId(id),
                request.displayName(),
                request.contactEmail(),
                request.contactPhone(),
                request.dateOfBirth(),
                request.primaryOrganizationId(),
                toExternalInputs(request.externalIdentifiers())
        );
    }

    private static List<CreatePatientCommand.ExternalIdentifierInput> toExternalInputs(
            List<ExternalIdentifierRequest> identifiers
    ) {
        if (identifiers == null) {
            return List.of();
        }
        return identifiers.stream()
                .map(item -> new CreatePatientCommand.ExternalIdentifierInput(item.type(), item.value()))
                .toList();
    }
}
