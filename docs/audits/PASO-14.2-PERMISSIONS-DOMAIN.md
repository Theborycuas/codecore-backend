# PASO 14.2 — Permissions Domain

**Fecha:** 2026-05-27  
**Alcance:** Aggregate `Permission` global, persistencia R2DBC y migración Flyway V10.  
**Referencias:** [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md) · [PASO-14.1](PASO-14.1-ROLES-DOMAIN.md)

---

## 1. Resumen ejecutivo

### Objetivo

Implementar el catálogo global de permisos atómicos (`resource:action`) sin scope de tenant.

### Archivos creados / modificados

| Área | Archivos |
|------|----------|
| Dominio | `Permission.java`, `PermissionId`, `PermissionCode` |
| Puerto | `PermissionRepository.java` |
| Infraestructura | `IamPermissionEntity`, `IamPermissionMapper`, `SpringDataIamPermissionRepository`, `R2dbcPermissionRepository` |
| Config | `IamModuleConfiguration.java` (+ `IamPermissionMapper`) |
| Flyway | `V10__create_iam_permission_table.sql` |
| Tests | `PermissionTest`, `R2dbcPermissionRepositoryIT`, `IamPermissionPersistenceTestConfiguration` |
| Ajuste | `MembershipBackfillMigrationIT` — versión Flyway esperada **10** |

### Tests ejecutados

```bash
./gradlew --stop
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.domain.model.permission.PermissionTest" \
  --tests "com.codecore.iam.domain.model.role.RoleTest"

./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionRepositoryIT" \
  --tests "com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleRepositoryIT" \
  --tests "com.codecore.iam.infrastructure.persistence.migration.MembershipBackfillMigrationIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `PermissionTest` | 5 | ✅ PASSED |
| `RoleTest` (regresión 14.1) | 5 | ✅ PASSED |
| `R2dbcPermissionRepositoryIT` | 3 | ✅ PASSED |
| `R2dbcRoleRepositoryIT` (regresión) | 3 | ✅ PASSED |
| `MembershipBackfillMigrationIT` | 3 | ✅ PASSED |
| **Total verificado** | **19** | **✅ 100%** |

> **Nota:** La suite completa (~115 tests) incluye IT HTTP con Testcontainers que pueden tardar varios minutos. Ejecutar con `./gradlew --stop` antes si hay locks en `build/test-results` (Windows).

### Resultado

**14.2 completado.** Catálogo global de permisos operativo.

---

## 2. Modelo implementado

### Aggregate `Permission`

| Atributo | Tipo | Notas |
|----------|------|-------|
| `id` | `PermissionId` | UUID |
| `code` | `PermissionCode` | Inmutable; formato `resource:action` |
| `description` | `String` | Opcional, max 500 |
| `systemPermission` | `boolean` | Inmutable si `true` |

**Sin `tenantId`** — permiso global de plataforma (ADR-007).

### `PermissionCode`

| Regla | Ejemplo válido | Ejemplo inválido |
|-------|----------------|------------------|
| Lowercase `resource:action` | `user:create` | `USER_CREATE` |
| Normalización | `User:Create` → `user:create` | `user:` |
| Helpers | `resource()`, `action()` | — |

### Comportamientos

* `Permission.create(code, description, now)` — permiso custom
* `Permission.createSystemPermission(...)` — seed plataforma (14.8)
* `updateDescription()` — solo permisos no-system

---

## 3. Schema Flyway V10

```sql
iam.permission (
  permission_id, code UNIQUE, description, system_permission, created_at, updated_at
)
```

---

## 4. Regresión

| Área | Verificación |
|------|--------------|
| Roles 14.1 | `RoleTest` + `R2dbcRoleRepositoryIT` ✅ |
| Migraciones V1–V10 | `MembershipBackfillMigrationIT` ✅ |
| Compilación | `compileJava` + `compileTestJava` ✅ |

---

## 5. Próximo paso

**14.3 — Role Permissions:** tabla `iam.role_permission`, N:M Role ↔ Permission.
