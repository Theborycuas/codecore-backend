# ADR-011 — Organization Integration Patterns

**Status:** Accepted  
**Date:** 2026-06-22  
**Deciders:** CodeCore architecture (FASE 16.8)  
**Relates to:** ADR-003, ADR-006, ADR-007, ADR-010, [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md), [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)

---

## Context

FASE 16.1–16.7 delivered the **Organization Management** bounded context:

| Aggregate | Purpose |
|-----------|---------|
| `Organization` | Business unit inside a tenant |
| `Office` | Physical/logical site under an organization |
| `StaffAssignment` | Operational scope linking IAM `MembershipId` to org/office |

Administration APIs exist under `/api/v1/org/**`. IAM remains unchanged except permission seeds.

From FASE 17 onward, **Patient**, **Appointment**, **Medical Record**, **Billing**, **Inventory**, and other modules must consume Organization **without reopening its architecture**.

PASO 16.8 does **not** add features. It **validates** the model against real downstream scenarios and **closes** the bounded context as a stable integration surface.

---

## Decision

### 1. Organization Management is a closed bounded context (FASE 16.8)

After 16.8, Organization Management is considered **feature-complete for v1**:

- Structural model: Tenant → Organization → Office
- Staff operational scope: `StaffAssignment`
- Administration HTTP + RBAC seeds
- Schema `org` (Flyway V14–V17)

Changes to aggregates, lifecycle, or cross-BC contracts require a **new ADR** and explicit audit — not ad-hoc edits during Patient or Billing work.

### 2. Consumption model — IDs and Query Ports only

External bounded contexts **must not**:

- Import `organization-domain` or `organization-infrastructure`
- Inject `OrganizationRepository`, `OfficeRepository`, or `StaffAssignmentRepository`
- Execute SQL against schema `org` directly
- Load IAM `Membership` or `Identity` to infer org/office scope

External bounded contexts **must**:

- Store **reference IDs** (`OrganizationId`, `OfficeId`, `StaffAssignmentId`, `TenantId`) in their own aggregates
- Validate existence and ACTIVE status via **Query Ports** published in `organization-contract` (when needed at runtime)
- Resolve operator scope from **`StaffAssignment`**, not from Membership alone
- Filter all reads/writes by **`tenantId` from JWT** (ADR-003)

```text
Consumer BC (Patient, Appointment, …)
        │
        │  depends on (Gradle)
        ▼
organization-contract          ← IDs, permission constants, Query Port interfaces
        │
        │  implemented by
        ▼
organization-infrastructure    ← R2DBC, HTTP — NOT visible to consumers
```

**Today (16.8):** `organization-contract` exposes `OrganizationPermissionCatalog`. Reference Query Ports (`OrganizationReferencePort`, `OfficeReferencePort`, `StaffAssignmentReferencePort`) are **specified here** and implemented in 16.9+ or at first consumer need — never by bypassing the contract.

### 3. Three reference types — when to use which

| Reference | Meaning | Use when |
|-----------|---------|----------|
| `OrganizationId` | Business entity scope (clinic, department, branch) | Patient **primary** org (optional), billing entity, report grouping |
| `OfficeId` | Location scope under an organization | Appointment/Encounter site, inventory warehouse, room — **not** on Patient |
| `StaffAssignmentId` | **Who operates where** (membership + scope) | Appointments, clinical actions, audit of performer |

**Rule:** `StaffAssignmentId` answers *“where does this staff member operate?”*  
`OfficeId` answers *“where is this thing located?”*  
Never substitute one for the other.

### 4. IAM vs Organization — separation of concerns

| Question | Ask | Never ask |
|----------|-----|-----------|
| Can this user access the tenant? | IAM — `Membership` + RBAC | Organization aggregate |
| What org/office can this user **operate** in? | `StaffAssignment` (list/filter by `membershipId`) | Office tree traversal from JWT alone |
| Is this organization active? | `OrganizationReferencePort` | `IdentityRepository` |
| Who is the person? | IAM — `Identity` | Organization module |

RBAC remains **membership-scoped** (ADR-007). FASE 16 does **not** introduce organization-scoped roles. Application-layer filters combine:

1. JWT `tenantId` (mandatory)
2. Permission grant (`patient:read`, etc.)
3. Optional data scope from `StaffAssignment` or explicit `organizationId` on the resource

### 5. Archive semantics for consumers

| Entity | Archived behavior for consumers |
|--------|----------------------------------|
| `Organization` / `Office` | **No new** child resources or assignments (409 on create). **Existing** references remain valid — historical data preserved (PASO-16.3.1). |
| `StaffAssignment` | **Physical delete** — revoked scope. Consumers must not assume assignment rows exist after delete. |

**Operational vs historical reads (clinical modules):**

- **Write / schedule / assign:** require ACTIVE org/office; resolve staff via ACTIVE `StaffAssignment`
- **Read history:** may display data tied to archived org/office or deleted assignment — show labels from snapshot or reference query at read time; do **not** block solely because org archived

### 6. Physical database boundaries

- Schema `org` — owned by Organization Management
- Schema `iam` — owned by IAM
- **No physical FK** from `org` to `iam` or vice versa
- Consumer tables store UUID references only; validation in application layer

### 7. Gradle dependency rule

```kotlin
// ✅ Allowed in patient-module, appointment-module, …
implementation(project(":modules:organization-management:organization-contract"))

// ❌ Forbidden
implementation(project(":modules:organization-management:organization-infrastructure"))
implementation(project(":modules:organization-management:organization-domain"))
```

---

## Module integration matrix (prescriptive)

| Consumer BC | Primary references | Resolves scope via | Must NOT use |
|-------------|-------------------|-------------------|--------------|
| **Patient** | `TenantId` + optional `PrimaryOrganizationId` | Registry identity; org = default grouping — **not** ownership; **no** `OfficeId` | `OfficeRepository`; IAM `Membership` for patient “location”; parallel registry aggregates |
| **Appointment** | `StaffAssignmentId`, `OrganizationId`, optional `OfficeId` | Provider = assignment, not membership | `MembershipRepository`; direct office lookup for “who treats” |
| **Medical Record** | `OrganizationId` and/or `StaffAssignmentId` | Org = custodian; assignment = author context | `Office` aggregate for clinical content ownership |
| **Billing** | `OrganizationId` | Invoice / subscription line entity | `Identity`, `Membership` |
| **Inventory** | `OfficeId`, `OrganizationId` | Stock location = office | `StaffAssignment` (staff location ≠ warehouse) |
| **Scheduling** (future) | `StaffAssignmentId`, `OfficeId` | Availability tied to assignment + room | Membership email lookup for calendar |

---

## Anti-patterns (prohibited)

| Anti-pattern | Why wrong | Correct approach |
|--------------|-----------|------------------|
| Patient module asks “what office does this user belong to?” | User scope ≠ patient data scope | Filter patients by policy; operator scope via `StaffAssignment` |
| Appointment stores `membershipId` as provider | Couples scheduling to IAM lifecycle | Store `StaffAssignmentId` |
| Medical Record loads `Office` aggregate | Violates BC boundary | Store `OrganizationId`; optional `OfficeId` as reference |
| Billing reads `Identity` for org name | Wrong BC | `OrganizationReferencePort.findNameById` |
| Inventory uses `StaffAssignment` for bin location | Assignment = people, not places | `OfficeId` on warehouse/location |
| Cross-module `@Autowired OrganizationRepository` | Hexagonal violation | Query Port in contract |
| `organizationId` in JWT (FASE 16) | Out of scope ADR-010 | Explicit query param or resource field + tenant filter |

---

## Validation — model vs scenarios (16.8)

| Scenario | Model support | Verdict |
|----------|---------------|---------|
| Multi-org tenant (3 clinics) | `Organization` per tenant, unique code | ✅ |
| Multi-office per org | `Office` aggregate + org FK | ✅ |
| Staff operates org-wide vs one office | `StaffAssignment` with optional `officeId` | ✅ |
| Staff linked to IAM without duplicating Identity | `membershipId` reference + `MembershipReferencePort` | ✅ |
| Cross-tenant isolation | `tenant_id` on all org tables + JWT filter | ✅ |
| Admin CRUD without org-scoped RBAC | Global permissions on membership | ✅ |
| Patient optional org scope | Not implemented — pattern defined in ADR-011 | ✅ Ready |
| Appointment provider reference | `StaffAssignmentId` pattern defined | ✅ Ready |
| Billing per organization | `OrganizationId` on billing aggregate (**FASE 21**) | ✅ Ready |
| Inventory per office | `OfficeId` on stock location (future) | ✅ Ready |

**Conclusion:** Organization Management v1 is **architecturally sufficient** for downstream modules. Remaining work is **consumption** (contract ports + consumer aggregates), not Organization redesign.

---

## Contract evolution (post-16.8)

**Normative pattern:** [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) (Accepted PASO 17.2).

Implemented in `organization-contract` / `organization-infrastructure`:

```java
public interface OrganizationReferencePort {
    Mono<Boolean> existsActiveByIdAndTenant(OrganizationId id, TenantId tenantId);
}

public interface OfficeReferencePort {
    Mono<Boolean> existsActiveInOrganization(OfficeId id, OrganizationId orgId, TenantId tenantId);
}

// StaffAssignmentReferencePort — add when Appointment (or first consumer) needs it
```

Shared ID value objects may be duplicated as lightweight UUID wrappers in contract (anti-corruption) or published as a shared kernel package — **never** import full aggregates.

---

## Consequences

### Positive

- FASE 17+ modules integrate predictably — no recurring architecture debates
- BC boundary enforced by Gradle and documentation
- Organization team can evolve internals without breaking consumers (contract-stable)

### Negative

- Consumers need thin Query Port adapters (acceptable hexagonal cost)
- Some display names require reference queries (no eager join across BC)

---

## Out of scope

- Implementing Patient, Appointment, or Billing aggregates
- Organization-scoped RBAC
- `organizationId` in JWT claims
- CQRS read models across BC (future ADR if needed)

---

## Acceptance criteria (16.8)

| Criterion | Status |
|-----------|--------|
| ADR-011 accepted | ✅ |
| Consumption guide published | ✅ |
| BC validation checklist complete | ✅ |
| ROADMAP 16.8 renamed and closed | ✅ |
| No Organization feature code in 16.8 | ✅ |

---

## Revision History

| Version | Date | Change |
|---------|------|--------|
| 1.0 | 2026-06-22 | Initial — Organization BC closure and integration patterns |
