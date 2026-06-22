package com.codecore.organization.domain.model.organization;

import com.codecore.organization.domain.exception.InvalidOrganizationStateException;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Organization aggregate root — tenant-scoped business structural unit (ADR-010).
 * <p>
 * Represents clinics, departments, or regional branches inside a tenant account.
 * {@code tenantId} is mandatory and immutable after creation.
 */
public final class Organization {

    private final OrganizationId id;
    private final TenantId tenantId;
    private final OrganizationCode code;
    private OrganizationName name;
    private OrganizationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Organization(
            OrganizationId id,
            TenantId tenantId,
            OrganizationCode code,
            OrganizationName name,
            OrganizationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Organization create(
            OrganizationId id,
            TenantId tenantId,
            OrganizationCode code,
            OrganizationName name,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Organization(
                id,
                tenantId,
                code,
                name,
                OrganizationStatus.ACTIVE,
                now,
                now
        );
    }

    public static Organization reconstitute(
            OrganizationId id,
            TenantId tenantId,
            OrganizationCode code,
            OrganizationName name,
            OrganizationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Organization(id, tenantId, code, name, status, createdAt, updatedAt);
    }

    public OrganizationId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public OrganizationCode code() {
        return code;
    }

    public OrganizationName name() {
        return name;
    }

    public OrganizationStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void rename(OrganizationName newName) {
        this.name = Objects.requireNonNull(newName, "newName");
        touch();
    }

    public void archive() {
        if (status == OrganizationStatus.ARCHIVED) {
            throw new InvalidOrganizationStateException("Organization is already archived");
        }
        this.status = OrganizationStatus.ARCHIVED;
        touch();
    }

    public void activate() {
        if (status == OrganizationStatus.ACTIVE) {
            throw new InvalidOrganizationStateException("Organization is already active");
        }
        this.status = OrganizationStatus.ACTIVE;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
