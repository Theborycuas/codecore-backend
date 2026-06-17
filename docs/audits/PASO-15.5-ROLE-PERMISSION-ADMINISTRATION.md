# PASO 15.5 — Role Permission Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP para administrar asignaciones Role ↔ Permission bajo `/api/v1/iam/roles/{roleId}/permissions`.  
**Prerequisitos:** PASO-15.3 (Role Administration), PASO-15.4 (Permission catalog), ADR-007/008.

---

## 1. Resumen ejecutivo

Quinta capa administrativa IAM: sincronización de permisos sobre roles custom del tenant. El PUT recibe la lista completa deseada; el sistema calcula altas y bajas vía dominio `Role.assignPermission` / `revokePermission` y persiste con `RolePermissionRepository.replaceAll`.

| Ítem | Estado |
|------|--------|
| `GET /api/v1/iam/roles/{roleId}/permissions` | ✅ |
| `PUT /api/v1/iam/roles/{roleId}/permissions` | ✅ |
| `@RequiresPermission("permission:assign")` | ✅ |
| System roles inmutables (PUT) | ✅ 403 |
| Tenant isolation (role del contexto) | ✅ 404 cross-tenant |
| Permiso inexistente en PUT | ✅ 404 |
| Tests 15.5 | ✅ 11 tests |
| Suite IAM completa | No ejecutada (por diseño) |



---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| PUT replace semantics | Lista completa `permissionIds` → diff dominio → `replaceAll` |
| GET system roles | Permitido (solo lectura de asignaciones provisionadas) |
| PUT system roles | `SystemRoleImmutableException` → 403 |
| Validación permisos | Cada `permissionId` debe existir en catálogo global |
| IDs duplicados en request | `InvalidDomainValueException` → 400 |
| Tenant scope | `role.tenantId()` vs `AuthorizationContext.tenantId()` |
| Cadena RBAC | Role (tenant) → RolePermission → Permission (global) |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/roles/{roleId}/permissions` | `permission:assign` |
| PUT | `/api/v1/iam/roles/{roleId}/permissions` | `permission:assign` |

### Request PUT

```json
{
  "permissionIds": ["uuid-1", "uuid-2"]
}
```

### Response (lista)

```json
[
  {
    "permissionId": "uuid",
    "code": "user:read",
    "description": "...",
    "assignedAt": "2026-06-17T..."
  }
]
```

---

## 4. Archivos principales

| Área | Archivos |
|------|----------|
| Use cases | `RolePermissionAdministrationUseCaseImpl`, `port.in.Get/ReplaceAdminRolePermissions*` |
| Command | `ReplaceAdminRolePermissionsCommand` |
| Queries | `RolePermissionAdminQueryRepository`, `R2dbcRolePermissionAdminQueryRepository`, `AdminRolePermissionView` |
| HTTP | `IamRolePermissionAdminController`, `RolePermissionResponse`, `ReplaceRolePermissionsRequest` |
| Config | `IamAdministrationConfiguration` (beans role-permission) |
| Tests | `RolePermissionAdministrationUseCaseTest`, `IamRolePermissionAdminControllerIT` |

---

## 5. Tests ejecutados (solo 15.5)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.RolePermissionAdministrationUseCaseTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamRolePermissionAdminControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `RolePermissionAdministrationUseCaseTest` | 3 | ✅ |
| `IamRolePermissionAdminControllerIT` | 8 | ✅ |
| **Total** | **11** | **BUILD SUCCESSFUL** |

### Cobertura HTTP (IT)

- Asignar / listar permisos en rol custom
- Remover permisos (PUT con lista reducida)
- Listar permisos de system role (GET)
- 403 PUT en system role
- 404 rol otro tenant / permiso desconocido
- 401 sin JWT
- 403 sin `permission:assign`

---

## 6. Próximo paso

**15.6 — Membership Role Administration** (`/api/v1/iam/memberships/{membershipId}/roles` + `OwnershipPolicy`).
