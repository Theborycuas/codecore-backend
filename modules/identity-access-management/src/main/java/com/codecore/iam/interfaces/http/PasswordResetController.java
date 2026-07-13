package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.command.CompletePasswordResetCommand;
import com.codecore.iam.application.command.RequestPasswordResetCommand;
import com.codecore.iam.application.port.in.CompletePasswordResetUseCase;
import com.codecore.iam.application.port.in.RequestPasswordResetUseCase;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.security.Sha256TokenHasher;
import com.codecore.iam.interfaces.http.dto.ForgotPasswordRequest;
import com.codecore.iam.interfaces.http.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordResetController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final CompletePasswordResetUseCase completePasswordResetUseCase;

    public PasswordResetController(
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            CompletePasswordResetUseCase completePasswordResetUseCase
    ) {
        this.requestPasswordResetUseCase = Objects.requireNonNull(
                requestPasswordResetUseCase,
                "requestPasswordResetUseCase"
        );
        this.completePasswordResetUseCase = Objects.requireNonNull(
                completePasswordResetUseCase,
                "completePasswordResetUseCase"
        );
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        TenantId tenantId = request.tenantId() == null ? null : new TenantId(request.tenantId());
        RequestPasswordResetCommand command = new RequestPasswordResetCommand(
                tenantId,
                EmailAddress.of(request.email())
        );
        return requestPasswordResetUseCase.execute(command);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        TenantId tenantId = request.tenantId() == null ? null : new TenantId(request.tenantId());
        String tokenHash = Sha256TokenHasher.hash(request.token());
        CompletePasswordResetCommand command = new CompletePasswordResetCommand(
                tenantId,
                ResetTokenHash.ofHashedValue(tokenHash),
                RawPassword.of(request.password())
        );
        return completePasswordResetUseCase.execute(command);
    }
}
