# CodeCore — Roadmap de implementación

**Última actualización:** 2026-07-11  
**Módulos de plataforma:** `identity-access-management` · `organization-management`  
**Arquitectura:** Spring Boot 3 · Java 21 · WebFlux · R2DBC · DDD · Hexagonal · Modular Monolith  
**IAM:** ✅ **FOUNDATION COMPLETE** (FASE 15 + 15.9.2–15.9.4)  
**Organization Management:** ✅ **BOUNDED CONTEXT CLOSED** (FASE 16 + ADR-010/011)  
**Metodología FASE 16+:** [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
**Planificación FASE 17:** [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)

---

## Estado por fase

| Fase | Nombre | Estado | Último paso |
|------|--------|--------|-------------|
| **10** | IAM Foundation | ✅ Cerrada | 10.9 |
| **11** | JWT & Security HTTP | ✅ Cerrada | 11.3 |
| **12** | Tenant & Membership | ✅ Cerrada | 12.9 |
| **13** | Identity Global Migration | ✅ Cerrada | 13.6 |
| **14** | Authorization Foundation | ✅ Cerrada | 14.9 + 14.9.1 audit |
| **15** | IAM Administration | ✅ Cerrada | 15.9.4 |
| **16** | Organization Management | ✅ Cerrada | 16.10 — BC estable (ADR-011) |
| **17** | Clinical Foundation | 🟡 En curso | **17.4** Patient Persistence — siguiente **17.5** |
| **18+** | Scheduling · Records · Inventory · Billing · Platform | ⏳ Pendiente | Ver § Roadmap por BC |

---

## Fases cerradas (resumen)

| Fase | Objetivo | Documentación |
|------|----------|---------------|
| **10** | Identity, R2DBC, registro, autenticación | `docs/audits/PASO-10.*` |
| **11** | JWT, login HTTP, WebFilter | `docs/audits/PASO-11.*` |
| **12** | Tenant, membership N:M, TenantContext | `docs/audits/PASO-12.*` |
| **13** | Identity Global + Membership (ADR-006) | `docs/audits/PASO-13.*` |
| **14** | RBAC membership-scoped, AuthorizationService, seeds V13 (ADR-007) | `docs/audits/PASO-14.*` · [ADR-007](ADR-007-AUTHORIZATION-MODEL.md) |
| **15** | IAM Administration HTTP completa (ADR-008), OpenAPI, verificación E2E | `docs/audits/PASO-15.*` · [ADR-008](ADR-008-IAM-ADMINISTRATION-API.md) |

### FASE 14 — entregables clave

Identity → Membership → Role → Permission → `AuthorizationService` → `@RequiresPermission` (AOP) · V9–V13 · `IamPermissionCatalog` · `TenantSystemRolesProvisioner` · verificación E2E en tests.

**Auditoría operabilidad:** [PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md](../audits/PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md) — infraestructura RBAC completa; **sin API administrativa HTTP**.

---

# FASE 15 — IAM Administration ✅ (cerrada)

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
| **15.9** | IAM Administration Verification | ✅ | E2E `IamAdministrationVerificationIT` — cierre fase |

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

### 15.9 IAM Administration Verification ✅

- `IamAdministrationVerificationIT` — 8 verificaciones E2E (journey, RBAC, tenant, ownership, bootstrap, OpenAPI)
- Cierre FASE 15
- Documentación: `PASO-15.9-IAM-ADMINISTRATION-VERIFICATION.md`

### 15.9.1 IAM Production Readiness Audit ✅

- Auditoría profunda pre-producción
- Veredicto: READY WITH MAJOR DEBT; FASE 16 no bloqueada
- Documentación: `PASO-15.9.1-IAM-PRODUCTION-READINESS-AUDIT.md`

### 15.9.2 Bootstrap Strategy ✅

- `ApplicationRunner` + `BootstrapPlatformUseCase` — tenant + OWNER en BD vacía
- Config `codecore.platform.bootstrap.*` (env-gated, disabled por defecto)
- Documentación: `PASO-15.9.2-BOOTSTRAP-STRATEGY.md` · ADR-009 P0

### 15.9.3 Tenant Status Enforcement ✅

- `TenantOperationalGuard` — ACTIVE required en login + API
- Reactivación SUSPENDED vía `PUT /tenants/current`; DISABLED no reactivable
- Documentación: `PASO-15.9.3-TENANT-STATUS-ENFORCEMENT.md` · ADR-009 P1

### 15.9.4 Identity Disable Semantics ✅

- `DELETE /users` → `membership.deactivate()` (offboarding tenant)
- `PUT /users` status DISABLED → offboarding global identity
- Documentación: `PASO-15.9.4-IDENTITY-DISABLE-SEMANTICS.md` · ADR-009 P1

---

## IAM Foundation Complete

FASE 15 + pasos 15.9.2–15.9.4 cierran la **base IAM** para módulos de negocio.

Deuda de producción diferida registrada en [ADR-009](ADR-009-PRODUCTION-READINESS-BACKLOG.md) (Password Recovery, Audit, JWT stale, OpenAPI, Observability).

---

# FASE 16 — Organization Management ✅ (cerrada)

## Contexto

IAM cerrado (FASE 15). Comienza el **dominio de negocio**. Organizations modela la estructura interna del cliente SaaS (clínicas, departamentos, sucursales) **sin** alterar ADR-006 ni ADR-007.

**Regla:** No modificar IAM salvo bugs o seeds acotados de permisos `organization:*` / `office:*` (paso 16.3).

## Fuente de verdad

- Este `ROADMAP.md`
- [PASO-16.0-ORGANIZATIONS-AUDIT.md](../audits/PASO-16.0-ORGANIZATIONS-AUDIT.md)
- [PASO-16.0.1-ORGANIZATIONS-ROADMAP.md](../audits/PASO-16.0.1-ORGANIZATIONS-ROADMAP.md) — decisiones y definición de Organization
- [ADR-010](ADR-010-ORGANIZATIONS-MODEL.md) — Organizations Model ✅
- [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) — Organization Integration Patterns ✅
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md) — cómo consumir Organization desde otros BC ✅
- [CONTEXT-MAP.md](../../codecore-specifications/architecture/core/CONTEXT-MAP.md) §26 — `tenant != organization`

## Definición: ¿Qué es Organization?

Unidad estructural de **negocio acotada por tenant** — subdivisión operativa del cliente (clínica, departamento, sucursal). **No** es Tenant (aislamiento SaaS), Identity (auth), ni Membership (acceso IAM).

```text
Tenant (IAM)
 └── Organization (1..N)     ← FASE 16 (cerrada)
      └── Office (0..N)
           └── StaffAssignment (membershipId + scope)

Patient (Clinical Foundation)  ← FASE 17 — TenantId + opcional PrimaryOrganizationId
```

## Decisiones arquitectónicas (cerradas en 16.0.1)

| # | Pregunta | Decisión |
|---|----------|----------|
| 1 | Organization pertenece a | **Tenant** (A) |
| 2 | Múltiples orgs por tenant | **Sí** |
| 3 | Org sin tenant | **No** |
| 4 | Office depende de | **Organization** (+ `tenant_id` denormalizado) |
| 5 | Staff | **Membership** (IAM) + **StaffAssignment** (org/office) |
| 6 | Patient | **Tenant** + opcional org/office (datos en **FASE 17**) |
| 7 | Organization aggregate root | **Sí**; Office = aggregate root separado |
| 8 | Permisos propios | **Sí** — `organization:*`, `office:*`, `staff-assignment:*` |
| 9 | Ubicación | **Nuevo módulo** `organization-management` + schema `org` |

## Objetivo

Entregar jerarquía operativa tenant → organization → office, API administrativa `/api/v1/org/**`, asignación de staff por scope, y verificación E2E — sobre IAM Foundation Complete.

## Modo de trabajo (FASE 16+)

Política completa (*Constitution Document*): **[DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)**

Resumen:

| Principio | Regla |
|-----------|--------|
| **Filosofía** | Un framework de negocio coherente — no módulos aislados |
| **Foco** | Dominio primero — nunca desde DB ni controller |
| **Irreversibles** | Aggregate, ownership, BC, lifecycle, jerarquía → auditoría obligatoria |
| **IDs** | Solo referencias por ID entre aggregates — nunca objetos completos |
| **Consistencia** | Cada aggregate solo sus invariantes — sin transacciones cross-aggregate |
| **Checklist** | 12 ítems en verde antes de escribir código |
| **No reinventar** | Reutilizar patrones IAM / Organization salvo ADR |
| **Orden** | Checklist → Auditoría → ADR → Dominio → … → ROADMAP |

Pasos rutinarios (ej. 16.4–16.7): implementación directa + cierre documental.

**16.8** valida y cierra el bounded context — **sin código** — vía ADR-011.

Pasos 16.0–16.3.1 y **16.7** conservaron auditorías de implementación (cerradas).

## Restricciones

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003 tenant isolation.

No introducir: CQRS · Event Sourcing · Microservicios · organization-scoped RBAC (FASE 16).

## Resultado esperado al cerrar FASE 16

Flujo **vía HTTP real**:

1. Tenant OWNER autenticado  
2. Crear Organizations (ej. Dental Norte, Centro, Sur)  
3. Crear Offices bajo cada Organization  
4. Asignar staff (`membershipId`) a org/office  
5. Staff con permisos consume endpoints org scoped al tenant  
6. Cross-tenant org access → 403/404  
7. Verificación E2E `OrganizationVerificationIT`  

---

## Pasos FASE 16

| Paso | Nombre | Estado | Entregable principal |
|------|--------|--------|----------------------|
| **16.0** | Organizations Audit | ✅ | Auditoría IAM + hipótesis inicial |
| **16.0.1** | Organizations Roadmap & Decisions | ✅ | Decisiones obligatorias + modelo objetivo |
| **16.1** | Organizations Domain Foundation | ✅ | ADR-010, aggregate `Organization`, ports, 16 tests |
| **16.2** | Organization Persistence | ✅ | Schema `org`, Flyway V14, R2DBC, ITs |
| **16.3** | Organization Authorization Contract | ✅ | Catálogo 12 permisos, matriz RBAC, Flyway V15 |
| **16.3.1** | Organization Admin API Audit | ✅ | Decisiones HTTP/archive/pagination (cierre fundación) |
| **16.4** | Organization Administration API | ✅ | CRUD `/api/v1/org/organizations` |
| **16.5** | Office Domain & Persistence | ✅ | Aggregate `Office`, Flyway V16, R2DBC |
| **16.6** | Office Administration API | ✅ | CRUD `/api/v1/org/offices` + guard archive org |
| **16.7** | Staff Organizational Assignment | ✅ | `StaffAssignment`, `/staff-assignments`, V17 |
| **16.8** | Organization Validation & Integration Patterns | ✅ | ADR-011 + ORGANIZATION-CONSUMPTION-GUIDE |
| **16.9** | Organization Verification | ✅ | `OrganizationVerificationIT` (8 checks) |
| **16.10** | Organization Management Closeout | ✅ | OpenAPI `org-administration` · FASE 16 cerrada |

### 16.0 Organizations Audit ✅

- IAM Foundation Complete verificado; sin bloqueantes
- Hipótesis `Tenant → Organization → Office`
- Documentación: [PASO-16.0-ORGANIZATIONS-AUDIT.md](../audits/PASO-16.0-ORGANIZATIONS-AUDIT.md)

### 16.0.1 Organizations Roadmap & Decisions ✅

- Definición formal de Organization con evidencia del código IAM
- Respuesta a 9 decisiones obligatorias
- Veredicto escenarios: **SÍ**
- Documentación: [PASO-16.0.1-ORGANIZATIONS-ROADMAP.md](../audits/PASO-16.0.1-ORGANIZATIONS-ROADMAP.md)

### 16.1 Organizations Domain Foundation ✅

- ADR-010 accepted
- Módulo `organization-management` (domain, application, infrastructure, contract)
- Aggregate `Organization` + value objects + outbound ports
- 16 domain tests green
- Sin HTTP, sin Flyway
- Documentación: [PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md](../audits/PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md)

### 16.2 Organization Persistence ✅

- Schema `org`, tabla `org.organization`, Flyway V14
- `OrganizationEntity`, `OrganizationMapper`, `R2dbcOrganizationRepository`
- `OrganizationModuleConfiguration` — sin tocar IAM
- `R2dbcOrganizationRepositoryIT` — 6 escenarios Testcontainers
- Documentación: [PASO-16.2-ORGANIZATION-PERSISTENCE.md](../audits/PASO-16.2-ORGANIZATION-PERSISTENCE.md)

### 16.3 Organization Authorization Contract ✅

- Auditoría 10 preguntas + matriz RBAC formal
- `OrganizationPermissionCatalog` (12 permisos) + `IamPermissionCatalog` extendido (28 total)
- `SystemRoleTemplate` actualizado — OWNER/ADMIN/MANAGER/USER/READ_ONLY
- Flyway V15 idempotente + backfill role_permission
- Tests: `SystemRoleTemplateTest`, `OrganizationPermissionCatalogTest`, migration ITs
- Documentación: [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT-AUDIT.md](../audits/PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT-AUDIT.md) · [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md](../audits/PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md)

### 16.3.1 Organization Administration API Audit ✅

- Decisiones HTTP/DTO/queries/paginación/archive antes de 16.4
- Documentación: [PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md](../audits/PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md)

### 16.4 Organization Administration API ✅

- `OrganizationAdminController` + use cases + admin query repository
- Paginación, filtro `status=ACTIVE|ARCHIVED|ALL`, archive/activate
- `codecore-api` wired · 2 unit tests + 6 ITs (Docker)
- Documentación: [PASO-16.4-ORGANIZATION-ADMINISTRATION-API.md](../audits/PASO-16.4-ORGANIZATION-ADMINISTRATION-API.md)

### 16.5 Office Domain & Persistence ✅

- Aggregate `Office` + VOs + 3 domain tests
- Flyway V16 `org.office` · R2DBC repositories
- Documentación: [PASO-16.5-OFFICE-DOMAIN-PERSISTENCE.md](../audits/PASO-16.5-OFFICE-DOMAIN-PERSISTENCE.md)

### 16.6 Office Administration API ✅

- `OfficeAdminController` + `OfficeAdministrationUseCaseImpl`
- Guard: archivar org bloqueado si offices ACTIVE (409)
- 3 ITs WebFlux (Docker)
- Documentación: [PASO-16.6-OFFICE-ADMINISTRATION-API.md](../audits/PASO-16.6-OFFICE-ADMINISTRATION-API.md)

### 16.7 Staff Organizational Assignment ✅

- Aggregate `StaffAssignment` + Flyway V17 + `StaffAssignmentAdminController`
- Cross-BC: `MembershipReferencePort` (IAM membership ACTIVE)
- Delete físico (sin archive); scope org u office
- Auditoría: [PASO-16.7-STAFF-ASSIGNMENT-AUDIT.md](../audits/PASO-16.7-STAFF-ASSIGNMENT-AUDIT.md)
- Cierre: [PASO-16.7-STAFF-ORGANIZATIONAL-ASSIGNMENT.md](../audits/PASO-16.7-STAFF-ORGANIZATIONAL-ASSIGNMENT.md)

### 16.8 Organization Validation & Integration Patterns ✅

- Validación modelo 16.1–16.7 vs escenarios Patient / Appointment / Billing / Inventory
- Cierre oficial del bounded context Organization Management (v1)
- [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) — reglas de integración cross-BC
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md) — guía por módulo
- Sin código — documentación prescriptiva para FASE 17+
- Cierre: [PASO-16.8-ORGANIZATION-VALIDATION-INTEGRATION-PATTERNS.md](../audits/PASO-16.8-ORGANIZATION-VALIDATION-INTEGRATION-PATTERNS.md)

### 16.9 Organization Verification ✅

- `OrganizationVerificationIT` — 8 verificaciones E2E (journey, RBAC, tenant, archive guards, OpenAPI)
- Fix WebTestClient: security headers disabled en `PlatformSecurityAutoConfiguration`
- Documentación: [PASO-16.9-ORGANIZATION-VERIFICATION.md](../audits/PASO-16.9-ORGANIZATION-VERIFICATION.md)

### 16.10 Organization Management Closeout ✅

- `OrgOpenApiConfiguration` — springdoc grupo `org-administration`
- FASE 16 → **✅ Cerrada**
- Documentación: [PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md](../audits/PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md)

---

# Etapa post-Organization — Roadmap por Bounded Context

**Cambio de mentalidad (2026-07-11):** CodeCore deja de evolucionar por “features sueltas” y avanza por **bounded contexts completos** (diseño → implementación → verificación → cierre), consumiendo Organization vía ADR-011 **sin reabrir FASE 16**.

Planificación: [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)

## Secuencia definitiva

| Fase | Bounded Context | Estado | Dependencia clave |
|------|-----------------|--------|-------------------|
| **10–15** | IAM (plataforma) | ✅ | — |
| **16** | Organization Management | ✅ | IAM Foundation |
| **17** | **Clinical Foundation** (`Patient`) | 🟡 17.4 ✅ Persistence | ADR-012 · schema `clinical` · V18 |
| **18** | **Scheduling** (`Appointment`) | ⏳ | Patient + StaffAssignment |
| **19** | **Clinical Records** (`MedicalRecord`) | ⏳ | Patient · OrganizationId custodian |
| **20** | **Inventory** | ⏳ | OfficeId · OrganizationId |
| **21** | **Billing & Subscriptions** | ⏳ | OrganizationId · Membership seats |
| **22** | **Platform Services** | ⏳ | IAM — Invitations, password recovery (ADR-009) |
| **23** | **Audit & Observability** | ⏳ | Transversal (ADR-009 P2) |
| **24** | **Production Hardening** | ⏳ | Transversal (ADR-009) |

**Product packs** (Dental / PetNova / …): **después** de 17–19 (núcleo clínico estable). Componen BCs; no sustituyen FASE 17–19.

### Por qué no Invitations como FASE 17

| Pregunta | Respuesta |
|----------|-----------|
| ¿Valida Organization? | **No** |
| ¿Consume StaffAssignment? | **No** |
| ¿Aporta dominio clínico? | **No** |
| ¿Bloquea Patient si se aplaza? | **No** |
| ¿Qué es? | **Platform Service** (IAM-adjacent) → **FASE 22** |

Detalle: [PASO-17.0 §1](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md).

### Qué pasó con fases anteriores del roadmap

| Antes | Ahora | Rationale |
|-------|-------|-----------|
| 17 Invitations | **22 Platform Services** | No prueba ADR-011; onboarding de acceso ≠ clínica |
| 18 Business Module Framework | **Disuelto** | [DEVELOPMENT-POLICY-FASE-16-PLUS](DEVELOPMENT-POLICY-FASE-16-PLUS.md) ya es el framework |
| 19 Dental / PetNova | Product packs post 17–19 | Verticales sobre BCs clínicos |
| Patient “en FASE 19” | **FASE 17** | Primer consumer de Organization (ADR-011) |

### Deuda técnica programada

| Ítem | Origen | Cuándo |
|------|--------|--------|
| Password recovery | ADR-009 P1 | **FASE 22** Platform Services |
| Invitations | Roadmap histórico | **FASE 22** Platform Services |
| Audit trail | ADR-009 P2 | **FASE 23** |
| JWT stale mitigation | ADR-009 P2 | **FASE 24** |
| OpenAPI auth group | ADR-009 P2 | **FASE 24** |
| Drop `iam_user.tenant_id` | PASO 13.6 | Post-admin IAM / FASE 22+ |
| Consolidación datos 13.3 | Solo si prod duplicados | FUTURE-PROD |
| JWT `tenantId` desde membership | Mejora opcional | FASE 22+ |

---

# FASE 17 — Clinical Foundation 🟡

## Contexto

Organization Management está **cerrado**. El siguiente valor arquitectónico es el **primer bounded context clínico** que consume Organization por IDs + Query Ports (ADR-011), sin reabrir Org.

## Objetivo

Entregar el BC **Clinical Foundation** con aggregate **`Patient`**: dominio, persistencia, contrato de autorización, API admin, verificación E2E y closeout — patrón idéntico a FASE 15/16.

## Primer Aggregate Root: `Patient`

| Pregunta | Respuesta |
|----------|-----------|
| ¿Por qué Patient? | Sujeto del cuidado; Appointment / MedicalRecord / billing clínico referencian `PatientId` |
| ¿Por qué no Appointment? | Scheduling depende de Patient |
| ¿Por qué no MedicalRecord? | Documentación pertenece a Patient |
| Consume | IAM (`TenantId` JWT, inmutable) · Organization (`PrimaryOrganizationId` opcional vía contract ports) |
| No conoce | `OfficeId`, StaffAssignment, Membership, Identity como “ser paciente”, Scheduling, Billing |
| IDs | `PatientId`, `TenantId`, opcional `PrimaryOrganizationId` |
| Invariantes | Tenant inmutable; primary org ACTIVE en altas; única identidad clínica registral del sujeto en el tenant; archive soft; sin consistencia TX cross-BC |

## Pasos FASE 17

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **17.0** | Clinical Foundation Planning | ✅ | Este paso | — | ROADMAP + [PASO-17.0](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md) |
| **17.0.1** | Patient Aggregate Audit | ✅ | **Obligatoria** | Prep. ADR-012 | [PASO-17.0.1](../audits/PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md) |
| **17.1** | Clinical Foundation Contract | ✅ | — | **ADR-012 Accepted** | [PASO-17.1](../audits/PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md) · modelo **congelado** |
| **17.2** | Bounded Context Reference Contracts | ✅ | [PASO-17.2](../audits/PASO-17.2-REFERENCE-CONTRACTS.md) | **ADR-013 Accepted** | Patrón ReferencePort + `OrganizationReferencePort` (+ adapter/tests); `OfficeReferencePort` declarado |
| **17.3** | Patient Domain Foundation | ✅ | [PASO-17.3](../audits/PASO-17.3-PATIENT-DOMAIN-FOUNDATION.md) | ADR-012 | Aggregate `Patient` + VOs + 28 domain tests; módulos Gradle; ports out |
| **17.4** | Patient Persistence | ✅ | [PASO-17.4](../audits/PASO-17.4-PATIENT-PERSISTENCE.md) | — | V18 `clinical.patient` + R2DBC adapters + ITs |
| **17.5** | Patient Authorization Contract | ⏳ | Mínima permisos | — | `patient:*` + seed |
| **17.5.1** | Patient Admin API Audit | ⏳ | **Obligatoria** | — | HTTP/DTO/paginación/archive |
| **17.6** | Patient Administration API | ⏳ | — | — | CRUD `/api/v1/.../patients` |
| **17.7** | Patient Verification | ⏳ | — | — | E2E verification IT |
| **17.8** | Clinical Foundation Closeout | ⏳ | — | — | OpenAPI · FASE 17 ✅ |

## Restricciones FASE 17

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010/011/012/013.

No introducir: CQRS · Event Sourcing · Microservicios · org-scoped RBAC · Invitations · Appointment.

No modificar: aggregates IAM ni Organization/Office/StaffAssignment.

## Resultado esperado al cerrar FASE 17

1. Paciente creado bajo tenant (+ org/office opcionales validados por ports)  
2. Listado/archivo con RBAC `patient:*`  
3. Cross-tenant → 404  
4. Verification E2E verde  
5. Siguiente fase = **18 Scheduling**  

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
| ADR-009 | Production Readiness Backlog | Accepted (15.9.2) |
| ADR-010 | Organizations Model | Accepted (16.1) |
| ADR-011 | Organization Integration Patterns | Accepted (16.8) |
| ADR-012 | Patient Domain Model | **Accepted** (17.1) — **frozen**; cambios → nuevo ADR |
| ADR-013 | Bounded Context Reference Contracts | **Accepted** (17.2) — patrón oficial cross-BC |

**Ubicación:** `docs/architecture/ADR-*.md`

---

## Regla de escalamiento arquitectónico

Nueva ADR o paso estilo 13.x solo si el cambio altera tenancy, Identity/Membership de forma estructural, RBAC base, seguridad transversal o arquitectura SaaS.

Cambios rutinarios en FASE 15 (CRUD admin sobre modelo existente) **no** requieren escalamiento si respetan ADR-003, ADR-006 y ADR-007.

FASE 16 introduce **ADR-010** (dominio de negocio nuevo) sin modificar ADR-006/007. Seeds de permisos en IAM son cambio acotado, no reescritura de RBAC.

FASE 17 introduce **ADR-012 Accepted** (Patient frozen), **ADR-013** (Reference Contracts para todo CodeCore) y **consume** Organization vía ADR-011/013 — sin modificar ADR-010.

---

## Proceso autónomo Cursor

1. Revisar este ROADMAP y ADRs vigentes  
2. Revisar [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
3. Revisar último `PASO-*.md` de la fase activa  
4. Ejecutar el siguiente paso pendiente  
5. Tests acotados al paso (cuando haya código)  
6. Actualizar ROADMAP + audit de cierre  

### Siguiente acción

**PASO 17.5 — Patient Authorization Contract** — permisos `patient:*` + seed IAM. Sin HTTP aún.

Referencias: [PASO-17.4-PATIENT-PERSISTENCE.md](../audits/PASO-17.4-PATIENT-PERSISTENCE.md) · [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md).

---

## Historial de cierres

| Fecha | Fase | Evento |
|-------|------|--------|
| 2026-07-11 | **17.4** | Patient Persistence — V18 clinical.patient + R2DBC |
| 2026-07-11 | **17.3** | Patient Domain Foundation — Aggregate + VOs + 28 tests (ADR-012) |
| 2026-07-11 | **17.2** | ADR-013 Accepted — Reference Contracts + `OrganizationReferencePort` |
| 2026-07-11 | **17.1** | ADR-012 Accepted — Patient contract frozen · Clinical Foundation |
| 2026-07-11 | **17.0.1** | Patient Aggregate Audit — identidad clínica registral; ADR-012 Proposed |
| 2026-07-11 | **17.0** | ROADMAP reorganizado por BC — Clinical Foundation planificada; Invitations → FASE 22 |
| 2026-06-22 | **16** | **ORGANIZATION MANAGEMENT COMPLETE** — ADR-010/011, V14–V17, E2E verification |
| 2026-06-22 | 16.10 | OpenAPI org-administration + closeout |
| 2026-06-22 | 16.9 | OrganizationVerificationIT — 8 checks E2E |
| 2026-06-22 | 16.8 | ADR-011 integration patterns + consumption guide |
| 2026-06-22 | 16.2 | Organization persistence — schema org, V14, R2DBC |
| 2026-06-22 | 16.1 | Organizations domain foundation — ADR-010, aggregate, ports, tests |
| 2026-06-22 | 16.0.1 | Organizations roadmap — decisiones arquitectónicas cerradas |
| 2026-06-17 | 16.0 | Organizations audit — inicio FASE 16 |
| 2026-06-17 | 15.9.4 | Identity disable semantics — tenant offboarding |
| 2026-06-17 | 15.9.3 | Tenant status enforcement |
| 2026-06-17 | 15.9.2 | Platform bootstrap strategy |
| 2026-06-17 | 15.9.1 | IAM production readiness audit |
| 2026-06-17 | 15 | **IAM FOUNDATION COMPLETE** |
| 2026-06-17 | 15.9 | IAM Administration Verification — `IamAdministrationVerificationIT` |
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
