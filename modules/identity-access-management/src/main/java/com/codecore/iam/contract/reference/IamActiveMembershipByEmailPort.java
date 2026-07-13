package com.codecore.iam.contract.reference;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference: ACTIVE membership for email + tenant.
 */
public interface IamActiveMembershipByEmailPort {

    Mono<Boolean> existsActiveByEmailAndTenant(EmailAddress email, TenantId tenantId);
}
