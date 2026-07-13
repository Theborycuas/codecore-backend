# PASO 24.0.1 — AuditEntry Aggregate Audit (DDD Estratégico)

**AuditEntry** es el hecho inmutable de acción bajo un Tenant — *intentionally small*, append-only, primer Aggregate Root del BC **Audit**.

**Fecha:** 2026-07-13  
**Estado:** ✅ Completado (solo arquitectura)  
**Dependencias:** [PASO-24.0](PASO-24.0-AUDIT-FOUNDATION-PLANNING.md) · ADR-003 · ADR-007 · ADR-009 · ADR-013 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Checklist política (§8)

| # | Ítem | ✓ |
|---|------|---|
| 1–12 | Aggregate / ownership / invariants / lifecycle / permissions / IDs / BC / ADRs / escala | ✅ |

**Veredicto:** listo para **ADR-020**.

---

## Decisiones irreversibles

| Decisión | Elección |
|----------|----------|
| Naturaleza | Registro **inmutable** de que una acción significativa ocurrió en un Tenant |
| Root | **`AuditEntry`** |
| Lifecycle | **Append-only** — create → (terminal). Sin update, void, delete, un-append |
| Tenant | Obligatorio e inmutable |
| `actionCode` | Obligatorio · opaco · snake/dot style ≤64 (ej. `invitation.accepted`) — **no** free text |
| Actor | `actorMembershipId?` — opcional; si presente validar ACTIVE vía IAM port en append |
| Recurso | `resourceType` (≤64) + `resourceId` (UUID string) — **opacos**; sin validar existencia |
| Payload / diff / request body | **Fuera de v1** |
| Outcome | Opcional `outcome` SUCCESS\|FAILURE (default SUCCESS) — sin stack traces |
| Permisos | `audit:read` (admin). Append vía **port** (no HTTP público). Sin `audit:update\|delete` |
| Módulo / schema / HTTP | `audit-management` · `audit` · `/api/v1/audit/entries` |
| Productores v1 | Access Invitation · IAM PasswordReset (application only) |

---

## One-sentence

> **AuditEntry** = el registro inmutable de que una acción significativa ocurrió en un Tenant.

---

## Invariantes

1. Exactamente un `TenantId`, inmutable.  
2. `actionCode` no blank, ≤64.  
3. `resourceType` + `resourceId` siempre presentes (opacos).  
4. `occurredAt` obligatorio.  
5. Sin mutación tras create.  
6. Sin SQL/repos a BCs ajenos.  
7. Cross-tenant imposible en lecturas admin.  
8. Única “audit fact” role en Core — no “DentalAuditLog” paralelo.

---

## Permanence

> **AuditEntry is intentionally small.**

Metrics, traces, SIEM, payloads PII, Event Bus y retention engines **no** viven dentro. Ampliar = nuevo ADR.

---

## Prep. ADR-020

Congelar: definición, append-only, campos, actionCode opaco, actor opcional + port, resource opaco, permisos read, freeze rule.

## Siguiente

**PASO 24.1 — ADR-020**.
