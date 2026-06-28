package com.codecore.organization.domain.model.office;

import com.codecore.organization.domain.exception.InvalidOfficeStateException;
import com.codecore.organization.domain.valueobject.OfficeCode;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeName;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Office aggregate root — operational unit under an organization (ADR-010).
 */
public final class Office {

    private final OfficeId id;
    private final TenantId tenantId;
    private final OrganizationId organizationId;
    private final OfficeCode code;
    private OfficeName name;
    private OfficeStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Office(
            OfficeId id,
            TenantId tenantId,
            OrganizationId organizationId,
            OfficeCode code,
            OfficeName name,
            OfficeStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Office create(
            OfficeId id,
            TenantId tenantId,
            OrganizationId organizationId,
            OfficeCode code,
            OfficeName name,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Office(
                id,
                tenantId,
                organizationId,
                code,
                name,
                OfficeStatus.ACTIVE,
                now,
                now
        );
    }

    public static Office reconstitute(
            OfficeId id,
            TenantId tenantId,
            OrganizationId organizationId,
            OfficeCode code,
            OfficeName name,
            OfficeStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Office(id, tenantId, organizationId, code, name, status, createdAt, updatedAt);
    }

    public OfficeId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public OfficeCode code() {
        return code;
    }

    public OfficeName name() {
        return name;
    }

    public OfficeStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void rename(OfficeName newName) {
        this.name = Objects.requireNonNull(newName, "newName");
        touch();
    }

    public void archive() {
        if (status == OfficeStatus.ARCHIVED) {
            throw new InvalidOfficeStateException("Office is already archived");
        }
        this.status = OfficeStatus.ARCHIVED;
        touch();
    }

    public void activate() {
        if (status == OfficeStatus.ACTIVE) {
            throw new InvalidOfficeStateException("Office is already active");
        }
        this.status = OfficeStatus.ACTIVE;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
