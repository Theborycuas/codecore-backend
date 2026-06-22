# PASO 16.1 — Organizations Domain Foundation

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-16.0.1-ORGANIZATIONS-ROADMAP.md](PASO-16.0.1-ORGANIZATIONS-ROADMAP.md) · [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)

---

## Objetivo

Implementar el foundation del bounded context **Organization Management**: módulo Gradle, aggregate `Organization`, value objects, outbound ports y tests de dominio — **sin** persistencia ni HTTP.

---

## Entregables

| Entregable | Estado | Ubicación |
|------------|--------|-----------|
| ADR-010 Organizations Model | ✅ Accepted | `docs/architecture/ADR-010-ORGANIZATIONS-MODEL.md` |
| Módulo `organization-management` | ✅ | `modules/organization-management/` |
| Aggregate `Organization` | ✅ | `organization-domain/.../Organization.java` |
| Value objects | ✅ | `OrganizationId`, `OrganizationCode`, `OrganizationName`, `OrganizationStatus`, `TenantId` (referencia) |
| Outbound ports | ✅ | `OrganizationRepository`, `OrganizationQueryPort` |
| Tests de dominio | ✅ | 16 tests — todos verdes |
| Sin Flyway / HTTP / R2DBC | ✅ | Confirmado |

---

## Módulo Gradle

```text
modules/organization-management/
├── organization-domain/          ← aggregate, VOs, excepciones, tests
├── organization-application/     ← outbound ports (reactive)
├── organization-infrastructure/  ← placeholder (16.2)
└── organization-contract/        ← api(organization-domain) para consumidores
```

Registrado en `settings.gradle.kts`.

### Dependencias entre submódulos

```text
organization-domain        (framework-free main; spring-boot-library solo para tests)
organization-application → organization-domain + reactor
organization-contract    → api(organization-domain)
organization-infrastructure → organization-domain + organization-application
```

---

## Aggregate Organization

### Propiedades

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `OrganizationId` | Inmutable |
| `tenantId` | `TenantId` | Obligatorio, inmutable |
| `code` | `OrganizationCode` | Inmutable, normalizado `UPPER_SNAKE_CASE` |
| `name` | `OrganizationName` | Mutable vía `rename` |
| `status` | `OrganizationStatus` | `ACTIVE` \| `ARCHIVED` |
| `createdAt` / `updatedAt` | `Instant` | Auditoría |

### Factory y reconstitute

- `Organization.create(id, tenantId, code, name, now)` → status `ACTIVE`
- `Organization.reconstitute(...)` → para persistencia (16.2)

### Comportamientos

| Método | Efecto |
|--------|--------|
| `rename(OrganizationName)` | Actualiza nombre |
| `archive()` | `ACTIVE` → `ARCHIVED` |
| `activate()` | `ARCHIVED` → `ACTIVE` |

Sin delete físico. Transiciones inválidas → `InvalidOrganizationStateException`.

### OrganizationCode

- Formato: `^[A-Z][A-Z0-9_]*$` (longitud 2–64)
- Normalización: trim, uppercase, espacios/guiones → `_`
- Ejemplos válidos: `DENTAL_NORTE`, `CARDIOLOGIA`, `EMERGENCIAS`

---

## Outbound ports (application layer)

Hexagonal — patrón IAM (`application.port.out`):

**OrganizationRepository** (escritura):

- `save(Organization)`
- `findById(OrganizationId)`
- `existsByTenantIdAndCode(TenantId, OrganizationCode)`

**OrganizationQueryPort** (lectura — preparado 16.2+):

- `findAllByTenantId(TenantId)`
- `findByIdAndTenantId(OrganizationId, TenantId)`
- `countByTenantId(TenantId)`

Implementación R2DBC: **FASE 16.2**.

---

## Tests ejecutados

```text
:modules:organization-management:organization-domain:test
16 tests — 0 failures
```

Cobertura del paso:

- Creación válida y `reconstitute`
- `rename`, `archive`, `activate`
- Rechazo de transiciones inválidas
- `tenantId` obligatorio e inmutable
- Códigos inválidos / normalización
- Nombres en blanco

---

## IAM — sin cambios

No se modificó `identity-access-management`. ADR-006 y ADR-007 intactos.

`TenantId` en org-domain es **value object de referencia** (UUID) — anti-corruption layer; el aggregate `Tenant` sigue en IAM.

---

## Conflictos con PASO-16.0.1

**Ninguno detectado.** Implementación alineada con decisiones cerradas en 16.0.1.

---

## Próximo paso

**PASO 16.2 — Organization Persistence**

- Schema `org.organization`
- Flyway V14+
- `R2dbcOrganizationRepository` implementando ports
- Tests de integración persistencia

---

## Referencias

- [ROADMAP.md](../architecture/ROADMAP.md)
- [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)
- [PASO-16.0.1-ORGANIZATIONS-ROADMAP.md](PASO-16.0.1-ORGANIZATIONS-ROADMAP.md)
