# ADR-015 — Encounter Domain Model

**Status:** Accepted  
**Date:** 2026-07-11  
**Accepted:** 2026-07-11 (PASO 19.1)  
**Deciders:** CodeCore architecture (FASE 19.1)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-010 · ADR-011 · ADR-012 · ADR-013 · ADR-014 · [PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md](../audits/PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md) · [PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md](../audits/PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Context

FASE 18 closed **Scheduling** (`Appointment`, ADR-014). Clinical Foundation (`Patient`, ADR-012) and Organization Management remain closed. CodeCore must introduce **Clinical Records** as the bounded context that documents **care that occurred** — without reopening IAM, Organization, Patient, or Appointment.

PASO-19.0.1 audited `Encounter` as the first Aggregate Root of Clinical Records.

A recurring failure mode in clinical systems is the **God Aggregate EHR**: over years, SOAP notes, odontograms, diagnoses, prescriptions, images, labs, and billing lines are stuffed into “Encounter” or “MedicalRecord” until the Core becomes a vertical product. This ADR freezes a **deliberately small** Encounter so Dental, Veterinary, Hospital, Lab, Psychology, Physiotherapy, and future verticals share one **occurred-care-episode** foundation **without** that fate.

---

## Decision

### 1. Bounded context

**Clinical Records** — downstream of IAM, Organization Management, Clinical Foundation, and Scheduling.

Gradle module (FASE 19 slice): `encounter-management`  
SQL schema: `records`  
HTTP surface (shape deferred to PASO 19.5.1): `/api/v1/records/encounters`

### 2. What Encounter is

`Encounter` is:

> **The care episode that occurred (or is recorded as having occurred) for a care subject at a determined operational context.**

It is the tenant-scoped **occurred care episode** (who was served, who operated, where, when it ran, and episode status).

It is the **only** Aggregate Root in the Core that owns that role within a Tenant. Vertical packs must not introduce parallel roots (“DentalVisit”, “VetConsult”, …) that duplicate this role. Downstream aggregates **reference** `EncounterId` when needed.

It is **not**:

| Not Encounter | Belongs instead |
|---------------|-----------------|
| Appointment / booking (planned commitment) | Scheduling (ADR-014) |
| Medical Record / longitudinal chart bag | Query projection or future thin aggregate — **never** inflate Encounter |
| SOAP / clinical note / document | Future ClinicalDocument / Note aggregate (same BC or later) |
| Diagnosis / Observation / Lab result | Future clinical observation BCs |
| Prescription / Treatment plan | Future aggregates |
| Consent / Attachment / Clinical image | Future aggregates |
| Odontogram / dental chair rules | Product packs |
| Billing / invoice line | Billing (FASE 21+) |
| EpisodeOfCare (longitudinal multi-encounter) | Future aggregate if needed |
| Inventory / Notifications / Auth | Other BCs |

### 3. Permanence principle — Encounter is intentionally small

> **Encounter is intentionally small.**

This is a **permanent architectural decision**, not a temporary limitation of FASE 19.

`Encounter` exists **only** to represent the **occurred care episode**. It may contain **only**:

- episode identity and status  
- time bounds of the episode (`startedAt`, optional/`required-on-complete` `endedAt`)  
- ID references required for that episode  
- invariants **intrinsic to the occurred episode** (tenant, time order, status transitions, reference coherence on write)

**Everything else lives elsewhere** — in its own Aggregate Root — and **must never** be embedded, nested, or “conveniently” co-located inside `Encounter`.

| Belongs on Encounter | Must never live inside Encounter |
|----------------------|----------------------------------|
| Occurred episode identity | SOAP / notes / evolution text |
| Time bounds (`startedAt`, `endedAt`) | Odontogram / charts / images |
| Lifecycle `IN_PROGRESS` / `CANCELLED` / `COMPLETED` | Diagnoses / observations / labs |
| `PatientId`, `StaffAssignmentId`, `OrganizationId`, optional `OfficeId`, optional `AppointmentId` | Prescriptions / treatment plans |
| Intrinsic episode invariants | Billing / invoices / payments |
| | Inventory / stock |
| | Notifications / workflows |
| | Vertical-specific product rules |
| | Longitudinal EpisodeOfCare children |

**Rule for future contributors and agents:** if a feature answers *“what was written clinically / what was measured / what was billed / what does Dental need?”* rather than *“what care episode occurred for this care subject in this operational context?”*, it **does not** belong on `Encounter`. Add a new aggregate. Expanding Encounter “just this once” is an architecture violation.

This principle aligns with DEVELOPMENT-POLICY-FASE-16-PLUS (§4–§5–§9), ADR-012 / ADR-014 permanence patterns, and ADR-011 / ADR-013 consumption.

### 4. Why Encounter is the Aggregate Root

| Criterion | Rationale |
|-----------|-----------|
| Transaction boundary | Owns the episode’s time, status, and reference set |
| Own lifecycle | Open → cancel / complete — independent of Appointment completion |
| Stable ID | `EncounterId` for notes, labs, billing, consent links |
| Single-aggregate invariants | Does **not** transactionalize Patient, Appointment, or clinical documents |

#### Why not MedicalRecord

A longitudinal chart bag invites the God Aggregate. Without an episode boundary, documents orphan. Encounter is the correct first root; the “chart” is Patient + Encounters + documents (query) or a later thin aggregate.

#### Why not Appointment

Appointment answers *what is planned*. Encounter answers *what occurred*. Walk-in: Encounter without Appointment. No-show: Appointment without Encounter.

#### Why not ClinicalDocument / Note

A document is not an episode. Many documents per Encounter. Different ownership.

#### Why not EpisodeOfCare / Visit / Consultation

Longitudinal multi-encounter, ambiguous synonyms, or specialty bias — rejected in PASO-19.0 / 19.0.1.

### 5. Ownership

| Concern | Owner BC | Aggregate |
|---------|----------|-----------|
| **Encounter** (occurred episode) | **Clinical Records** | `Encounter` |
| Planned commitment | Scheduling | `Appointment` |
| Care-subject registry | Clinical Foundation | `Patient` |
| Business structure | Organization Management | `Organization` |
| Physical / logical site | Organization Management | `Office` |
| Staff operational scope | Organization Management | `StaffAssignment` |
| Identity / Membership / RBAC | IAM | Identity, Membership, Role, … |

**Rule:** Clinical Records stores **IDs** of foreign aggregates and validates them via ReferencePorts (ADR-013). It never owns Patient, Appointment, Organization, Office, or StaffAssignment data.

Who may mutate Encounter: Membership + `encounter:*` permissions (ADR-007; catalog seeded in PASO 19.5).

### 6. Identity & time

| Element | Rule |
|---------|------|
| `EncounterId` (UUID) | **Hard** unique identity of the aggregate |
| `startedAt` | Required `Instant` (UTC) |
| `endedAt` | Optional while `IN_PROGRESS`; **required** on `complete`; if present must be **≥** `startedAt` |
| Time zone storage | Not part of the aggregate v1 — presentation concern |
| External visit numbers | Out of scope v1 |

Equality `endedAt == startedAt` is allowed (minimal / instantaneous recorded episode).

### 7. References (IDs only)

Encounter maintains **only IDs**. It **never** loads foreign aggregates, **never** depends on foreign repositories, **never** runs SQL against other BC schemas, and **never** calls internal HTTP to other modules.

| ID | On Encounter | Cardinality | Semantics |
|----|--------------|-------------|-----------|
| `TenantId` | Required | 1 | SaaS isolation — **immutable; never changes tenant** |
| `PatientId` | Required | 1 | Care subject of the episode |
| `StaffAssignmentId` | Required | 1 | **Who operated** — **never** `MembershipId` / `IdentityId` |
| `OrganizationId` | Required | 1 | Business / custodial context of the episode |
| `OfficeId` | Optional | 0..1 | Where the episode took place |
| `AppointmentId` | Optional | 0..1 | Planned origin (walk-in = absent) |

**Forbidden on Encounter:** `MembershipId`, `IdentityId`, nested notes/diagnoses/images, billing lines, odontogram, MedicalRecord bag.

#### Denormalized `OrganizationId` (normative)

Same rationale as ADR-014: query locality inside `records`, explicit episode context, write-time coherence with StaffAssignment. **Do not remove** in future “normalizations.”

#### StaffAssignment ↔ Organization / Office coherence (write-time)

On create / update while `IN_PROGRESS`:

1. `encounter.organizationId` **must equal** the assignment’s `organizationId`.  
2. If assignment has `officeId` → encounter `officeId` **must equal** that office.  
3. If assignment is org-wide (`officeId` null) → encounter `officeId` may be null or any ACTIVE office of that organization (`OfficeReferencePort`).

#### Optional `AppointmentId` (write-time)

When present:

1. Appointment **exists** in the same tenant.  
2. Appointment status ∈ {`SCHEDULED`, `COMPLETED`} — **not** `CANCELLED` (blocks **new** links).  
3. Appointment `patientId` **must equal** `encounter.patientId`.

Org/office/staff on Encounter **may differ** from the Appointment (coverage / room change) — equality of those IDs is **not** a hard invariant.

**No** auto-create Encounter when Appointment completes.  
**No** auto-complete Appointment when Encounter completes.

### 8. Lifecycle (frozen)

| Status | Meaning |
|--------|---------|
| `IN_PROGRESS` | Open occurred episode (live or being recorded) |
| `CANCELLED` | Episode voided / aborted (terminal) |
| `COMPLETED` | Episode closed operationally (terminal) |

```text
(create/open) → IN_PROGRESS
                   ├── cancel   → CANCELLED
                   ├── complete → COMPLETED  (endedAt required)
                   └── update (remains IN_PROGRESS)
```

| Behavior | Rule |
|----------|------|
| `open` (create) | Enters `IN_PROGRESS`; refs + `startedAt` after port validation |
| `update` | Only from `IN_PROGRESS`; re-validate ports on write |
| `cancel` | Only from `IN_PROGRESS` → `CANCELLED` |
| `complete` | Only from `IN_PROGRESS` → `COMPLETED`; `endedAt` required (application may supply `now`) |
| Physical delete | **Forbidden** in v1 |
| Reactivate | **Forbidden** in v1 — open a **new** Encounter if needed |
| FHIR sub-states (arrived, on-leave, …) | **Out of model** — product mapping |
| Notes / labs / prescriptions inside Encounter | **Out of model** — violates §3 |

There is **no** `PLANNED` status — planning belongs to Appointment.

### 9. Invariants (normative)

1. Exactly one `TenantId`, set at `create` — **never changes**.  
2. Status ∈ {`IN_PROGRESS`, `CANCELLED`, `COMPLETED`}.  
3. `cancel` / `complete` / mutating `update` only from `IN_PROGRESS`.  
4. `startedAt` always present; if `endedAt` present → `endedAt` ≥ `startedAt`.  
5. `complete` requires `endedAt`.  
6. `PatientId`, `StaffAssignmentId`, and `OrganizationId` are always present.  
7. On write while `IN_PROGRESS`: Patient ACTIVE in tenant; Organization ACTIVE; StaffAssignment exists and satisfies §7 coherence; Office if present is ACTIVE in that organization; Appointment if present satisfies §7 Appointment rules.  
8. Encounter does **not** enforce uniqueness of Appointment→Encounter (0..n allowed; products may impose 0..1).  
9. Encounter does not transactionalize consistency of Patient, Appointment, Organization, Office, StaffAssignment, Notes, or Billing.  
10. `EncounterId` is never reassigned.  
11. Cross-tenant access is impossible.  
12. Completing Encounter does not mutate Appointment; creating Encounter is never performed inside Scheduling.

*(Structural permanence — “do not embed clinical/vertical children” — is stated in §3.)*

### 10. Reference Ports (ADR-013)

Encounter consumes **only** Reference Contracts — never provider repositories, never cross-BC SQL, never internal HTTP loopback.

| Port | Purpose |
|------|---------|
| `PatientReferencePort` | Patient exists and is ACTIVE in tenant |
| `OrganizationReferencePort` | Organization exists and is ACTIVE in tenant |
| `OfficeReferencePort` | Office exists, ACTIVE, and belongs to the encounter’s organization |
| `StaffAssignmentReferencePort` | Assignment exists; minimal scope for §7 coherence |
| `AppointmentReferencePort` (evolved in PASO 19.2) | When `AppointmentId` present: linkable status + `patientId` for §7 |

**Normative port evolution for Appointment (PASO 19.2 — just-in-time):**  
`existsScheduledByIdAndTenant` alone is **insufficient**. Provide a **minimal** surface (boolean linkable **or** small reference view with `status` + `patientId`) that implements §7 Appointment rules. Prefer boolean+view only as large as the invariant requires (ADR-013). **Do not** reopen ADR-014. **Do not** expose appointment time window or demographics.

Gradle: `encounter-application` depends on `patient-contract`, `organization-contract`, and `appointment-contract` only (for these ports/IDs).

Archived Patient / Organization / Office **block new** encounter links; historical Encounters remain readable.

### 11. Permissions (seeded in PASO 19.5)

Catalog:

`encounter:read` · `encounter:create` · `encounter:update` · `encounter:cancel`

`complete` maps to `encounter:update` (mirror Appointment / Patient activate) — **no** vertical verbs.

RBAC remains membership-scoped (ADR-007). Seed: `V23__seed_encounter_authorization_contract.sql`.

### 12. Multi-organization attendance

One care subject (`PatientId`) may have Encounters under many organizations/offices.  
Encounter carries its own `OrganizationId` / optional `OfficeId`.  
Patient’s optional `PrimaryOrganizationId` is **not** a substitute for Encounter’s organization context.

---

## Consequences

### Positive

- Stable occurred-episode ID for notes, labs, billing, and consent links  
- **Permanent protection against an Encounter / EHR God Aggregate** (§3)  
- Clear Core triad: Patient (who) · Appointment (planned) · Encounter (occurred)  
- Proves ADR-013 consumption including optional Scheduling link without reopening FASE 18  
- Vertical-agnostic care-episode foundation  

### Negative / deferred

- Appointment port must evolve slightly in 19.2 for COMPLETED links  
- No FHIR sub-state machine in Core v1  
- StaffAssignment physical delete leaves historical UUID without live row  
- No automatic Appointment↔Encounter orchestration (intentional)  
- Clinical documents require **new** aggregates later — higher short-term ceremony, lower long-term risk  

### Neutral

- Permission seed and HTTP shape deferred to 19.5 / 19.5.1 — **done** (19.5 seed · 19.5.1 HTTP audit)  
- `EncounterReferencePort` deferred to closeout 19.8  

---

## Alternatives considered

| Alternative | Rejected because |
|-------------|------------------|
| MedicalRecord as first Aggregate Root | Chart bag → God Aggregate; weak episode boundary |
| ClinicalDocument as first root | Document ≠ episode |
| EpisodeOfCare as first root | Longitudinal multi-encounter; premature |
| Visit / Consultation naming | Ambiguous / specialty-biased |
| PLANNED status on Encounter | Duplicates Appointment |
| Embed SOAP / odontogram / billing | Violates §3 |
| Auto-create Encounter on Appointment complete | Couples Scheduling to Records; violates ADR-014 |
| Remove denormalized `OrganizationId` | Forces cross-BC joins |
| Vertical-specific Encounter subtypes in Core | Fractures the platform |
| Grow Encounter “temporarily” then split | Temporary growth becomes permanent |

---

## Compatibility check

| Document | Impact |
|----------|--------|
| ADR-003 | Encounter always tenant-scoped; `TenantId` immutable |
| ADR-006 | Unchanged — operator is StaffAssignment, not Identity |
| ADR-007 | Unchanged — membership-scoped RBAC; `encounter:*` later |
| ADR-010 | Unchanged — Org / Office / StaffAssignment boundaries respected |
| ADR-011 | Encounter recipe: StaffAssignment + Organization + optional Office; never Membership as operator |
| ADR-012 | Unchanged — Patient remains registry; Encounter references `PatientId` |
| ADR-013 | Normative consumption via ReferencePorts; Appointment port evolution is contract growth |
| ADR-014 | Unchanged — Appointment remains planned commitment; no Encounter ownership in Scheduling |
| SCHEDULING / PATIENT / ORGANIZATION guides | Encounter recipes align; guides may gain Records section at closeout |
| DEVELOPMENT-POLICY-FASE-16-PLUS | §3 reinforces §§4–5–9 |

**No existing ADR is modified by this decision.**

---

## Freeze rule

The Encounter domain model defined by this ADR is **frozen** as of PASO **19.1**.

Any change to Aggregate Root boundaries, references (including removal of denormalized `OrganizationId`), lifecycle, optional Appointment link rules, or the §3 permanence principle **requires**:

1. A new architecture audit (DEVELOPMENT-POLICY-FASE-16-PLUS §2)  
2. A **new ADR**

Implementation steps 19.2+ must implement this contract — they must not reopen it.

---

## Acceptance

**Accepted** in PASO **19.1** (2026-07-11).  
Evidence: this ADR · [PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md](../audits/PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md) · [PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md](../audits/PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md).

---

## References

- [PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md](../audits/PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md)  
- [PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md](../audits/PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md)  
- [ADR-014-APPOINTMENT-DOMAIN-MODEL.md](ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ADR-012-PATIENT-DOMAIN-MODEL.md](ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [SCHEDULING-CONSUMPTION-GUIDE.md](SCHEDULING-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md)  
- [ROADMAP.md](ROADMAP.md)  
