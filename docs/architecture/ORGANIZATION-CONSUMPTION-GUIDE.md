# Organization Consumption Guide

**Audience:** Developers building modules after FASE 16 (Patient, Appointment, Billing, …)  
**Authority:** [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-010](ADR-010-ORGANIZATIONS-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
**Status:** Vigente desde PASO 16.8 · Reference Ports: PASO 17.2

---

## Mental model

FASE 16 is not “we built CRUD for orgs”. FASE 16 is **a stable bounded context** that other modules reference by ID.

```text
Tenant (IAM)
 └── Organization          ← business unit
      └── Office           ← location
           └── StaffAssignment  ← staff operates here (links MembershipId)
```

Three questions — three different answers:

| Question | Where to look |
|----------|---------------|
| Can this user log in to this tenant? | IAM — Membership + RBAC |
| Where does this **staff member operate**? | `StaffAssignment` (by `membershipId`) |
| Where does this **patient / stock / invoice** live? | `OrganizationId` / `OfficeId` on **that** aggregate |

---

## Decision tree (30 seconds)

```text
Need to scope DATA (patient, invoice, warehouse)?
  → Store OrganizationId and/or OfficeId on your aggregate

Need to scope an ACTION to a STAFF MEMBER (appointment, signature)?
  → Store StaffAssignmentId — never MembershipId alone

Need to validate org/office exists and is ACTIVE?
  → OrganizationReferencePort / OfficeReferencePort (`organization-contract`, ADR-013)
  → Never repositories, never full aggregates, never mutate via ports

Need to list “where can I work today?” for logged-in user?
  → StaffAssignmentReferencePort.findActiveByMembership(membershipId, tenantId)

Need permission to admin org structure?
  → organization:* / office:* / staff-assignment:* via @RequiresPermission
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:organization-management:organization-contract"))
```

Only. Never infrastructure or domain.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| `PrimaryOrganizationId` (`OrganizationId`) on Patient | `@Autowired R2dbcOrganizationRepository` |
| Validate via Query Port in use case | `SELECT * FROM org.organization` in Patient module |
| Filter by JWT `tenantId` always | Trust client-sent `tenantId` without JWT check |
| Use `StaffAssignmentId` for provider on Appointment | Store `membershipId` as provider key |

---

## Module recipes

### Patient (FASE 17 — closed)

**Owns:** clinical registry identity of the care subject  
**References:** `TenantId` (required, immutable), optional `PrimaryOrganizationId` (`OrganizationId`)  
**Published for consumers:** [PATIENT-CONSUMPTION-GUIDE.md](PATIENT-CONSUMPTION-GUIDE.md) · `PatientReferencePort`

```text
Patient
  tenantId                 ← ADR-003 (never changes)
  primaryOrganizationId    ← optional — registration / default grouping (NOT ownership)
```

**Never:**

- Store `OfficeId` on Patient — office belongs on Appointment / Encounter / Inventory
- Ask “what office does the logged-in user belong to?” to decide patient visibility
- Call `OfficeRepository` / `OrganizationRepository` from Patient module
- Treat primary organization as ownership of the person/subject

**Visibility pattern:**

1. JWT → `tenantId`
2. RBAC → `patient:read`
3. Application filter → assignments of current user **or** explicit org filter on query
4. Optional filter `WHERE tenant_id = ? AND primary_organization_id IN (?)`

---

### Appointment

**Owns:** scheduled encounter  
**References:** `StaffAssignmentId` (provider), `OrganizationId`, optional `OfficeId` (room/site)

```text
Appointment
  staffAssignmentId   ← who performs (NOT membershipId)
  organizationId
  officeId              ← where it happens
  patientId
```

**Never:**

- Load `Membership` to resolve provider name at write time without a port
- Infer provider office by walking Organization tree

**Read path:** resolve display name via IAM Query Port (identity) + optional org label via `OrganizationReferencePort`.

---

### Medical Record

**Owns:** clinical documentation  
**References:** `OrganizationId` (custodian / legal entity), optional `StaffAssignmentId` (author context)

```text
MedicalRecord
  organizationId        ← custodian org (required for multi-org tenants)
  authorAssignmentId    ← optional — who documented (StaffAssignmentId)
```

**Never:**

- Import `Office` aggregate — office is optional metadata on record, not ownership
- Use `OfficeId` as sole custodian when org-level custody is required

---

### Billing (FASE 21)

**Owns:** charges, invoices, payment allocation  
**References:** `OrganizationId` (billing entity / cost center)

```text
Invoice
  organizationId    ← bill under this clinic/branch
  tenantId
```

**Never:**

- Read `Identity` or `Membership` to determine billing entity
- Use `StaffAssignment` for invoice ownership (people ≠ billing unit)

Seat/subscription billing stays on IAM **Membership** count (ADR-006) — orthogonal to `OrganizationId`.

---

### Inventory

**Owns:** stock, movements, warehouses  
**References:** `OfficeId` (location), `OrganizationId` (denormalized guard)

```text
StockLocation
  officeId
  organizationId   ← must match office.organizationId (validate via port)
```

**Never:**

- Use `StaffAssignment` for warehouse location — assignments are for **people**
- Skip tenant filter because office “implies” tenant

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | Query Ports in `organization-contract` |
| **Frontend / mobile** | Call `/api/v1/org/**` for admin; business APIs return embedded IDs |
| **Reporting / analytics** | Read replicas + documented views — separate ADR if cross-schema |

Organization Administration API is for **tenant admins**, not for Patient module internals. Patient module validates references via ports, not via HTTP loopback.

---

## Testing consumers

When testing Patient or Appointment modules:

- Mock `OrganizationReferencePort` / `StaffAssignmentReferencePort`
- Do **not** spin up full Organization infrastructure unless running E2E
- Use Testcontainers org schema only in cross-BC integration tests (16.9 style)

---

## Checklist before merging a new consumer module

| # | Item |
|---|------|
| 1 | Gradle depends only on `organization-contract` |
| 2 | Aggregates store IDs, not foreign entity objects |
| 3 | No SQL against schema `org` outside Organization module |
| 4 | Staff actions use `StaffAssignmentId` where applicable |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | Archived org/office blocks **new** links, not historical reads |
| 7 | ADR-011 consulted for new reference types |

---

## Related documents

- [ADR-011 — Organization Integration Patterns](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)
- [ADR-010 — Organizations Model](ADR-010-ORGANIZATIONS-MODEL.md)
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) — §4 IDs, §5 consistency
- [PASO-16.8-ORGANIZATION-VALIDATION-INTEGRATION-PATTERNS.md](../audits/PASO-16.8-ORGANIZATION-VALIDATION-INTEGRATION-PATTERNS.md)
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [PASO-17.2-REFERENCE-CONTRACTS.md](../audits/PASO-17.2-REFERENCE-CONTRACTS.md)
