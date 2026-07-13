package com.codecore.iam.infrastructure.persistence.mapper;

import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.PasswordResetRequestId;
import com.codecore.iam.domain.valueobject.PasswordResetStatus;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TokenExpiration;
import com.codecore.iam.infrastructure.persistence.entity.IamPasswordResetRequestEntity;

/**
 * Isomorphic mapping between {@link IamPasswordResetRequestEntity} and {@link PasswordResetRequest}.
 */
public final class IamPasswordResetRequestMapper {

    public PasswordResetRequest toDomain(IamPasswordResetRequestEntity entity) {
        long version = entity.getVersion() == null ? 0L : entity.getVersion();
        return new PasswordResetRequest(
                new PasswordResetRequestId(entity.getId()),
                new TenantId(entity.getTenantId()),
                new IdentityId(entity.getIdentityId()),
                ResetTokenHash.ofHashedValue(entity.getTokenHash()),
                TokenExpiration.at(entity.getExpiresAt()),
                PasswordResetStatus.valueOf(entity.getStatus()),
                entity.getUsedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                version
        );
    }

    public IamPasswordResetRequestEntity toEntity(PasswordResetRequest request, boolean isNew) {
        IamPasswordResetRequestEntity entity = new IamPasswordResetRequestEntity();
        entity.setNewEntity(isNew);
        entity.setId(request.id().value());
        entity.setTenantId(request.tenantId().value());
        entity.setIdentityId(request.identityId().value());
        entity.setTokenHash(request.resetTokenHash().value());
        entity.setExpiresAt(request.expiration().expiresAt());
        entity.setStatus(request.status().name());
        entity.setUsedAt(request.usedAt());
        entity.setCreatedAt(request.createdAt());
        entity.setUpdatedAt(request.updatedAt());
        if (!isNew) {
            entity.setVersion(request.version());
        }
        return entity;
    }
}
