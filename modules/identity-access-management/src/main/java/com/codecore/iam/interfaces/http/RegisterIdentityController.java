package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.dto.RegisterIdentityRequest;
import com.codecore.iam.interfaces.http.dto.RegisterIdentityResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/identities")
public class RegisterIdentityController {

    private final RegisterIdentityUseCase registerIdentityUseCase;

    public RegisterIdentityController(RegisterIdentityUseCase registerIdentityUseCase) {
        this.registerIdentityUseCase = registerIdentityUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterIdentityResponse> register(@Valid @RequestBody RegisterIdentityRequest request) {
        RegisterIdentityCommand command = new RegisterIdentityCommand(
                new TenantId(request.tenantId()),
                request.email(),
                request.password()
        );
        return registerIdentityUseCase.execute(command)
                .map(result -> new RegisterIdentityResponse(
                        result.identityId().value(),
                        result.tenantId().value(),
                        result.email().value(),
                        result.status().name()
                ));
    }
}
