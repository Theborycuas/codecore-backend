# Clinical Records Consumption Guide

**Audience:** Developers building modules after FASE 19 (ClinicalDocument / Notes, Labs, Billing, Consent, …)  
**Authority:** [ADR-015](ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-014](ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md)  
**Status:** Vigente desde PASO 19.8 — Clinical Records **cerrada** (slice Encounter)

---

## Mental model

FASE 19 no es “CRUD de visitas clínicas”. FASE 19 entrega un **bounded context estable**: el episodio de atención **ocurrido** (*intentionally small*).

```text
Tenant (IAM)
 └── Encounter                   ← occurred care episode (intentionally small)
      ├── patientId              ← care subject (PatientId)
      ├── staffAssignmentId      ← who operated
      ├── organizationId         ← episode org (denormalized)
      ├── officeId?              ← optional place
      ├── appointmentId?         ← optional planned origin
      ├── timeBounds             ← startedAt / endedAt?
      └── status                 ← IN_PROGRESS → CANCELLED | COMPLETED
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Cuál es el episodio ocurrido? | `EncounterId` |
| ¿Puedo adjuntar docs a un episodio **abierto**? | `EncounterReferencePort.existsInProgressByIdAndTenant` |
| ¿Puedo enlazar Notes / Labs / Billing a este episodio? | `EncounterReferencePort.findLinkableByIdAndTenant` (IN_PROGRESS \| COMPLETED + `patientId`) |
| ¿Quién es el sujeto? | `PatientId` → `PatientReferencePort` |
| ¿De qué cita surgió? | `AppointmentId?` → `AppointmentReferencePort` (opcional) |
| ¿Notas / SOAP / odontogram? | **No en Encounter** — futuros aggregates |

---

## Decision tree (30 seconds)

```text
Need to store a link to an occurred care episode?
  → Store EncounterId on your aggregate + tenantId

Need to validate encounter is still open (IN_PROGRESS) at write time?
  → EncounterReferencePort.existsInProgressByIdAndTenant
  → Never EncounterRepository, never SQL against records.encounter from another BC

Need to link Notes / Labs / Billing (open or completed episode)?
  → EncounterReferencePort.findLinkableByIdAndTenant → check view.patientId
  → CANCELLED → empty (blocks new clinical/billing links)

Need patient / staff / office labels for display?
  → Prefer your own read model / Query Port later — do not import Encounter aggregate

Need SOAP / odontogram / prescriptions inside Encounter?
  → Wrong — grow around Encounter via IDs + ReferencePorts (ADR-015 §3)
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:encounter-management:encounter-contract"))
```

Only. Never `encounter-application` or `encounter-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `EncounterId` + `tenantId` | `@Autowired R2dbcEncounterRepository` |
| Validate via `EncounterReferencePort` | `SELECT * FROM records.encounter` outside Records |
| Filter by JWT `tenantId` | Accept client-sent `tenantId` as authority |
| Treat Encounter as occurred episode | Put Notes / SOAP / Billing fields on Encounter |

---

## Contract surface (Clinical Records closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `EncounterId` | `encounter-contract` → domain VO | Hard identity |
| `EncounterPermissionCatalog` | `encounter-contract` | `encounter:create\|read\|update\|cancel` |
| `EncounterReferencePort` | `encounter-contract` | IN_PROGRESS check + clinical linkable view |
| `EncounterReferenceView` | `encounter-contract` | `encounterId` · `patientId` · `status` |
| `R2dbcEncounterReferenceAdapter` | `encounter-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface EncounterReferencePort {
    Mono<Boolean> existsInProgressByIdAndTenant(EncounterId encounterId, TenantId tenantId);

    Mono<Optional<EncounterReferenceView>> findLinkableByIdAndTenant(
            EncounterId encounterId,
            TenantId tenantId
    );
}
```

`existsInProgress…`: `CANCELLED` / `COMPLETED` → `false` (open-episode links only).  
`findLinkable…`: `IN_PROGRESS` \| `COMPLETED` → view; `CANCELLED` / unknown → empty.

---

## Module recipes

### ClinicalDocument / Notes (post-19)

**Owns:** clinical documentation artifacts  
**References:** `EncounterId` (episode), `PatientId` (subject)

```text
Note / ClinicalDocument
  encounterId      ← findLinkableByIdAndTenant; view.patientId must match
  patientId
```

**Never:** embed SOAP text inside Encounter aggregate.

### Labs / Observations

**Owns:** results / observations  
**References:** `EncounterId` when collected in an episode; `PatientId` always.

### Billing (FASE 21)

**Owns:** charges / invoices  
**References:** `EncounterId` when billed against an occurred episode; never owns Encounter lifecycle.

### Consent / Attachments

**Owns:** consent / media metadata  
**References:** `EncounterId` when scoped to an episode.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `EncounterReferencePort` in `encounter-contract` |
| **Frontend / mobile** | `/api/v1/records/encounters` for admin; business APIs return embedded `encounterId` |
| **Reporting** | Separate read models — not ad-hoc joins into `records` from other BCs |

Encounter Administration API is for **tenant admins**, not for Notes / Billing module internals.

---

## OpenAPI

Grupo springdoc: **`records-administration`**

```text
GET /v3/api-docs/records-administration
```

Paths: `/api/v1/records/encounters` (+ cancel / complete).

---

## Testing consumers

- Mock `EncounterReferencePort` in unit / module tests  
- Do **not** load full Encounter infrastructure unless E2E  
- Testcontainers `records` schema only in cross-BC ITs  

---

## Checklist before merging a consumer of Encounter

| # | Item |
|---|------|
| 1 | Gradle depends only on `encounter-contract` |
| 2 | Aggregates store `EncounterId`, not Encounter entity |
| 3 | No SQL against `records.encounter` outside Clinical Records |
| 4 | Write-time open/linkable check via `EncounterReferencePort` |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | CANCELLED blocks **new** clinical/billing links |
| 7 | ADR-015 consulted — do not grow Encounter for vertical / EHR needs |

---

## Related documents

- [ADR-015 — Encounter Domain Model](ADR-015-ENCOUNTER-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [SCHEDULING-CONSUMPTION-GUIDE.md](SCHEDULING-CONSUMPTION-GUIDE.md)
- [PATIENT-CONSUMPTION-GUIDE.md](PATIENT-CONSUMPTION-GUIDE.md)
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)
- [PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md](../audits/PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md)
- [PASO-19.7-ENCOUNTER-VERIFICATION.md](../audits/PASO-19.7-ENCOUNTER-VERIFICATION.md)
