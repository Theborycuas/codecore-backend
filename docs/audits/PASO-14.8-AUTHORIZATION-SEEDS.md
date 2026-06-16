# PASO 14.8 — Authorization Seeds

**Fecha:** 2026-05-27  
**Alcance:** Contrato de autorización IAM base de la plataforma (no permisos de negocio).

---

## 1. Resumen ejecutivo

### Objetivo

Definir el catálogo global de permisos IAM y los roles system mínimos por tenant.

### Principios aplicados

| Principio | Implementación |
|-----------|----------------|
| Solo permisos IAM | 16 grants `resource:action` — sin `patient:*`, `invoice:*`, etc. |
| Roles universales | `OWNER`, `ADMIN`, `MANAGER`, `USER`, `READ_ONLY` |
| Roles system | `Role.createSystemRole(...)` — inmutables en dominio |
| Permisos system | `system_permission = TRUE` en Flyway V13 |
| Idempotencia | `INSERT … WHERE NOT EXISTS` en SQL; provisioner idempotente por tenant |

### Catálogo IAM (`IamPermissionCatalog`)

| Recurso | Acciones |
|---------|----------|
| `tenant` | read, update |
| `membership` | read, create, update, delete |
| `role` | read, create, update, delete |
| `permission` | read, assign |
| `user` | read, create, update, delete |

### Matriz de roles (tenant-scoped)

| Rol | Permisos |
|-----|----------|
| **OWNER** | Todos los IAM (16) |
| **ADMIN** | membership:*, role:*, permission:assign, user:* |
| **MANAGER** | membership:read, user:read, user:update |
| **USER** | user:read |
| **READ_ONLY** | tenant:read, membership:read, role:read |

### Archivos principales

| Área | Archivos |
|------|----------|
| Flyway | `V13__seed_authorization_foundation.sql` |
| Catálogo | `IamPermissionCatalog.java`, `SystemRoleTemplate.java` |
| Provisioner | `TenantSystemRolesProvisionerImpl.java` |
| Integración | `CreateTenantUseCaseImpl` (provisiona roles al crear tenant) |

### Tests (solo 14.8)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.infrastructure.persistence.migration.AuthorizationSeedMigrationIT" \
  --tests "com.codecore.iam.application.authorization.TenantSystemRolesProvisionerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `AuthorizationSeedMigrationIT` | 1 | ✅ |
| `TenantSystemRolesProvisionerIT` | 1 | ✅ |

### Resultado

**14.8 completado.**

---

## 2. Notas de diseño

- **Permisos** = globales en PostgreSQL (V13).
- **Roles** = tenant-scoped; se provisionan vía application al crear tenant (`CreateTenantUseCase`).
- **role_permission** para system roles se inserta por repositorio (dominio bloquea `assignPermission` en system roles).

Listo para **14.9 Authorization Verification**.
