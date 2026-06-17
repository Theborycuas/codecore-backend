# PASO 15.3 — Role Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP administrativa de roles bajo `/api/v1/iam/roles`.  
**Prerequisitos:** PASO-15.1-15.3 Design Audit §4, roles de sistema provisionados en FASE 14.8.

---

## 1. Resumen ejecutivo

Tercera capa administrativa IAM: gestión de roles custom por tenant. Los system roles (OWNER, ADMIN, …) son solo lectura; mutaciones y delete devuelven 403.

| Ítem | Estado |
|------|--------|
| Endpoints `/api/v1/iam/roles` | ✅ |
| `@RequiresPermission` (`role:*`) | ✅ |
| Listado paginado (incluye system roles) | ✅ |
| POST solo roles custom (`systemRole=false`) | ✅ |
| PUT rename / activate / deactivate (custom) | ✅ |
| DELETE físico custom sin asignaciones | ✅ |
| System roles inmutables | ✅ 403 |
| Rol en uso (`membership_role`) | ✅ 409 |
| Tests 15.3 | ✅ 17 tests |
| Suite IAM completa | No ejecutada (por diseño) |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| Listado tenant | `RoleAdminQueryRepository` paginado por `tenantId` del contexto |
| GET por id | `findById` + filtro `tenantId` → `404` cross-tenant |
| Alta custom | `Role.create(tenantId, code, name, now)` — `systemRole=false` |
| Código duplicado | `RoleAlreadyExistsException` → `409` |
| `code` inmutable | No expuesto en `UpdateRoleRequest` |
| System role mutación | `SystemRoleImmutableException` → `403` (antes de dominio) |
| DELETE custom | Físico vía `RoleRepository.delete` (CASCADE en `role_permission`) |
| Rol asignado a memberships | `existsByRoleId` → `RoleInUseException` → `409` |
| Paginación | `page`, `size`, `sort` (`code`, `name`, `status`, `createdAt`) |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/roles` | `role:read` |
| GET | `/api/v1/iam/roles/{id}` | `role:read` |
| POST | `/api/v1/iam/roles` | `role:create` |
| PUT | `/api/v1/iam/roles/{id}` | `role:update` |
| DELETE | `/api/v1/iam/roles/{id}` | `role:delete` |

---

## 4. Archivos principales

| Área | Archivos |
|------|----------|
| Dominio | `SystemRoleImmutableException`, `RoleNotFoundException`, `RoleAlreadyExistsException`, `RoleInUseException` |
| Repository | `RoleRepository.delete`, `MembershipRoleRepository.existsByRoleId` |
| Use cases | `RoleAdministrationUseCaseImpl`, `port.in.*AdminRole*` |
| Commands | `CreateAdminRoleCommand`, `UpdateAdminRoleCommand` |
| Queries | `RoleAdminQueryRepository`, `R2dbcRoleAdminQueryRepository`, `AdminRoleView` |
| HTTP | `IamRoleAdminController`, DTOs (`RoleResponse`, `PagedRoleResponse`, …) |
| Rutas | `IamAdminApiPaths.ROLES` |
| Config | `IamAdministrationConfiguration` (beans role) |
| Errores | `IamHttpExceptionHandler` (+404/403/409 role) |
| Tests | `RoleAdministrationUseCaseTest`, `IamRoleAdminControllerIT` |

---

## 5. Tests ejecutados (solo 15.3)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.RoleAdministrationUseCaseTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamRoleAdminControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `RoleAdministrationUseCaseTest` | 7 | ✅ |
| `IamRoleAdminControllerIT` | 10 | ✅ |
| **Total** | **17** | **BUILD SUCCESSFUL** |

### Cobertura HTTP (IT)

- List (≥5 system roles) / Create / Update / Delete custom
- 401 sin JWT
- 403 sin `role:read`
- 403 delete system role
- 404 rol de otro tenant
- 409 código duplicado / rol en uso

---

## 6. Próximo paso

**15.4 — Permission Administration** (catálogo `permission:read`).
