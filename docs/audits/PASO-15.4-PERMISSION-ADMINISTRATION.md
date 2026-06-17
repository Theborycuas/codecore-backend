# PASO 15.4 — Permission Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP de lectura del catálogo global de permisos bajo `/api/v1/iam/permissions`.  
**Prerequisitos:** ADR-008, seeds V13, `IamPermissionCatalog`.

---

## 1. Resumen ejecutivo

Cuarta capa administrativa IAM: exposición **solo lectura** del catálogo global de `Permission` (no tenant-scoped). Sin mutaciones en este paso — asignación a roles queda en 15.5.

| Ítem | Estado |
|------|--------|
| Endpoints `/api/v1/iam/permissions` | ✅ |
| `@RequiresPermission("permission:read")` | ✅ |
| Listado paginado global | ✅ |
| GET por id | ✅ |
| Mutaciones (create/update/delete) | Fuera de alcance |
| Tests 15.4 | ✅ 8 tests |
| Suite IAM completa | No ejecutada (por diseño) |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| Alcance read-only | Solo `GET` list + `GET` by id (ADR-008) |
| Catálogo global | Sin filtro por tenant; permisos sembrados en V13 |
| Autorización | JWT + `permission:read` vía `@RequiresPermission` |
| Contexto | `AuthorizationContextAccessor.current()` antes de consultar |
| Paginación | `page`, `size`, `sort` (`code`, `createdAt`; default `code,asc`) |
| Not found | `PermissionNotFoundException` → `404` |
| DTO HTTP | `PermissionResponse` con `systemPermission` expuesto |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/permissions` | `permission:read` |
| GET | `/api/v1/iam/permissions/{id}` | `permission:read` |

---

## 4. Archivos principales

| Área | Archivos |
|------|----------|
| Dominio | `PermissionNotFoundException` |
| Use cases | `PermissionAdministrationUseCaseImpl`, `port.in.List/GetAdminPermission*` |
| Queries | `PermissionAdminQueryRepository`, `R2dbcPermissionAdminQueryRepository`, `AdminPermissionView` |
| HTTP | `IamPermissionAdminController`, `PermissionResponse`, `PagedPermissionResponse` |
| Rutas | `IamAdminApiPaths.PERMISSIONS` |
| Config | `IamAdministrationConfiguration` (beans permission) |
| Errores | `IamHttpExceptionHandler` (+404 permission) |
| Tests | `PermissionAdministrationUseCaseTest`, `IamPermissionAdminControllerIT` |

---

## 5. Tests ejecutados (solo 15.4)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.PermissionAdministrationUseCaseTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamPermissionAdminControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `PermissionAdministrationUseCaseTest` | 3 | ✅ |
| `IamPermissionAdminControllerIT` | 5 | ✅ |
| **Total** | **8** | **BUILD SUCCESSFUL** |

### Cobertura HTTP (IT)

- List (≥16 permisos del catálogo V13) / Get by id
- 401 sin JWT
- 403 sin `permission:read` (rol READ_ONLY)
- 404 id desconocido

---

## 6. Próximo paso

**15.5 — Role Permission Administration** (`permission:assign` → `/api/v1/iam/roles/{id}/permissions`).
