# PASO 24.5.1 — Audit Administration API Audit

**Entregable:** Shape HTTP (sin código de implementación en este paso documental) — espejo Payment read-only.

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/audit/entries` | `audit:read` |
| GET | `/api/v1/audit/entries/{id}` | `audit:read` |

**No:** POST/PUT/DELETE · `audit:create|update|delete`. Append vía `AuditAppendPort`.

Filtros list: `actionCode?`, `resourceType?`, `resourceId?`. Sort: `occurredAt`, `createdAt`, `actionCode`.

## Siguiente

**PASO 24.6 — Audit Administration API.**
