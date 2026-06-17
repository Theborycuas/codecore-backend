package com.codecore.iam.domain.exception;

/**
 * Caller lacks ownership hierarchy to mutate the target resource (PASO 15.0.1).
 */
public class OwnershipDeniedException extends IamDomainException {

    public OwnershipDeniedException(String message) {
        super(message);
    }
}
