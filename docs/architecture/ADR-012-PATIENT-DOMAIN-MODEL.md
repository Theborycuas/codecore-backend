# ADR-012 — Patient Domain Model

**Status:** Accepted  
**Date:** 2026-07-11  
**Accepted:** 2026-07-11 (PASO 17.1)  
**Deciders:** CodeCore architecture (FASE 17.1)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-010 · ADR-011 · [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](../audits/PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md) · [PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md](../audits/PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Context

FASE 16 closed **Organization Management** as a stable bounded context (ADR-010, ADR-011). CodeCore must introduce the first **clinical** bounded context.

PASO-17.0.1 audited `Patient` as the first clinical Aggregate Root and applied a **closure refinement** before acceptance: remove ownership-like “home” semantics, drop `OfficeId` from Patient, and harden tenant/registry exclusivity invariants.

A recurring failure mode in clinical systems is the **God Aggregate**: over years, odontograms, SOAP notes, encounters, appointments, treatment plans, consents, files, diagnoses, prescriptions, and billing are stuffed into `Patient` until the bounded context collapses. This ADR freezes a **deliberately small** Patient so Dental, Veterinary, Hospital, Lab, and other verticals share one care-subject foundation **without** that fate.

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

### 3. Permanence principle — Patient is intentionally small

> **Patient is intentionally small.**

This is a **permanent architectural decision**, not a temporary limitation of FASE 17.

`Patient` exists **only** to represent the **clinical registry identity** of the care subject. It may contain **only**:

- registry / demographic information belonging to that identity  
- clinical identity keys and optional external identifiers  
- invariants that are **intrinsic to the patient registry** (tenant, status, primary organization rules)

**Everything clinical-operational or documentary lives elsewhere** — in its own Aggregate Root and bounded context — and **must never** be embedded, nested, or “conveniently” co-located inside `Patient`.

| Belongs on Patient | Must never live inside Patient |
|--------------------|--------------------------------|
| Registry identity | Encounter |
| Demographics / contacts | Appointment |
| Lifecycle `ACTIVE` / `ARCHIVED` | Medical Record / Clinical Record |
| Optional `PrimaryOrganizationId` | Clinical notes / SOAP / evolution |
| Optional external identifiers | Odontogram / charts |
| Intrinsic patient invariants | Treatment Plan |
| | Documents / attachments |
| | Consents (as clinical artifacts) |
| | Diagnoses / prescriptions |
| | Billing / invoices |
| | Inventory |
| | Operational context (office, staff assignment, room) |

**Rule for future contributors and agents:** if a feature answers *“what happened / was scheduled / was documented / was billed / where did care occur?”* rather than *“who is this care subject in this tenant’s registry?”*, it **does not** belong on `Patient`. Add a new aggregate (or extend the correct BC). Expanding Patient “just this once” is an architecture violation.

This principle aligns with DEVELOPMENT-POLICY-FASE-16-PLUS (§4 IDs only, §5 one aggregate’s invariants only, §9 do not reinvent mega-entities) and with ADR-011’s consumption model (downstream stores `PatientId`; it does not grow Patient).

### 4. Aggregate Root

| Aggregate | Root | Boundary |
|-----------|------|----------|
| **Patient** | `Patient` | Demographics, registry status, optional primary organization, optional external identifiers — **intentionally minimal** |

No nested appointments, encounters, offices, clinical notes, treatment plans, documents, or billing artifacts inside the aggregate. Collections of operational or clinical children are **forbidden by design** (§3).

### 5. Ownership

| Concern | Owner |
|---------|--------|
| Patient data | Clinical Foundation |
| Who may mutate | Membership + `patient:*` permissions (ADR-007) |
| Structure of clinics | Organization Management (IDs only) |
| Physical / operational location | Appointment, Encounter, Inventory, … via their own `OfficeId` |
| Staff operating location | `StaffAssignment` — **never** on Patient |
| Clinical documentation & operations | Downstream BCs — **never** owned by Patient |

### 6. Identity & uniqueness

| Key | Rule |
|-----|------|
| `PatientId` (UUID) | **Hard** unique identity of the aggregate |
| External identifiers | Optional `(type, value)`; soft/unique **per tenant** when enabled — **no** country-specific national ID mandated |
| Email / phone | Contact fields only — **not** uniqueness constraints |

### 7. References (IDs only)

| ID | On Patient | Cardinality | Semantics |
|----|------------|-------------|-----------|
| `TenantId` | Required | 1 | SaaS isolation — **immutable; never changes tenant** |
| `PrimaryOrganizationId` (`OrganizationId`) | Optional | 0..1 | Primary **registration / default grouping** org — **not** ownership of the subject |

**Forbidden on Patient:** `OfficeId`, `StaffAssignmentId`, `MembershipId`, `IdentityId`, `AppointmentId`, `ClinicalRecordId`.

**Naming:** use `PrimaryOrganizationId` in domain language. Do **not** use “Home Organization” (ownership connotation). “Registration organization” is an allowed synonym in prose for the same field.

### 8. Multi-organization attendance

One `Patient` per care subject **per tenant**.  
The same patient may receive care at multiple organizations/offices through **Appointment/Encounter** aggregates that carry their own `OrganizationId` / `OfficeId`.  
**Do not** create one Patient row per organization.

### 9. Lifecycle

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Operational registry entry |
| `ARCHIVED` | Soft-retired; retained for history |

Behaviors: `create`, update demographics / `PrimaryOrganizationId`, `archive`, `activate`.  
**No** physical delete in v1.  
**No** tenant transfer.  
**Anonymize** and **patient portal Identity link** are out of scope (future ADR).

### 10. Merge

Duplicate detection may use search + external ids.  
**Automatic merge is forbidden.**  
Explicit merge is a future application capability (`patient:merge`); loser archived; survivor remains canonical. Not required to ship domain foundation (17.3).  
Merge **never** moves a Patient to another Tenant.

### 11. Invariants (normative)

1. Exactly one `TenantId`, set at `create`.  
2. **`TenantId` never changes.**  
3. Status ∈ {`ACTIVE`, `ARCHIVED`}.  
4. When `PrimaryOrganizationId` is present on write, it must be same-tenant and ACTIVE.  
5. Patient does **not** carry `OfficeId`.  
6. Patient does not enforce uniqueness of email/phone.  
7. Patient is the sole clinical registry identity role for the care subject within the Tenant.  
8. Patient does not transactionalize other aggregates’ consistency.

*(Structural permanence — “do not embed clinical/operational children” — is stated in §3; it is a design law, not an additional mutable-state invariant.)*

### 12. Consumption rules (downstream)

Other BCs store `PatientId` and never load the Patient aggregate to mutate it.  
Validate ACTIVE patient via a future `PatientReferencePort` in `patient-contract` when needed (mirror ADR-011).  
Operational location uses `OfficeId` on the **operational** aggregate, not on Patient.  
Downstream modules **must not** “push” clinical content into Patient for convenience; they own their aggregates and reference `PatientId` (§3).

### 13. Organization ports

Runtime validation of `PrimaryOrganizationId` uses **`OrganizationReferencePort`** in `organization-contract` ([ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · PASO 17.2).  
`OfficeReferencePort` is **not** required by Patient v1 (offices validated by Appointment/Encounter later).

---

## Consequences

### Positive

- Stable care-subject ID for all clinical and commercial modules  
- **Permanent protection against a Patient God Aggregate** (§3)  
- No false “belongs to office/org” ownership semantics  
- Smaller aggregate; clearer split registry vs operational location  
- Compatible with multi-org tenants and non-human care subjects  
- Aligns with ADR-011 location rule (`OfficeId` = where something is located)  
- Aligns with DEVELOPMENT-POLICY-FASE-16-PLUS small-aggregate discipline  

### Negative / deferred

- First-visit office must be captured on Appointment/Encounter (not on Patient)  
- Merge orchestration across BCs is non-trivial (deferred)  
- Veterinary/guardian profile needs extension model later  
- No patient self-service portal until Identity link ADR  
- New clinical features require **new** aggregates/BCs — higher short-term ceremony, lower long-term risk  

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
| Embed MedicalRecord / notes / odontogram / appointments in Patient | **God Aggregate** — violates §3 permanence principle |
| Grow Patient “temporarily” in FASE 17 then split later | Temporary growth becomes permanent; split cost explodes |
| National ID as primary key | Not international SaaS-safe |
| StaffAssignment on Patient | Confuses staff scope with care subject |
| Parallel “Client/Subject” aggregate in another BC | Duplicates clinical registry role |

---

## Compatibility check

| Document | Impact |
|----------|--------|
| ADR-003 | Patient always tenant-scoped; `TenantId` immutable |
| ADR-006 | Unchanged — Patient ≠ Identity |
| ADR-007 | Unchanged — membership-scoped RBAC; `patient:*` permissions |
| ADR-010 | Unchanged — Organization/Office/StaffAssignment boundaries |
| ADR-011 | Patient consumer: optional `PrimaryOrganizationId` only; `OfficeId` on operational BCs; downstream grows around `PatientId` |
| ORGANIZATION-CONSUMPTION-GUIDE | Patient recipe aligned |
| DEVELOPMENT-POLICY-FASE-16-PLUS | §3 reinforces §§4–5–9 (IDs only, single-aggregate invariants, no mega-entities) |

---

## Freeze rule

The Patient domain model defined by this ADR is **frozen** as of PASO **17.1**.

Any change to Aggregate Root boundaries, references, lifecycle, exclusivity, or the §3 permanence principle **requires a new ADR** (and architecture audit per DEVELOPMENT-POLICY-FASE-16-PLUS §2). Implementation steps 17.2+ must implement this contract — they must not reopen it.

---

## Acceptance

**Accepted** in PASO **17.1** (2026-07-11).  
Evidence: [PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md](../audits/PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md).

---

## References

- [PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md](../audits/PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md)  
- [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](../audits/PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md)  
- [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)  
- [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ROADMAP.md](ROADMAP.md)  
