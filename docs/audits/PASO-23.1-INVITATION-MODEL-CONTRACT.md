# PASO 23.1 — Invitation Model Contract (ADR-019)

**Entregable:** [ADR-019](../architecture/ADR-019-INVITATION-DOMAIN-MODEL.md) **Accepted**.

| Elemento | Valor |
|----------|-------|
| Definición | Intention to grant Membership in a Tenant to an email |
| Estados | `PENDING` · `ACCEPTED` · `REVOKED` · `EXPIRED` |
| Refs | `TenantId` · email · roleCode ≠ OWNER · invitedByMembershipId |
| Ports | IAM read + `TenantAccessProvisionPort` (accept) |
| Permisos | `invitation:create\|read\|revoke` (+ accept por token) |
| HTTP / schema | `/api/v1/access/invitations` · `access` |
| Módulo | `access-management` (Opción A) |

## Siguiente

**PASO 23.2 — Access / IAM Reference Readiness**.
