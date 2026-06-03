package com.codecore.iam.domain.model.membership;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Association aggregate: links one {@link IdentityId} to one {@link TenantId} (N:M across rows).
 */
public final class IdentityTenantMembership {

    private final MembershipId id;
    private final IdentityId identityId;
    private final TenantId tenantId;
    private MembershipStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public IdentityTenantMembership(
            MembershipId id,
            IdentityId identityId,
            TenantId tenantId,
            MembershipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.identityId = Objects.requireNonNull(identityId, "identityId");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static IdentityTenantMembership create(
            IdentityId identityId,
            TenantId tenantId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new IdentityTenantMembership(
                MembershipId.generate(),
                identityId,
                tenantId,
                MembershipStatus.ACTIVE,
                now,
                now
        );
    }

    public MembershipId id() {
        return id;
    }

    public IdentityId identityId() {
        return identityId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public MembershipStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void deactivate() {
        this.status = MembershipStatus.INACTIVE;
        touch();
    }

    public void activate() {
        this.status = MembershipStatus.ACTIVE;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
