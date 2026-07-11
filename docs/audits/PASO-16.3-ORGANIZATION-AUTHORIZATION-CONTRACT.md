# PASO 16.3 — Organization Authorization Contract

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-16.2-ORGANIZATION-PERSISTENCE.md](PASO-16.2-ORGANIZATION-PERSISTENCE.md) · [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT-AUDIT.md](PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT-AUDIT.md)

---

## Objetivo

Definir e implementar el **contrato completo de autorización** para Organization Management — catálogo, matriz RBAC, seeds Flyway V15 y verificación — sin HTTP ni dominio adicional.

---

## Catálogo de permisos (12 grants)

### Organization

| Código | Descripción |
|--------|-------------|
| `organization:create` | Create organizations |
| `organization:read` | Read organizations |
| `organization:update` | Update organizations |
| `organization:archive` | Archive organizations |

### Office

| Código | Descripción |
|--------|-------------|
| `office:create` | Create offices |
| `office:read` | Read offices |
| `office:update` | Update offices |
| `office:archive` | Archive offices |

### Staff Assignment

| Código | Descripción |
|--------|-------------|
| `staff-assignment:create` | Create staff organizational assignments |
| `staff-assignment:read` | Read staff organizational assignments |
| `staff-assignment:update` | Update staff organizational assignments |
| `staff-assignment:delete` | Delete staff organizational assignments |

**Fuente de verdad (strings):** `OrganizationPermissionCatalog` (`organization-contract`).

**Mapeo IAM:** `IamPermissionCatalog` (`PermissionCode` + seeds).

---

## Matriz RBAC formal

Leyenda: ✓ = grant · — = sin grant

### Organization Management

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| organization:create | ✓ | ✓ | — | — | — |
| organization:read | ✓ | ✓ | ✓ | ✓ | ✓ |
| organization:update | ✓ | ✓ | — | — | — |
| organization:archive | ✓ | ✓ | — | — | — |
| office:create | ✓ | ✓ | ✓ | — | — |
| office:read | ✓ | ✓ | ✓ | ✓ | ✓ |
| office:update | ✓ | ✓ | ✓ | — | — |
| office:archive | ✓ | ✓ | ✓ | — | — |
| staff-assignment:create | ✓ | ✓ | ✓ | — | — |
| staff-assignment:read | ✓ | ✓ | ✓ | ✓ | ✓ |
| staff-assignment:update | ✓ | ✓ | ✓ | — | — |
| staff-assignment:delete | ✓ | ✓ | ✓ | — | — |

### IAM (sin cambios de matriz 14.8 — resumen)

| Rol | IAM grants |
|-----|------------|
| **OWNER** | Todos (16 IAM + 12 org = **28**) |
| **ADMIN** | membership:*, role:*, permission:assign, user:* + org completo (**25**) |
| **MANAGER** | membership:read, user:read, user:update + org operacional (**12**) |
| **USER** | user:read + structure read (**4**) |
| **READ_ONLY** | tenant:read, membership:read, role:read + structure read (**6**) |

### Respuestas explícitas

| Pregunta | Respuesta |
|----------|-----------|
| ¿OWNER tiene todos? | **Sí** — 28 permisos plataforma |
| ¿ADMIN todos menos tenant governance? | **Sí** — sin `tenant:update` ni `permission:read`; con org completo |
| ¿MANAGER administra offices? | **Sí** — CRUD + archive |
| ¿MANAGER administra staff assignments? | **Sí** — CRUD completo |
| ¿READ_ONLY navega toda la estructura? | **Sí** — 3 permisos read org/office/staff |

---

## Extensibilidad futura (post-FASE 16)

| Convención | Regla |
|------------|-------|
| Naming | `{resource}:{action}` lowercase; resource compuesto con `-` (ej. `staff-assignment`) |
| Seeds | Nuevo Flyway por bounded context; `INSERT … WHERE NOT EXISTS` |
| Roles | Nuevos grants vía `SystemRoleTemplate` + backfill SQL |
| Clinical | Prefijos `patient:` — **FASE 17**; `appointment:` — **FASE 18** |
| Clinical Records | Prefijo `medical-record:` (u homólogo) — **FASE 19** |
| Billing | Prefijos `billing:`, `subscription:` — **FASE 21** |
| Invitations | Prefijo `invitation:` — **FASE 22 Platform Services** |

El contrato 16.3 **no requiere rehacer** permisos org cuando lleguen Office (16.5) o StaffAssignment (16.7) — ya están sembrados.

Ver secuencia vigente: [ROADMAP.md](../architecture/ROADMAP.md) · [PASO-17.0](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md).

---

## Implementación

### Artefactos

| Artefacto | Ubicación |
|-----------|-----------|
| `OrganizationPermissionCatalog` | `organization-contract/.../authorization/` |
| `IamPermissionCatalog` (extendido) | `IAM` — `ALL` = 28 permisos |
| `SystemRoleTemplate` (matriz) | `IAM` |
| Flyway V15 | `V15__seed_organization_authorization_contract.sql` |
| `PermissionCode` | Hyphen permitido en resource/action |

### Flyway V15

1. Inserta 12 permisos (idempotente)
2. Backfill `iam.role_permission` para roles system existentes (idempotente)

### Touch IAM acotado (permitido 16.3)

- `IamPermissionCatalog`, `SystemRoleTemplate`, `PermissionCode` regex
- Dependencia Gradle: `identity-access-management` → `organization-contract`
- **Sin** cambios a Membership, Identity, Tenant aggregates, AuthorizationService

---

## Tests

| Suite | Tipo | Resultado |
|-------|------|-----------|
| `OrganizationPermissionCatalogTest` | Unit | ✅ 2 tests |
| `SystemRoleTemplateTest` | Unit | ✅ 6 tests |
| `PermissionTest` (hyphen) | Unit | ✅ |
| `OrganizationAuthorizationSeedMigrationIT` | Testcontainers | Requiere Docker |
| `AuthorizationSeedMigrationIT` | Actualizado — 28 permisos | Requiere Docker |
| `TenantSystemRolesProvisionerIT` | Extendido MANAGER/READ_ONLY | Requiere Docker |

```bash
# Sin Docker
./gradlew :modules:organization-management:organization-contract:test \
  :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.authorization.SystemRoleTemplateTest"

# Con Docker
./gradlew :modules:identity-access-management:test \
  --tests "*.OrganizationAuthorizationSeedMigrationIT" \
  --tests "*.TenantSystemRolesProvisionerIT"
```

---

## Fuera de alcance (confirmado)

- Organization / Office HTTP API (16.4+)
- Office domain (16.5)
- StaffAssignment entity (16.7)
- OpenAPI org-administration

---

## Próximo paso

**PASO 16.4 — Organization Administration API** — CRUD HTTP `/api/v1/org/organizations` con `@RequiresPermission("organization:*")`.

---

## Referencias

- [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md)
- [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)
- [V15__seed_organization_authorization_contract.sql](../../apps/codecore-api/src/main/resources/db/migration/V15__seed_organization_authorization_contract.sql)
