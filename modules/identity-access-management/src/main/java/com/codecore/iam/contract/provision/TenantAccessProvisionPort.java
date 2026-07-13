package com.codecore.iam.contract.provision;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Provisions tenant access for an email: link existing identity or register a new one,
 * then assign an allowed system role.
 * <p>
 * Behavior:
 * <ul>
 *   <li>If an ACTIVE membership already exists for email + tenant → error</li>
 *   <li>If Identity exists by email → create membership linking to it (password not required)</li>
 *   <li>Else require a non-null password and create Identity + Credential + Membership</li>
 *   <li>Assign system role by code: {@code ADMIN}, {@code MANAGER}, {@code USER}, {@code READ_ONLY} only
 *       — never {@code OWNER}</li>
 *   <li>Returns the created/linked {@link MembershipId}</li>
 * </ul>
 */
public interface TenantAccessProvisionPort {

    record ProvisionTenantAccessCommand(
            TenantId tenantId,
            EmailAddress email,
            String roleCode,
            RawPassword passwordOrNull,
            Instant now
    ) {
    }

    Mono<MembershipId> provision(ProvisionTenantAccessCommand cmd);
}
