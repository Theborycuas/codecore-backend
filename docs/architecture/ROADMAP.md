# CodeCore — Roadmap de implementación

**Última actualización:** 2026-06-15  
**Módulo principal:** `identity-access-management`  
**Arquitectura:** Spring Boot 3 · Java 21 · WebFlux · R2DBC · DDD · Hexagonal · Modular Monolith

---

## Estado por fase

| Fase | Nombre | Estado | Último paso |
|------|--------|--------|-------------|
| **10** | IAM Foundation | ✅ **CERRADA** | 10.9 Authentication Use Case |
| **11** | JWT & Security HTTP | ✅ **CERRADA** | 11.3 Authentication WebFilter |
| **12** | Tenant & Membership | ✅ **CERRADA** | 12.9 Tenant Context |
| **13** | Identity Global Migration | ✅ **CERRADA** | 13.6 Deprecate Analysis |
| **14** | Authorization Foundation | 🔵 **EN CURSO** | — |
| **15+** | RBAC · Invitations · Schema cleanup | ⏳ Pendiente | — |

---

## FASE 10 — IAM Foundation ✅

**Objetivo:** Modelo de dominio Identity, persistencia R2DBC, registro y autenticación base.

| Paso | Entregable | Estado |
|------|------------|--------|
| 10.4 | IAM Persistence Model | ✅ |
| 10.5 | Domain ↔ Persistence alignment | ✅ |
| 10.6 | RegisterIdentityUseCase | ✅ |
| 10.7 | Persistence integration tests | ✅ |
| 10.8 | HTTP adapter (register) | ✅ |
| 10.9 | AuthenticateIdentityUseCase | ✅ |

**Documentación:** `docs/audits/PASO-10.4` – `PASO-10.9`

---

## FASE 11 — JWT & Security HTTP ✅

**Objetivo:** Tokens JWT, validación, login HTTP, filtro de seguridad reactivo.

| Paso | Entregable | Estado |
|------|------------|--------|
| 11.0 | JWT Foundation | ✅ |
| 11.1 | Login HTTP | ✅ |
| 11.2 | JWT Validation | ✅ |
| 11.3 | Authentication WebFilter | ✅ |

**Documentación:** `docs/audits/PASO-11.0` – `PASO-11.3`

---

## FASE 12 — Tenant & Membership ✅

**Objetivo:** Tenants, membership N:M, integración auth, JWT tenant claim, TenantContext.

| Paso | Entregable | Estado |
|------|------------|--------|
| 12.0 | Tenant Foundation | ✅ |
| 12.1 | Create Tenant | ✅ |
| 12.2 | Identity Tenant Membership | ✅ |
| 12.3 | Membership Integration | ✅ |
| 12.4 | Membership Audit | ✅ |
| 12.5 | Membership Backfill (V8) | ✅ |
| 12.6 | Transaction Audit | ✅ |
| 12.7 | Transactional Registration | ✅ |
| 12.8 | JWT Tenant Claim | ✅ |
| 12.9 | Tenant Context | ✅ |

**Documentación:** `docs/audits/PASO-12.0` – `PASO-12.9`

---

## FASE 13 — Identity Global Migration ✅

**Objetivo:** Transición a Identity Global + Membership (ADR-006).

| Paso | Entregable | Estado | Notas |
|------|------------|--------|-------|
| 13.0 | Tenant-Aware Operations Audit | ✅ | |
| 13.0.1 | Identity Strategy Decision (ADR-006) | ✅ | Opción B |
| 13.1 | Identity Lookup Migration | ✅ | `findByEmail` / `existsByEmail` |
| 13.2 | Membership-Centric Auth Audit | ✅ | |
| 13.3 | Identity Consolidation Strategy | ✅ | Documentado |
| 13.4 | Consolidation Migration Audit | ✅ | Inventario SQL |
| 13.5 | Source Of Truth Verification | ✅ | Auth path verificado |
| 13.6 | Deprecate `tenant_id` Analysis | ✅ | **NO** eliminable aún |

### Decisiones de cierre FASE 13

- **ADR-006** adoptada; flujo auth/registro alineado operativamente.
- **Consolidación de datos (13.3 ejecutable):** diferida — BD solo dev/test, sin usuarios productivos.
- **`iam_user.tenant_id`:** permanece; eliminación programada post-FASE 14 (FASE 15.x).
- **JWT `tenantId` desde membership:** mejora opcional futura; no bloqueante.

**Documentación:** `docs/audits/PASO-13.0` – `PASO-13.6` · `docs/architecture/ADR-006-IDENTITY-STRATEGY.md`

---

# FASE 14 — Authorization Foundation

## Objetivo

Construir el modelo completo de autorización multi-tenant para CodeCore.

La autorización debe basarse en:

Identity Global
+
Membership
+
Roles
+
Permissions

y no directamente sobre Identity.

---

# Estado inicial

Al comenzar la FASE 14 ya existe:

✓ Identity Global

✓ IdentityTenantMembership

✓ JWT Tenant Claim

✓ TenantContext

✓ Login

✓ Registro

✓ Membership ACTIVE validation

---

# Arquitectura objetivo

Identity
↓
IdentityTenantMembership
↓
RoleAssignment
↓
Role
↓
Permission
↓
Authorization

---

# 14.0 Authorization Foundation

## Objetivo

Definir el modelo de autorización.

### Entregables

ADR-007-AUTHORIZATION-MODEL.md

PASO-14.0-AUTHORIZATION-FOUNDATION.md

### Decisiones

* Role-based authorization
* Permission-based authorization
* Scope por tenant
* Fuente de verdad

### Resultado

Modelo aprobado.

---

# 14.1 Roles Domain

## Objetivo

Crear Aggregate Role.

### Entidades

Role

### Value Objects

RoleId
RoleCode
RoleName

### Reglas

Role pertenece a un Tenant.

RoleCode único por tenant.

### Ejemplos

ADMIN
MANAGER
VET
RECEPTIONIST
READ_ONLY

### Resultado

Dominio de Roles implementado.

---

# 14.2 Permissions Domain

## Objetivo

Crear Aggregate Permission.

### Entidades

Permission

### Value Objects

PermissionId
PermissionCode

### Reglas

Permission global.

No depende del tenant.

### Ejemplos

user:create
user:update
user:delete

patient:create
patient:update
patient:view

appointment:create
appointment:update

### Resultado

Catálogo de permisos implementado.

---

# 14.3 Role Permissions

## Objetivo

Relacionar:

Role
↔
Permission

### Tabla

role_permission

### Reglas

N:M

### Ejemplo

ADMIN

* user:create
* user:update
* user:delete

VET

* patient:view
* patient:update

### Resultado

Roles con permisos.

---

# 14.4 Membership Roles

## Objetivo

Relacionar:

Membership
↔
Role

### Tabla

membership_role

### Reglas

Un membership puede tener múltiples roles.

### Ejemplo

Juan

Tenant A

Roles:

* ADMIN
* BILLING

Tenant B

Roles:

* VET

### Resultado

Roles asignados por tenant.

---

# 14.5 Authorization Service

## Objetivo

Crear servicio de autorización.

### API

hasPermission()

hasAnyPermission()

hasRole()

### Fuente

Membership
↓
Role
↓
Permission

### Resultado

Motor de autorización funcional.

---

# 14.6 Authorization Context

## Objetivo

Integrar:

JWT
↓
TenantContext
↓
AuthorizationContext

### Resultado

Autorización disponible en Use Cases.

---

# 14.7 HTTP Authorization

## Objetivo

Proteger endpoints.

### Ejemplo

@RequiresPermission("patient:create")

o equivalente WebFlux.

### Resultado

Autorización HTTP.

---

# 14.8 Seeds

## Objetivo

Crear roles iniciales.

### Roles

ADMIN

OWNER

READ_ONLY

### Permisos

Catálogo mínimo inicial.

### Resultado

Instalación lista para usar.

---

# 14.9 Authorization Verification

## Objetivo

Validar:

* roles
* permisos
* memberships
* tenant isolation

### Resultado

FASE 14 cerrada.

---

# Resultado esperado al finalizar

Identity Global
↓
Membership
↓
Roles
↓
Permissions
↓
Authorization

Listo para:

FASE 15 — Organizations

FASE 16 — Invitations

FASE 17 — Billing

FASE 18 — Business Modules
(PetNova, Dental, CognisoftOne, etc.)




## FASE 15+ — Pendiente ⏳

| Área | Descripción | Dependencia |
|------|-------------|-------------|
| **15.x** | RBAC completo (roles, permissions runtime) | FASE 14 |
| **15.x** | Drop `iam_user.tenant_id` + domain refactor | 13.6 análisis |
| **16.x** | Invitations workflow | ADR-006 + membership |
| **16.x** | Tenant switching / org picker | Multi-membership |
| **17.x** | Billing seats por membership | Subscription module |
| **FUTURE-PROD** | Data consolidation (13.3 ejecutable) | Solo si prod tiene duplicados |

---

## ADRs vigentes

| ADR | Tema | Estado |
|-----|------|--------|
| ADR-001 | Reactive-first | Accepted |
| ADR-002 | Event-driven | Accepted |
| ADR-003 | Multi-tenant isolation | Accepted |
| ADR-004 | Hexagonal architecture | Accepted |
| ADR-005 | DDD | Accepted |
| ADR-006 | Identity Global + Membership | Accepted |

**Ubicación specs:** `codecore-specifications/architecture/adr/`  
**ADR-006 backend:** `docs/architecture/ADR-006-IDENTITY-STRATEGY.md`

---

## Regla de escalamiento arquitectónico

Escalar a **revisión arquitectónica** (nueva ADR o paso 13.x-style) únicamente cuando el cambio propuesto:

- Modifique el modelo de **tenancy**
- Modifique **Identity** o **Membership**
- Introduzca o altere **RBAC** de forma estructural
- Cambie el modelo de **seguridad** (auth, JWT, sessions)
- Altere la arquitectura **SaaS** transversal
- Requiera una **nueva ADR**

Cambios rutinarios dentro de FASE 14 (roles, permissions sobre membership existente) **no** requieren escalamiento si respetan ADR-003 y ADR-006.

---

## Proceso autónomo Cursor (post-FASE 13)

Tras cerrar una fase, el agente debe:

1. **Revisar** este `ROADMAP.md`
2. **Revisar** ADRs en `docs/architecture/` y `codecore-specifications/architecture/adr/`
3. **Revisar** últimos `docs/audits/PASO-*.md` de la fase anterior
4. **Identificar** el siguiente paso lógico (primero pendiente de la fase activa)
5. **Proponer** implementación al usuario (breve) o ejecutar si el paso es claro
6. **Implementar** código, tests, migraciones según alcance del paso
7. **Ejecutar** `./gradlew :modules:identity-access-management:test` (y módulos afectados)
8. **Actualizar** `ROADMAP.md` y generar `docs/audits/PASO-X.Y-*.md` de cierre

### Siguiente acción recomendada

**FASE 14.0 — Authorization Foundation Audit**

- Inventariar blueprints `authorization-management` en specifications
- Definir aggregates Role, Permission, MembershipRole
- Alinear con ADR-006 (rol scoped a membership)
- Sin implementación en 14.0 — solo auditoría y plan

---

## Historial de cierres

| Fecha | Fase | Evento |
|-------|------|--------|
| 2026-06-15 | 13 | Cierre formal Identity Global Migration |
| — | 12 | Tenant & Membership completa |
| — | 11 | JWT & Security HTTP completa |
| — | 10 | IAM Foundation completa |
