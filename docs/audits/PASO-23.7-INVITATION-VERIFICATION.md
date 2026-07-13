# PASO 23.7 — Invitation Verification

**Entregable:** `InvitationVerificationIT` — 8 verificaciones E2E HTTP sobre stack completo (IAM + Access).

| # | Verificación | Resultado |
|---|--------------|-----------|
| 1 | Journey create → list → get → accept → Membership | ✅ |
| 2 | READ_ONLY no puede crear invitation | ✅ 403 |
| 3 | Cross-tenant access | ✅ 404 |
| 4 | Rol OWNER rechazado | ✅ |
| 5 | Duplicate ACTIVE membership email+tenant | ✅ |
| 6 | MANAGER puede revoke per matriz RBAC | ✅ 200 |
| 7 | Accept de invitation expirada rechazado | ✅ |
| 8 | OpenAPI documenta `access-administration` | ✅ |

## Test configuration

`AccessAdministrationVerificationTestConfiguration` → `AccessAdminIntegrationTestConfiguration`:

- IAM completo (auth, tenants, roles, `TenantAccessProvisionAdapter`)
- `AccessModuleConfiguration` + `InvitationAdminController` + `InvitationAcceptController`
- R2DBC invitation repos + `R2dbcInvitationReferenceAdapter`

## Resultado

```
InvitationVerificationIT → 8/8 verde
```

## Siguiente

**PASO 23.8 — Access Closeout.**
