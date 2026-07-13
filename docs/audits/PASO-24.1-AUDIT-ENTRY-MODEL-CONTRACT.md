# PASO 24.1 — Audit Entry Model Contract (ADR-020)

**Entregable:** [ADR-020](../architecture/ADR-020-AUDIT-ENTRY-DOMAIN-MODEL.md) **Accepted**.

| Elemento | Valor |
|----------|-------|
| Definición | Immutable record that a significant action occurred in a Tenant |
| Lifecycle | Append-only |
| Refs | TenantId · actorMembershipId? · resourceType+resourceId opacos |
| Write | `AuditAppendPort` |
| Permisos | `audit:read` |
| HTTP / schema | `/api/v1/audit/entries` · `audit` |

## Siguiente

**PASO 24.2 → implementación** (domain … closeout).
