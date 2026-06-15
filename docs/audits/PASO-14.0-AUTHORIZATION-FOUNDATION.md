# PASO 14.0 — Authorization Foundation

**Fecha:** 2026-05-27  
**Alcance:** Análisis del sistema actual y definición del modelo de autorización para FASE 14.  
**Restricción:** Sin implementación de código en este paso — solo diseño y decisión arquitectónica.

**Referencias:** [ROADMAP.md](../architecture/ROADMAP.md) · [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md) · [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md)

---

## 1. Resumen ejecutivo

### Objetivo

Definir el modelo de autorización multi-tenant de CodeCore antes de implementar roles, permisos y el motor de evaluación (pasos 14.1–14.9).

### Decisión

Se adopta **RBAC membership-scoped** con permisos globales y roles por tenant, documentado formalmente en **ADR-007**.

### Cadena obligatoria

```text
Identity → IdentityTenantMembership → Role (assignment) → Permission → Authorization
```

Nunca autorización directa sobre `Identity`.

### Archivos producidos

| Archivo | Propósito |
|---------|-----------|
| `docs/architecture/ADR-007-AUTHORIZATION-MODEL.md` | Decisión arquitectónica |
| `docs/audits/PASO-14.0-AUTHORIZATION-FOUNDATION.md` | Este documento |
| `docs/architecture/ROADMAP.md` | Actualizado — 14.0 ✅ |

### Tests ejecutados

N/A — paso de documentación únicamente.

### Resultado

**Modelo aprobado.** Listo para 14.1 (Roles Domain).

---

## 2. Estado actual del sistema

### 2.1 Capacidades IAM existentes (post-FASE 13)

| Capacidad | Estado | Evidencia |
|-----------|--------|-----------|
| Identity Global (lookup email) | ✅ | `IdentityRepository.findByEmail` |
| Membership N:M | ✅ | `iam.identity_tenant_membership` (V7) |
| Gate ACTIVE en login/registro | ✅ | `AuthenticateIdentityUseCaseImpl`, `RegisterIdentityUseCaseImpl` |
| JWT `identityId` + `tenantId` | ✅ | `JwtTokenProvider` |
| TenantContext reactivo | ✅ | `ReactorTenantContext` |
| Roles / permisos | ❌ | No existe código ni tablas |
| AuthorizationService | ❌ | No existe |

### 2.2 Schema PostgreSQL actual (`iam`)

| Tabla | Rol |
|-------|-----|
| `iam.iam_user` | Identity (legacy `tenant_id` pendiente 15.x) |
| `iam.tenant` | Tenant aggregate |
| `iam.identity_tenant_membership` | Belonging formal |

**V7 comment:** *«formal membership; no roles»* — FASE 14 añade roles sobre membership.

### 2.3 Módulos Gradle

| Módulo | Estado físico |
|--------|---------------|
| `identity-access-management` | ✅ Implementado (único módulo con código) |
| `authorization-management:*` | ⏳ Declarado en `settings.gradle.kts`, sin directorios |
| `tenant-management:*` | ⏳ Placeholder |
| `user-management:*` | ⏳ Placeholder |

`apps/codecore-api` depende únicamente de `identity-access-management`.

### 2.4 Blueprints specifications

Inventario en `codecore-specifications/module-blueprints/authorization-management/`:

| Documento | Contenido relevante |
|-----------|---------------------|
| `overview.md` | RBAC, capas de autorización, deny-by-default |
| `aggregates.md` | Role, Permission, Policy, AccessContext, Privilege |
| `entities.md` | Atributos Role/Permission, PermissionAssignment |
| `value-objects.md` | PermissionCode, RoleName (formato legacy UPPER_SNAKE) |

**Gap vs FASE 14:** Blueprints incluyen Policy, Privilege, AuthorizationDecision — **fuera de alcance** en 14.0–14.9 (ADR-007).

---

## 3. Modelo de autorización definido

### 3.1 Tipo de autorización

| Dimensión | Decisión |
|-----------|----------|
| Paradigma | **RBAC** (Role-Based Access Control) |
| Granularidad | Permisos atómicos (`resource:action`) |
| Asignación | Roles asignados a **Membership**, no a Identity |
| Scope tenant | Roles tenant-scoped; permisos globales |
| Políticas dinámicas (ABAC) | Diferidas |

### 3.2 Aggregates

| Aggregate | Root | Scope | Invariantes clave |
|-----------|------|-------|-------------------|
| **Role** | `Role` | Tenant | `RoleCode` único por tenant; pertenece a un `TenantId` |
| **Permission** | `Permission` | Global | `PermissionCode` único global; inmutable post-seed |
| **IdentityTenantMembership** | `IdentityTenantMembership` | Par (identity, tenant) | Ya en IAM; recibe asignaciones de rol (14.4) |

**No aggregates (FASE 14):** Policy, Privilege, AuthorizationDecision persistente.

### 3.3 Entidades de asociación

| Tabla | Relación | Reglas |
|-------|----------|--------|
| `iam.role_permission` | Role ↔ Permission | N:M; sin duplicados; consistencia en aggregate Role (14.3) |
| `iam.membership_role` | Membership ↔ Role | N:M; `role.tenant_id` = `membership.tenant_id` |

### 3.4 Value Objects (planificados)

| VO | Paso | Validación |
|----|------|------------|
| `RoleId` | 14.1 | UUID |
| `RoleCode` | 14.1 | `UPPER_SNAKE_CASE`, max 100 |
| `RoleName` | 14.1 | Texto humano, max 200 |
| `RoleStatus` | 14.1 | ACTIVE / INACTIVE |
| `PermissionId` | 14.2 | UUID |
| `PermissionCode` | 14.2 | `resource:action`, lowercase |

### 3.5 Relaciones

```text
Identity (1) ──< IdentityTenantMembership (N) >── (1) Tenant
                        │
                        │ membership_role (N:M)
                        ▼
                     Role (N) ──> (1) Tenant
                        │
                        │ role_permission (N:M)
                        ▼
                   Permission (global)
```

### 3.6 Ownership

| Recurso | Owner lógico | Owner físico FASE 14 |
|---------|--------------|----------------------|
| Identity, Membership | IAM | `identity-access-management` |
| Role, Permission, assignments | Authorization Management | `identity-access-management` (packages `domain.model.role`) |
| JWT, TenantContext | IAM / Platform Security | Existente |
| Authorization evaluation | Authorization | Nuevo application service (14.5) |

### 3.7 Límites de contexto (bounded contexts)

```text
┌─────────────────────────────┐     ┌──────────────────────────────┐
│  Identity & Access Mgmt     │     │  Authorization (logical)     │
│  ─────────────────────      │     │  ───────────────────────     │
│  • Identity / credentials   │────▶│  • Role / Permission         │
│  • Membership               │     │  • membership_role           │
│  • JWT issuance             │     │  • AuthorizationService      │
│  • TenantContext            │     │  • HTTP @RequiresPermission  │
└─────────────────────────────┘     └──────────────────────────────┘
         Customer/Supplier (Context Map 6.2)
```

**Regla crítica:** Authentication ≠ Authorization.

---

## 4. Análisis de ownership y tenancy

### 4.1 ¿Por qué membership y no identity?

| Escenario | Identity-scoped (rechazado) | Membership-scoped (aceptado) |
|-----------|------------------------------|------------------------------|
| Juan en Tenant A = ADMIN, Tenant B = VET | Requiere duplicar lógica o filtrar por tenant post-hoc | Roles distintos por fila membership |
| Invitación cross-tenant | Ambigüedad de rol | Rol asignado al crear membership |
| Billing por seat | No correlaciona rol con tenant | Membership + roles = seat con capacidades |

### 4.2 Aislamiento tenant (ADR-003)

Tres puntos de control:

1. **JWT `tenantId`** → operaciones acotadas al tenant activo.
2. **Membership ACTIVE** en ese tenant → sin membership no hay roles.
3. **Role.tenantId = membership.tenantId** → imposible asignar rol de otro tenant.

### 4.3 Legacy `iam_user.tenant_id`

No participa en la cadena de autorización. Eliminación programada FASE 15.x (PASO 13.6).

---

## 5. Decisiones de diseño

| # | Pregunta | Decisión |
|---|----------|----------|
| 1 | ¿RBAC o ABAC? | RBAC en FASE 14; ABAC/policies futuro |
| 2 | ¿Permisos globales o por tenant? | **Globales** (catálogo plataforma) |
| 3 | ¿Roles globales o por tenant? | **Por tenant** (custom roles por organización) |
| 4 | ¿Dónde vive el código FASE 14? | **`identity-access-management`** (co-location pragmática) |
| 5 | ¿Schema `auth` o `iam`? | Extender **`iam`** schema |
| 6 | ¿Formato permission code? | `resource:action` (ROADMAP) |
| 7 | ¿JWT con roles embebidos? | Opcional futuro; **no autoritativo** |
| 8 | ¿Framework externo? | **No** |

---

## 6. Fuera de alcance FASE 14

* PolicyAggregate / reglas condicionales
* PrivilegeAggregate
* Role hierarchy / herencia
* AuthorizationDecision persistente
* OPA / Casbin / Spring Authorization Server
* CQRS / event sourcing para permisos
* Consolidación identities (FASE 13.3 ejecutable) — diferida, BD solo dev

---

## 7. Plan de implementación (14.1–14.9)

| Paso | Entregable | Dependencia |
|------|------------|-------------|
| 14.1 | Aggregate `Role`, `iam.role`, repository | 14.0 ✅ |
| 14.2 | Aggregate `Permission`, `iam.permission` | 14.1 |
| 14.3 | `iam.role_permission` | 14.1, 14.2 |
| 14.4 | `iam.membership_role` | 14.1, membership V7 |
| 14.5 | `AuthorizationService` | 14.3, 14.4 |
| 14.6 | `AuthorizationContext` | 14.5, TenantContext |
| 14.7 | HTTP authorization WebFlux | 14.6 |
| 14.8 | Seeds ADMIN/OWNER/READ_ONLY | 14.3 |
| 14.9 | Verification IT suite | 14.7, 14.8 |

---

## 8. Riesgos identificados

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Asignación cross-tenant role↔membership | Alta | Validación dominio + tests 14.9 |
| Co-location IAM/Auth confunde límites | Media | Packages separados; ADR-007 |
| Divergencia specs vs ROADMAP (naming) | Baja | ROADMAP gana en FASE 14; sync specs después |
| Performance joins membership→roles→permissions | Media | Query optimizado en repository; cache post-14.9 |

---

## 9. Escalamiento arquitectónico

**No requerido** para FASE 14.0 — ADR-007 es nueva ADR pero no modifica ADR-006 ni tenancy; formaliza RBAC sobre membership ya previsto en ADR-006.

Escalar solo si:

* Se cambia membership como ancla de autorización
* Se adopta framework externo de autorización
* Se mueve tenancy o identity model

---

## 10. Criterios de aceptación 14.0

| Criterio | Estado |
|----------|--------|
| Modelo documentado | ✅ |
| Aggregates identificados | ✅ |
| Relaciones y ownership definidos | ✅ |
| Límites de contexto claros | ✅ |
| ADR-007 creada | ✅ |
| ROADMAP actualizado | ✅ |
| Sin código en 14.0 | ✅ |

---

## 11. Siguiente paso

**14.1 — Roles Domain:** implementar aggregate `Role`, value objects, migración Flyway `V9`, persistencia R2DBC y tests.
