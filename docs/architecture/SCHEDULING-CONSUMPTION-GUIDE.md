# Scheduling Consumption Guide

**Audience:** Developers building modules after FASE 18 (Encounter, MedicalRecord, Notifications, Billing, …)  
**Authority:** [ADR-014](ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
**Status:** Vigente desde PASO 18.8 — Scheduling **cerrada**

---

## Mental model

FASE 18 no es “CRUD de citas”. FASE 18 entrega un **bounded context estable**: el compromiso planificado de atención en el tiempo (*intentionally small*).

```text
Tenant (IAM)
 └── Appointment                 ← planned commitment (intentionally small)
      ├── patientId              ← care subject (PatientId)
      ├── staffAssignmentId      ← provider assignment
      ├── organizationId         ← operational org (denormalized)
      ├── officeId?              ← optional place
      ├── timeWindow             ← startsAt / endsAt
      └── status                 ← SCHEDULED → CANCELLED | COMPLETED
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Cuál es el compromiso planificado? | `AppointmentId` |
| ¿Puedo enlazar un Encounter / reminder a esta cita abierta? | `AppointmentReferencePort.existsScheduledByIdAndTenant` |
| ¿Quién es el sujeto? | `PatientId` → `PatientReferencePort` |
| ¿Quién atiende / dónde? | `StaffAssignmentId` / `OfficeId` → Org ports |
| ¿El episodio clínico real? | **No en Appointment** — Encounter / Records |

---

## Decision tree (30 seconds)

```text
Need to store a link to a planned commitment?
  → Store AppointmentId on your aggregate + tenantId

Need to validate appointment exists and is SCHEDULED at write time?
  → AppointmentReferencePort (`appointment-contract`, ADR-013)
  → Never AppointmentRepository, never SQL against scheduling.appointment from another BC

Need patient / staff / office labels for display?
  → Prefer your own read model / Query Port later — do not import Appointment aggregate

Need Encounter / clinical notes / billing lines?
  → Wrong BC — grow around Appointment via IDs + ReferencePorts
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:appointment-management:appointment-contract"))
```

Only. Never `appointment-application` or `appointment-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `AppointmentId` + `tenantId` | `@Autowired R2dbcAppointmentRepository` |
| Validate via `AppointmentReferencePort` | `SELECT * FROM scheduling.appointment` outside Scheduling |
| Filter by JWT `tenantId` | Accept client-sent `tenantId` as authority |
| Treat Appointment as planned commitment | Put Encounter / Notes / Billing fields on Appointment |

---

## Contract surface (FASE 18 closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `AppointmentId` | `appointment-contract` → domain VO | Hard identity |
| `AppointmentPermissionCatalog` | `appointment-contract` | `appointment:create\|read\|update\|cancel` |
| `AppointmentReferencePort` | `appointment-contract` | SCHEDULED + tenant existence check |
| `R2dbcAppointmentReferenceAdapter` | `appointment-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface AppointmentReferencePort {
    Mono<Boolean> existsScheduledByIdAndTenant(AppointmentId appointmentId, TenantId tenantId);
}
```

`CANCELLED` / `COMPLETED` → `false` (blocks **new** operational links to open commitments).  
Historical existence for completed appointments → separate `existsByIdAndTenant` **only if** a consumer invariant requires it (ADR-013).

---

## Module recipes

### Encounter / Clinical episode (post-18)

**Owns:** occurred care episode  
**References:** `AppointmentId?` (optional origin), `PatientId`, org/office as needed

```text
Encounter
  appointmentId?     ← validate SCHEDULED via AppointmentReferencePort when linking an open commitment
  patientId
```

**Never:** embed Appointment time window as source of truth after the episode starts — Encounter owns occurrence.

### Medical Record (FASE 19)

**Owns:** clinical documentation  
**References:** `PatientId` primarily; Appointment only if the record is explicitly tied to a planned visit.

### Billing (FASE 21)

**Owns:** charges / invoices  
**References:** `AppointmentId` when billed against a visit commitment; never owns Appointment lifecycle.

### Notifications

**Owns:** delivery  
**References:** `AppointmentId` for reminders while `SCHEDULED`.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `AppointmentReferencePort` in `appointment-contract` |
| **Frontend / mobile** | `/api/v1/scheduling/appointments` for admin; business APIs return embedded `appointmentId` |
| **Reporting** | Separate read models — not ad-hoc joins into `scheduling` from other BCs |

Appointment Administration API is for **tenant admins**, not for Encounter module internals.

---

## OpenAPI

Grupo springdoc: **`scheduling-administration`**

```text
GET /v3/api-docs/scheduling-administration
```

Paths: `/api/v1/scheduling/appointments` (+ cancel / complete).

---

## Testing consumers

- Mock `AppointmentReferencePort` in unit / module tests  
- Do **not** load full Appointment infrastructure unless E2E  
- Testcontainers `scheduling` schema only in cross-BC ITs  

---

## Checklist before merging a consumer of Appointment

| # | Item |
|---|------|
| 1 | Gradle depends only on `appointment-contract` |
| 2 | Aggregates store `AppointmentId`, not Appointment entity |
| 3 | No SQL against `scheduling.appointment` outside Scheduling |
| 4 | Write-time SCHEDULED check via `AppointmentReferencePort` |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | CANCELLED / COMPLETED blocks **new** operational links (default port) |
| 7 | ADR-014 consulted — do not grow Appointment for vertical needs |

---

## Related documents

- [ADR-014 — Appointment Domain Model](ADR-014-APPOINTMENT-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [PATIENT-CONSUMPTION-GUIDE.md](PATIENT-CONSUMPTION-GUIDE.md)
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)
- [PASO-18.8-SCHEDULING-CLOSEOUT.md](../audits/PASO-18.8-SCHEDULING-CLOSEOUT.md)
- [PASO-18.7-APPOINTMENT-VERIFICATION.md](../audits/PASO-18.7-APPOINTMENT-VERIFICATION.md)
