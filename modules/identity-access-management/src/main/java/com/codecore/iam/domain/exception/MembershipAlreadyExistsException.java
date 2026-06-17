package com.codecore.iam.domain.exception;

public class MembershipAlreadyExistsException extends IamDomainException {

    public MembershipAlreadyExistsException(String message) {
        super(message);
    }
}
