package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.interfaces.http.dto.CreateTenantHttpResponse;
import com.codecore.iam.interfaces.http.dto.CreateTenantRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/tenants")
public class CreateTenantController {

    private final CreateTenantUseCase createTenantUseCase;

    public CreateTenantController(CreateTenantUseCase createTenantUseCase) {
        this.createTenantUseCase = createTenantUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateTenantHttpResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        CreateTenantCommand command = new CreateTenantCommand(request.name());
        return createTenantUseCase.execute(command)
                .map(result -> new CreateTenantHttpResponse(
                        result.tenantId().value(),
                        result.name().value(),
                        result.status().name()
                ));
    }
}
