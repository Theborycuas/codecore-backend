package com.codecore.iam.contract.reference;

import com.codecore.iam.domain.valueobject.EmailAddress;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference: identity exists for a globally unique email (ADR-006).
 */
public interface IamIdentityEmailReferencePort {

    Mono<Boolean> existsByEmail(EmailAddress email);
}
