# PASO 24.4 — Audit Persistence

**Entregable:** Schema `audit` · V33 `audit.audit_entry` · R2DBC adapters · persistence ITs.

| Elemento | Valor |
|----------|-------|
| Migration | `V33__create_audit_entry_table.sql` |
| Columns | audit_entry_id, tenant_id, occurred_at, action_code, actor_membership_id?, resource_type, resource_id, outcome, created_at |
| Indexes | tenant_id; (tenant_id, occurred_at DESC); (tenant_id, action_code); (tenant_id, resource_type, resource_id) |
| FK | **Ninguna** a `iam.*` (ADR-013) |
| CHECK | outcome ∈ SUCCESS\|FAILURE |

## Resultado

`R2dbcAuditEntryRepositoryIT` + `AuditReferencePortIT` verdes.

## Siguiente

**PASO 24.5 — Audit Authorization Contract.**
