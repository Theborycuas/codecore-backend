package com.codecore.iam.infrastructure.adapters;

import com.codecore.iam.application.port.out.SendPasswordResetEmailPort;
import com.codecore.iam.domain.valueobject.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * No-op email adapter for local/dev — logs the reset token instead of sending mail.
 * Wired as a {@code @Bean} in {@code IamModuleConfiguration}.
 */
public class LoggingSendPasswordResetEmailAdapter implements SendPasswordResetEmailPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSendPasswordResetEmailAdapter.class);

    @Override
    public Mono<Void> send(EmailAddress email, String rawResetToken) {
        log.info(
                "Password reset email (noop): to={} token={} (dev/logging adapter — do not use in production)",
                email.value(),
                rawResetToken
        );
        return Mono.empty();
    }
}
