# ADR-012 — Patient Domain Model

**Status:** Proposed  
**Date:** 2026-07-11  
**Deciders:** CodeCore architecture (FASE 17.1 — pending acceptance)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-010 · ADR-011 · [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](../audits/PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Context

FASE 16 closed **Organization Management** as a stable bounded context (ADR-010, ADR-011). CodeCore must introduce the first **clinical** bounded context.

PASO-17.0.1 audited `Patient` as the first clinical Aggregate Root and applied a **closure refinement** before acceptance: remove ownership-like “home” semantics, drop `OfficeId` from Patient, and harden tenant/registry exclusivity invariants.

This ADR freezes that model so Dental, Veterinary, Hospital, Lab, and other verticals share one care-subject foundation without reopening Patient for years.

---

## Decision

### 1. Bounded context

**Clinical Foundation** — downstream of IAM and Organization Management.

Gradle module (FASE 17 slice): `patient-management`  
SQL schema: `clinical`

### 2. What Patient is

`Patient` is the **tenant-scoped clinical registry identity of a care subject** (human or animal).

It is the **only** clinical registry identity for that care subject **within a Tenant**. No future bounded context may introduce an equivalent aggregate (“Client”, “Subject”, “CareRecipient”, …) that duplicates this role. Downstream BCs **reference** `PatientId`.

It is **not**:

- IAM `Identity` (authentication — ADR-006)
- A clinical document / chart (`MedicalRecord`)
- An encounter or appointment
- An organizational ownership record

### 3. Aggregate Root

| Aggregate | Root | Boundary |
|-----------|------|----------|
| **Patient** | `Patient` | Demographics, registry status, optional primary organization, optional external identifiers |

No nested appointments, encounters, offices, or clinical notes inside the aggregate.

### 4. Ownership

| Concern | Owner |
|---------|--------|
| Patient data | Clinical Foundation |
| Who may mutate | Membership + `patient:*` permissions (ADR-007) |
| Structure of clinics | Organization Management (IDs only) |
| Physical / operational location | Appointment, Encounter, Inventory, … via their own `OfficeId` |
| Staff operating location | `StaffAssignment` — **never** on Patient |

### 5. Identity & uniqueness

| Key | Rule |
|-----|------|
| `PatientId` (UUID) | **Hard** unique identity of the aggregate |
| External identifiers | Optional `(type, value)`; soft/unique **per tenant** when enabled — **no** country-specific national ID mandated |
| Email / phone | Contact fields only — **not** uniqueness constraints |

### 6. References (IDs only)

| ID | On Patient | Cardinality | Semantics |
|----|------------|-------------|-----------|
| `TenantId` | Required | 1 | SaaS isolation — **immutable; never changes tenant** |
| `PrimaryOrganizationId` (`OrganizationId`) | Optional | 0..1 | Primary **registration / default grouping** org — **not** ownership of the subject |

**Forbidden on Patient:** `OfficeId`, `StaffAssignmentId`, `MembershipId`, `IdentityId`, `AppointmentId`, `ClinicalRecordId`.

**Naming:** use `PrimaryOrganizationId` in domain language. Do **not** use “Home Organization” (ownership connotation). “Registration organization” is an allowed synonym in prose for the same field.

### 7. Multi-organization attendance

One `Patient` per care subject **per tenant**.  
The same patient may receive care at multiple organizations/offices through **Appointment/Encounter** aggregates that carry their own `OrganizationId` / `OfficeId`.  
**Do not** create one Patient row per organization.

### 8. Lifecycle

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Operational registry entry |
| `ARCHIVED` | Soft-retired; retained for history |

Behaviors: `create`, update demographics / `PrimaryOrganizationId`, `archive`, `activate`.  
**No** physical delete in v1.  
**No** tenant transfer.  
**Anonymize** and **patient portal Identity link** are out of scope (future ADR).

### 9. Merge

Duplicate detection may use search + external ids.  
**Automatic merge is forbidden.**  
Explicit merge is a future application capability (`patient:merge`); loser archived; survivor remains canonical. Not required to ship domain foundation (17.3).  
Merge **never** moves a Patient to another Tenant.

### 10. Invariants (normative)

1. Exactly one `TenantId`, set at `create`.  
2. **`TenantId` never changes.**  
3. Status ∈ {`ACTIVE`, `ARCHIVED`}.  
4. When `PrimaryOrganizationId` is present on write, it must be same-tenant and ACTIVE.  
5. Patient does **not** carry `OfficeId`.  
6. Patient does not enforce uniqueness of email/phone.  
7. Patient is the sole clinical registry identity role for the care subject within the Tenant.  
8. Patient does not transactionalize other aggregates’ consistency.

### 11. Consumption rules (downstream)

Other BCs store `PatientId` and never load the Patient aggregate to mutate it.  
Validate ACTIVE patient via a future `PatientReferencePort` in `patient-contract` when needed (mirror ADR-011).  
Operational location uses `OfficeId` on the **operational** aggregate, not on Patient.

### 12. Organization ports

Runtime validation of `PrimaryOrganizationId` uses **`OrganizationReferencePort`** in `organization-contract` (PASO 17.2).  
`OfficeReferencePort` is **not** required by Patient v1 (offices validated by Appointment/Encounter later).

---

## Consequences

### Positive

- Stable care-subject ID for all clinical and commercial modules  
- No false “belongs to office/org” ownership semantics  
- Smaller aggregate; clearer split registry vs operational location  
- Compatible with multi-org tenants and non-human care subjects  
- Aligns with ADR-011 location rule (`OfficeId` = where something is located)  

### Negative / deferred

- First-visit office must be captured on Appointment/Encounter (not on Patient)  
- Merge orchestration across BCs is non-trivial (deferred)  
- Veterinary/guardian profile needs extension model later  
- No patient self-service portal until Identity link ADR  

### Neutral

- Permission catalog `patient:*` seeded in FASE 17.5  
- HTTP shape deferred to PASO 17.5.1  

---

## Alternatives considered

| Alternative | Rejected because |
|-------------|------------------|
| Patient owned by Organization | Breaks multi-org attendance; duplicates people |
| “Home Organization” naming | Ownership connotation |
| `OfficeId` on Patient | Location belongs to operational aggregates; extra invariants without registry value |
| Patient = Identity | Mixes auth with clinical registry (ADR-006) |
| Embed MedicalRecord in Patient | Mega-aggregate; scalability failure |
| National ID as primary key | Not international SaaS-safe |
| StaffAssignment on Patient | Confuses staff scope with care subject |
| Parallel “Client/Subject” aggregate in another BC | Duplicates clinical registry role |

---

## Compatibility check (closure refinement)

| Document | Impact |
|----------|--------|
| ADR-006 | Unchanged — Patient ≠ Identity |
| ADR-007 | Unchanged — membership-scoped RBAC; `patient:*` permissions |
| ADR-010 | Unchanged — Organization/Office/StaffAssignment boundaries |
| ADR-011 | Patient consumer recipe refined: optional `PrimaryOrganizationId` only; `OfficeId` on operational BCs |
| ORGANIZATION-CONSUMPTION-GUIDE | Patient recipe aligned with this ADR |

---

## Acceptance

This ADR becomes **Accepted** in PASO **17.1**. Until then status remains **Proposed**, backed by PASO-17.0.1 (including closure refinement).

---

## References

- [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](../audits/PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md)  
- [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)  
- [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](ROADMAP.md)  
