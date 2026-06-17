# PASO 15.6 — Membership Role Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP para administrar asignaciones Membership ↔ Role bajo `/api/v1/iam/memberships/{membershipId}/roles`.  
**Prerequisitos:** PASO-15.0.1 (Ownership Rules), PASO-15.2 (Membership Administration), PASO-15.3 (Role Administration).

---

## 1. Resumen ejecutivo

Sexta capa administrativa IAM: sincronización de roles sobre memberships del tenant. El PUT recibe la lista completa de `roleIds`; el sistema calcula altas/bajas vía dominio `IdentityTenantMembership.assignRole` / `revokeRole` y persiste con `MembershipRoleRepository.replaceAll`.

| Ítem | Estado |
|------|--------|
| `GET /api/v1/iam/memberships/{membershipId}/roles` | ✅ |
| `PUT /api/v1/iam/memberships/{membershipId}/roles` | ✅ |
| `@RequiresPermission("membership:update")` | ✅ |
| `OwnershipPolicy` en PUT | ✅ |
| Membership ACTIVE requerida en PUT | ✅ |
| Roles del mismo tenant | ✅ 404 si no |
| Tests unitarios 15.6 | ✅ 3 tests |
| Tests IT 15.6 | ✅ escritos (requieren Docker/Testcontainers) |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| PUT replace semantics | Lista completa `roleIds` → diff dominio → `replaceAll` |
| Permiso HTTP | `membership:update` (ADR-008) en GET y PUT |
| Ownership | `OwnershipPolicy.assertCanModifyUser` antes de mutar (15.0.1) |
| Membership INACTIVE | PUT rechazado → `InvalidDomainValueException` → 400 |
| Rol cross-tenant / inexistente | `RoleNotFoundException` → 404 |
| Membership cross-tenant | `MembershipNotFoundException` → 404 |
| GET en membership inactive | Permitido (solo lectura de asignaciones) |
| Cadena RBAC | Membership → MembershipRole → Role (tenant) → RolePermission → Permission |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/memberships/{membershipId}/roles` | `membership:update` |
| PUT | `/api/v1/iam/memberships/{membershipId}/roles` | `membership:update` |

### Request PUT

```json
{
  "roleIds": ["uuid-1", "uuid-2"]
}
```

### Response (lista)

```json
[
  {
    "roleId": "uuid",
    "code": "USER",
    "name": "User",
    "status": "ACTIVE",
    "systemRole": true,
    "assignedAt": "2026-06-17T..."
  }
]
```

---

## 4. Archivos principales

| Área | Archivos |
|------|----------|
| Use cases | `MembershipRoleAdministrationUseCaseImpl`, `port.in.Get/ReplaceAdminMembershipRoles*` |
| Command | `ReplaceAdminMembershipRolesCommand` |
| Queries | `MembershipRoleAdminQueryRepository`, `R2dbcMembershipRoleAdminQueryRepository`, `AdminMembershipRoleView` |
| HTTP | `IamMembershipRoleAdminController`, `MembershipRoleResponse`, `ReplaceMembershipRolesRequest` |
| Ownership | `OwnershipPolicy` (reutilizado, sin cambios) |
| Config | `IamAdministrationConfiguration` (beans membership-role) |
| Tests | `MembershipRoleAdministrationUseCaseTest`, `IamMembershipRoleAdminControllerIT` |

---

## 5. Tests ejecutados (solo 15.6)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.MembershipRoleAdministrationUseCaseTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamMembershipRoleAdminControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `MembershipRoleAdministrationUseCaseTest` | 3 | ✅ BUILD SUCCESSFUL |
| `IamMembershipRoleAdminControllerIT` | 8 | ⏳ Requiere Docker (Testcontainers) |

### Cobertura unitaria

- Listar roles de membership
- Replace con ownership + `replaceAll`
- Rechazo PUT si membership INACTIVE

### Cobertura HTTP (IT — diseñada)

- Asignar / listar / remover roles
- 403 ownership (ADMIN → OWNER)
- 403 sin `membership:update`
- 404 membership otro tenant / rol desconocido
- 400 membership inactive en PUT
- 401 sin JWT

---

## 6. Próximo paso

**15.7 — Tenant Administration** (`GET/PUT /api/v1/iam/tenants/current`).
