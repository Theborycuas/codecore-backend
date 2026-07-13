# PASO 23.4 — Invitation Persistence

**Entregable:** Schema `access`, tabla `access.invitation` (Flyway V30), adaptador R2DBC completo, capa de aplicación (`access-application`).

| Elemento | Valor |
|----------|-------|
| Migración | `V30__create_invitation_table.sql` |
| Tabla | `access.invitation` — sin FK a `iam.*` (ADR-013/ADR-019) |
| Índices | `token_hash` UNIQUE · `tenant_id` · `(tenant_id, status)` · `(tenant_id, invited_email)` |
| Check | `status IN ('PENDING','ACCEPTED','REVOKED','EXPIRED')` |
| Entidad | `InvitationEntity` (`Persistable<UUID>`) |
| Mapper | `InvitationMapper` — isomórfico `InvitationEntity` ↔ `Invitation` |
| Repos | `SpringDataInvitationRepository` · `R2dbcInvitationRepository` · `R2dbcInvitationAdminQueryRepository` (`DatabaseClient`, filtro status) |

## Application layer (`access-application`)

- Commands: `CreateInvitationCommand` · `AcceptInvitationCommand`
- Use cases in: `ListInvitationsUseCase` · `GetInvitationUseCase` · `CreateInvitationUseCase` · `RevokeInvitationUseCase` · `AcceptInvitationUseCase`
- `InvitationAdministrationUseCaseImpl` — IAM read ports en create; `TenantAccessProvisionPort` en accept; `TransactionalOperator` para atomicidad.
- `revoke` / `expire` **no** revalidan ports IAM (mismo patrón void Payment / void Invoice).

## Tests (Testcontainers PostgreSQL)

```
R2dbcInvitationRepositoryIT — 3/3 (persist, revoke lifecycle, findByTokenHash + pending email)
```

## Siguiente

**PASO 23.5 — Invitation Authorization Contract.**
