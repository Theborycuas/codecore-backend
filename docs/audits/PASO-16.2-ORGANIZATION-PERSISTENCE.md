# PASO 16.2 — Organization Persistence

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md](PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md) · [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)

---

## Objetivo

Persistir el aggregate `Organization` en PostgreSQL vía R2DBC, sin HTTP, sin tocar IAM.

---

## Entregables

| Entregable | Estado | Ubicación |
|------------|--------|-----------|
| Schema `org` + tabla `org.organization` | ✅ | `V14__create_organization_table.sql` |
| Flyway V14 | ✅ | `apps/codecore-api/src/main/resources/db/migration/` |
| `OrganizationEntity` | ✅ | `organization-infrastructure/.../entity/` |
| `OrganizationMapper` | ✅ | `organization-infrastructure/.../mapper/` |
| `SpringDataOrganizationRepository` | ✅ | `organization-infrastructure/.../repository/` |
| `R2dbcOrganizationRepository` | ✅ | Implementa `OrganizationRepository` + `OrganizationQueryPort` |
| `OrganizationModuleConfiguration` | ✅ | `@EnableR2dbcRepositories` + mapper bean |
| `R2dbcOrganizationRepositoryIT` | ✅ | 6 escenarios Testcontainers |
| IAM sin cambios | ✅ | Confirmado |

---

## Base de datos

### Schema

```sql
CREATE SCHEMA IF NOT EXISTS org;
```

### Tabla `org.organization`

| Columna | Tipo | Notas |
|---------|------|-------|
| `organization_id` | UUID PK | Domain `OrganizationId` |
| `tenant_id` | UUID NOT NULL | Referencia lógica a `iam.tenant` |
| `code` | VARCHAR(64) | `OrganizationCode` normalizado |
| `name` | VARCHAR(200) | `OrganizationName` |
| `status` | VARCHAR(50) | `ACTIVE` \| `ARCHIVED` |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

### Restricciones

- `PRIMARY KEY (organization_id)`
- `UNIQUE (tenant_id, code)` — `uq_organization_tenant_code`
- `CHECK status IN ('ACTIVE','ARCHIVED')`
- Checks de code/name no blank

### Índices

- `idx_org_organization_tenant_id`
- `idx_org_organization_status`

### Sin FK hacia IAM — justificación

`tenant_id` es una **referencia lógica** al tenant IAM, no una FK física a `iam.tenant`.

| Razón | Detalle |
|-------|---------|
| Desacoplamiento de módulos | Organization Management es bounded context separado (ADR-010) |
| Evolución independiente | Migraciones IAM no bloquean ni revierten schema `org` |
| Modular monolith | Comunicación por ID, no por integridad referencial cross-schema |
| Aislamiento ADR-003 | Enforcement en aplicación (JWT tenant + queries filtradas), no FK |

La existencia del tenant se validará en capa de aplicación/API (16.4+) cuando corresponda.

---

## Stack de persistencia

```text
Organization (domain)
    ↓ OrganizationMapper
OrganizationEntity
    ↓ Spring Data R2DBC
SpringDataOrganizationRepository
    ↓ R2dbcOrganizationRepository
org.organization (PostgreSQL)
```

### Operaciones implementadas

| Port | Método | Implementación |
|------|--------|----------------|
| `OrganizationRepository` | `save` | insert/update vía `Persistable.isNew()` |
| `OrganizationRepository` | `findById` | `ReactiveCrudRepository.findById` |
| `OrganizationRepository` | `existsByTenantIdAndCode` | derived query |
| `OrganizationQueryPort` | `findAllByTenantId` | derived query |
| `OrganizationQueryPort` | `findByIdAndTenantId` | `findByOrganizationIdAndTenantId` |
| `OrganizationQueryPort` | `countByTenantId` | derived query |

Patrón idéntico a `R2dbcRoleRepository` / `R2dbcTenantRepository` (IAM).

---

## Configuración Spring

`OrganizationModuleConfiguration`:

- `@EnableR2dbcRepositories` en `com.codecore.organization.infrastructure.persistence.repository`
- Bean `OrganizationMapper`
- `R2dbcOrganizationRepository` auto-registrado vía `@Repository`

**No** se modificó `identity-access-management` ni `codecore-api` wiring HTTP (16.4).

---

## Tests

### `R2dbcOrganizationRepositoryIT`

| # | Escenario |
|---|-----------|
| 1 | save + findById |
| 2 | existsByTenantIdAndCode |
| 3 | mismo code en tenants distintos → permitido |
| 4 | mismo code en mismo tenant → `DuplicateKeyException` |
| 5 | findByIdAndTenantId (cross-tenant → empty) |
| 6 | countByTenantId + findAllByTenantId |

Infraestructura de test (patrón IAM):

- `AbstractPostgresIntegrationTest` — Testcontainers PostgreSQL 16 + Flyway
- `OrganizationPersistenceTestConfiguration`
- `OrganizationPersistenceTestApplication` — `@SpringBootConfiguration`
- `@DataR2dbcTest`

**Ejecución:** requiere Docker (Testcontainers). Mismo requisito que `R2dbcTenantRepositoryIT`.

```bash
./gradlew :modules:organization-management:organization-infrastructure:test
```

---

## Conflictos con ADR-010

**Ninguno.** Persistencia alineada con decisiones de 16.0.1 y ADR-010.

---

## Fuera de alcance (confirmado)

- HTTP / OpenAPI / use cases admin
- Office / StaffAssignment
- Permission seeds (16.3)
- Wiring en `codecore-api` (16.4)

---

## Próximo paso

**PASO 16.3 — Organization Permission Seeds**

- `organization:*` en `iam.permission` (Flyway V15)
- Grants en `SystemRoleTemplate` OWNER/ADMIN
- `OrganizationPermissionCatalog`

---

## Referencias

- [ROADMAP.md](../architecture/ROADMAP.md)
- [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)
- [V14__create_organization_table.sql](../../apps/codecore-api/src/main/resources/db/migration/V14__create_organization_table.sql)
