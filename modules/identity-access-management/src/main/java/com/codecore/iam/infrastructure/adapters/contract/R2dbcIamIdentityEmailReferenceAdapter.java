package com.codecore.iam.infrastructure.adapters.contract;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.contract.reference.IamIdentityEmailReferencePort;
import com.codecore.iam.domain.valueobject.EmailAddress;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class R2dbcIamIdentityEmailReferenceAdapter implements IamIdentityEmailReferencePort {

    private final IdentityRepository identityRepository;

    public R2dbcIamIdentityEmailReferenceAdapter(IdentityRepository identityRepository) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
    }

    @Override
    public Mono<Boolean> existsByEmail(EmailAddress email) {
        return identityRepository.existsByEmail(email);
    }
}
