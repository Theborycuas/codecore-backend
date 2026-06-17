# CodeCore — Roadmap de implementación

**Última actualización:** 2026-06-17  
**Módulo principal:** `identity-access-management`  
**Arquitectura:** Spring Boot 3 · Java 21 · WebFlux · R2DBC · DDD · Hexagonal · Modular Monolith

---

## Estado por fase

| Fase | Nombre | Estado | Último paso |
|------|--------|--------|-------------|
| **10** | IAM Foundation | ✅ Cerrada | 10.9 |
| **11** | JWT & Security HTTP | ✅ Cerrada | 11.3 |
| **12** | Tenant & Membership | ✅ Cerrada | 12.9 |
| **13** | Identity Global Migration | ✅ Cerrada | 13.6 |
| **14** | Authorization Foundation | ✅ Cerrada | 14.9 + 14.9.1 audit |
| **15** | IAM Administration | 🔵 **En curso** | 15.8 |
| **16+** | Organizations · Invitations · Billing · Business | ⏳ Pendiente | — |

---

## Fases cerradas (resumen)

| Fase | Objetivo | Documentación |
|------|----------|---------------|
| **10** | Identity, R2DBC, registro, autenticación | `docs/audits/PASO-10.*` |
| **11** | JWT, login HTTP, WebFilter | `docs/audits/PASO-11.*` |
| **12** | Tenant, membership N:M, TenantContext | `docs/audits/PASO-12.*` |
| **13** | Identity Global + Membership (ADR-006) | `docs/audits/PASO-13.*` |
| **14** | RBAC membership-scoped, AuthorizationService, seeds V13 (ADR-007) | `docs/audits/PASO-14.*` · [ADR-007](ADR-007-AUTHORIZATION-MODEL.md) |

### FASE 14 — entregables clave

Identity → Membership → Role → Permission → `AuthorizationService` → `@RequiresPermission` (AOP) · V9–V13 · `IamPermissionCatalog` · `TenantSystemRolesProvisioner` · verificación E2E en tests.

**Auditoría operabilidad:** [PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md](../audits/PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md) — infraestructura RBAC completa; **sin API administrativa HTTP**.

---

# FASE 15 — IAM Administration 🔵

## Contexto

PASO-14.9.1 confirmó: RBAC operativo en dominio/persistencia/runtime, pero **no** hay API administrativa para Users, Memberships, Roles, Permissions ni Assignments.

Antes de Organizations (FASE 16), cerrar la **capa administrativa IAM** sobre la infraestructura FASE 14.

## Fuente de verdad

- Este `ROADMAP.md`
- [ADR-006](ADR-006-IDENTITY-STRATEGY.md) · [ADR-007](ADR-007-AUTHORIZATION-MODEL.md)
- [ADR-008](ADR-008-IAM-ADMINISTRATION-API.md)
- [PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md](../audits/PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md)

## Objetivo

Construir la capa administrativa HTTP completa sobre IAM existente.

## Modo de trabajo (cada paso)

1. Auditoría mínima  
2. Diseño  
3. Implementación  
4. Tests (solo del paso)  
5. Documentación (`PASO-15.x-*.md`)  
6. Actualizar este ROADMAP  

## Restricciones

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC.

No introducir: CQRS · Event Sourcing · Microservicios · frameworks RBAC externos.

## Regla crítica

Toda operación administrativa debe protegerse con `@RequiresPermission(...)` (infraestructura FASE 14).

## Resultado esperado al cerrar FASE 15

Flujo **vía HTTP real** (no solo tests internos):

1. Crear Tenant  
2. Crear Usuario  
3. Crear Membership  
4. Crear Role  
5. Asignar Permission  
6. Asignar Role  
7. Login  
8. Consumir endpoint protegido  

---

## Pasos FASE 15

| Paso | Nombre | Estado | Entregable principal |
|------|--------|--------|----------------------|
| **15.0** | IAM Administration Foundation | ✅ | ADR-008, convenciones API, primer endpoint protegido productivo |
| **15.0.1** | Ownership Rules Audit | ✅ | Matriz jerárquica OWNER→READ_ONLY |
| **15.1** | User Administration | ✅ | CRUD/list `user:*` → `/api/v1/iam/users` |
| **15.2** | Membership Administration | ✅ | CRUD/list `membership:*` → `/api/v1/iam/memberships` |
| **15.3** | Role Administration | ✅ | CRUD/list `role:*` → `/api/v1/iam/roles` |
| **15.4** | Permission Administration | ✅ | Catálogo `permission:read` → `/api/v1/iam/permissions` |
| **15.5** | Role Permission Administration | ✅ | `permission:assign` → `/roles/{id}/permissions` |
| **15.6** | Membership Role Administration | ✅ | `membership:update` → `/memberships/{id}/roles` |
| **15.7** | Tenant Administration | ✅ | `tenant:*` → `/tenants/current` + bootstrap endurecido |
| **15.8** | OpenAPI | ✅ | Contrato `/v3/api-docs/iam-administration` + Swagger UI |
| **15.9** | IAM Administration Verification | ⏳ | E2E HTTP completo, cierre fase |

---

### 15.0 IAM Administration Foundation ✅

- ADR-008: rutas, permisos, bootstrap vs admin, mapeo Identity → User API  
- Paquete `interfaces.http.admin`, `IamAdminApiPaths`  
- `GET /api/v1/iam/administration/status` con `@RequiresPermission("role:read")`  
- Documentación: `PASO-15.0-IAM-ADMINISTRATION-FOUNDATION.md`

### 15.1 User Administration ✅

- `GET/POST/PUT/DELETE /api/v1/iam/users` con `@RequiresPermission`
- Membership-first listado; `Identity.disable()` para delete
- `OwnershipPolicy` (15.0.1); `IdentityRegistrationOrchestrator`
- Documentación: `PASO-15.1-USER-ADMINISTRATION.md`

### 15.2 Membership Administration ✅

- `GET/POST/PUT/DELETE /api/v1/iam/memberships` con `@RequiresPermission`
- Alta: vincular identity existente o crear con `IdentityRegistrationOrchestrator`
- `membership.deactivate()` para delete; ownership en DELETE (15.0.1)
- Documentación: `PASO-15.2-MEMBERSHIP-ADMINISTRATION.md`

### 15.3 Role Administration ✅

- `GET/POST/PUT/DELETE /api/v1/iam/roles` con `@RequiresPermission`
- Roles custom mutables; system roles solo lectura (403)
- DELETE físico si no hay `membership_role`; CASCADE en `role_permission`
- Documentación: `PASO-15.3-ROLE-ADMINISTRATION.md`

### 15.4 Permission Administration ✅

- `GET /api/v1/iam/permissions` y `GET /{id}` con `permission:read`
- Catálogo global read-only (semillas V13); sin mutaciones
- Documentación: `PASO-15.4-PERMISSION-ADMINISTRATION.md`

### 15.5 Role Permission Administration ✅

- `GET/PUT /api/v1/iam/roles/{roleId}/permissions` con `permission:assign`
- PUT replace: diff dominio + `RolePermissionRepository.replaceAll`
- System roles: GET ok, PUT → 403
- Documentación: `PASO-15.5-ROLE-PERMISSION-ADMINISTRATION.md`

### 15.6 Membership Role Administration ✅

- `GET/PUT /api/v1/iam/memberships/{membershipId}/roles` con `membership:update`
- PUT replace: diff dominio + `MembershipRoleRepository.replaceAll`
- `OwnershipPolicy` en PUT; membership ACTIVE requerida
- Documentación: `PASO-15.6-MEMBERSHIP-ROLE-ADMINISTRATION.md`

### 15.7 Tenant Administration ✅

- `GET/PUT /api/v1/iam/tenants/current` con `tenant:read` / `tenant:update`
- `Tenant.rename()`; estados ACTIVE / SUSPENDED / DISABLED
- Bootstrap: `POST /tenants` y `POST /identities` requieren JWT (ADR-008 15.7)
- Documentación: `PASO-15.7-TENANT-ADMINISTRATION.md`

### 15.8 OpenAPI ✅

- springdoc-openapi WebFlux; grupo `iam-administration` para `/api/v1/iam/**`
- JWT Bearer + extensión `x-permission` desde `@RequiresPermission`
- Swagger UI en dev; deshabilitado en prod
- Documentación: `PASO-15.8-OPENAPI-IAM-ADMINISTRATION.md`

### 15.9 (pendiente)

Ver tabla anterior. Cada paso consume permisos ya sembrados en V13 / `IamPermissionCatalog`.

---

## Roadmap futuro (post-FASE 15)

| Fase | Nombre | Dependencia |
|------|--------|-------------|
| **16** | Organizations | FASE 15 |
| **17** | Invitations | ADR-006 + membership |
| **18** | Business Module Framework | FASE 15+ |
| **19** | Dental / PetNova | Módulos negocio |
| **20** | Billing & Subscriptions | Membership seats |
| **21** | Audit & Observability | Transversal |
| **22** | Production Hardening | Transversal |

### Deuda técnica programada

| Ítem | Origen | Cuándo |
|------|--------|--------|
| Drop `iam_user.tenant_id` | PASO 13.6 | Post-admin IAM |
| Consolidación datos 13.3 | Solo si prod duplicados | FUTURE-PROD |
| JWT `tenantId` desde membership | Mejora opcional | FASE 16+ |

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
| ADR-007 | Authorization Model | Accepted |
| ADR-008 | IAM Administration API | Accepted (15.0) |

**Ubicación:** `docs/architecture/ADR-*.md`

---

## Regla de escalamiento arquitectónico

Nueva ADR o paso estilo 13.x solo si el cambio altera tenancy, Identity/Membership de forma estructural, RBAC base, seguridad transversal o arquitectura SaaS.

Cambios rutinarios en FASE 15 (CRUD admin sobre modelo existente) **no** requieren escalamiento si respetan ADR-003, ADR-006 y ADR-007.

---

## Proceso autónomo Cursor

1. Revisar este ROADMAP y ADRs vigentes  
2. Revisar último `PASO-*.md` de la fase activa  
3. Ejecutar el siguiente paso pendiente  
4. Tests acotados al paso  
5. Actualizar ROADMAP + audit de cierre  

### Siguiente acción

**FASE 15.9 — IAM Administration Verification** (tras cerrar 15.8)

---

## Historial de cierres

| Fecha | Fase | Evento |
|-------|------|--------|
| 2026-06-17 | 15.8 | OpenAPI IAM — springdoc grupo `iam-administration` |
| 2026-06-17 | 15.7 | Tenant Administration — `/tenants/current` + bootstrap endurecido |
| 2026-06-17 | 15.6 | Membership Role Administration — `/memberships/{id}/roles` |
| 2026-06-17 | 15.5 | Role Permission Administration — `/roles/{id}/permissions` |
| 2026-06-17 | 15.4 | Permission Administration — catálogo `/api/v1/iam/permissions` |
| 2026-06-17 | 15.3 | Role Administration — `/api/v1/iam/roles` |
| 2026-06-17 | 15.2 | Membership Administration — `/api/v1/iam/memberships` |
| 2026-06-17 | 15.1 | User Administration — `/api/v1/iam/users` + ownership |
| 2026-06-17 | 15.0.1 | Ownership Rules Audit |
| 2026-06-15 | 15.0 | IAM Administration Foundation — ADR-008 + admin API base |
| 2026-05-27 | 14 | Cierre FASE 14 — RBAC + seeds + verificación |
| 2026-05-27 | 14.9.1 | RBAC Operability Audit |
| 2026-06-15 | 13 | Cierre Identity Global Migration |
| — | 12 | Tenant & Membership |
| — | 11 | JWT & Security HTTP |
| — | 10 | IAM Foundation |



FASE 16
ORGANIZATIONS

FASE 17
INVITATIONS

FASE 18
BUSINESS MODULE FRAMEWORK

FASE 19
DENTAL / PETNOVA

FASE 20
BILLING & SUBSCRIPTIONS

FASE 21
AUDIT & OBSERVABILITY

FASE 22
PRODUCTION HARDENING
