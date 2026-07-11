# ADR-013 — Bounded Context Reference Contracts

**Status:** Accepted  
**Date:** 2026-07-11  
**Deciders:** CodeCore architecture (FASE 17.2)  
**Relates to:** ADR-001 · ADR-003 · ADR-004 · ADR-005 · ADR-010 · ADR-011 · ADR-012 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md) · [PASO-17.2-REFERENCE-CONTRACTS.md](../audits/PASO-17.2-REFERENCE-CONTRACTS.md)

---

## Context

CodeCore integrates bounded contexts **only by IDs** (policy §4, ADR-011). Consumers must still **validate** foreign references at write time (e.g. Patient → `PrimaryOrganizationId` ACTIVE in tenant — ADR-012).

Without a shared pattern, each team invents ad-hoc queries, repository leaks, fat DTOs, or “internal REST” between modules — collapsing modular monolith boundaries over years.

PASO 17.2 needs `OrganizationReferencePort` for Patient. The same shape will recur for Patient, Office, StaffAssignment, Subscription, Billing, and dozens of future BCs.

---

## Decision

### 1. Name

**Reference Contract** — a published, read-only integration surface owned by the **providing** bounded context and consumed by **downstream** BCs.

Primary artifact: `{Resource}ReferencePort` in `{bc}-contract`.

### 2. Responsibility of a ReferencePort

A ReferencePort answers **reference questions** required to protect the **consumer’s** invariants:

- Does this ID exist in this tenant?
- Is it in a state that allows a new link (typically ACTIVE)?
- (Optional, rare) What minimal immutable label/scope is needed for display or denormalized guard — **never** a full aggregate.

It is the **anti-corruption / query** face of the provider BC toward other BCs.

### 3. What a ReferencePort must never do

| Forbidden | Why |
|-----------|-----|
| Expose domain aggregates / entities | Leaks BC internals; invites mutation |
| Expose persistence entities or SQL | Breaks hexagonal boundary |
| Accept or perform **mutations** | Write ownership stays in provider’s application API / use cases |
| Replace Administration HTTP APIs | Humans/admin UIs use `/api/v1/{bc}/**`; ports are for **in-process** module integration |
| Become a generic “god query API” | Only operations justified by consumer invariants |
| Be injected as the provider’s repository | Repositories stay inside the provider BC |
| Create circular Gradle dependencies | Provider contract ← consumer; never the reverse into provider domain/infra |

### 4. Return types

| Allowed | Disallowed |
|---------|------------|
| `Mono<Boolean>` / `boolean` for existence / active checks | Full aggregates |
| Small immutable **reference views** (IDs + status + optional display code/name) when a boolean is insufficient | Open-ended DTO graphs, collections of children, permissions blobs |
| `Mono<Optional<…>>` / empty Mono for resolve | Mutable builders shared across BCs |

**Default:** prefer **boolean existence/active** methods. Add a view type only when a consumer invariant needs more than true/false (e.g. StaffAssignment scope).

### 5. Valid operations (vocabulary)

| Operation | Purpose | Typical signature |
|-----------|---------|-------------------|
| `exists…` | ID present in tenant (any status) | `Mono<Boolean>` |
| `existsActive…` | ID present **and** linkable for writes | `Mono<Boolean>` — **default for new links** |
| `resolve…` / `find…Reference` | Minimal view for display or multi-field guard | `Mono<Optional<XReferenceView>>` |

**Avoid** a vague `validate(...)` that throws domain exceptions of the **provider** — consumers own how to map `false` → their own domain error (e.g. `OrganizationNotActiveException` equivalent in Patient BC).

**No** mutate / save / archive / assign methods on ReferencePorts.

### 6. Not an internal remote API

In the modular monolith:

- ReferencePorts are **Java interfaces** + in-process adapters (R2DBC, etc.).
- They are **not** HTTP controllers, Feign clients, or message commands.
- If CodeCore later splits deployables, the **same contract** may gain a remote adapter — without changing consumer call sites. Do **not** design HTTP first.

### 7. Module placement

```text
{providing-bc}-contract
  └── …reference/{Resource}ReferencePort.java
  └── …reference/{Resource}ReferenceView.java   (only if needed)

{providing-bc}-infrastructure
  └── …adapters/R2dbc{Resource}ReferenceAdapter implements …ReferencePort
```

Consumers:

```kotlin
implementation(project(":modules:…:{bc}-contract"))
// NEVER: -domain or -infrastructure of the provider
```

Gradle: provider `infrastructure` implements the port; `codecore-api` wires the module. Consumer application depends on **contract only**.

### 8. ID types on the contract

Prefer **provider-owned ID value objects** published via `{bc}-contract` (today: `organization-contract` → `api` on `organization-domain` IDs).  

Do **not** force consumers to depend on provider **application** or **aggregates**.  
Future option: move ID VOs into contract / shared-kernel — without changing ReferencePort semantics.

### 9. Family (repeatable template)

| Port | Provider contract | First consumer need |
|------|-------------------|---------------------|
| `OrganizationReferencePort` | `organization-contract` | Patient `PrimaryOrganizationId` (17.2) |
| `OfficeReferencePort` | `organization-contract` | Appointment / Inventory |
| `StaffAssignmentReferencePort` | `organization-contract` | Appointment |
| `PatientReferencePort` | `patient-contract` | Appointment / MedicalRecord / Billing |
| `MembershipReferencePort` | future `iam-contract` | Org StaffAssignment (today: transitional port in org-application) |
| `SubscriptionReferencePort` / `BillingReferencePort` | future commercial contracts | Seat checks, invoice links |

Each new port: **minimal methods**, tenant-scoped, read-only, documented consumer invariant.

### 10. First concrete port (normative for 17.2)

```java
public interface OrganizationReferencePort {
    /**
     * True when organization exists for tenant and status is ACTIVE.
     * Use for write-time links (e.g. Patient.PrimaryOrganizationId).
     */
    Mono<Boolean> existsActiveByIdAndTenant(OrganizationId organizationId, TenantId tenantId);
}
```

Historical reads that allow archived orgs use a separate `existsByIdAndTenant` **only when a consumer invariant requires it** — not by default on this port in 17.2.

---

## Consequences

### Positive

- One integration dialect for decades of BCs  
- Protects ADR-012 “intentionally small” Patient and all future roots  
- Enforces ADR-011 Gradle/contract rules in code  
- Testable with mocks at consumer boundary  

### Negative

- Slight ceremony per provider resource  
- Display names may need an extra resolve call (acceptable)  

### Migration note

`MembershipReferencePort` inside `organization-application` is a **transitional** outbound port (Org → IAM) until IAM publishes `iam-contract`. New cross-BC reads **must** follow this ADR (provider `*-contract`).

---

## Alternatives considered

| Alternative | Rejected because |
|-------------|------------------|
| Shared database joins across schemas | Couples BCs; breaks ownership |
| Consumer calls provider HTTP admin API | Wrong audience; latency; auth noise |
| Expose `OrganizationRepository` to consumers | Hexagonal / BC violation |
| Fat “OrganizationDto” on every call | God DTO; version churn |
| Only document pattern without ADR | Irreversible convention — needs ADR |

---

## Acceptance

Accepted with PASO **17.2**. First implementation: `OrganizationReferencePort` + R2DBC adapter.

---

## References

- [PASO-17.2-REFERENCE-CONTRACTS.md](../audits/PASO-17.2-REFERENCE-CONTRACTS.md)  
- [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
