package com.codecore.iam.contract.reference;

import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference: ACTIVE membership exists for id + tenant.
 */
public interface IamMembershipReferencePort {

    Mono<Boolean> existsActiveByIdAndTenant(MembershipId membershipId, TenantId tenantId);
}
