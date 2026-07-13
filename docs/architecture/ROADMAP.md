# CodeCore — Roadmap de implementación

**Última actualización:** 2026-07-12  
**Módulos de plataforma:** `identity-access-management` · `organization-management`  
**Arquitectura:** Spring Boot 3 · Java 21 · WebFlux · R2DBC · DDD · Hexagonal · Modular Monolith  
**IAM:** ✅ **FOUNDATION COMPLETE** (FASE 15 + 15.9.2–15.9.4)  
**Organization Management:** ✅ **BOUNDED CONTEXT CLOSED** (FASE 16 + ADR-010/011)  
**Clinical Foundation:** ✅ **BOUNDED CONTEXT CLOSED** (FASE 17 + ADR-012/013)  
**Scheduling:** ✅ **BOUNDED CONTEXT CLOSED** (FASE 18 + ADR-014)  
**Clinical Records:** ✅ **BOUNDED CONTEXT CLOSED** (FASE 19 + ADR-015)  
**Inventory:** ✅ **ITEM SLICE CLOSED** (FASE 20 · ADR-016 frozen · [guía](INVENTORY-CONSUMPTION-GUIDE.md))  
**Billing:** ✅ **INVOICE SLICE CLOSED** (FASE 21 · ADR-017 frozen · [guía](BILLING-CONSUMPTION-GUIDE.md))  
**Payments:** ✅ **PAYMENT SLICE CLOSED** (FASE 22 · ADR-018 frozen · [guía](PAYMENTS-CONSUMPTION-GUIDE.md))  
**Platform Services:** ✅ **ACCESS / INVITATION SLICE CLOSED** (FASE 23 · ADR-019 frozen · [guía](ACCESS-CONSUMPTION-GUIDE.md)) · Password Recovery **Done** · Subscription **después**  
**Metodología FASE 16+:** [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
**Planificación FASE 17:** [PASO-17.0](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md) · cierre [PASO-17.8](../audits/PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md)  
**Planificación FASE 18:** [PASO-18.0](../audits/PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · cierre [PASO-18.8](../audits/PASO-18.8-SCHEDULING-CLOSEOUT.md) · guía [SCHEDULING-CONSUMPTION-GUIDE.md](SCHEDULING-CONSUMPTION-GUIDE.md)  
**Planificación FASE 19:** [PASO-19.0](../audits/PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) · cierre [PASO-19.8](../audits/PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md) · guía [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](CLINICAL-RECORDS-CONSUMPTION-GUIDE.md)  
**Planificación FASE 20:** [PASO-20.0](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) · cierre [PASO-20.8](../audits/PASO-20.8-INVENTORY-CLOSEOUT.md) · guía [INVENTORY-CONSUMPTION-GUIDE.md](INVENTORY-CONSUMPTION-GUIDE.md) · ADR [ADR-016](ADR-016-ITEM-DOMAIN-MODEL.md) · review [CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md)  
**Planificación FASE 21:** [PASO-21.0](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md) · cierre [PASO-21.8](../audits/PASO-21.8-BILLING-CLOSEOUT.md) · guía [BILLING-CONSUMPTION-GUIDE.md](BILLING-CONSUMPTION-GUIDE.md) · ADR [ADR-017](ADR-017-INVOICE-DOMAIN-MODEL.md)  
**Planificación FASE 22:** [PASO-22.0](../audits/PASO-22.0-PAYMENTS-FOUNDATION-PLANNING.md) · cierre [PASO-22.8](../audits/PASO-22.8-PAYMENTS-CLOSEOUT.md) · guía [PAYMENTS-CONSUMPTION-GUIDE.md](PAYMENTS-CONSUMPTION-GUIDE.md) · ADR [ADR-018](ADR-018-PAYMENT-DOMAIN-MODEL.md)  
**Planificación FASE 23:** [PASO-23.0](../audits/PASO-23.0-PLATFORM-SERVICES-FOUNDATION-PLANNING.md) · cierre [PASO-23.8](../audits/PASO-23.8-ACCESS-CLOSEOUT.md) · guía [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md) · ADR [ADR-019](ADR-019-INVITATION-DOMAIN-MODEL.md) · review [CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md)  
**Architecture Review:** [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ARCHITECTURE-REVIEW-2026-07.md)

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
| **17** | Clinical Foundation | ✅ Cerrada | 17.8 — BC estable (ADR-012/013) |
| **18** | Scheduling | ✅ Cerrada | 18.8 — BC estable (ADR-014) |
| **19** | Clinical Records | ✅ Cerrada | 19.8 — BC estable (ADR-015) |
| **20** | Inventory (Item) | ✅ Cerrada | 20.8 — Item slice estable (ADR-016) |
| **21** | Billing (Invoice) | ✅ Cerrada | 21.8 — Invoice slice estable (ADR-017) |
| **22** | Payments (Payment) | ✅ Cerrada | 22.8 — Payment slice estable (ADR-018) |
| **23** | Platform Services (Access / Invitation) | ✅ Cerrada | 23.8 — Invitation slice estable (ADR-019) · Password Recovery Done · Subscription después |
| **24+** | Stock · Audit · Production Hardening · Subscription · … | ⏳ Pendiente | Ver § Roadmap por BC |

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

Deuda de producción en [ADR-009](ADR-009-PRODUCTION-READINESS-BACKLOG.md): Password Recovery **Done (FASE 23)**; P2 restante (Audit, JWT stale, OpenAPI, Observability).

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
| **17** | **Clinical Foundation** (`Patient`) | ✅ Cerrada | Organization + ADR-012/013 |
| **18** | **Scheduling** (`Appointment`) | ✅ Cerrada | Patient + StaffAssignment + Org/Office |
| **19** | **Clinical Records** (`Encounter`) | ✅ 19.8 Closed | Notes / Labs / Billing via EncounterId |
| **20** | **Inventory** (`Item`) | ✅ 20.8 Closed (Item) | Stock (mismo BC) · Billing material lines via ItemId |
| **21** | **Billing** (`Invoice`) | ✅ 21.8 Closed (Invoice) | Payments via InvoiceId · Stock (Inventory) en paralelo |
| **22** | **Payments** (`Payment`) | ✅ 22.8 Closed | InvoiceId + InvoiceReferencePort · Refunds/PSP adapters vía PaymentReferencePort (futuro) |
| **23** | **Platform Services** (umbrella) | ✅ Access/Invitation 23.8 Closed | Password Recovery **Done** · Subscription **después** (BC propio) |
| **24** | **Audit & Observability** | ⏳ | Transversal (ADR-009 P2) |
| **25** | **Production Hardening** | ⏳ | Transversal (ADR-009) |

**Product packs** (Dental / PetNova / …): **después** de 17–19 (núcleo clínico estable). Componen BCs; no sustituyen FASE 17–19.

### Por qué no Invitations como FASE 17

| Pregunta | Respuesta |
|----------|-----------|
| ¿Valida Organization? | **No** |
| ¿Consume StaffAssignment? | **No** |
| ¿Aporta dominio clínico? | **No** |
| ¿Bloquea Patient si se aplaza? | **No** |
| ¿Qué es? | **Platform Service** (IAM-adjacent) → **FASE 23** |

Detalle: [PASO-17.0 §1](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md).

### Qué pasó con fases anteriores del roadmap

| Antes | Ahora | Rationale |
|-------|-------|-----------|
| 17 Invitations | **23 Platform Services** | No prueba ADR-011; onboarding de acceso ≠ clínica |
| 18 Business Module Framework | **Disuelto** | [DEVELOPMENT-POLICY-FASE-16-PLUS](DEVELOPMENT-POLICY-FASE-16-PLUS.md) ya es el framework |
| 19 Dental / PetNova | Product packs post 17–19 | Verticales sobre BCs clínicos |
| Patient “en FASE 19” | **FASE 17** | Primer consumer de Organization (ADR-011) |

### Deuda técnica programada

| Ítem | Origen | Cuándo |
|------|--------|--------|
| Password recovery | ADR-009 P1 | **Done (FASE 23)** — HTTP/DB/use cases IAM |
| Invitations | Roadmap histórico | **Done (FASE 23)** — Access slice 23.8 |
| Audit trail | ADR-009 P2 | **FASE 24** |
| JWT stale mitigation | ADR-009 P2 | **FASE 25** |
| OpenAPI auth group | ADR-009 P2 | **FASE 25** |
| Drop `iam_user.tenant_id` | PASO 13.6 | Post-admin IAM / FASE 23+ |
| Consolidación datos 13.3 | Solo si prod duplicados | FUTURE-PROD |
| JWT `tenantId` desde membership | Mejora opcional | FASE 23+ |

---

# FASE 17 — Clinical Foundation ✅

## Contexto

Organization Management está **cerrado**. FASE 17 entregó el **primer bounded context clínico** que consume Organization por IDs + Query Ports (ADR-011/013), sin reabrir Org.

## Objetivo

Entregar el BC **Clinical Foundation** con aggregate **`Patient`**: dominio, persistencia, contrato de autorización, API admin, verificación E2E y closeout — patrón idéntico a FASE 15/16.

**Cierre:** [PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md](../audits/PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md) · consumo [PATIENT-CONSUMPTION-GUIDE.md](PATIENT-CONSUMPTION-GUIDE.md)

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
| **17.5** | Patient Authorization Contract | ✅ | [PASO-17.5](../audits/PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md) · [Audit](../audits/PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT-AUDIT.md) | — | `PatientPermissionCatalog` + V19 seeds + RBAC matrix |
| **17.5.1** | Patient Admin API Audit | ✅ | [PASO-17.5.1](../audits/PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md) | — | HTTP/DTO/paginación/archive — espejo Org; sin código |
| **17.6** | Patient Administration API | ✅ | [PASO-17.6](../audits/PASO-17.6-PATIENT-ADMINISTRATION-API.md) | — | CRUD `/api/v1/clinical/patients` |
| **17.7** | Patient Verification | ✅ | [PASO-17.7](../audits/PASO-17.7-PATIENT-VERIFICATION.md) | — | E2E + Core validation (BC listo para consumo) |
| **17.8** | Clinical Foundation Closeout | ✅ | [PASO-17.8](../audits/PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md) | — | OpenAPI · `PatientReferencePort` · guía · FASE 17 ✅ |

## Restricciones FASE 17

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010/011/012/013.

No introducir: CQRS · Event Sourcing · Microservicios · org-scoped RBAC · Invitations · Appointment.

No modificar: aggregates IAM ni Organization/Office/StaffAssignment.

## Resultado al cerrar FASE 17

1. Paciente creado bajo tenant (+ org opcional validada por ports) ✅  
2. Listado/archivo con RBAC `patient:*` ✅  
3. Cross-tenant → 404 ✅  
4. Verification E2E verde ✅  
5. `PatientReferencePort` publicado ✅  
6. Siguiente fase = **18 Scheduling**  

---

# FASE 18 — Scheduling ✅ (cerrada)

## Contexto

Clinical Foundation e Organization están **cerrados**. FASE 18 introduce **Scheduling** como primer BC que consume el grafo completo Patient + StaffAssignment + Organization/Office **sin reabrir** FASE 16/17 — validación viva del Core Platform ([Architecture Review 17.9](CODECORE-ARCHITECTURE-REVIEW-2026-07.md)).

## Objetivo

Entregar el BC **Scheduling** con aggregate **`Appointment`**: compromiso planificado de atención, vertical-agnóstico, *intentionally small*.

Planificación: [PASO-18.0](../audits/PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · Audit: [PASO-18.0.1](../audits/PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md)

## Primer Aggregate Root: `Appointment`

| Pregunta | Respuesta |
|----------|-----------|
| One-sentence | El compromiso planificado de atención en el tiempo |
| ¿Por qué no Encounter? | Encounter = episodio ocurrido |
| ¿Por qué no Slot engine? | Sobreingeniería v1; capacidad diferida |
| Consume | `PatientId` · `StaffAssignmentId` · `OrganizationId` · `OfficeId?` · `TenantId` |
| No conoce | Membership/Identity como proveedor · MedicalRecord · Odontogram · Billing |
| Lifecycle v1 | `SCHEDULED` → `CANCELLED` \| `COMPLETED` |
| IDs | Solo referencias; validación vía ReferencePorts (ADR-013) |

## Pasos FASE 18

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **18.0** | Scheduling Foundation Planning | ✅ | Este paso | — | [PASO-18.0](../audits/PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) |
| **18.0.1** | Appointment Aggregate Audit | ✅ | **Obligatoria** | Prep. ADR-014 | [PASO-18.0.1](../audits/PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md) |
| **18.1** | Appointment Model ADR | ✅ | [PASO-18.1](../audits/PASO-18.1-APPOINTMENT-MODEL-CONTRACT.md) | **ADR-014 Accepted** | Modelo **congelado** |
| **18.2** | Scheduling Reference Ports | ✅ | [PASO-18.2](../audits/PASO-18.2-REFERENCE-PORTS.md) | ADR-013 | Office + StaffAssignment ports + adapters |
| **18.3** | Appointment Domain Foundation | ✅ | [PASO-18.3](../audits/PASO-18.3-APPOINTMENT-DOMAIN-FOUNDATION.md) | ADR-014 | Aggregate `Appointment` + VOs + 20 domain tests; módulos Gradle; ports out |
| **18.4** | Appointment Persistence | ✅ | [PASO-18.4](../audits/PASO-18.4-APPOINTMENT-PERSISTENCE.md) | — | V20 `scheduling.appointment` + R2DBC adapters + ITs |
| **18.5** | Appointment Authorization Contract | ✅ | [PASO-18.5](../audits/PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT.md) · [Audit](../audits/PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT-AUDIT.md) | — | `AppointmentPermissionCatalog` + V21 seeds + RBAC matrix |
| **18.5.1** | Appointment Admin API Audit | ✅ | [PASO-18.5.1](../audits/PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md) | — | HTTP/DTO/paginación/cancel·complete — espejo Patient; sin código |
| **18.6** | Appointment Administration API | ✅ | [PASO-18.6](../audits/PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md) | — | `/api/v1/scheduling/appointments` + multi-ReferencePort + ITs |
| **18.7** | Appointment Verification | ✅ | [PASO-18.7](../audits/PASO-18.7-APPOINTMENT-VERIFICATION.md) | — | `AppointmentVerificationIT` 8/8 + auditoría Core |
| **18.8** | Scheduling Closeout | ✅ | [PASO-18.8](../audits/PASO-18.8-SCHEDULING-CLOSEOUT.md) | — | `AppointmentReferencePort` · guía · FASE 18 ✅ |

## Restricciones FASE 18

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010/011/012/013.

**No modificar:** aggregates ni schemas IAM · Organization · Patient.

No introducir: CQRS · Event Sourcing · microservicios · org-scoped RBAC · Encounter · MedicalRecord · motor de slots · product packs verticales.

---

# FASE 19 — Clinical Records ✅

IAM · Organization · Clinical Foundation · Scheduling están **cerrados**. FASE 19 entregó **Clinical Records** (`Encounter`) — episodio de atención ocurrido *intentionally small* — **sin reabrir** FASE 16–18.

Cierre: [PASO-19.8](../audits/PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md) · Guía: [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](CLINICAL-RECORDS-CONSUMPTION-GUIDE.md)

## Contexto

IAM · Organization · Clinical Foundation · Scheduling estaban **cerrados**. FASE 19 introdujo **Clinical Records**: documentar lo que **ocurrió** en la atención — sin reabrir FASE 16–18 ([Scheduling Architecture Review](CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md) · opción A).

## Objetivo

Entregar el BC **Clinical Records** con primer aggregate **`Encounter`**: episodio de atención ocurrido, vertical-agnóstico, *intentionally small*.

Planificación: [PASO-19.0](../audits/PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md)

## Primer Aggregate Root: `Encounter`

| Pregunta | Respuesta |
|----------|-----------|
| One-sentence | El episodio de atención que **ocurrió** para un sujeto de cuidado en un contexto operativo |
| ¿Por qué no MedicalRecord? | Chart longitudinal → God Aggregate; proyección o aggregate posterior |
| ¿Por qué no Appointment? | Appointment **planifica**; Encounter **registra ocurrencia** |
| Consume | `PatientId` · `StaffAssignmentId` · `OrganizationId` · `OfficeId?` · `AppointmentId?` · `TenantId` |
| No conoce | Membership/Identity como proveedor · SOAP · Odontogram · Billing · Inventory |
| IDs | Solo referencias; validación vía ReferencePorts (ADR-013) |

## Pasos FASE 19

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **19.0** | Clinical Records Foundation Planning | ✅ | Este paso | — | [PASO-19.0](../audits/PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) |
| **19.0.1** | Encounter Aggregate Audit | ✅ | **Obligatoria** | Prep. ADR-015 | [PASO-19.0.1](../audits/PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md) |
| **19.1** | Encounter Model ADR | ✅ | [PASO-19.1](../audits/PASO-19.1-ENCOUNTER-MODEL-CONTRACT.md) | **ADR-015 Accepted** | Modelo **congelado** |
| **19.2** | Clinical Records Reference Readiness | ✅ | [PASO-19.2](../audits/PASO-19.2-REFERENCE-READINESS.md) | ADR-013 | `findLinkableByIdAndTenant` + `AppointmentReferenceView` |
| **19.3** | Encounter Domain Foundation | ✅ | [PASO-19.3](../audits/PASO-19.3-ENCOUNTER-DOMAIN-FOUNDATION.md) | ADR-015 | Aggregate `Encounter` + VOs + domain tests |
| **19.4** | Encounter Persistence | ✅ | [PASO-19.4](../audits/PASO-19.4-ENCOUNTER-PERSISTENCE.md) | — | V22 `records.encounter` + R2DBC + ITs |
| **19.5** | Encounter Authorization Contract | ✅ | [PASO-19.5](../audits/PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT.md) · [AUDIT](../audits/PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT-AUDIT.md) | — | `encounter:*` + V23 + RBAC matrix |
| **19.5.1** | Encounter Admin API Audit | ✅ | **Obligatoria** · [PASO-19.5.1](../audits/PASO-19.5.1-ENCOUNTER-ADMINISTRATION-API-AUDIT.md) | — | HTTP shape — sin código |
| **19.6** | Encounter Administration API | ✅ | [PASO-19.6](../audits/PASO-19.6-ENCOUNTER-ADMINISTRATION-API.md) | — | `/api/v1/records/encounters` |
| **19.7** | Encounter Verification | ✅ | [PASO-19.7](../audits/PASO-19.7-ENCOUNTER-VERIFICATION.md) | — | E2E VerificationIT 8/8 |
| **19.8** | Clinical Records Closeout | ✅ | [PASO-19.8](../audits/PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md) | — | `EncounterReferencePort` · guía · FASE 19 ✅ |

## Restricciones FASE 19

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010/011/012/013/014/015.

**No modificar / no reabrir:** IAM · Organization · Patient · Appointment · ADR-010…015.

No introducir: CQRS · Event Sourcing · microservicios · org-scoped RBAC · SOAP/odontogram/recetas en el Core · MedicalRecord God Aggregate · product packs verticales · event bus preventivo.

---

## FASE 20 — Inventory (Item slice) ✅

**Estado:** ✅ Cerrada · **ADR-016 Accepted** (Item frozen)  
**Cierre:** [PASO-20.8-INVENTORY-CLOSEOUT.md](../audits/PASO-20.8-INVENTORY-CLOSEOUT.md) · consumo [INVENTORY-CONSUMPTION-GUIDE.md](INVENTORY-CONSUMPTION-GUIDE.md)  
**Plan:** [PASO-20.0](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) · [ADR-016](ADR-016-ITEM-DOMAIN-MODEL.md)

### Primer Aggregate Root: `Item`

| Pregunta | Respuesta |
|----------|-----------|
| One-sentence | Identidad inventariable de algo que puede stockearse, moverse o consumirse bajo un Tenant |
| ¿Por qué no Stock? | Qty necesita `ItemId`; Stock es aggregate posterior del mismo BC |
| ¿Por qué no Product? | Sesgo comercio; `code` opcional cubre SKU |
| Consume | `TenantId` · `PrimaryOrganizationId?` · `OrganizationReferencePort` |
| No conoce | `OfficeId` · qty · price · BOM · Encounter/Patient · UoM en v1 |
| IDs | Solo referencias; validación vía ReferencePorts (ADR-013) |

### Pasos FASE 20

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **20.0** | Inventory Foundation Planning | ✅ | Este paso | — | [PASO-20.0](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) |
| **20.0.1** | Item Aggregate Audit | ✅ | **Obligatoria** | Prep. ADR-016 | [PASO-20.0.1](../audits/PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md) |
| **20.1** | Item Model ADR | ✅ | [PASO-20.1](../audits/PASO-20.1-ITEM-MODEL-CONTRACT.md) | **ADR-016 Accepted** | Modelo **congelado** |
| **20.2** | Inventory Reference Readiness | ✅ | [PASO-20.2](../audits/PASO-20.2-INVENTORY-REFERENCE-READINESS.md) | ADR-013 | Org port suficiente — **sin evolución** |
| **20.3** | Item Domain Foundation | ✅ | [PASO-20.3](../audits/PASO-20.3-ITEM-DOMAIN-FOUNDATION.md) | ADR-016 | Aggregate `Item` + VOs + 27 domain tests |
| **20.4** | Item Persistence | ✅ | [PASO-20.4](../audits/PASO-20.4-ITEM-PERSISTENCE.md) | — | V24 `inventory.item` + R2DBC + ITs 12/12 |
| **20.5** | Item Authorization Contract | ✅ | [PASO-20.5](../audits/PASO-20.5-ITEM-AUTHORIZATION-CONTRACT.md) · [AUDIT](../audits/PASO-20.5-ITEM-AUTHORIZATION-CONTRACT-AUDIT.md) | — | `item:*` + V25 + RBAC matrix |
| **20.5.1** | Item Admin API Audit | ✅ | **Obligatoria** · [PASO-20.5.1](../audits/PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md) | — | HTTP shape `/api/v1/inventory/items` |
| **20.6** | Item Administration API | ✅ | [PASO-20.6](../audits/PASO-20.6-ITEM-ADMINISTRATION-API.md) | — | `/api/v1/inventory/items` + ITs 6/6 |
| **20.7** | Item Verification | ✅ | [PASO-20.7](../audits/PASO-20.7-ITEM-VERIFICATION.md) | — | `ItemVerificationIT` 8/8 |
| **20.8** | Inventory Closeout (Item) | ✅ | [PASO-20.8](../audits/PASO-20.8-INVENTORY-CLOSEOUT.md) | — | `ItemReferencePort` · guía · FASE 20 ✅ |

### Restricciones FASE 20

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010–016.

**No modificar / no reabrir:** IAM · Organization · Patient · Appointment · Encounter · ADR-010…015.

No introducir: Stock/qty en Item · precios · BOM · lotes · ports clínicos preventivos · org-scoped RBAC · WMS/POS en el Core · event bus preventivo.

---

## FASE 21 — Billing (Invoice slice) ✅

**Estado:** ✅ Cerrada · **ADR-017 Accepted** (Invoice frozen)  
**Cierre:** [PASO-21.8-BILLING-CLOSEOUT.md](../audits/PASO-21.8-BILLING-CLOSEOUT.md) · consumo [BILLING-CONSUMPTION-GUIDE.md](BILLING-CONSUMPTION-GUIDE.md)  
**Plan:** [PASO-21.0](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md) · [ADR-017](ADR-017-INVOICE-DOMAIN-MODEL.md)

### Primer Aggregate Root: `Invoice`

| Pregunta | Respuesta |
|----------|-----------|
| One-sentence | El reclamo comercial de que un monto es debido por un deudor a una organización emisora, dentro de un Tenant |
| ¿Por qué no Payment? | Payment es aggregate posterior (BC Payments) que referencia `InvoiceId` |
| ¿Por qué no Subscription? | Subscriptions es SaaS billing de la plataforma (FASE 22), no facturación clínica/comercial del Tenant |
| Consume | `TenantId` · `OrganizationId` (issuer) · `BillToPatientId` \| `BillToOrganizationId` · `ItemId?` · `EncounterId?` — todos vía ReferencePorts (ADR-013) |
| No conoce | Pagos · impuestos · asientos contables · Stock · Appointment · cantidades/UoM en línea |
| Lifecycle | `DRAFT → ISSUED → VOIDED` — sin `PAID`, sin delete físico, sin un-void |

### Pasos FASE 21

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **21.0** | Billing Foundation Planning | ✅ | [PASO-21.0](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md) | — | BC Billing · primer root `Invoice` |
| **21.0.1** | Invoice Aggregate Audit | ✅ | **Obligatoria** · [PASO-21.0.1](../audits/PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md) | Prep. ADR-017 | Bill-to Patient\|Org; DRAFT/ISSUED/VOIDED |
| **21.1** | Invoice Model ADR | ✅ | [PASO-21.1](../audits/PASO-21.1-INVOICE-MODEL-CONTRACT.md) | **ADR-017 Accepted** | Modelo **congelado** |
| **21.2** | Billing Reference Readiness | ✅ | [PASO-21.2](../audits/PASO-21.2-BILLING-REFERENCE-READINESS.md) | ADR-013 | Org/Patient/Item/Encounter ports suficientes — **sin evolución** |
| **21.3** | Invoice Domain Foundation | ✅ | [PASO-21.3](../audits/PASO-21.3-INVOICE-DOMAIN-FOUNDATION.md) | ADR-017 | Aggregate `Invoice` + `InvoiceLine` + VOs + 38 domain tests |
| **21.4** | Invoice Persistence | ✅ | [PASO-21.4](../audits/PASO-21.4-INVOICE-PERSISTENCE.md) | — | V26 `billing.invoice`+`invoice_line` + R2DBC + ITs 10/10 |
| **21.5** | Invoice Authorization Contract | ✅ | [PASO-21.5](../audits/PASO-21.5-INVOICE-AUTHORIZATION-CONTRACT.md) | — | `invoice:*` + V27 + RBAC matrix (ALL 44→49) |
| **21.5.1** | Invoice Admin API Audit | ✅ | **Obligatoria** · [PASO-21.5.1](../audits/PASO-21.5.1-INVOICE-ADMINISTRATION-API-AUDIT.md) | — | HTTP shape `/api/v1/billing/invoices` |
| **21.6** | Invoice Administration API | ✅ | [PASO-21.6](../audits/PASO-21.6-INVOICE-ADMINISTRATION-API.md) | — | `/api/v1/billing/invoices` + multi-ReferencePort + unit 14/14 |
| **21.7** | Invoice Verification | ✅ | [PASO-21.7](../audits/PASO-21.7-INVOICE-VERIFICATION.md) | — | `InvoiceVerificationIT` 8/8 |
| **21.8** | Billing Closeout (Invoice) | ✅ | [PASO-21.8](../audits/PASO-21.8-BILLING-CLOSEOUT.md) | — | `InvoiceReferencePort` · guía · FASE 21 ✅ |

### Restricciones FASE 21

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010–017.

**No modificar / no reabrir:** IAM · Organization · Patient · Appointment · Encounter · Inventory · ADR-010…016.

No introducir: pagos · notas de crédito · impuestos · asientos contables (GL) · Subscriptions SaaS · un-void · `PAID` · delete físico · org-scoped RBAC · event bus preventivo.

---

## FASE 22 — Payments (Payment slice) ✅

**Estado:** ✅ Cerrada · **ADR-018 Accepted** (Payment frozen)  
**Cierre:** [PASO-22.8-PAYMENTS-CLOSEOUT.md](../audits/PASO-22.8-PAYMENTS-CLOSEOUT.md) · consumo [PAYMENTS-CONSUMPTION-GUIDE.md](PAYMENTS-CONSUMPTION-GUIDE.md)  
**Plan:** [PASO-22.0](../audits/PASO-22.0-PAYMENTS-FOUNDATION-PLANNING.md) · [ADR-018](ADR-018-PAYMENT-DOMAIN-MODEL.md)

### Primer Aggregate Root: `Payment`

| Pregunta | Respuesta |
|----------|-----------|
| One-sentence | El registro de que un importe se aplicó hacia la liquidación de una Invoice bajo un Tenant |
| ¿Por qué no Refund? | Refund es aggregate posterior (BC futuro) que referencia `PaymentId` |
| ¿Por qué no PSP capture? | Orquestación de gateway externo; Payment solo registra el resultado |
| Consume | `TenantId` · `InvoiceId` (ISSUED) — vía `InvoiceReferencePort.existsIssuedByIdAndTenant` (ADR-013) |
| No conoce | Refund · impuestos · asientos contables · Subscription · `PAID` en Invoice |
| Lifecycle | `(create) → RECORDED → void → VOIDED` — sin DRAFT, sin content update, sin un-void |

### Pasos FASE 22

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **22.0** | Payments Foundation Planning | ✅ | [PASO-22.0](../audits/PASO-22.0-PAYMENTS-FOUNDATION-PLANNING.md) | — | BC Payments · primer root `Payment` |
| **22.0.1** | Payment Aggregate Audit | ✅ | **Obligatoria** · [PASO-22.0.1](../audits/PASO-22.0.1-PAYMENT-AGGREGATE-AUDIT.md) | Prep. ADR-018 | Registro de liquidación; RECORDED/VOIDED |
| **22.1** | Payment Model ADR | ✅ | [PASO-22.1](../audits/PASO-22.1-PAYMENT-MODEL-CONTRACT.md) | **ADR-018 Accepted** | Modelo **congelado** |
| **22.2** | Payments Reference Readiness | ✅ | [PASO-22.2](../audits/PASO-22.2-PAYMENTS-REFERENCE-READINESS.md) | ADR-013 | `InvoiceReferencePort.existsIssuedByIdAndTenant` suficiente — **sin evolución** |
| **22.3** | Payment Domain Foundation | ✅ | [PASO-22.3](../audits/PASO-22.3-PAYMENT-DOMAIN-FOUNDATION.md) | ADR-018 | Aggregate `Payment` + VOs + 20 domain tests |
| **22.4** | Payment Persistence | ✅ | [PASO-22.4](../audits/PASO-22.4-PAYMENT-PERSISTENCE.md) | — | V28 `payments.payment` + R2DBC + ITs 6/6 |
| **22.5** | Payment Authorization Contract | ✅ | [PASO-22.5](../audits/PASO-22.5-PAYMENT-AUTHORIZATION-CONTRACT.md) | — | `payment:*` + V29 + RBAC matrix (ALL 49→52) |
| **22.5.1** | Payment Admin API Audit | ✅ | **Obligatoria** · [PASO-22.5.1](../audits/PASO-22.5.1-PAYMENT-ADMINISTRATION-API-AUDIT.md) | — | HTTP shape `/api/v1/payments` |
| **22.6** | Payment Administration API | ✅ | [PASO-22.6](../audits/PASO-22.6-PAYMENT-ADMINISTRATION-API.md) | — | `/api/v1/payments` + `InvoiceReferencePort` · unit 5/5 |
| **22.7** | Payment Verification | ✅ | [PASO-22.7](../audits/PASO-22.7-PAYMENT-VERIFICATION.md) | — | `PaymentVerificationIT` 8/8 |
| **22.8** | Payments Closeout | ✅ | [PASO-22.8](../audits/PASO-22.8-PAYMENTS-CLOSEOUT.md) | — | `PaymentReferencePort` · guía · FASE 22 ✅ |

### Restricciones FASE 22

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010–018.

**No modificar / no reabrir:** IAM · Organization · Patient · Appointment · Encounter · Inventory · Billing (`Invoice`) · ADR-010…017.

No introducir: refund · captura PSP · impuestos · asientos contables (GL) · Subscriptions SaaS · `PAID` en Invoice · un-void · DELETE HTTP · org-scoped RBAC · event bus preventivo.

---

## FASE 23 — Platform Services (Access / Invitation slice) ✅

**Estado:** ✅ Cerrada (slice Invitation) · **ADR-019 Accepted** (Invitation frozen) · Password Recovery **Done**  
**Cierre:** [PASO-23.8-ACCESS-CLOSEOUT.md](../audits/PASO-23.8-ACCESS-CLOSEOUT.md) · consumo [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md)  
**Plan:** [PASO-23.0](../audits/PASO-23.0-PLATFORM-SERVICES-FOUNDATION-PLANNING.md) · [ADR-019](ADR-019-INVITATION-DOMAIN-MODEL.md)  
**Review:** [CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md) — **A** · ~9.2/10

### Primer Aggregate Root: `Invitation` (BC Access)

| Pregunta | Respuesta |
|----------|-----------|
| One-sentence | La intención de otorgar Membership en un Tenant a un email |
| ¿Por qué no Membership? | Membership es IAM; Invitation solo el join-intent |
| ¿Por qué no Subscription? | Comercial SaaS — BC **distinto**, **después** de 23.8 |
| Consume | IAM via reference ports + `TenantAccessProvisionPort` (accept) |
| No conoce | StaffAssignment · seats/plans · Notification inbox · PasswordReset |
| Lifecycle | `(create) → PENDING → accept\|revoke\|expire` — sin DRAFT, sin content update |

### Track A — Password Recovery ✅ Done

IAM completion (ADR-009 P1): V32 `iam.password_reset_request` + `POST /api/v1/auth/forgot-password` · `/reset-password`. **No** es el BC Access.

### Pasos FASE 23

| Paso | Nombre | Estado | Auditoría | ADR | Entregable principal |
|------|--------|--------|-----------|-----|----------------------|
| **23.0** | Platform Services Foundation Planning | ✅ | [PASO-23.0](../audits/PASO-23.0-PLATFORM-SERVICES-FOUNDATION-PLANNING.md) | — | Umbrella · BC Access · root `Invitation` |
| **23.0.1** | Invitation Aggregate Audit | ✅ | **Obligatoria** · [PASO-23.0.1](../audits/PASO-23.0.1-INVITATION-AGGREGATE-AUDIT.md) | Prep. ADR-019 | Opción A · PENDING→ACCEPTED\|REVOKED\|EXPIRED |
| **23.1** | Invitation Model ADR | ✅ | [PASO-23.1](../audits/PASO-23.1-INVITATION-MODEL-CONTRACT.md) | **ADR-019 Accepted** | Modelo **congelado** |
| **23.2** | Access / IAM Reference Readiness | ✅ | [PASO-23.2](../audits/PASO-23.2-ACCESS-REFERENCE-READINESS.md) | ADR-013 | IAM contract ports + `TenantAccessProvisionPort` |
| **23.3** | Invitation Domain Foundation | ✅ | [PASO-23.3](../audits/PASO-23.3-INVITATION-DOMAIN-FOUNDATION.md) | ADR-019 | Aggregate `Invitation` + VOs + 28 domain tests |
| **23.4** | Invitation Persistence | ✅ | [PASO-23.4](../audits/PASO-23.4-INVITATION-PERSISTENCE.md) | — | V30 `access.invitation` + R2DBC + ITs 3/3 |
| **23.5** | Invitation Authorization Contract | ✅ | [PASO-23.5](../audits/PASO-23.5-INVITATION-AUTHORIZATION-CONTRACT.md) | — | `invitation:*` + V31 + RBAC matrix (ALL 52→55) |
| **23.5.1** | Invitation Admin/Public API Audit | ✅ | **Obligatoria** · [PASO-23.5.1](../audits/PASO-23.5.1-INVITATION-ADMINISTRATION-API-AUDIT.md) | — | HTTP shape `/api/v1/access/invitations` + accept público |
| **23.6** | Invitation Administration API | ✅ | [PASO-23.6](../audits/PASO-23.6-INVITATION-ADMINISTRATION-API.md) | — | Admin + accept · unit 10/10 |
| **23.7** | Invitation Verification | ✅ | [PASO-23.7](../audits/PASO-23.7-INVITATION-VERIFICATION.md) | — | `InvitationVerificationIT` 8/8 |
| **23.8** | Access Closeout | ✅ | [PASO-23.8](../audits/PASO-23.8-ACCESS-CLOSEOUT.md) | — | `InvitationReferencePort` · guía · slice ✅ |

### Restricciones FASE 23

Mantener: DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC · ADR-003/006/007/010–019.

**No modificar / no reabrir:** IAM foundation (salvo seeds + Password Recovery track + contract ports) · Organization · Patient · Appointment · Encounter · Inventory · Billing · Payments · ADR-010…018.

No introducir en Invitation: Subscription / seats · StaffAssignment · Notification BC · Org-scoped invites · custom roles · un-revoke · DELETE HTTP · event bus preventivo.

**Subscription** = planning **después** de 23.8 — nunca dentro de Access.

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
| ADR-014 | Appointment Domain Model | **Accepted** (18.1) — **frozen**; cambios → nuevo ADR |
| ADR-015 | Encounter Domain Model | **Accepted** (19.1) — **frozen**; cambios → nuevo ADR |
| ADR-016 | Item Domain Model | **Accepted** (20.1) — **frozen**; cambios → nuevo ADR |
| ADR-017 | Invoice Domain Model | **Accepted** (21.1) — **frozen**; cambios → nuevo ADR |
| ADR-018 | Payment Domain Model | **Accepted** (22.1) — **frozen**; cambios → nuevo ADR |
| ADR-019 | Invitation Domain Model | **Accepted** (23.1) — **frozen**; cambios → nuevo ADR |

**Ubicación:** `docs/architecture/ADR-*.md`

---

## Regla de escalamiento arquitectónico

Nueva ADR o paso estilo 13.x solo si el cambio altera tenancy, Identity/Membership de forma estructural, RBAC base, seguridad transversal o arquitectura SaaS.

Cambios rutinarios en FASE 15 (CRUD admin sobre modelo existente) **no** requieren escalamiento si respetan ADR-003, ADR-006 y ADR-007.

FASE 16 introduce **ADR-010** (dominio de negocio nuevo) sin modificar ADR-006/007. Seeds de permisos en IAM son cambio acotado, no reescritura de RBAC.

FASE 17 introduce **ADR-012 Accepted** (Patient frozen), **ADR-013** (Reference Contracts para todo CodeCore) y **consume** Organization vía ADR-011/013 — sin modificar ADR-010.

FASE 20 introduce **ADR-016 Accepted** (Item frozen) y **consume** Organization vía ADR-011/013 — sin modificar ADR-010…015 ni BCs clínicos.

FASE 21 introduce **ADR-017 Accepted** (Invoice frozen) y **consume** Organization/Patient/Item/Encounter vía ADR-011/013 — sin modificar ADR-010…016 ni BCs previos.

FASE 22 introduce **ADR-018 Accepted** (Payment frozen) y **consume** Billing (`Invoice`) vía `InvoiceReferencePort` (ADR-013) — sin modificar ADR-010…017 ni BCs previos, sin embeber `PAID` en Invoice.

FASE 23 introduce **ADR-019 Accepted** (Invitation frozen) y **consume** IAM vía contract ports + `TenantAccessProvisionPort` — sin modificar ADR-010…018 ni BCs clínicos/económicos; Subscription **fuera** del slice.

---

## Proceso autónomo Cursor

1. Revisar este ROADMAP y ADRs vigentes  
2. Revisar [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
3. Revisar último `PASO-*.md` de la fase activa  
4. Ejecutar el siguiente paso pendiente  
5. Tests acotados al paso (cuando haya código)  
6. Actualizar ROADMAP + audit de cierre  

### Siguiente acción

**Subscription Foundation Planning** (BC propio, **después** de Access 23.8) — o **Stock** (continuación Inventory) en paralelo de producto.

Password Recovery (ADR-009 P1) = **Done**. Access/Invitation = **cerrado**.

**Sin reabrir** ADR-012…019 ni FASE 16–23 Invitation slice.

Referencias: [PASO-23.8](../audits/PASO-23.8-ACCESS-CLOSEOUT.md) · [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md) · [CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md).

---

## Historial de cierres

| Fecha | Fase | Evento |
|-------|------|--------|
| 2026-07-12 | **23.8** | **ACCESS INVITATION SLICE COMPLETE** — InvitationReferencePort · consumption guide · Password Recovery Done · FASE 23 Access ✅ |
| 2026-07-12 | **23.review** | Architecture Review Access — **A** · 9.2/10 · [CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md) |
| 2026-07-12 | **23.7** | Invitation Verification — E2E 8/8 (journey, RBAC, cross-tenant, OWNER, duplicate, revoke MANAGER, expired accept, OpenAPI) |
| 2026-07-12 | **23.6** | Invitation Administration API — `/api/v1/access/invitations` + accept · unit 10/10 |
| 2026-07-12 | **23.5** | Invitation Authorization Contract — `invitation:*` + V31 + RBAC matrix (ALL 52→55) |
| 2026-07-12 | **23.4** | Invitation Persistence — V30 `access.invitation` + R2DBC ITs 3/3 |
| 2026-07-12 | **23.3** | Invitation Domain Foundation — aggregate + VOs + 28 domain tests (ADR-019) |
| 2026-07-12 | **23.2** | Access / IAM Reference Readiness — IAM contract ports + `TenantAccessProvisionPort` listos |
| 2026-07-12 | **23.1** | ADR-019 Accepted — Invitation model frozen (*intentionally small*) |
| 2026-07-12 | **22.8** | **PAYMENTS SLICE COMPLETE** — PaymentReferencePort · consumption guide · FASE 22 ✅ |
| 2026-07-12 | **22.review** | Architecture Review Payments — **A** · 9.2/10 · [CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md) |
| 2026-07-12 | **23.0** | Platform Services Foundation Planning — umbrella · BC Access · root `Invitation` (≠ Subscription) |
| 2026-07-12 | **23.0.1** | Invitation Aggregate Audit — Opción A · PENDING→ACCEPTED\|REVOKED\|EXPIRED · prep. ADR-019 |
| 2026-07-12 | **22.7** | Payment Verification — E2E 8/8 (journey, RBAC, cross-tenant, invoice inválida/DRAFT, void MANAGER, OpenAPI, 401) |
| 2026-07-12 | **22.6** | Payment Administration API — `/api/v1/payments` + `InvoiceReferencePort` · unit 5/5 |
| 2026-07-12 | **22.5.1** | Payment Admin API Audit — HTTP shape `/api/v1/payments`; default `status=RECORDED` |
| 2026-07-12 | **22.5** | Payment Authorization Contract — `payment:*` + V29 + RBAC matrix (ALL 49→52) |
| 2026-07-12 | **22.4** | Payment Persistence — V28 `payments.payment` + R2DBC ITs 6/6 |
| 2026-07-12 | **22.3** | Payment Domain Foundation — aggregate + VOs + 20 domain tests (ADR-018) |
| 2026-07-12 | **22.2** | Payments Reference Readiness — `InvoiceReferencePort.existsIssuedByIdAndTenant` suficiente; sin evolución |
| 2026-07-12 | **22.1** | ADR-018 Accepted — Payment model frozen (*intentionally small*) |
| 2026-07-12 | **22.0.1** | Payment Aggregate Audit — registro de liquidación; prep. ADR-018 |
| 2026-07-12 | **22.0** | Payments Foundation Planning — BC Payments · primer root `Payment` |
| 2026-07-12 | **21.8** | **BILLING INVOICE SLICE COMPLETE** — InvoiceReferencePort · consumption guide · FASE 21 ✅ |
| 2026-07-12 | **21.7** | Invoice Verification — E2E 8/8 (journey, RBAC, cross-tenant, referencias inválidas, duplicado, OpenAPI, 401) |
| 2026-07-12 | **21.6** | Invoice Administration API — `/api/v1/billing/invoices` + multi-ReferencePort (Org/Patient/Item/Encounter) · unit 14/14 |
| 2026-07-12 | **21.5.1** | Invoice Admin API Audit — HTTP shape `/api/v1/billing/invoices`; lifecycle propio (no soft-entity) |
| 2026-07-12 | **21.5** | Invoice Authorization Contract — `invoice:*` + V27 + RBAC matrix (ALL 44→49) |
| 2026-07-12 | **21.4** | Invoice Persistence — V26 `billing.invoice`+`invoice_line` + R2DBC ITs 10/10 |
| 2026-07-12 | **21.3** | Invoice Domain Foundation — aggregate + `InvoiceLine` + VOs + 38 domain tests (ADR-017) |
| 2026-07-12 | **21.2** | Billing Reference Readiness — Org/Patient/Item/Encounter ports suficientes; sin evolución |
| 2026-07-12 | **21.1** | ADR-017 Accepted — Invoice model frozen (*intentionally small*) |
| 2026-07-12 | **21.0.1** | Invoice Aggregate Audit — bill-to Patient\|Org; DRAFT/ISSUED/VOIDED; prep. ADR-017 |
| 2026-07-12 | **21.0** | Billing Foundation Planning — BC Billing · primer root `Invoice` (≠ Subscriptions) |
| 2026-07-12 | **20.8** | **INVENTORY ITEM SLICE COMPLETE** — ItemReferencePort · consumption guide · FASE 20 ✅ |
| 2026-07-12 | **20.7** | Item Verification — E2E 8/8 + Core validation |
| 2026-07-12 | **20.6** | Item Administration API — `/api/v1/inventory/items` + unit 3/3 · IT 6/6 |
| 2026-07-12 | **20.5.1** | Item Admin API Audit — HTTP shape `/api/v1/inventory/items` |
| 2026-07-12 | **20.5** | Item Authorization Contract — `item:*` + V25 + RBAC matrix (ALL 44) |
| 2026-07-12 | **20.4** | Item Persistence — V24 inventory.item + R2DBC ITs 12/12 |
| 2026-07-12 | **20.3** | Item Domain Foundation — aggregate + VOs + 27 domain tests (ADR-016) |
| 2026-07-12 | **20.2** | Inventory Reference Readiness — `OrganizationReferencePort` suficiente; sin evolución |
| 2026-07-12 | **20.1** | ADR-016 Accepted — Item model frozen (*intentionally small*) |
| 2026-07-12 | **20.0.1** | Item Aggregate Audit — identidad inventariable; tenant-scoped; prep. ADR-016 |
| 2026-07-12 | **20.0** | Inventory Foundation Planning — BC Inventory · primer root `Item` |
| 2026-07-11 | **19.8** | **CLINICAL RECORDS COMPLETE** — EncounterReferencePort · consumption guide · FASE 19 ✅ |
| 2026-07-11 | **19.7** | Encounter Verification — E2E 8/8 + Core validation |
| 2026-07-11 | **19.6** | Encounter Administration API — `/api/v1/records/encounters` + multi-ReferencePort |
| 2026-07-11 | **19.5.1** | Encounter Admin API Audit — HTTP shape `/api/v1/records/encounters` |
| 2026-07-11 | **19.5** | Encounter Authorization Contract — `encounter:*` + V23 + RBAC matrix |
| 2026-07-11 | **19.4** | Encounter Persistence — V22 records.encounter + R2DBC ITs |
| 2026-07-11 | **19.3** | Encounter Domain Foundation — aggregate + VOs + domain tests (ADR-015) |
| 2026-07-11 | **19.2** | AppointmentReferencePort linkable view — Encounter readiness (ADR-015) |
| 2026-07-11 | **19.1** | ADR-015 Accepted — Encounter model frozen (*intentionally small*) |
| 2026-07-11 | **19.0.1** | Encounter Aggregate Audit — episodio ocurrido; prep. ADR-015 |
| 2026-07-11 | **19.0** | Clinical Records Foundation Planning — BC Clinical Records · primer root `Encounter` |
| 2026-07-11 | **18.8** | **SCHEDULING COMPLETE** — AppointmentReferencePort · consumption guide · FASE 18 ✅ |
| 2026-07-11 | **18.7** | Appointment Verification — E2E 8/8 + Core validation |
| 2026-07-11 | **18.6** | Appointment Administration API — scheduling HTTP + multi-ReferencePort |
| 2026-07-11 | **18.5.1** | Appointment Admin API Audit — espejo Patient; listo para 18.6 |
| 2026-07-11 | **18.5** | Appointment Authorization Contract — `appointment:*` + V21 + RBAC matrix |
| 2026-07-11 | **18.4** | Appointment Persistence — V20 scheduling.appointment + R2DBC |
| 2026-07-11 | **18.3** | Appointment Domain Foundation — aggregate + 20 tests (ADR-014) |
| 2026-07-11 | **18.2** | Org ReferencePorts complete — Office + StaffAssignment adapters (ADR-013) |
| 2026-07-11 | **18.1** | ADR-014 Accepted — Appointment model frozen (*intentionally small*) |
| 2026-07-11 | **18.0.1** | Appointment Aggregate Audit — compromiso planificado; prep. ADR-014 |
| 2026-07-11 | **18.0** | Scheduling Foundation Planning — FASE 18 definida |
| 2026-07-11 | **17.9** | Core Architecture Review — listo para FASE 18 (score 8.2) |
| 2026-07-11 | **17.8** | Clinical Foundation Closeout — FASE 17 ✅ · PatientReferencePort · consumption guide |
| 2026-07-11 | **17.7** | Patient Verification — E2E 8/8 + Core validation; BC listo para consumo |
| 2026-07-11 | **17.6** | Patient Administration API — `/api/v1/clinical/patients` espejo Org; ITs verdes |
| 2026-07-11 | **17.5.1** | Patient Admin API Audit — espejo Org; listo para 17.6 |
| 2026-07-11 | **17.5** | Patient Authorization Contract — `patient:*` + V19 + RBAC matrix |
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
