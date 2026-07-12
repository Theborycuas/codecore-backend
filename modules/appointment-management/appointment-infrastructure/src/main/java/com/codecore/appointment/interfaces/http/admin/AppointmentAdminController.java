package com.codecore.appointment.interfaces.http.admin;

import com.codecore.appointment.application.command.CreateAppointmentCommand;
import com.codecore.appointment.application.command.UpdateAppointmentCommand;
import com.codecore.appointment.application.port.in.CancelAppointmentUseCase;
import com.codecore.appointment.application.port.in.CompleteAppointmentUseCase;
import com.codecore.appointment.application.port.in.CreateAppointmentUseCase;
import com.codecore.appointment.application.port.in.GetAppointmentUseCase;
import com.codecore.appointment.application.port.in.ListAppointmentsUseCase;
import com.codecore.appointment.application.port.in.UpdateAppointmentUseCase;
import com.codecore.appointment.application.query.AppointmentListQuery;
import com.codecore.appointment.application.query.PageQueryParser;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.interfaces.http.admin.dto.AppointmentResponse;
import com.codecore.appointment.interfaces.http.admin.dto.CreateAppointmentRequest;
import com.codecore.appointment.interfaces.http.admin.dto.PagedAppointmentResponse;
import com.codecore.appointment.interfaces.http.admin.dto.UpdateAppointmentRequest;
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
@RequestMapping(AppointmentAdminApiPaths.APPOINTMENTS)
@Tag(name = "Appointments", description = "Appointment scheduling administration (`appointment:*` permissions)")
public class AppointmentAdminController {

    private final ListAppointmentsUseCase listAppointmentsUseCase;
    private final GetAppointmentUseCase getAppointmentUseCase;
    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final UpdateAppointmentUseCase updateAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final CompleteAppointmentUseCase completeAppointmentUseCase;

    public AppointmentAdminController(
            ListAppointmentsUseCase listAppointmentsUseCase,
            GetAppointmentUseCase getAppointmentUseCase,
            CreateAppointmentUseCase createAppointmentUseCase,
            UpdateAppointmentUseCase updateAppointmentUseCase,
            CancelAppointmentUseCase cancelAppointmentUseCase,
            CompleteAppointmentUseCase completeAppointmentUseCase
    ) {
        this.listAppointmentsUseCase = listAppointmentsUseCase;
        this.getAppointmentUseCase = getAppointmentUseCase;
        this.createAppointmentUseCase = createAppointmentUseCase;
        this.updateAppointmentUseCase = updateAppointmentUseCase;
        this.cancelAppointmentUseCase = cancelAppointmentUseCase;
        this.completeAppointmentUseCase = completeAppointmentUseCase;
    }

    @GetMapping
    @RequiresPermission("appointment:read")
    public Mono<PagedAppointmentResponse> listAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startsAt,asc") String sort,
            @RequestParam(defaultValue = "SCHEDULED") String status,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID staffAssignmentId,
            @RequestParam(required = false) UUID officeId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        AppointmentListQuery filter = AppointmentListQuery.of(
                status,
                organizationId,
                patientId,
                staffAssignmentId,
                officeId,
                from,
                to
        );
        return listAppointmentsUseCase
                .execute(filter, PageQueryParser.parseAppointmentPageQuery(page, size, sort))
                .map(PagedAppointmentResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("appointment:read")
    public Mono<AppointmentResponse> getAppointment(@PathVariable UUID id) {
        return getAppointmentUseCase.execute(new AppointmentId(id)).map(AppointmentResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("appointment:create")
    public Mono<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return createAppointmentUseCase.execute(toCreateCommand(request)).map(AppointmentResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("appointment:update")
    public Mono<AppointmentResponse> updateAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        return updateAppointmentUseCase.execute(toUpdateCommand(id, request)).map(AppointmentResponse::from);
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("appointment:cancel")
    public Mono<AppointmentResponse> cancelAppointment(@PathVariable UUID id) {
        return cancelAppointmentUseCase.cancel(new AppointmentId(id)).map(AppointmentResponse::from);
    }

    @PostMapping("/{id}/complete")
    @RequiresPermission("appointment:update")
    public Mono<AppointmentResponse> completeAppointment(@PathVariable UUID id) {
        return completeAppointmentUseCase.complete(new AppointmentId(id)).map(AppointmentResponse::from);
    }

    private static CreateAppointmentCommand toCreateCommand(CreateAppointmentRequest request) {
        return new CreateAppointmentCommand(
                request.patientId(),
                request.staffAssignmentId(),
                request.organizationId(),
                request.officeId(),
                request.startsAt(),
                request.endsAt()
        );
    }

    private static UpdateAppointmentCommand toUpdateCommand(UUID id, UpdateAppointmentRequest request) {
        return new UpdateAppointmentCommand(
                new AppointmentId(id),
                request.patientId(),
                request.staffAssignmentId(),
                request.organizationId(),
                request.officeId(),
                request.startsAt(),
                request.endsAt()
        );
    }
}
