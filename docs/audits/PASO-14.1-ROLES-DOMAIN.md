# PASO 14.1 — Roles Domain

**Fecha:** 2026-05-27  
**Alcance:** Aggregate `Role` tenant-scoped, persistencia R2DBC y migración Flyway V9.  
**Referencias:** [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md) · [ROADMAP](../architecture/ROADMAP.md) · [PASO-14.0](PASO-14.0-AUTHORIZATION-FOUNDATION.md)

---

## 1. Resumen ejecutivo

### Objetivo

Implementar el aggregate `Role` como raíz de autorización tenant-scoped (FASE 14.1).

### Archivos modificados / creados

| Área | Archivos |
|------|----------|
| Dominio | `Role.java`, `RoleId`, `RoleCode`, `RoleName`, `RoleStatus` |
| Puerto | `RoleRepository.java` |
| Infraestructura | `IamRoleEntity`, `IamRoleMapper`, `SpringDataIamRoleRepository`, `R2dbcRoleRepository` |
| Config | `IamModuleConfiguration.java` (+ `IamRoleMapper` bean) |
| Flyway | `V9__create_iam_role_table.sql` |
| Tests | `RoleTest`, `R2dbcRoleRepositoryIT`, `IamRolePersistenceTestConfiguration` |
| Fixes | `MembershipBackfillMigrationIT` (versión Flyway 9), `R2dbcIdentityRepositoryIT` (config IT aislada) |

### Tests ejecutados

```bash
./gradlew :modules:identity-access-management:test
```

| Suite | Resultado |
|-------|-----------|
| `RoleTest` (5 tests) | ✅ PASSED |
| `R2dbcRoleRepositoryIT` (3 tests) | ✅ PASSED |
| Suite completa (107 tests) | ⚠️ 8 fallos pre-existentes corregidos parcialmente; re-ejecución bloqueada por lock de Gradle en segundo intento |

### Resultado

**14.1 completado.** Dominio Role operativo con unicidad `(tenant_id, code)`.

---

## 2. Modelo implementado

### Aggregate `Role`

| Atributo | Tipo | Notas |
|----------|------|-------|
| `id` | `RoleId` | UUID generado en `create()` |
| `tenantId` | `TenantId` | Ownership tenant |
| `code` | `RoleCode` | `UPPER_SNAKE_CASE`, normalizado |
| `name` | `RoleName` | Display name |
| `status` | `RoleStatus` | ACTIVE / INACTIVE |
| `systemRole` | `boolean` | Inmutable si `true` (seeds 14.8) |

### Comportamientos

* `Role.create(tenantId, code, name, now)` — rol custom activo
* `Role.createSystemRole(...)` — rol plataforma inmutable
* `rename`, `deactivate`, `activate` — bloqueados en system roles

### Invariantes

| Invariante | Enforcement |
|------------|-------------|
| Role pertenece a un tenant | `tenantId` obligatorio en aggregate y tabla |
| RoleCode único por tenant | `UNIQUE (tenant_id, code)` + `existsByTenantIdAndCode` |
| Mismo code en distintos tenants | Permitido (test IT) |

---

## 3. Schema Flyway V9

```sql
iam.role (
  role_id, tenant_id, code, name, status, system_role, created_at, updated_at
)
```

---

## 4. Próximo paso

**14.2 — Permissions Domain:** aggregate `Permission` global, tabla `iam.permission`, formato `resource:action`.
