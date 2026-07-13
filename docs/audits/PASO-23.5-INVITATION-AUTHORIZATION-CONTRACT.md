# PASO 23.5 — Invitation Authorization Contract

**Entregable:** `InvitationPermissionCatalog` (3 permisos) + `IamPermissionCatalog` extendido + `SystemRoleTemplate` actualizado + Flyway V31.

| Elemento | Valor |
|----------|-------|
| Permisos | `invitation:create` · `invitation:read` · `invitation:revoke` |
| Migración | `V31__seed_invitation_authorization_contract.sql` — idempotente (`NOT EXISTS`), backfill `role_permission` |
| `IamPermissionCatalog.ALL` | **52 → 55** |

Accept es **público por token** — **no** hay `invitation:accept` en el catálogo RBAC.

## Matriz RBAC

| Rol | `invitation:create` | `invitation:read` | `invitation:revoke` |
|-----|:---:|:---:|:---:|
| OWNER | ✅ | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ |
| MANAGER | ✅ | ✅ | ✅ |
| USER | ❌ | ✅ | ❌ |
| READ_ONLY | ❌ | ✅ | ❌ |

Mismo patrón que `payment:*` (V29) — sin resend/update/delete/seat grants.

## Tests actualizados

- `SystemRoleTemplateTest` — `INVITATION_PLATFORM_ALL` (3), `ALL` = 55, matrices OWNER/ADMIN/MANAGER/USER/READ_ONLY.
- `InvitationPermissionCatalogTest` — niega `invitation:accept|resend|update|delete|expire`.
- `EXPECTED_TOTAL_PERMISSION_COUNT` 52→55 en `*AuthorizationSeedMigrationIT`; `appliedMigrationVersion()` → 31.

## Gradle

`identity-access-management` → `implementation(projects.modules.accessManagement.accessContract)`.

## Resultado

```
:modules:identity-access-management:test --tests "*SystemRoleTemplateTest*" --tests "*AuthorizationSeedMigrationIT*" → verde
```

## Siguiente

**PASO 23.6 — Invitation Administration API** (HTTP shape ya fijado en ADR-019 / 23.0.1).
