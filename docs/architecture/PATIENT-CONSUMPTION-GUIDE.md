# Patient Consumption Guide

**Audience:** Developers building modules after FASE 17 (Appointment, MedicalRecord, Billing, …)  
**Authority:** [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
**Status:** Vigente desde PASO 17.8 — Clinical Foundation **cerrada**

---

## Mental model

FASE 17 no es “CRUD de pacientes”. FASE 17 entrega un **bounded context estable**: la identidad clínica registral del sujeto de cuidado.

```text
Tenant (IAM)
 └── Patient                    ← clinical registry identity (intentionally small)
      ├── demographics          ← display name, contacts, DOB
      ├── externalIdentifiers   ← optional typed keys (MRN, …)
      └── primaryOrganizationId ← optional OrganizationId (NOT ownership)
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Quién es el sujeto de cuidado? | `PatientId` |
| ¿Puedo enlazar un Appointment a este patient? | `PatientReferencePort.existsActiveByIdAndTenant` |
| ¿Dónde “pertenece” el patient? | Tenant (obligatorio) · primary org (opcional, agrupación) |
| ¿En qué office atiende? | **No en Patient** — en Appointment / Encounter |

---

## Decision tree (30 seconds)

```text
Need to store a link to a care subject?
  → Store PatientId on your aggregate + tenantId

Need to validate patient exists and is ACTIVE at write time?
  → PatientReferencePort (`patient-contract`, ADR-013)
  → Never PatientRepository, never SQL against clinical.patient from another BC

Need demographics / MRN for display?
  → Prefer your own read model / Query Port later — do not import Patient aggregate

Need office / staff / scheduling?
  → Wrong BC — use Appointment + StaffAssignmentId / OfficeId
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:patient-management:patient-contract"))
```

Only. Never `patient-application` or `patient-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `PatientId` + `tenantId` | `@Autowired R2dbcPatientRepository` |
| Validate via `PatientReferencePort` | `SELECT * FROM clinical.patient` outside Patient module |
| Filter by JWT `tenantId` | Accept client-sent `tenantId` as authority |
| Treat Patient as registry identity | Put Appointment / Notes / Billing fields on Patient |

---

## Contract surface (FASE 17 closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `PatientId` | `patient-contract` → domain VO | Hard identity |
| `PatientPermissionCatalog` | `patient-contract` | `patient:create\|read\|update\|archive` |
| `PatientReferencePort` | `patient-contract` | ACTIVE + tenant existence check |
| `R2dbcPatientReferenceAdapter` | `patient-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface PatientReferencePort {
    Mono<Boolean> existsActiveByIdAndTenant(PatientId patientId, TenantId tenantId);
}
```

Archived patients → `false` (blocks **new** links; historical reads may need a separate method later if a consumer invariant requires it).

---

## Module recipes

### Appointment (FASE 18)

**Owns:** scheduled encounter  
**References:** `PatientId` (required), `StaffAssignmentId`, `OrganizationId`, optional `OfficeId`

```text
Appointment
  patientId           ← validate ACTIVE via PatientReferencePort
  staffAssignmentId
  organizationId
  officeId?
```

**Never:** embed Patient demographics in Appointment aggregate as source of truth.

### Medical Record (FASE 19)

**Owns:** clinical documentation  
**References:** `PatientId` (subject), `OrganizationId` (custodian)

### Billing (FASE 21)

**Owns:** charges / invoices  
**References:** `PatientId` when clinically billed; never owns Patient lifecycle.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `PatientReferencePort` in `patient-contract` |
| **Frontend / mobile** | `/api/v1/clinical/patients` for admin; business APIs return embedded `patientId` |
| **Reporting** | Separate read models — not ad-hoc joins into `clinical` from other BCs |

Patient Administration API is for **tenant admins**, not for Appointment module internals.

---

## OpenAPI

Grupo springdoc: **`clinical-administration`**

```text
GET /v3/api-docs/clinical-administration
```

Paths: `/api/v1/clinical/patients` (+ archive/activate).

---

## Testing consumers

- Mock `PatientReferencePort` in unit / module tests  
- Do **not** load full Patient infrastructure unless E2E  
- Testcontainers `clinical` schema only in cross-BC ITs  

---

## Checklist before merging a consumer of Patient

| # | Item |
|---|------|
| 1 | Gradle depends only on `patient-contract` |
| 2 | Aggregates store `PatientId`, not Patient entity |
| 3 | No SQL against `clinical.patient` outside Patient module |
| 4 | Write-time ACTIVE check via `PatientReferencePort` |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | Archived patient blocks **new** links |
| 7 | ADR-012 consulted — do not grow Patient for vertical needs |

---

## Related documents

- [ADR-012 — Patient Domain Model](ADR-012-PATIENT-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)
- [PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md](../audits/PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md)
- [PASO-17.7-PATIENT-VERIFICATION.md](../audits/PASO-17.7-PATIENT-VERIFICATION.md)
