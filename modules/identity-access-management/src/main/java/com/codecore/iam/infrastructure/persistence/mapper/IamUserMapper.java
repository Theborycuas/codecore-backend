package com.codecore.iam.infrastructure.persistence.mapper;

import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.EmailVerifiedProjection;
import com.codecore.iam.infrastructure.persistence.entity.IamUserEntity;

/**
 * Isomorphic mapping between {@link IamUserEntity} and {@link Identity}.
 * <p>
 * Lifecycle master: {@link IdentityStatus} ↔ column {@code status}. Column {@code email_verified} is written via
 * {@link EmailVerifiedProjection} and is never read on {@link #toDomain(IamUserEntity)}.
 */
public final class IamUserMapper {

    public Identity toDomain(IamUserEntity entity) {
        IdentityId identityId = new IdentityId(entity.getId());
        IdentityStatus status = IdentityStatus.valueOf(entity.getStatus());

        Credential credential = new Credential(
                new CredentialId(entity.getId()),
                PasswordHash.ofHashedValue(entity.getPasswordHash()),
                null,
                null,
                status == IdentityStatus.PASSWORD_RESET_REQUIRED,
                0L
        );

        return new Identity(
                identityId,
                new TenantId(entity.getTenantId()),
                EmailAddress.of(entity.getNormalizedEmail()),
                status,
                credential,
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public IamUserEntity toEntity(Identity identity, boolean isNew) {
        String normalizedEmail = identity.email().value();

        IamUserEntity entity = new IamUserEntity();
        entity.setNewEntity(isNew);
        entity.setId(identity.id().value());
        entity.setTenantId(identity.tenantId().value());
        entity.setEmail(normalizedEmail);
        entity.setNormalizedEmail(normalizedEmail);
        entity.setPasswordHash(identity.credential()
                .orElseThrow(() -> new IllegalStateException("Identity has no credential to persist"))
                .passwordHash()
                .value());
        entity.setStatus(identity.status().name());
        entity.setEmailVerifiedProjection(EmailVerifiedProjection.fromStatus(identity.status()));
        entity.setLastLoginAt(identity.lastLoginAt());
        entity.setCreatedAt(identity.createdAt());
        entity.setUpdatedAt(identity.updatedAt());
        entity.setVersion(identity.version());
        return entity;
    }
}
