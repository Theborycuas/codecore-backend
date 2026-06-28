# PASO 16.4 — Organization Administration API

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** PASO-16.3.1 · ADR-008 · ADR-010

---

## Entregables

| Área | Detalle |
|------|---------|
| HTTP | `OrganizationAdminController` — `/api/v1/org/organizations` |
| Use cases | `OrganizationAdministrationUseCaseImpl` (list, get, create, update, archive, activate) |
| Query | `R2dbcOrganizationAdminQueryRepository` — paginación SQL + filtro `status` |
| Tenant | `IamTenantContextAccessor` → JWT `tenantId` |
| RBAC | `@RequiresPermission("organization:*")` |
| Wiring | `OrganizationAdministrationConfiguration` · `codecore-api` scan `com.codecore.organization` |

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/organizations` | `organization:read` |
| GET | `/organizations/{id}` | `organization:read` |
| POST | `/organizations` | `organization:create` |
| PUT | `/organizations/{id}` | `organization:update` |
| POST | `/organizations/{id}/archive` | `organization:archive` |
| POST | `/organizations/{id}/activate` | `organization:update` |

## Tests

- `OrganizationAdministrationUseCaseTest` — unit (create, duplicate code)
- `OrganizationAdminControllerIT` — WebFlux + Testcontainers (requiere Docker)

## Notas

- Sin endpoint `DELETE` — lifecycle soft únicamente.
- Cross-tenant → **404** vía `findByIdAndTenantId`.
- Archive bloqueado si existen offices ACTIVE (implementado en 16.6 guard).
