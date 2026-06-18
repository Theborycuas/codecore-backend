# PASO 15.9.1 — IAM Production Readiness Audit

**Fecha:** 2026-06-17  
**Alcance:** Auditoría técnica profunda post-cierre FASE 15 (15.0 → 15.9).  
**Método:** Revisión de código, repositorios, filtros, tests y contrato OpenAPI — **no** asunciones desde documentación sola.  
**Restricción:** Sin implementación de código en este paso.

**Fuentes revisadas:** ADR-006, ADR-007, ADR-008, ROADMAP, PASO-14.9, PASO-15.0–15.9, PASO-15.0.1, Flyway V1–V13, módulo `identity-access-management`.

---

## 1. Resumen ejecutivo

| Veredicto global | **READY WITH MAJOR DEBT** |
|------------------|---------------------------|
| FASE 16 (Organizations) — desarrollo | **SÍ** (con condiciones) |
| Producción multi-tenant sin intervención ops | **NO** |
| Base IAM para módulos de negocio | **SÍ** (admin API + RBAC operativos) |

La FASE 15 entregó una **capa administrativa HTTP funcional y verificada E2E** (`IamAdministrationVerificationIT`). El aislamiento cross-tenant por ID y el RBAC en admin están **bien implementados**. Existen **huecos operativos y de lifecycle** que no bloquean el diseño de Organizations pero **sí bloquean** un despliegue greenfield autónomo y varios escenarios de producción.

---

## 2. Tabla final de readiness

| Área | Estado | Severidad |
|------|--------|-----------|
| Bootstrap | **NOT READY** | **Crítica** (ops) |
| Identity lifecycle | **READY WITH DEBT** | **Mayor** |
| Membership lifecycle | **READY WITH MINOR DEBT** | Menor |
| Tenant suspension | **NOT READY** | **Mayor** |
| Password recovery | **NOT READY** | **Mayor** |
| Audit trail | **NOT READY** | **Mayor** (prod) / Menor (dev) |
| Ownership | **READY** | — |
| Cross-tenant security | **READY** | — |
| OpenAPI | **READY WITH MINOR DEBT** | Menor |

---

## 3. Pregunta 1 — Bootstrap del primer tenant

### Estado actual (código)

| Endpoint | Auth | Qué hace |
|----------|------|----------|
| `POST /api/v1/tenants` | **JWT requerido** (15.7) | Crea `Tenant` ACTIVE + provisiona 5 roles system vía `TenantSystemRolesProvisioner` |
| `POST /api/v1/identities` | **JWT requerido** (15.7) | Crea `Identity` PENDING_VERIFICATION + `IdentityTenantMembership` ACTIVE — **sin roles** |
| `POST /api/v1/auth/login` | Público | Requiere identity ACTIVE + membership ACTIVE en `X-Tenant-Id` |

**Evidencia seguridad:** `PublicApiPaths.java` (solo health, login, swagger); `JwtAuthenticationWebFilter` → 401 sin Bearer; `IamAdministrationVerificationIT.verification5`.

### ¿Cómo nace el primer tenant?

1. **Flyway V1–V12:** schema.
2. **Flyway V13:** solo catálogo global de 16 permisos — **sin tenant, sin usuario, sin asignaciones**.
3. **Aplicación:** `CreateTenantUseCaseImpl` → tenant + roles system (OWNER, ADMIN, …) como **definiciones de rol**, no usuarios.

**No existe** seed SQL, `ApplicationRunner`, ni script ops en el repositorio para el primer tenant.

### ¿Cómo nace el primer OWNER?

**No automáticamente.** `TenantSystemRolesProvisioner` crea el rol OWNER con todos los permisos, pero **nadie recibe ese rol** hasta una asignación manual en `membership_role`.

Los tests (p. ej. `IamAdministrationVerificationIT`) hacen:

1. `CreateTenantUseCase` (in-process, no HTTP)
2. `identityRepository` + `membershipRepository` (identity ACTIVE)
3. `membershipRoleRepository.assign` → rol OWNER
4. `POST /auth/login` → JWT

### ¿Cómo se instala CodeCore en entorno nuevo?

| Paso | Disponible vía HTTP público | Disponible hoy |
|------|----------------------------|----------------|
| 1. Crear tenant | ❌ (401 sin JWT) | Use case interno / SQL / futuro tooling ops |
| 2. Crear usuario ACTIVE | ❌ | Admin API con JWT o repositorio directo |
| 3. Asignar rol OWNER | ❌ | `PUT /memberships/{id}/roles` requiere JWT + `membership:update` |
| 4. Login | ✅ | Solo si 1–3 ya ocurrieron |

**Documentado en PASO-15.7:** *"provisioning interno de tenant + POST /api/v1/iam/users"* — **no implementado como herramienta ejecutable**.

### ¿Dependencia circular?

**Sí.**

```text
POST /tenants, POST /identities  →  requieren JWT
JWT admin                         →  requiere login + roles
Login                             →  requiere tenant + identity ACTIVE + membership ACTIVE + roles (para admin)
Primer OWNER                      →  requiere asignación manual membership_role
```

### ¿Puede desplegarse instancia vacía?

- **Arranque de aplicación:** SÍ (Flyway, health OK).
- **Operable sin intervención:** **NO** — no hay bootstrap HTTP ni automatización in-repo.

### Clasificación

| Ítem | Clasificación |
|------|---------------|
| Bootstrap funcional end-to-end | **Riesgo crítico** (operacional) |
| Dependencia circular post-15.7 | **Deuda mayor** |
| Endpoints legacy sin `@RequiresPermission` | **Deuda menor** (cualquier JWT autenticado puede llamarlos) |

### Flujo exacto documentado (único viable hoy)

```text
[Ops / Test harness — fuera de HTTP público]
  CreateTenantUseCase.execute(name)
    → iam.tenant INSERT
    → TenantSystemRolesProvisioner → 5 roles + role_permission seeds
  Identity + Membership INSERT (ACTIVE)
  membership_role INSERT (OWNER)
[HTTP público]
  POST /api/v1/auth/login + X-Tenant-Id
    → JWT
[HTTP admin]
  POST /api/v1/iam/users, … (resto del journey 15.9)
```

---

## 4. Pregunta 2 — Identity Global Lifecycle

### Implementación real

`DELETE /api/v1/iam/users/{id}` → `UserAdministrationUseCaseImpl.deactivate()` → `Identity.disable()` → status global `DISABLED`.

**No** invoca `MembershipRepository`. **No** desactiva memberships en ningún tenant.

### Escenario multi-tenant

```text
Identity (global, email único)
├── Membership Tenant A (puede quedar ACTIVE)
├── Membership Tenant B (puede quedar ACTIVE)
└── Membership Tenant C (puede quedar ACTIVE)
```

| Pregunta | Respuesta (evidencia) |
|----------|----------------------|
| ¿Qué pasa con memberships al DELETE user? | **Sin cambio** — filas `ACTIVE` persisten |
| ¿Qué pasa con otros tenants? | **Login bloqueado globalmente** (`AuthenticateIdentityUseCaseImpl` exige identity ACTIVE) aunque membership siga ACTIVE |
| ¿ADMIN Tenant A afecta Tenant B? | **Sí, indirectamente** — mutación global de identity (email, status, disable) afecta login en todos los tenants; el guard admin solo exige membership en tenant del JWT para **encontrar** al usuario |
| ¿Riesgo multi-tenant? | **Deuda mayor** — semántica "delete user" = disable global, no "quitar del tenant" |
| ¿Semántica correcta SaaS? | **Parcialmente** — coherente con ADR-006 (identity global), **confuso** para operadores que esperan offboarding por tenant |

### Guard admin (Tenant A)

`loadIdentityInTenant`: `membershipRepository.exists(identityId, ctx.tenantId())` → 404 si no hay membership en tenant del actor. **No impide** disable global si el usuario existe en ambos tenants.

### Clasificación

| Aspecto | Clasificación |
|---------|---------------|
| Aislamiento de lectura cross-tenant | **Correcto** |
| Disable sin cascade membership | **Deuda mayor** (inconsistencia datos vs auth) |
| PUT user (email/status) global | **Deuda menor** (documentado ADR-006) |
| JWT tras disable (sin re-validar identity) | **Deuda menor** (token válido hasta expiry si membership ACTIVE) |

---

## 5. Pregunta 3 — Membership Lifecycle

### Estados

`ACTIVE` | `INACTIVE` — dominio `IdentityTenantMembership.activate()` / `deactivate()`.

### Implementación

| Operación | Efecto |
|-----------|--------|
| `DELETE /memberships/{id}` | `membership.deactivate()` — solo ese tenant |
| `PUT /memberships/{id}` status INACTIVE/ACTIVE | `applyStatus()` — reactivación soportada |
| Ownership | `OwnershipPolicy` en PUT y DELETE |

### Escenarios

| Escenario | Comportamiento |
|-----------|----------------|
| Identity sin memberships | Login falla — `IdentityNotMemberOfTenantException` (403) |
| Todas memberships INACTIVE | Login falla en todos los tenants — membership no ACTIVE |
| Reactivar membership (PUT ACTIVE) | **Funciona** si identity sigue ACTIVE |
| Reactivar con identity DISABLED | Membership ACTIVE pero **login sigue bloqueado** (identity no ACTIVE) |
| Identity sin roles asignados | Login OK si ACTIVE; **admin denegado** (sin permisos RBAC) — verificado 14.9 |

### Comportamiento inesperado

- Tras `DELETE /users/{id}`, memberships **ACTIVE huérfanas** en BD — estado inconsistente semántico (membership dice activo, identity dice disabled).
- Roles en `membership_role` **no se revocan** al desactivar membership.

### Clasificación

**READY WITH MINOR DEBT** — lifecycle tenant-scoped correcto; inconsistencia con identity global disable.

---

## 6. Pregunta 4 — Tenant Suspension Semantics

### Estados en dominio

`TenantStatus`: ACTIVE, SUSPENDED, DISABLED — mutables vía `PUT /api/v1/iam/tenants/current` (`TenantAdministrationUseCaseImpl`).

### ¿Dónde se enforced?

| Capa | ¿Lee TenantStatus? |
|------|-------------------|
| `AuthenticateIdentityUseCaseImpl` | **NO** |
| `JwtAuthenticationWebFilter` | **NO** |
| `ReactorAuthorizationContextAccessor` | **NO** |
| `AuthorizationContextWebFilter` | **NO** |
| `RequiresPermissionAspect` | **NO** |

**Evidencia:** búsqueda en codebase — `TenantStatus` solo en admin tenant y tests.

### Semántica real actual

| Pregunta | Respuesta |
|----------|-----------|
| ¿Puede iniciar sesión con tenant SUSPENDED? | **SÍ** (si identity + membership ACTIVE) |
| ¿Puede usar endpoints admin? | **SÍ** |
| ¿Puede ejecutar autorización RBAC? | **SÍ** |
| ¿Puede OWNER reactivar tenant? | **SÍ** — `PUT /tenants/current` con `status: ACTIVE` (`tenant:update`) |
| ¿Puede ADMIN reactivar? | **NO** — ADMIN no tiene `tenant:update` en `SystemRoleTemplate` |
| ¿Riesgo lockout? | **SÍ** — si OWNER pierde acceso, solo ops/DB puede reactivar; SUSPENDED no bloquea nada hoy |

### Clasificación

**NOT READY** — campo persistido sin efecto en runtime. **Deuda mayor** para producción SaaS (billing, compliance, offboarding).

---

## 7. Pregunta 5 — Password Recovery

### Verificación implementación

| Componente | Existe |
|------------|--------|
| Dominio `PasswordResetRequest` | ✅ |
| Puertos `RequestPasswordResetUseCase`, `CompletePasswordResetUseCase` | ✅ (solo interfaces) |
| `*UseCaseImpl` | ❌ |
| HTTP `/auth/forgot-password`, `/auth/reset-password` | ❌ |
| Tabla Flyway `password_reset_request` | ❌ |
| `PasswordResetRepository` | ❌ |

**Implementado parcialmente:** admin puede poner `PASSWORD_RESET_REQUIRED` vía `PUT /users/{id}` → bloquea login; **no hay flujo** para completar el reset y volver a ACTIVE.

### Clasificación

**NOT READY** — **Prioridad alta** pre-producción (usuarios finales).

---

## 8. Pregunta 6 — Audit Trail

### Verificación

Búsqueda en use cases admin (`*AdministrationUseCaseImpl`): **sin** logging estructurado de auditoría, **sin** tabla append-only, **sin** eventos de dominio persistidos.

| Operación | Auditoría |
|-----------|-----------|
| Creación/actualización/disable usuario | ❌ |
| Membership CRUD | ❌ |
| Cambios roles / permisos | ❌ |
| Cambios tenant | ❌ |

`FailedLoginAttempt` / `LoginAttemptTracker` existen en dominio pero **no están cableados** a `AuthenticateIdentityUseCaseImpl`.

**ROADMAP:** FASE 21 Audit & Observability.

### Clasificación

**NOT READY** para producción regulada — **Prioridad media-alta** (puede paralelizarse con FASE 16 en dev).

---

## 9. Pregunta 7 — Cross-Tenant Security Review

### Metodología

Revisión de `load*InTenant`, `findByIdAndTenantId`, filtros SQL en `R2dbc*AdminQueryRepository`, y tests IT cross-tenant.

### Resultado por recurso

| Recurso | Filtro tenant | Cross-tenant por ID | Evidencia |
|---------|---------------|---------------------|-----------|
| Users | `exists(identity, tenant)` / query JOIN membership | **404** | `IamUserAdminControllerIT`, verification3 |
| Memberships | `findByIdAndTenantId` | **404** | `IamMembershipAdminControllerIT` |
| Roles | `loadRoleInTenant` | **404** | `IamRoleAdminControllerIT` |
| Role permissions | `loadRoleInTenant` primero | **404** | `IamRolePermissionAdminControllerIT` |
| Membership roles | `findByIdAndTenantId` + `validateRolesInTenant` | **404** | `IamMembershipRoleAdminControllerIT` |
| Tenant | Solo `ctx.tenantId()` | N/A (solo current) | `TenantAdministrationUseCaseImpl` |
| Permissions | Catálogo **global** intencional | N/A | ADR-007 / V13 |

### Efectos cross-tenant (no IDOR)

1. **User PUT/DELETE** — muta identity global (email, disable) → impacto en todos los tenants donde exista la identity.
2. **Membership POST por email** — vincula identity global existente al tenant actual (ADR-006, by design).

### ¿Alguna operación modifica recursos de otro tenant por ID?

**NO** — verificado en código y tests.

### Clasificación

**READY** — cross-tenant IDOR: **PASS**. Efectos globales identity: **deuda documentada**.

---

## 10. Pregunta 8 — Ownership Rules Verification

### Implementación (`OwnershipPolicy.java`)

Matriz 15.0.1 codificada en `canModify()`: OWNER→todos; ADMIN→≤MANAGER; MANAGER→≤USER.

### Dónde se aplica (código real)

| Área | OwnershipPolicy |
|------|-----------------|
| User PUT, DELETE | ✅ |
| Membership PUT, DELETE | ✅ (vía identityId del membership) |
| MembershipRole PUT | ✅ |
| User CREATE | ❌ (diseño 15.0.1) |
| Role admin | ❌ (solo RBAC + system role guard) |
| Role assignment (role permissions) | ❌ |

### Tests de ownership

- `IamUserAdminControllerIT.shouldReturn403WhenAdminTriesToModifyOwner`
- `IamMembershipRoleAdminControllerIT` — ADMIN → OWNER 403
- `IamAdministrationVerificationIT.verification4`

### Inconsistencias detectadas

| Ítem | Severidad |
|------|-----------|
| Ownership solo en mutaciones "user-targeting" | **Correcto** per 15.0.1 |
| Role admin sin jerarquía (ADMIN puede borrar roles custom de otro ADMIN) | **Menor** — mitigado por RBAC `role:delete` |
| Documentación vs código | **Alineado** |

### Clasificación

**READY**

---

## 11. Pregunta 9 — OpenAPI Audit

### Cobertura endpoints admin

`IamAdministrationOpenApiTest` verifica **12 path patterns** en grupo `iam-administration` — alineados con controllers admin.

| Incluido en OpenAPI | Excluido (correcto) |
|---------------------|---------------------|
| `/api/v1/iam/**` (22 ops aprox.) | `/api/v1/auth/*` |
| | `POST /tenants`, `/identities` (legacy, sin grupo) |

### Permisos visibles

`RequiresPermissionOperationCustomizer` (`GlobalOperationCustomizer`) → extensión `x-permission` + descripción. **Verificado** en test.

### Gaps OpenAPI

| Gap | Severidad |
|-----|-----------|
| Sin `@ApiResponse` explícitos — 403/404/409 inferidos parcialmente | Menor |
| Sin ejemplos de request/response en anotaciones | Menor |
| Swagger deshabilitado en prod | Correcto |
| Auth/login no documentado en contrato IAM | Menor (grupo separado futuro) |
| DTOs generados desde records — **correctos** en runtime | OK |

### Clasificación

**READY WITH MINOR DEBT** — contrato útil para dev/integración; no sustituye runbook ops.

---

## 12. Pregunta 10 — IAM Readiness global

### Fortalezas (evidencia)

1. **RBAC HTTP completo** — 8 controllers admin, `@RequiresPermission` en cada método.
2. **Cadena auth verificada** — 14.9 + 15.9 E2E.
3. **Cross-tenant IDOR** — sin fugas por ID en recursos tenant-scoped.
4. **Ownership** — implementado donde 15.0.1 lo exige; tests pasan.
5. **OpenAPI** — grupo `iam-administration` con permisos.
6. **Hexagonal / ADR** — sin violaciones estructurales en FASE 15.
7. **System roles + V13 seeds** — provisioning por tenant operativo.

### Debilidades (evidencia)

1. **Bootstrap greenfield** — imposible sin ops (crítico operacional).
2. **Tenant status** — sin enforcement (mayor).
3. **Password recovery** — no implementado (mayor).
4. **Audit trail** — ausente (mayor prod).
5. **Identity disable** — sin cascade membership; semántica confusa (mayor).
6. **JWT stale post-disable** — identity status no revalidada (menor).
7. **ADR-008** — bootstrap aún documentado como público (doc drift).

### Veredicto

**READY WITH MAJOR DEBT**

- **Listo para:** desarrollo FASE 16+, integraciones internas, demos con seed programático.
- **No listo para:** despliegue producción autónomo, SaaS self-service, compliance audit fuerte.

---

## 13. Recommended Fixes Before Phase 16

Priorizadas para **no bloquear diseño** de Organizations pero **sí** cerrar antes de producción o piloto externo.

| # | Fix | Prioridad | Impacto | Riesgo si no se hace | Esfuerzo |
|---|-----|-----------|---------|----------------------|----------|
| 1 | **Bootstrap greenfield** — CLI/seed/endpoint break-glass (env-gated) que cree tenant + OWNER inicial | **P0** | Despliegue | Instancia inutilizable sin ops manual | M |
| 2 | **Enforcement TenantStatus** en login y/o `AuthorizationContextWebFilter` | **P1** | Suspensión tenant | Tenant "suspendido" sigue operativo | S–M |
| 3 | **Password reset** — implementar use cases + HTTP + migración | **P1** | UX producción | Usuarios bloqueados sin recuperación | L |
| 4 | **Documentar/clarificar semántica User DELETE** — o cascade membership INACTIVE, o renombrar API a "disable identity" | **P1** | Multi-tenant | Offboarding incorrecto cross-tenant | S |
| 5 | **Revalidar identity status** en pipeline JWT (o TTL corto + refresh) | **P2** | Seguridad | Usuario deshabilitado sigue con token | S |
| 6 | **Audit mínimo admin** — eventos estructurados o tabla `iam_audit_log` | **P2** | Compliance | Sin trazabilidad | M–L |
| 7 | **Proteger bootstrap legacy** — `@RequiresPermission` o eliminar endpoints HTTP | **P2** | Seguridad | Cualquier JWT crea tenants | S |
| 8 | **Actualizar ADR-008** — bootstrap ya no público | **P3** | Docs | Confusión operadores | XS |
| 9 | **IT drift** — `CreateTenantControllerIT` sin security vs prod | **P3** | Tests | Falsa confianza | S |
| 10 | **OpenAPI auth group** — documentar login/me | **P3** | DX | Integración manual | S |

---

## 14. ¿Qué tenemos hoy (inventario verificado)

### API administrativa (`/api/v1/iam/**`)

- Foundation, Users, Memberships, Membership Roles, Roles, Role Permissions, Permissions, Tenant current.
- Paginación en listados; PUT replace en asignaciones.
- 22 operaciones aproximadamente; contrato OpenAPI `iam-administration`.

### Seguridad

- JWT Bearer; tenant desde claim; `AuthorizationContext` con membership ACTIVE.
- RBAC deny-by-default; ownership en mutaciones de usuario.
- Bootstrap legacy endurecido (401 sin JWT).

### Datos

- Flyway V1–V13; permisos globales; roles por tenant al crear tenant.
- Identity global + membership N:M (ADR-006).

### Tests

- IT por recurso (15.1–15.7); OpenAPI contract; `IamAdministrationVerificationIT` (8 checks).

### Herramientas dev

- Swagger UI (`/swagger-ui.html`); export OpenAPI JSON.

---

## 15. ¿Qué podemos hacer hoy?

| Capacidad | ¿Posible? | Condición |
|-----------|-----------|-----------|
| Administrar tenant con OWNER (post-seed) | ✅ | Seed programático previo |
| CRUD usuarios/memberships/roles | ✅ | JWT + permisos |
| Asignar RBAC (roles ↔ permissions, membership ↔ roles) | ✅ | |
| Aislar tenants en admin API | ✅ | |
| Onboarding HTTP desde cero | ❌ | Requiere fix P0 |
| Suspender tenant efectivamente | ❌ | Solo metadata |
| Recuperación de contraseña self-service | ❌ | |
| Auditoría forense de cambios admin | ❌ | |
| Construir dominio Organizations encima | ✅ | Usar admin API + tenantId JWT |

---

## 16. Decisión final — ¿Puede iniciarse FASE 16?

### Respuesta: **SÍ** (con condiciones)

**Justificación con evidencia:**

| Criterio FASE 16 | Estado |
|------------------|--------|
| API admin estable para tenant/membership/roles | ✅ 15.9 E2E |
| RBAC consumible desde nuevos módulos | ✅ ADR-007 sin cambios |
| Aislamiento tenant en operaciones IAM | ✅ verificación código + IT |
| ADR-006 identity/membership respetado | ✅ |
| Huecos que **bloquean diseño** de Organizations | **Ninguno identificado** |

**Condiciones (no bloqueantes para empezar a codear Organizations, sí para piloto/prod):**

1. Aceptar **bootstrap por ops** hasta implementar P0.
2. No asumir **tenant suspendido** como enforcement real.
3. Planificar P1–P2 en paralelo o antes de release candidato.

**NO** se recomienda declarar IAM **production-ready** sin cerrar al menos P0, P1 (tenant enforcement + password reset o alternativa ops), y clarificación P1 identity disable.

---

## 17. Contraste documentación vs código

| Documento | Dice | Código real | Delta |
|-----------|------|-------------|-------|
| ADR-008 | Bootstrap público hasta 15.7 | 15.7 cerró — JWT requerido | Doc desactualizado |
| PASO-15.1 | DELETE user = soft disable global | Coincide | OK |
| PASO-15.0.1 | Ownership en mutaciones user | Coincide | OK |
| PASO-15.7 | Onboarding vía provisioning interno | No hay herramienta | Gap ops |
| ROADMAP FASE 15 cerrada | E2E admin OK | `IamAdministrationVerificationIT` PASS | OK |
| Tenant SUSPENDED | Estados en API | Sin enforcement auth | **Gap crítico semántico** |

---

## 18. Referencias de código clave

| Tema | Archivo |
|------|---------|
| Bootstrap tenant | `CreateTenantUseCaseImpl.java` |
| Bootstrap identity | `RegisterIdentityUseCaseImpl.java` |
| Public paths | `PublicApiPaths.java` |
| User disable | `UserAdministrationUseCaseImpl.java`, `Identity.disable()` |
| Membership lifecycle | `MembershipAdministrationUseCaseImpl.java` |
| Tenant admin | `TenantAdministrationUseCaseImpl.java` |
| Login gates | `AuthenticateIdentityUseCaseImpl.java` |
| Auth context | `ReactorAuthorizationContextAccessor.java` |
| Ownership | `OwnershipPolicy.java` |
| System roles | `SystemRoleTemplate.java`, `TenantSystemRolesProvisionerImpl.java` |
| E2E cierre | `IamAdministrationVerificationIT.java` |
| OpenAPI | `IamOpenApiConfiguration.java` |
| Seeds | `V13__seed_authorization_foundation.sql` |

---

## 19. Próximo paso sugerido

1. **FASE 16 — Organizations** puede iniciarse en paralelo al fix **P0 bootstrap**.
2. Crear **PASO-15.9.2** o ítems en backlog para P0–P2 antes de cualquier piloto externo.
3. No requiere nuevo ADR salvo que Organizations altere tenancy estructuralmente.

---

**Auditoría completada sin modificación de código.**
