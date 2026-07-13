# ADR-020 — Audit Entry Domain Model

**Status:** Accepted  
**Date:** 2026-07-13  
**Accepted:** 2026-07-13 (PASO 24.1)  
**Deciders:** CodeCore architecture (FASE 24.1)  
**Relates to:** ADR-003 · ADR-007 · ADR-009 · ADR-013 · [PASO-24.0.1-AUDIT-ENTRY-AGGREGATE-AUDIT.md](../audits/PASO-24.0.1-AUDIT-ENTRY-AGGREGATE-AUDIT.md)

---

## Context

ADR-009 tracks **Audit Trail** as P2 production debt. After FASE 16–23, CodeCore has rich admin mutations (Org → Payment → Invitation) with **no** structured, tenant-scoped accountability store. Observability (metrics/tracing) remains a separate concern.

---

## Decision

### 1. Bounded context

**Audit** — transversal platform; independent of clinical/economic BCs and of Observability.

Gradle: `audit-management` · Schema: `audit` · HTTP: `/api/v1/audit/entries`

### 2. What AuditEntry is

> **The immutable record that a significant action occurred within a Tenant.**

**Not:** log line · span · metric · SIEM alert · Domain Event bus · business aggregate mutation.

### 3. Permanence — intentionally small

May contain only: identity, tenant, occurredAt, actionCode, optional actorMembershipId, resourceType + resourceId, optional outcome, timestamps. Append-only.

### 4. Lifecycle

```text
(create/append) → (immutable terminal)
```

No update, void, delete, or un-append in v1.

### 5. References

| Ref | Rule |
|-----|------|
| `TenantId` | Required, immutable |
| `actorMembershipId?` | Optional; if present, ACTIVE via IAM reference port at append |
| `resourceType` + `resourceId` | Required opaque identifiers — **no** cross-BC existence check |

### 6. Writing

Other BCs append via **`AuditAppendPort`** (contract). Prefer port over public HTTP write API in v1.

### 7. Permissions

`audit:read` for admin query. No `audit:update` / `audit:delete`.

### 8. Freeze

Adding payloads/PII dumps, metrics, Event Bus requirement, or mutable corrections requires a **new ADR**.

---

## Consequences

**Positive:** Horizontal accountability for every SaaS on CodeCore; Access/IAM can append without engordar Invitation.  
**Deferred:** Full retrofit of FASE 16–22, retention, Observability stack.  
**Neutral:** `AuditReferencePort` at closeout.

---

## Acceptance

**Accepted** PASO 24.1 (2026-07-13).
