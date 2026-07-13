# PASO 23.6 — Invitation Administration API

**Entregable:** `InvitationAdminController` + `InvitationAcceptController` + `AccessModuleConfiguration` + OpenAPI — HTTP completo sobre 23.3/23.4/23.5.

| Elemento | Valor |
|----------|-------|
| Admin | `InvitationAdminController` — `GET /api/v1/access/invitations` · `GET /{id}` · `POST /` · `POST /{id}/revoke` |
| Accept (público) | `InvitationAcceptController` — `POST /api/v1/access/invitations/accept` (token + password opcional) |
| Permisos | `@RequiresPermission("invitation:read\|create\|revoke")` — accept **sin** JWT RBAC |
| DTOs admin | `CreateInvitationRequest` · `InvitationResponse` · `InvitationCreatedResponse` · `PagedInvitationResponse` |
| DTOs accept | `AcceptInvitationRequest` · `AcceptInvitationResponse` |
| Email | `LoggingSendInvitationEmailAdapter` (`SendInvitationEmailPort`) — stub logging |
| OpenAPI | `AccessOpenApiConfiguration` — grupo `access-administration` |

## Wiring `codecore-api`

- `implementation(projects.modules.accessManagement.accessInfrastructure)`
- `scanBasePackages` += `"com.codecore.access"`
- Beans IAM: `TenantAccessProvisionPort` + reference ports (23.2)

## Unit tests

```
InvitationAdministrationUseCaseImplTest — 10/10
(create OK, inviter inactive, active membership, pending duplicate, role missing, OWNER rejected,
 revoke sin revalidar ports, accept→provision, expire, token unknown)
```

## Siguiente

**PASO 23.7 — Invitation Verification.**
