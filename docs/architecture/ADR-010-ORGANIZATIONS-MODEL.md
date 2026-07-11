# ADR-010 — Organizations Model

**Status:** Accepted  
**Date:** 2026-06-22  
**Deciders:** CodeCore architecture (FASE 16)  
**Relates to:** ADR-003 (Multi-Tenant Isolation), ADR-006 (Identity), ADR-007 (Authorization), ADR-008 (IAM Admin API), [PASO-16.0.1](../audits/PASO-16.0.1-ORGANIZATIONS-ROADMAP.md)

---

## Context

IAM Foundation is complete (FASE 15). CodeCore must model **business structure** inside a tenant account — clinics, hospital departments, regional branches — without altering Identity, Membership, or RBAC (ADR-006, ADR-007).

CONTEXT-MAP §26 mandates:

```text
tenant != organization
```

PASO-16.0.1 closed all architectural decisions. FASE 16.1 implements the domain foundation.

---

## Decision

### 1. Bounded context

**Organization Management** — new bounded context, downstream of IAM.

IAM provides authentication, tenant isolation, and membership-scoped RBAC. Organization Management provides structural grouping for business operations.

### 2. Target hierarchy

```text
Tenant (IAM — SaaS isolation)
 └── Organization (1..N)
      └── Office (0..N)              — FASE 16.5+
           └── StaffAssignment         — FASE 16.7+ (references MembershipId)
```

### 3. Ownership rules

| Entity | Belongs to | Notes |
|--------|------------|-------|
| Organization | **Tenant** | `tenant_id` NOT NULL, immutable |
| Office | **Organization** | `organization_id` required; `tenant_id` denormalized (ADR-003) |
| StaffAssignment | **Tenant** + scope | References `membershipId` (IAM); optional `organizationId` / `officeId` |

Organization **does not** belong to Identity or Membership.

### 4. Cardinality

- One tenant → **many** organizations (required for multi-site scenarios).
- Organization **cannot** exist without a tenant.

### 5. Aggregates (FASE 16 scope)

| Aggregate | Root | Transaction boundary |
|-----------|------|----------------------|
| **Organization** | `Organization` | Name, code, status lifecycle |
| **Office** | `Office` (separate) | FASE 16.5 — not nested inside Organization aggregate |

Small aggregates per ADR-005 / aggregate design rules.

### 6. Organization lifecycle

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Operational |
| `ARCHIVED` | Soft-retired; no physical delete |

Behaviors: `rename`, `activate`, `archive`. No physical delete.

### 7. OrganizationCode

Functional business identifier, unique per `(tenant_id, code)`.

Format: `UPPER_SNAKE_CASE` — e.g. `DENTAL_NORTE`, `CARDIOLOGIA`, `EMERGENCIAS`.

Normalized on construction (trim, uppercase).

### 8. Permissions (deferred to 16.3)

New global catalog entries — **not** IAM permissions:

| Resource | Permissions |
|----------|-------------|
| Organization | `organization:read`, `organization:create`, `organization:update`, `organization:delete` |
| Office | `office:read`, `office:create`, `office:update`, `office:delete` |
| Staff assignment | `staff-assignment:read`, `staff-assignment:update` |

RBAC remains **membership-scoped** (ADR-007). FASE 16 does not introduce organization-scoped roles.

### 9. Physical placement

New Gradle module:

```text
modules/organization-management/
  organization-domain/
  organization-application/
  organization-infrastructure/
  organization-contract/
```

- Schema PostgreSQL: `org` (Flyway — FASE 16.2)
- HTTP base path: `/api/v1/org/**` (FASE 16.4+, symmetric to ADR-008)
- **No** code inside `identity-access-management` except permission seeds (16.3)

### 10. IAM boundaries (unchanged)

| IAM concept | Role | Modified in FASE 16? |
|-------------|------|----------------------|
| Tenant | SaaS isolation | No |
| Identity | Global authentication | No |
| Membership | Person ↔ tenant access | No |
| Role / Permission | RBAC | Seeds only (16.3) |

Staff operational scope uses `StaffAssignment(membershipId, …)` — references IAM without duplicating Identity/Membership.

### 11. TenantId reference

Organization domain defines `TenantId` as a **reference value object** (UUID) — tenant aggregate remains owned by IAM. No compile-time dependency on IAM domain from `organization-domain`.

---

## Compliance

| ADR | Compliance |
|-----|------------|
| ADR-003 | All org tables include `tenant_id`; queries filtered by JWT tenant |
| ADR-006 | Membership unchanged; staff links via `membershipId` |
| ADR-007 | Permissions evaluated on membership; org permissions are resource grants |
| ADR-008 | Admin API pattern replicated under `/api/v1/org/**` |

---

## Consequences

### Positive

- Clear separation IAM vs business structure.
- Supports clinics, hospitals, multi-branch enterprises, SaaS multi-tenant.
- Incremental delivery 16.1 → 16.10 without reopening IAM.

### Negative

- `TenantId` duplicated as reference VO in org bounded context (anti-corruption).
- Permission seeds require minimal IAM touch in step 16.3.

---

## Out of scope (FASE 16.1)

- Flyway / R2DBC / HTTP / OpenAPI
- Office, StaffAssignment aggregates
- Permission seeds
- Patient entity (FASE 17 — Clinical Foundation)
- `organizationId` in JWT

---

## Acceptance criteria (16.1)

| Criterion | Status |
|-----------|--------|
| ADR-010 accepted | ✅ |
| Module `organization-management` created | ✅ |
| Aggregate `Organization` + value objects | ✅ |
| Outbound ports defined | ✅ |
| Domain tests green | ✅ |
| No persistence or HTTP | ✅ |

---

## Revision History

| Version | Date | Change |
|---------|------|--------|
| 1.0 | 2026-06-22 | Initial acceptance — Organizations domain foundation |
| 1.1 | 2026-06-22 | Superseded §8 permissions table — see V15 catalog; integration closed in ADR-011 |
