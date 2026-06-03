# PASO 12.0 — Tenant Foundation (auditoría previa)

**Fecha:** 2026-06-01

---

## 1. Estructura actual del módulo IAM

| Capa | Paquete | Ejemplos |
|------|---------|----------|
| Domain | `domain.model.*`, `domain.valueobject`, `domain.exception` | `Identity`, `IdentityStatus`, `TenantId` |
| Application | `application.port.out`, `application.command` | `IdentityRepository` |
| Infrastructure | `infrastructure.persistence.*`, `infrastructure.security` | `R2dbcIdentityRepository`, `IamUserEntity` |
| Interfaces | `interfaces.http` | Controllers (sin HTTP en 12.0) |
| Configuration | `configuration` | `IamModuleConfiguration` |

Flyway vive en `apps/codecore-api/src/main/resources/db/migration` (V1–V4). Tests IAM copian migraciones vía `processTestResources`.

---

## 2. Patrones de referencia (Identity)

| Elemento | Patrón |
|----------|--------|
| Aggregate | `Identity` extends `AggregateRoot` (tenant-scoped + `version`) |
| Id VO | `IdentityId extends UuidIdentifier` |
| Repository port | `application.port.out.IdentityRepository` — `Mono` reactivo |
| Adapter | `R2dbcIdentityRepository` + `SpringDataIamUserRepository` |
| Mapper | `IamUserMapper` (bean en `IamModuleConfiguration`) |
| Entity | `IamUserEntity` — `@Table(schema = "iam")`, `Persistable`, `@Version` |
| Save | `findById` → `exists` → `save(entity, isNew)` |

**Nota:** `Tenant` es aggregate **raíz de tenancy** (no tenant-scoped respecto a otro tenant). **No** extiende `AggregateRoot` (evita `tenantId` redundante). **No** incluye `version` en 12.0 (alcance mínimo).

---

## 3. `TenantId` existente

`com.codecore.iam.domain.valueobject.TenantId` ya existe (`UuidIdentifier`, `generate()`). Usado por `Identity`, login (`X-Tenant-Id`), comandos.

**Decisión:** reutilizar `TenantId` como identificador del aggregate `Tenant`. **No** duplicar clase.

---

## 4. Ubicación propuesta

| Artefacto | Ubicación |
|-----------|-----------|
| `Tenant` aggregate | `domain.model.tenant.Tenant` |
| `TenantName` | `domain.valueobject.TenantName` |
| `TenantStatus` | `domain.valueobject.TenantStatus` |
| `TenantRepository` | `application.port.out.TenantRepository` |
| `IamTenantEntity` | `infrastructure.persistence.entity.IamTenantEntity` |
| `IamTenantMapper` | `infrastructure.persistence.mapper.IamTenantMapper` |
| `SpringDataIamTenantRepository` | `infrastructure.persistence.repository` |
| `R2dbcTenantRepository` | `infrastructure.persistence.repository` |
| Flyway | `V5__create_iam_tenant_table.sql` |
| Unit tests | `domain.model.tenant.TenantTest` |
| IT | `R2dbcTenantRepositoryIT` |

---

## 5. Modelo de datos (Flyway)

Tabla `iam.tenant`:

| Columna | Tipo |
|---------|------|
| `tenant_id` | UUID PK |
| `name` | VARCHAR(200) NOT NULL |
| `status` | VARCHAR(50) NOT NULL |
| `created_at` | TIMESTAMPTZ NOT NULL |
| `updated_at` | TIMESTAMPTZ NOT NULL |

CHECK `status IN ('ACTIVE','SUSPENDED','DISABLED')`. Sin índices adicionales (PK suficiente para `findById` / `existsById`).

---

## 6. Fuera de alcance (12.0)

HTTP, onboarding, memberships, FK `iam_user.tenant_id` → `iam.tenant`, JWT `tenantId`, Tenant Context, roles.

---

## 7. Riesgos

1. Tenant no relacionado formalmente con `Identity` (sin FK).
2. JWT sin claim `tenantId`.
3. Tenant Context no implementado.
4. Roles/permisos fuera de alcance.
