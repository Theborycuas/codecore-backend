package com.codecore.appointment.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC row mapping for {@code scheduling.appointment}.
 */
@Table(name = "appointment", schema = "scheduling")
public class AppointmentEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity;

    @Id
    @Column("appointment_id")
    private UUID appointmentId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("patient_id")
    private UUID patientId;

    @Column("staff_assignment_id")
    private UUID staffAssignmentId;

    @Column("organization_id")
    private UUID organizationId;

    @Column("office_id")
    private UUID officeId;

    @Column("starts_at")
    private Instant startsAt;

    @Column("ends_at")
    private Instant endsAt;

    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    public AppointmentEntity() {
    }

    @Override
    public UUID getId() {
        return appointmentId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getStaffAssignmentId() {
        return staffAssignmentId;
    }

    public void setStaffAssignmentId(UUID staffAssignmentId) {
        this.staffAssignmentId = staffAssignmentId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getOfficeId() {
        return officeId;
    }

    public void setOfficeId(UUID officeId) {
        this.officeId = officeId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
