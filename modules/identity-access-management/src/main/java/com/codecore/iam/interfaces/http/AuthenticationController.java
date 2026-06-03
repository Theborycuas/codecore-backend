package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticateIdentityUseCase authenticateIdentityUseCase;

    public AuthenticationController(AuthenticateIdentityUseCase authenticateIdentityUseCase) {
        this.authenticateIdentityUseCase = authenticateIdentityUseCase;
    }

    @PostMapping("/login")
    public Mono<AuthenticationResponse> login(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody LoginRequest request
    ) {
        AuthenticationCommand command = new AuthenticationCommand(
                new TenantId(tenantId),
                request.email(),
                request.password()
        );
        return authenticateIdentityUseCase.execute(command);
    }
}
