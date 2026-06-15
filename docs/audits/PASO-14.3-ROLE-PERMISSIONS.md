# PASO 14.3 — Role Permissions

**Fecha:** 2026-05-27  
**Alcance:** Relación N:M Role ↔ Permission sin aggregate separado ni `tenant_id` redundante.

---

## 1. Resumen ejecutivo

### Objetivo

Vincular roles tenant-scoped con permisos globales mediante `iam.role_permission`.

### Decisiones aplicadas

| Decisión | Implementación |
|----------|----------------|
| Tabla `role_permission` | `role_id`, `permission_id`, `assigned_at` |
| Unicidad | `PRIMARY KEY` + `UNIQUE(role_id, permission_id)` |
| Sin `tenant_id` | Scope tenant heredado de `iam.role` |
| Sin aggregate `RolePermission` | `RolePermissionAssignment` interno al aggregate `Role` |
| FK | `role_id` → `iam.role` CASCADE; `permission_id` → `iam.permission` RESTRICT |

### Archivos principales

| Área | Archivos |
|------|----------|
| Dominio | `RolePermissionAssignment.java`, `Role.java` (assign/revoke/hasPermission) |
| Puerto | `RolePermissionRepository.java` |
| Infra | `R2dbcRolePermissionRepository.java` (SQL explícito vía `DatabaseClient`) |
| Flyway | `V11__create_role_permission_table.sql` |

### Tests (solo 14.3)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.domain.model.role.RoleTest" \
  --tests "com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionRepositoryIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `RoleTest` | 8 | ✅ |
| `R2dbcRolePermissionRepositoryIT` | 3 | ✅ |

### Resultado

**14.3 completado.**

---

## 2. Aggregate `Role` — permisos internos

```text
Role (aggregate root)
  └── Set<RolePermissionAssignment>
        └── permissionId + assignedAt
```

Métodos: `assignPermission`, `revokePermission`, `hasPermission`, `assignedPermissionIds`.

System roles: asignación bloqueada en dominio (seeds 14.8 vía SQL directo).

---

## 3. Próximo paso

**14.4 — Membership Roles:** `iam.membership_role` (Membership ↔ Role).
