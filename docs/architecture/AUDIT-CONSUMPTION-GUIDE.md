# Audit Consumption Guide

**Audience:** Developers appending audit facts from other BCs (Access, IAM, future admin mutations)  
**Authority:** [ADR-020](ADR-020-AUDIT-ENTRY-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-009](ADR-009-PRODUCTION-READINESS-BACKLOG.md)  
**Status:** Vigente desde PASO 24.8 — Audit (AuditEntry) **cerrado**

---

## Mental model

FASE 24 Audit entrega un **bounded context estable**: el registro inmutable de que una acción significativa ocurrió en un Tenant (*intentionally small*, append-only).

```text
Tenant (IAM)
 └── AuditEntry                 ← immutable fact
      ├── actionCode            ← opaque ≤64 (e.g. invitation.created)
      ├── actorMembershipId?    ← optional; ACTIVE validated at append
      ├── resourceType + id     ← opaque; no cross-BC existence check
      └── outcome               ← SUCCESS|FAILURE (default SUCCESS)
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Cómo escribo un audit fact? | `AuditAppendPort.append` (`audit-contract`) |
| ¿Existe un entry en el tenant? | `AuditReferencePort.existsByIdAndTenant` |
| ¿Puedo leer audits? | Admin API + `audit:read` |
| ¿Hay POST HTTP para crear? | **No** — port only |

---

## Decision tree (30 seconds)

```text
Need to record that an action happened in a Tenant?
  → AuditAppendPort — do not invent DentalAuditLog / parallel tables

Need payloads / diffs / SIEM / metrics?
  → Wrong — Observability / future ADR; never grow AuditEntry for that

Need to mutate or delete an audit row?
  → Wrong — append-only forever in v1
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:audit-management:audit-contract"))
```

Only. Never `audit-application` or `audit-infrastructure` from another BC.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Append via `AuditAppendPort` | `INSERT INTO audit.audit_entry` from another BC |
| Best-effort `onErrorResume` in producers | Fail business TX because audit failed |
| Validate actor only when present | Require actor for public flows (password reset) |

---

## Contract surface (Audit closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `AuditEntryId` | `audit-contract` → domain VO | Hard identity |
| `AuditPermissionCatalog` | `audit-contract` | `audit:read` |
| `AuditAppendPort` | `audit-contract` | Cross-BC write |
| `AuditReferencePort` | `audit-contract` | Existence check |

### Producers v1

| Producer | actionCode | actor |
|----------|------------|-------|
| Invitation create | `invitation.created` | invitedBy |
| Invitation accept | `invitation.accepted` | resultingMembershipId |
| Invitation revoke | `invitation.revoked` | current membership or invitedBy |
| Password reset request | `password_reset.requested` | null |
| Password reset complete | `password_reset.completed` | null |

---

## References

- [ADR-020 — Audit Entry Domain Model](ADR-020-AUDIT-ENTRY-DOMAIN-MODEL.md)
- [PASO-24.8-AUDIT-CLOSEOUT.md](../audits/PASO-24.8-AUDIT-CLOSEOUT.md)
- [ADR-009 — Production Readiness Backlog](ADR-009-PRODUCTION-READINESS-BACKLOG.md) — Audit Trail **Done (FASE 24)**
