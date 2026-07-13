package com.codecore.access.infrastructure.adapters;

import com.codecore.access.application.port.out.SendInvitationEmailPort;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * No-op email adapter for local/dev — logs the invitation token instead of sending mail.
 */
@Component
public class LoggingSendInvitationEmailAdapter implements SendInvitationEmailPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSendInvitationEmailAdapter.class);

    @Override
    public Mono<Void> send(TenantId tenantId, InvitationId invitationId, EmailAddress email, String rawToken) {
        log.info(
                "Invitation email (noop): tenant={} invitationId={} to={} token={} (dev/logging adapter — do not use in production)",
                tenantId.value(),
                invitationId.value(),
                email.value(),
                rawToken
        );
        return Mono.empty();
    }
}
