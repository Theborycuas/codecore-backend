# PASO 12.0 — Tenant Foundation

**Fecha:** 2026-06-01

---

## 1. Objetivo

Introducir **Tenant** como aggregate root de primer nivel en IAM: dominio + puerto de persistencia + R2DBC + Flyway. Sin HTTP, onboarding, memberships, roles ni JWT.

---

## 2. Decisiones arquitectónicas

| Decisión | Motivo |
|----------|--------|
| Reutilizar `TenantId` existente | Ya usado por `Identity`, login y comandos; evita duplicación |
| `Tenant` no extiende `AggregateRoot` | Aggregate raíz de tenancy, no “pertenece” a otro tenant |
| Sin `version` en 12.0 | Alcance mínimo; optimistic locking fuera de este paso |
| `TenantName` como value object | Consistencia con `EmailAddress` y validación en dominio |
| Módulo IAM único | Tenant vive en el bounded context IAM (mismo esquema `iam`) |

---

## 3. Modelo de dominio

### Aggregate: `Tenant`

| Campo | Tipo |
|-------|------|
| `id` | `TenantId` |
| `name` | `TenantName` |
| `status` | `TenantStatus` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |

### `TenantStatus`

`ACTIVE`, `SUSPENDED`, `DISABLED`

### Comportamiento

- `Tenant.create(id, name, now)` — estado inicial `ACTIVE`
- `suspend()`, `disable()`, `activate()` — transiciones de ciclo de vida

### Puerto

`TenantRepository`: `save`, `findById`, `existsById`

---

## 4. Persistencia

| Capa | Artefacto |
|------|-----------|
| Flyway | `V5__create_iam_tenant_table.sql` → `iam.tenant` |
| Entity | `IamTenantEntity` |
| Mapper | `IamTenantMapper` |
| Spring Data | `SpringDataIamTenantRepository` |
| Adapter | `R2dbcTenantRepository` |

Tabla:

```
tenant_id (PK), name, status, created_at, updated_at
```

---

## 5. Tests

| Test | Alcance |
|------|---------|
| `TenantTest` | Creación, nombre inválido, transiciones de estado |
| `R2dbcTenantRepositoryIT` | save / findById / existsById, update de status (Testcontainers + Flyway) |

---

## 6. Riesgos identificados

1. **Tenant aún no relacionado con Identity** — `iam_user.tenant_id` sin FK a `iam.tenant`.
2. **Tenant aún no aparece en JWT** — login sigue usando header `X-Tenant-Id`.
3. **Tenant Context no implementado** — sin resolución automática de tenant en pipeline HTTP.
4. **Roles y permisos fuera de alcance.**

---

## 7. Verificación

- DDD: aggregate + VOs + puerto outbound.
- Hexagonal: dominio sin Spring; adapter en infrastructure.
- `./gradlew build` → **BUILD SUCCESSFUL**.
