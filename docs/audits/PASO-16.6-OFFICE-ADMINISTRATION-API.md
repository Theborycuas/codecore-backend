# PASO 16.6 — Office Administration API

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** PASO-16.4 · PASO-16.5 · PASO-16.3.1

---

## Entregables

| Área | Detalle |
|------|---------|
| HTTP | `OfficeAdminController` — `/api/v1/org/offices` |
| Use cases | `OfficeAdministrationUseCaseImpl` |
| Query | `R2dbcOfficeAdminQueryRepository` — filtro por `organizationId` + paginación |
| RBAC | `@RequiresPermission("office:*")` |
| Guard org | `ArchiveOrganizationUseCase` → 409 si offices ACTIVE |

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/offices?organizationId=` | `office:read` |
| GET | `/offices/{id}` | `office:read` |
| POST | `/offices` | `office:create` |
| PUT | `/offices/{id}` | `office:update` |
| POST | `/offices/{id}/archive` | `office:archive` |
| POST | `/offices/{id}/activate` | `office:update` |

## Reglas aplicación

- Crear office solo bajo organization **ACTIVE** del tenant JWT.
- Código office único por `(organizationId, code)`.
- Reactivar office valida org ACTIVE.

## Tests

- `OfficeAdminControllerIT` — create/list, archive guard org, cascade archive

## Notas

- Listado offices requiere `organizationId` query param (scope explícito).
- ITs WebFlux requieren Docker (Testcontainers).
