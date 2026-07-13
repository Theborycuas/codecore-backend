# PASO 24.7 — Audit Verification

**Entregable:** `AuditVerificationIT` 8/8 + persistence/reference ITs.

| # | Escenario |
|---|-----------|
| 1 | Append via port + GET |
| 2 | List filters |
| 3 | RBAC `audit:read` + unauthenticated 401 |
| 4 | Cross-tenant isolation |
| 5 | Invitation create → `invitation.created` |
| 6 | Invitation accept → `invitation.accepted` |
| 7 | Invalid actor → `ActorMembershipNotFoundException` |
| 8 | No HTTP POST (404/405) + OpenAPI group |

## Siguiente

**PASO 24.8 — Audit Closeout.**
