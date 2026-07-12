# ADR-014 — Appointment Domain Model

**Status:** Accepted  
**Date:** 2026-07-11  
**Accepted:** 2026-07-11 (PASO 18.1)  
**Deciders:** CodeCore architecture (FASE 18.1)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-010 · ADR-011 · ADR-012 · ADR-013 · [PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md](../audits/PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md) · [PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md](../audits/PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Context

FASE 17 closed **Clinical Foundation** (`Patient`, ADR-012). Organization Management remains closed (ADR-010, ADR-011). CodeCore must introduce **Scheduling** as the first bounded context that consumes Patient, Organization, Office, and StaffAssignment **without reopening** those BCs — proving Reference Contracts (ADR-013) under real multi-BC load.

PASO-18.0.1 audited `Appointment` as the Aggregate Root of Scheduling.

A recurring failure mode in scheduling systems is the **God Aggregate / God Engine**: over years, slots, availability matrices, recurrence, waitlists, clinical notes, odontograms, billing lines, and vertical chair rules are stuffed into “Appointment” until the Core becomes a product. This ADR freezes a **deliberately small** Appointment so Dental, Veterinary, Hospital, Lab, Psychology, and future verticals share one planned-commitment foundation **without** that fate.

---

## Decision

### 1. Bounded context

**Scheduling** — downstream of IAM, Organization Management, and Clinical Foundation.

Gradle module (FASE 18 slice): `appointment-management`  
SQL schema: `scheduling`  
HTTP surface (shape deferred to PASO 18.5.1): `/api/v1/scheduling/appointments`

### 2. What Appointment is

`Appointment` is:

> **The planned commitment to provide a service to a care subject at a determined time and operational context.**

It is the tenant-scoped **planned care commitment** (who is served, who operates, where, when, and commitment status).

It is the **only** Aggregate Root in the Core that owns that role within a Tenant. Vertical packs must not introduce parallel roots (“DentalAppointment”, “VetVisit”, …) that duplicate this role. Downstream BCs **reference** `AppointmentId` when needed.

It is **not**:

| Not Appointment | Belongs instead |
|-----------------|-----------------|
| Encounter / Visit (care episode that occurred) | Future clinical / encounter BC |
| Medical Record / chart / SOAP / notes | Clinical Records (FASE 19+) |
| Treatment / procedure catalog | Treatment / clinical BCs |
| Billing / invoice line | Billing (FASE 21+) |
| Calendar / Schedule (view) | Query / read model over Appointments |
| Availability Engine / Slot inventory | Future capacity BC or product layer |
| Waitlist / recurrence series | Future Scheduling evolution (new audit) |
| Vertical Dental (chair, odontogram, …) | Product packs — never Core Appointment |

### 3. Permanence principle — Appointment is intentionally small

> **Appointment is intentionally small.**

This is a **permanent architectural decision**, not a temporary limitation of FASE 18.

`Appointment` exists **only** to represent the **planned commitment of care**. It may contain **only**:

- planning identity and commitment status  
- time window of the commitment  
- ID references required for that commitment  
- invariants **intrinsic to the planned commitment** (tenant, time order, status transitions, reference coherence on write)

**Everything else lives elsewhere** — in its own Aggregate Root and bounded context — and **must never** be embedded, nested, or “conveniently” co-located inside `Appointment`.

| Belongs on Appointment | Must never live inside Appointment |
|------------------------|------------------------------------|
| Planned commitment identity | Encounter / Visit |
| Time window (`startsAt`, `endsAt`) | Medical Record / clinical documentation |
| Lifecycle `SCHEDULED` / `CANCELLED` / `COMPLETED` | SOAP / notes / evolution |
| `PatientId`, `StaffAssignmentId`, `OrganizationId`, optional `OfficeId` | Odontogram / charts / images |
| Intrinsic scheduling invariants | Treatment plans / procedures as clinical content |
| | Billing / invoices / payments |
| | Inventory / stock |
| | Availability engine / slots / overbooking rules |
| | Recurrence series / waitlist |
| | Notifications / workflows / automations |
| | Vertical-specific product rules |

**Rule for future contributors and agents:** if a feature answers *“what was documented / what happened clinically / what was billed / what capacity remains / how does this recur / what does Dental need?”* rather than *“what planned commitment exists for this care subject at this time in this operational context?”*, it **does not** belong on `Appointment`. Add a new aggregate (or extend the correct BC). Expanding Appointment “just this once” is an architecture violation.

This principle aligns with DEVELOPMENT-POLICY-FASE-16-PLUS (§4 IDs only, §5 one aggregate’s invariants only, §9 no mega-entities), ADR-012’s permanence pattern for Patient, and ADR-011’s consumption model.

### 4. Why Appointment is the Aggregate Root

| Criterion | Rationale |
|-----------|-----------|
| Transaction boundary | Owns the commitment’s time, status, and reference set |
| Own lifecycle | Schedule → cancel / complete — independent of Patient or Encounter |
| Stable ID | `AppointmentId` for future Encounter, notifications, billing links |
| Single-aggregate invariants | Does **not** transactionalize Patient, Org, or clinical documentation |

#### Why not Schedule / Calendar

Views and projections over many appointments. They do not own a single commitment’s truth.

#### Why not Slot

Slot models **capacity inventory**. Capacity policies (buffers, overbooking, multi-resource) are product-sensitive and over-engineer the Core if forced into v1.

#### Why not Encounter / Visit

Encounter answers *what occurred in care*. It may exist without a prior appointment (walk-in) and an appointment may never become an encounter (cancel / no-show). Different aggregate, different BC timing.

#### Why not Availability Engine

An engine is infrastructure/product capability over many resources. It may **consult** Appointments; it must not **become** Appointment.

### 5. Ownership

| Concern | Owner BC | Aggregate |
|---------|----------|-----------|
| **Appointment** (planned commitment) | **Scheduling** | `Appointment` |
| Care-subject registry | Clinical Foundation | `Patient` |
| Business structure | Organization Management | `Organization` |
| Physical / logical site | Organization Management | `Office` |
| Staff operational scope | Organization Management | `StaffAssignment` |
| Identity / Membership / RBAC | IAM | Identity, Membership, Role, … |

**Rule:** each bounded context owns **only** its aggregates. Scheduling stores **IDs** of foreign aggregates and validates them via ReferencePorts (ADR-013). It never becomes owner of Patient, Organization, Office, or StaffAssignment data.

Who may mutate Appointment: Membership + `appointment:*` permissions (ADR-007; catalog seeded in PASO 18.5).

### 6. Identity & time

| Element | Rule |
|---------|------|
| `AppointmentId` (UUID) | **Hard** unique identity of the aggregate |
| `startsAt` | Required `Instant` (UTC) |
| `endsAt` | Required `Instant` (UTC); must be **strictly after** `startsAt` |
| Time zone storage | Not part of the aggregate v1 — presentation concern |
| External confirmation codes | Out of scope v1 |

### 7. References (IDs only)

Appointment maintains **only IDs**. It **never** loads foreign aggregates, **never** depends on foreign repositories, **never** runs SQL against other BC schemas, and **never** calls internal HTTP to other modules.

| ID | On Appointment | Cardinality | Semantics |
|----|----------------|-------------|-----------|
| `TenantId` | Required | 1 | SaaS isolation — **immutable; never changes tenant** |
| `PatientId` | Required | 1 | Care subject of the commitment |
| `StaffAssignmentId` | Required | 1 | **Who operates** (provider scope) — **never** `MembershipId` / `IdentityId` |
| `OrganizationId` | Required | 1 | Business context of the commitment |
| `OfficeId` | Optional | 0..1 | Where the commitment is located (room / site) |

**Forbidden on Appointment:** `MembershipId`, `IdentityId`, `EncounterId`, `MedicalRecordId`, nested clinical collections, billing lines, slot graphs.

#### Denormalized `OrganizationId` (normative)

`OrganizationId` remains on Appointment **intentionally**, even though `StaffAssignment` also carries an organization scope.

| Rationale | Detail |
|-----------|--------|
| Query efficiency | List/filter appointments by organization without joining StaffAssignment across BC boundaries |
| Explicit commitment context | The scheduled business context is part of the commitment’s meaning |
| Write-time coherence | Application validates consistency with StaffAssignment via **`StaffAssignmentReferencePort`** (and org ACTIVE via `OrganizationReferencePort`) |

**Do not remove `OrganizationId`** in future “normalizations.” Removing it would force cross-BC joins or inflate ReferencePorts into query APIs — both violate Core boundaries.

#### StaffAssignment ↔ Organization / Office coherence (write-time)

On create / update while `SCHEDULED`:

1. `appointment.organizationId` **must equal** the assignment’s `organizationId`.  
2. If assignment has `officeId` → appointment `officeId` **must equal** that office.  
3. If assignment is org-wide (`officeId` null) → appointment `officeId` may be null or any ACTIVE office of that organization (`OfficeReferencePort`).

### 8. Lifecycle (frozen)

| Status | Meaning |
|--------|---------|
| `SCHEDULED` | Open planned commitment |
| `CANCELLED` | Commitment voided (terminal) |
| `COMPLETED` | Commitment fulfilled operationally (terminal) |

```text
(create) → SCHEDULED
              ├── cancel   → CANCELLED
              ├── complete → COMPLETED
              └── update / reschedule (remains SCHEDULED)
```

| Behavior | Rule |
|----------|------|
| `schedule` (create) | Enters `SCHEDULED`; refs + time set after port validation |
| `update` / reschedule | Only from `SCHEDULED`; re-validate ports on write |
| `cancel` | Only from `SCHEDULED` → `CANCELLED` |
| `complete` | Only from `SCHEDULED` → `COMPLETED` |
| Physical delete | **Forbidden** in v1 |
| Reactivate cancelled | **Forbidden** in v1 — schedule a **new** Appointment if needed |
| Recurrence / waitlist / overbooking / slot engine | **Out of model** — future audit/ADR |

`COMPLETED` does **not** create an Encounter inside Scheduling. Linking Encounter → `AppointmentId` is a future consumer concern.

### 9. Invariants (normative)

1. Exactly one `TenantId`, set at `create` — **never changes**.  
2. Status ∈ {`SCHEDULED`, `CANCELLED`, `COMPLETED`}.  
3. `cancel` / `complete` / mutating `update` only from `SCHEDULED`.  
4. `endsAt` > `startsAt`.  
5. `PatientId`, `StaffAssignmentId`, and `OrganizationId` are always present.  
6. On write while scheduling/updating a `SCHEDULED` appointment: Patient ACTIVE in tenant; Organization ACTIVE; StaffAssignment exists and satisfies §7 coherence; Office if present is ACTIVE in that organization.  
7. Appointment does **not** enforce staff/office time uniqueness (no hard double-booking in this ADR).  
8. Appointment does not transactionalize consistency of Patient, Organization, Office, StaffAssignment, Encounter, or MedicalRecord.  
9. `AppointmentId` is never reassigned.  
10. Cross-tenant access is impossible.

*(Structural permanence — “do not embed clinical/capacity/vertical children” — is stated in §3; it is a design law, not an additional mutable-state invariant.)*

### 10. Reference Ports (ADR-013)

Appointment consumes **only** Reference Contracts — never provider repositories, never cross-BC SQL, never internal HTTP loopback.

| Port | Purpose |
|------|---------|
| `PatientReferencePort` | Patient exists and is ACTIVE in tenant |
| `OrganizationReferencePort` | Organization exists and is ACTIVE in tenant |
| `OfficeReferencePort` | Office exists, ACTIVE, and belongs to the appointment’s organization |
| `StaffAssignmentReferencePort` | Assignment exists in tenant; exposes minimal scope for §7 coherence |

Gradle: `appointment-application` depends on `patient-contract` and `organization-contract` only (for these ports/IDs).  
Pattern authority: [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md).

Archived Patient / Organization / Office **block new** scheduled links; historical Appointments remain readable (ADR-011 archive semantics for consumers).

### 11. Permissions (deferred to 18.5)

Draft catalog (not seeded by this ADR):

`appointment:read` · `appointment:create` · `appointment:update` · `appointment:cancel`

`complete` maps to `appointment:update` **or** a dedicated verb decided in PASO 18.5 — **no** vertical verbs.

RBAC remains membership-scoped (ADR-007).

### 12. Multi-organization attendance

One care subject (`PatientId`) may have Appointments under many organizations/offices.  
Appointment carries its own `OrganizationId` / optional `OfficeId`.  
Patient’s optional `PrimaryOrganizationId` is **not** a substitute for Appointment’s organization context.

---

## Consequences

### Positive

- Stable planned-commitment ID for Encounter, notifications, and billing links  
- **Permanent protection against an Appointment God Aggregate / God Engine** (§3)  
- Proves ADR-013 multi-port consumption without reopening Patient or Organization  
- Vertical-agnostic Core scheduling foundation  
- Clear split: planning (Appointment) vs occurrence (Encounter) vs documentation (Records) vs capacity (future)  
- Denormalized `OrganizationId` keeps queries inside Scheduling schema  

### Negative / deferred

- No hard double-booking / capacity engine in Core v1 (product/UI or future BC)  
- StaffAssignment physical delete leaves historical UUID without live row (read-model labels later)  
- No recurrence, waitlist, `NO_SHOW`, or confirmed sub-states in v1  
- Completing an Appointment does not auto-create Encounter (intentional decoupling)  
- New scheduling features require **new** aggregates/BCs or new ADRs — higher short-term ceremony, lower long-term risk  

### Neutral

- Permission seed and HTTP shape deferred to 18.5 / 18.5.1  
- `StaffAssignmentReferencePort` + `OfficeReferencePort` adapter completed in PASO 18.2  

---

## Alternatives considered

| Alternative | Rejected because |
|-------------|------------------|
| Calendar / Schedule as Aggregate Root | Projection, not commitment ownership |
| Slot as Aggregate Root | Capacity inventory; over-engineers Core; vertical policies |
| Encounter as Aggregate Root of Scheduling | Different meaning (occurred care); wrong BC timing |
| Availability Engine inside Appointment | God Engine; product-specific; violates §3 |
| Recurrence / series inside Appointment v1 | Premature complexity; separate audit later |
| Waitlist / overbooking in domain v1 | Product policy, not Core permanence |
| Store `MembershipId` as provider | Violates ADR-011 — use `StaffAssignmentId` |
| Embed clinical notes / odontogram / billing | God Aggregate — violates §3 |
| Remove denormalized `OrganizationId` | Forces cross-BC joins; harms query locality |
| Vertical-specific Appointment subtypes in Core | Fractures the platform; packs compose Core |
| Grow Appointment “temporarily” then split | Temporary growth becomes permanent |

---

## Compatibility check

| Document | Impact |
|----------|--------|
| ADR-003 | Appointment always tenant-scoped; `TenantId` immutable |
| ADR-006 | Unchanged — provider is StaffAssignment, not Identity |
| ADR-007 | Unchanged — membership-scoped RBAC; `appointment:*` later |
| ADR-010 | Unchanged — Organization / Office / StaffAssignment boundaries respected |
| ADR-011 | Appointment recipe: `StaffAssignmentId` + `OrganizationId` + optional `OfficeId`; never Membership as provider |
| ADR-012 | Unchanged — Patient remains registry only; Appointment references `PatientId` |
| ADR-013 | Normative consumption via ReferencePorts only |
| PATIENT / ORGANIZATION consumption guides | Appointment recipes aligned |
| DEVELOPMENT-POLICY-FASE-16-PLUS | §3 reinforces §§4–5–9 |

**No existing ADR is modified by this decision.**

---

## Freeze rule

The Appointment domain model defined by this ADR is **frozen** as of PASO **18.1**.

Any change to Aggregate Root boundaries, references (including removal of denormalized `OrganizationId`), lifecycle, or the §3 permanence principle **requires**:

1. A new architecture audit (DEVELOPMENT-POLICY-FASE-16-PLUS §2)  
2. A **new ADR**

Implementation steps 18.2+ must implement this contract — they must not reopen it.

---

## Acceptance

**Accepted** in PASO **18.1** (2026-07-11).  
Evidence: this ADR · [PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md](../audits/PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md) · [PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md](../audits/PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md).

---

## References

- [PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md](../audits/PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md)  
- [PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md](../audits/PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md)  
- [ADR-012-PATIENT-DOMAIN-MODEL.md](ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [PATIENT-CONSUMPTION-GUIDE.md](PATIENT-CONSUMPTION-GUIDE.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ARCHITECTURE-REVIEW-2026-07.md)  
- [ROADMAP.md](ROADMAP.md)  
