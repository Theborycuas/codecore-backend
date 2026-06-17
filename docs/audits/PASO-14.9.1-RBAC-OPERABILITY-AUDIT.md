# PASO 14.9.1 — RBAC Operability Audit

**Fecha:** 2026-05-27  
**Alcance:** Auditoría funcional post-FASE 14 — qué está operable vía API vs. qué existe solo en dominio/aplicación.  
**Restricción:** Solo análisis y evidencia. Sin cambios de código.

---

## 1. Resumen ejecutivo

La FASE 14 entregó **infraestructura RBAC completa** (dominio, persistencia, evaluación runtime, seeds, verificación en tests). Sin embargo, **no existe una capa de administración IAM expuesta por HTTP** para usuarios, memberships, roles, permisos ni asignaciones.

| Dimensión | Estado |
|-----------|--------|
| Infraestructura RBAC (dominio + persistencia + `AuthorizationService`) | **Operativa** |
| Seeds + provisioning de roles system al crear tenant | **Operativo** (vía `CreateTenantUseCase`) |
| API de autenticación básica (login, me) | **Operativa** |
| API administrativa RBAC (`user:*`, `role:*`, `membership:*`) | **No existe** |
| Endpoints productivos con `@RequiresPermission` | **No existe** (solo tests) |
| OpenAPI | **No existe** |

### Conclusión anticipada

**CodeCore posee únicamente la infraestructura RBAC** (motor + contrato de permisos + seeds), **no** una plataforma IAM administrable de extremo a extremo mediante API.

Los permisos sembrados (`user:create`, `role:read`, etc.) definen el **contrato futuro**; ningún endpoint HTTP productivo los consume aún.

---

## 2. Nota terminológica

El dominio usa **`Identity`** (aggregate `Identity`, tabla `iam.iam_user`). El catálogo de permisos IAM (V13) usa el recurso **`user:*`**. En esta auditoría, «User» se mapea a **`Identity`** salvo que se indique lo contrario.

---

## 3. Gestión de usuarios (Identity)

| Capacidad | Domain | Use Case | HTTP API | OpenAPI | Evidencia |
|-----------|--------|----------|----------|---------|-----------|
| **Create User** | PARCIAL | YES | YES | NO | `RegisterIdentityUseCaseImpl` + `RegisterIdentityController` (`POST /api/v1/identities`) crea `Identity` + `IdentityTenantMembership` en una transacción. No existe `CreateUserUseCase` ni recurso `/users`. |
| **Update User** | PARCIAL | NO | NO | NO | `Identity` expone mutaciones puntuales (`enable`, `disable`, `lockAccount`, `unlockAccount`, `markEmailVerified`, `requirePasswordReset`, `attachCredential`, `markLastLogin`) — no un «update user» genérico. Sin use case ni controller. |
| **Delete User** | PARCIAL | NO | NO | NO | `IdentityRepository.delete(TenantId, IdentityId)` en puerto out. Sin use case ni HTTP. |
| **List Users** | NO | NO | NO | NO | `IdentityRepository` no expone `findAll` / `findByTenant`. Solo `findById`, `findByEmail`, `findByTenantAndEmail`. |
| **Get User** | PARCIAL | NO | NO | NO | `IdentityRepository.findById`, `findByEmail` — solo persistencia. `GET /api/v1/auth/me` devuelve claims del JWT, no un recurso user por id. |

### Clases exactas

| Capa | Clases |
|------|--------|
| Domain | `com.codecore.iam.domain.model.identity.Identity` |
| Use Case | `RegisterIdentityUseCase` / `RegisterIdentityUseCaseImpl` |
| HTTP | `RegisterIdentityController`, `AuthenticationController` (`GET /api/v1/auth/me` → `MeResponse`) |
| Persistence | `IdentityRepository`, `R2dbcIdentityRepository` |
| OpenAPI | No encontrado (sin `springdoc`, `swagger`, ni specs en repo) |

### Use cases declarados pero NO implementados (relacionados)

Interfaces en `application.port.in` sin `*UseCaseImpl` ni wiring en configuración:

- `ChangePasswordUseCase`
- `LogoutUseCase`
- `RefreshAccessTokenUseCase`
- `RequestPasswordResetUseCase`
- `CompletePasswordResetUseCase`
- `RevokeSessionUseCase`
- `AuthenticateUseCase` (duplicado conceptual de `AuthenticateIdentityUseCase`)

---

## 4. Gestión de memberships

| Capacidad | Domain | Use Case | HTTP API | OpenAPI | Evidencia |
|-----------|--------|----------|----------|---------|-----------|
| **Create Membership** | YES | PARCIAL | NO | NO | `IdentityTenantMembership.create()` en dominio. Creación automática en `RegisterIdentityUseCaseImpl` (líneas 104–109). No hay `CreateMembershipUseCase` ni endpoint dedicado. |
| **Update Membership** | PARCIAL | NO | NO | NO | `activate()` / `deactivate()` en `IdentityTenantMembership`. Sin use case/API. |
| **Deactivate Membership** | YES | NO | NO | NO | `IdentityTenantMembership.deactivate()` |
| **List Memberships** | PARCIAL | NO | NO | NO | `MembershipRepository.findByTenantId`, `findByIdentityId` — solo puerto out. |
| **Get Membership** | PARCIAL | NO | NO | NO | `findActiveByIdentityIdAndTenantId` — sin `findById(MembershipId)` en puerto. |

### Clases exactas

| Capa | Clases |
|------|--------|
| Domain | `IdentityTenantMembership`, `MembershipRoleAssignment` |
| Use Case | Ninguno dedicado; side-effect en `RegisterIdentityUseCaseImpl` |
| HTTP | Ninguno |
| Persistence | `MembershipRepository`, `R2dbcMembershipRepository`, `MembershipRoleRepository` |

---

## 5. Gestión de roles

| Capacidad | Domain | Use Case | HTTP API | OpenAPI | Evidencia |
|-----------|--------|----------|----------|---------|-----------|
| **Create Role** | YES | PARCIAL | NO | NO | `Role.create()`, `Role.createSystemRole()`. Provisioning automático en `TenantSystemRolesProvisionerImpl` al crear tenant. Sin `CreateRoleUseCase` ni API para roles custom. |
| **Update Role** | YES | NO | NO | NO | `Role.rename()`, `activate()`, `deactivate()` — bloqueados si `systemRole=true` (`ensureMutable()`). |
| **Delete Role** | NO | NO | NO | NO | Sin método `delete` en `Role` ni `RoleRepository`. |
| **Activate Role** | YES | NO | NO | NO | `Role.activate()` |
| **Deactivate Role** | YES | NO | NO | NO | `Role.deactivate()` |
| **List Roles** | NO | NO | NO | NO | `RoleRepository` solo: `findById`, `findByTenantIdAndCode`, `existsByTenantIdAndCode`. Sin listado por tenant. |

### Clases exactas

| Capa | Clases |
|------|--------|
| Domain | `Role`, `RolePermissionAssignment` |
| Application | `TenantSystemRolesProvisionerImpl`, `SystemRoleTemplate`, `IamPermissionCatalog` |
| HTTP | Ninguno |
| Persistence | `RoleRepository`, `RolePermissionRepository`, `R2dbcRoleRepository` |

---

## 6. Gestión de permisos

| Capacidad | Domain | Use Case | HTTP API | OpenAPI | Evidencia |
|-----------|--------|----------|----------|---------|-----------|
| **List Permissions** | NO | NO | NO | NO | `PermissionRepository` sin `findAll`. Catálogo fijo en `IamPermissionCatalog` + seeds V13. |
| **Get Permission** | PARCIAL | NO | NO | NO | `PermissionRepository.findByCode`, `findById` — solo persistencia. |

### API pública de permisos

**No existe.** Los 16 permisos IAM se insertan vía `V13__seed_authorization_foundation.sql`; no hay controller ni use case de consulta.

### Clases exactas

| Capa | Clases |
|------|--------|
| Domain | `Permission` |
| Catalog | `IamPermissionCatalog` |
| Seeds | `V13__seed_authorization_foundation.sql` |
| Persistence | `PermissionRepository`, `R2dbcPermissionRepository` |

---

## 7. Asignación de roles a memberships

| Capacidad | Aggregate | Use Case | Controller | Endpoint |
|-----------|-----------|----------|------------|----------|
| **Assign Role To Membership** | `IdentityTenantMembership.assignRole()` | NO | NO | — |
| **Remove Role From Membership** | `IdentityTenantMembership.revokeRole()` | NO | NO | — |
| **List Membership Roles** | — (lectura vía repo) | NO | NO | — |

### Evidencia de capa de persistencia (sin API)

| Componente | Clase |
|------------|-------|
| Domain | `IdentityTenantMembership.assignRole(roleId, roleTenantId, now)`, `revokeRole(roleId)` |
| Puerto out | `MembershipRoleRepository.assign`, `revoke`, `findByMembershipId` |
| Infra | `R2dbcMembershipRoleRepository` |
| Tests | `R2dbcMembershipRoleRepositoryIT`, `AuthorizationFoundationVerificationIT` (asignación programática en test) |

**No existe** ningún `AssignRoleToMembershipUseCase`, `MembershipRoleController` ni ruta HTTP.

---

## 8. Administración RBAC — brecha funcional

### ¿Existe API administrativa hoy?

| Operación administrativa | API | Evidencia |
|--------------------------|-----|-----------|
| Crear roles (custom) | NO | Solo provisioning system en `CreateTenantUseCase` |
| Consultar roles | NO | Sin controller; repo solo `findByTenantIdAndCode` |
| Asignar permisos a roles | NO | `Role.assignPermission()` en dominio + `RolePermissionRepository` — sin use case/API |
| Asignar roles a memberships | NO | `MembershipRoleRepository` — sin use case/API |

### Qué falta exactamente

1. **Use cases de aplicación** para cada permiso del catálogo IAM (`user:*`, `role:*`, `membership:*`, `permission:*`, `tenant:*`).
2. **Controllers HTTP** bajo rutas administrativas (p. ej. `/api/v1/users`, `/api/v1/roles`, `/api/v1/memberships`).
3. **Anotación `@RequiresPermission`** en esos endpoints productivos (hoy la infraestructura existe pero no se aplica en `src/main`).
4. **OpenAPI** — no hay generación ni contrato publicado.
5. **Operaciones de listado** en repositorios (`findAllByTenant`, `Flux<Role>`, `Flux<Permission>`).
6. **Use case de asignación de rol inicial** al registrar usuario (hoy el membership queda sin roles hasta asignación manual vía persistencia/tests).

---

## 9. Provisioning de tenants

### ¿Ocurre el flujo Tenant → 5 roles system?

**Sí**, con evidencia en código.

```text
POST /api/v1/tenants
  → CreateTenantController
  → CreateTenantUseCaseImpl.execute()
      → TenantRepository.save(tenant)
      → TenantSystemRolesProvisioner.provisionForTenant(tenantId)
          → TenantSystemRolesProvisionerImpl
              → por cada SystemRoleTemplate:
                  OWNER | ADMIN | MANAGER | USER | READ_ONLY
                  → Role.createSystemRole(...)
                  → RoleRepository.save(role)
                  → RolePermissionRepository.assign(...)  [vía SQL/repo, no dominio]
```

### Clases involucradas

| Paso | Clase |
|------|-------|
| HTTP | `CreateTenantController` |
| Use case | `CreateTenantUseCaseImpl` |
| Provisioner | `TenantSystemRolesProvisionerImpl` |
| Templates | `SystemRoleTemplate` (enum: OWNER, ADMIN, MANAGER, USER, READ_ONLY) |
| Permisos | `IamPermissionCatalog` + seeds `V13__seed_authorization_foundation.sql` |
| Config | `IamAuthorizationConfiguration` (bean `tenantSystemRolesProvisioner`) |

### Evidencia de tests

- `TenantSystemRolesProvisionerIT` — 5 roles `system_role=true` por tenant, idempotente.
- `CreateTenantControllerIT` — creación HTTP de tenant (sin assert de roles en ese IT; roles verificados en provisioner IT).

### Limitación operativa

El provisioning **no asigna ningún rol a ningún membership**. Solo crea roles vacíos de catálogo por tenant. Un usuario recién registrado **no recibe OWNER ni USER** automáticamente.

---

## 10. Authorization Runtime

### Infraestructura disponible

| Componente | Clase | Estado |
|------------|-------|--------|
| Evaluación | `AuthorizationService` / `AuthorizationServiceImpl` | Implementado |
| Consultas SQL | `R2dbcAuthorizationQueryRepository` | Implementado |
| Contexto | `AuthorizationContext`, `ReactorAuthorizationContextAccessor`, `AuthorizationContextWebFilter` | Implementado |
| HTTP enforcement | `RequiresPermission`, `RequiresPermissionAspect` | Implementado |
| Seguridad base | `JwtAuthenticationWebFilter`, `AuthenticatedPrincipalAuthorizationManager` | Implementado |

### Endpoints productivos con `@RequiresPermission`

**Ninguno** en `src/main/java`.

Búsqueda en código productivo:

```
modules/identity-access-management/src/main → RequiresPermissionAspect.java, RequiresPermission.java
(sin @RequiresPermission en controllers de main)
```

### Endpoints de test con `@RequiresPermission`

| Endpoint | Método | Permiso | Controller | Ubicación |
|----------|--------|---------|------------|-----------|
| `/api/v1/auth/authorization-probe` | GET | `probe:access` | `AuthorizationProbeController` | `src/test/java` |
| `/api/v1/auth/user-create-probe` | GET | `user:create` | `AuthorizationProbeController` | `src/test/java` |

Estos controllers **no se despliegan** en `codecore-api` (solo existen en configuraciones de test IT).

### Seguridad efectiva en producción

`AuthenticatedPrincipalAuthorizationManager` solo verifica que exista `AuthenticatedPrincipal` en Reactor Context (JWT válido). **No evalúa permisos RBAC** en endpoints productivos.

---

## 11. Flujo E2E

### Flujo solicitado

1. Crear Tenant  
2. Crear Usuario  
3. Crear Membership  
4. Asignar Role  
5. Consumir endpoint protegido  

### Clasificación: **PARCIAL**

| Paso | Estado | Evidencia |
|------|--------|-----------|
| 1. Crear Tenant | **COMPLETO** | `POST /api/v1/tenants` → `CreateTenantController` (público, sin JWT) |
| 2. Crear Usuario | **COMPLETO** | `POST /api/v1/identities` → `RegisterIdentityController` (público) |
| 3. Crear Membership | **COMPLETO** (implícito) | Auto-creado en `RegisterIdentityUseCaseImpl`; no hay API separada |
| 4. Asignar Role | **NO SOPORTADO** | Sin API; solo dominio (`assignRole`) + `MembershipRoleRepository` usados en tests |
| 5. Endpoint protegido | **NO SOPORTADO** (prod) | Sin `@RequiresPermission` en controllers productivos; `/me` solo exige JWT |

### Flujo parcialmente viable hoy (solo vía API pública)

```text
POST /api/v1/tenants          → tenant + 5 roles system (sin membership con rol)
POST /api/v1/identities       → identity + membership ACTIVE (sin roles)
POST /api/v1/auth/login       → JWT con tenant claim
GET  /api/v1/auth/me          → 200 con principal (sin chequeo de permiso)
```

### Flujo E2E RBAC completo

Solo demostrado en **`AuthorizationFoundationVerificationIT`** (tests), no reproducible íntegramente por un cliente HTTP contra `codecore-api`.

---

## 12. Inventario de endpoints IAM (productivos)

Solo controllers en `src/main/java` desplegados vía `CodeCoreApiApplication` (`scanBasePackages = com.codecore.iam`).

| Endpoint | Método | Función | Protegido | Permiso RBAC |
|----------|--------|---------|-----------|--------------|
| `/api/v1/tenants` | POST | Crear tenant (+ provision roles system) | No (público) | — |
| `/api/v1/identities` | POST | Registrar identity + membership | No (público) | — |
| `/api/v1/auth/login` | POST | Autenticar, emitir JWT | No (público) | — |
| `/api/v1/auth/me` | GET | Perfil desde JWT | Sí (JWT requerido) | — (solo autenticación, sin `@RequiresPermission`) |
| `/actuator/health` | GET | Health check (platform) | No (público) | — |

**Total endpoints IAM productivos: 4** (+ health platform).

Rutas públicas alineadas en `PublicApiPaths` y `PlatformSecurityAutoConfiguration`.

---

## 13. Matriz de operabilidad consolidada

| Capacidad | Dominio | UseCase | API | Operable |
|-----------|---------|---------|-----|----------|
| Identity (User) — Create | PARCIAL | YES | YES | **PARCIAL** (register only) |
| Identity (User) — Update | PARCIAL | NO | NO | **NO** |
| Identity (User) — Delete | PARCIAL | NO | NO | **NO** |
| Identity (User) — List | NO | NO | NO | **NO** |
| Identity (User) — Get | PARCIAL | NO | PARCIAL | **PARCIAL** (`/auth/me` only) |
| Membership — Create | YES | PARCIAL | NO | **PARCIAL** (via register) |
| Membership — Update/Deactivate | YES | NO | NO | **NO** |
| Membership — List/Get | PARCIAL | NO | NO | **NO** |
| Role — CRUD | PARCIAL | PARCIAL | NO | **NO** (provision only) |
| Permission — List/Get | PARCIAL | NO | NO | **NO** |
| Role ↔ Permission assign | YES | NO | NO | **NO** |
| Membership ↔ Role assign | YES | NO | NO | **NO** |
| Authorization evaluate | YES | YES | NO | **NO** (runtime interno/tests) |
| Authorization HTTP (`@RequiresPermission`) | YES | YES | NO | **NO** (prod) |
| Tenant provisioning (5 roles) | YES | YES | YES | **YES** |
| Authentication (login/me) | YES | YES | YES | **YES** |
| IAM Permission seeds (V13) | YES | N/A | N/A | **YES** (DB) |

---

## 14. Respuesta objetiva final

### ¿Infraestructura RBAC únicamente?

**Sí.** CodeCore dispone de:

- Modelo de dominio completo (Identity → Membership → Role → Permission)
- Persistencia y migraciones (V9–V13)
- Motor `AuthorizationService` con tenant isolation y deny-by-default
- Seeds y provisioning de roles system
- Mecanismo HTTP `@RequiresPermission` (implementado, no usado en producción)
- Verificación E2E en tests (`AuthorizationFoundationVerificationIT`)

### ¿Plataforma IAM administrable mediante API?

**No.** No hay endpoints para administrar usuarios, memberships, roles, permisos ni asignaciones. El catálogo `user:*`, `role:*`, etc. es un **contrato declarado** (V13 + `IamPermissionCatalog`) **sin superficie HTTP** que lo implemente.

### Brecha principal para operabilidad

```
Infraestructura RBAC  ──────────────────────────────►  FASE 14 ✓
        │
        │  (falta)
        ▼
API administrativa IAM  ──────────────────────────────►  FASE 15+ (pendiente)
        │
        ▼
Endpoints de negocio con @RequiresPermission  ───────►  FASE 18+ (pendiente)
```

---

## 15. Referencias de evidencia

| Artefacto | Ruta |
|-----------|------|
| Controllers productivos | `modules/identity-access-management/src/main/java/com/codecore/iam/interfaces/http/` |
| Use cases implementados | `RegisterIdentityUseCaseImpl`, `AuthenticateIdentityUseCaseImpl`, `CreateTenantUseCaseImpl`, `AuthorizationServiceImpl` |
| Provisioner | `TenantSystemRolesProvisionerImpl`, `SystemRoleTemplate` |
| Seeds | `apps/codecore-api/src/main/resources/db/migration/V13__seed_authorization_foundation.sql` |
| Seguridad HTTP | `RequiresPermissionAspect`, `JwtAuthenticationWebFilter`, `AuthenticatedPrincipalAuthorizationManager` |
| Verificación FASE 14 | `AuthorizationFoundationVerificationIT` |
| Probe (solo test) | `src/test/java/.../AuthorizationProbeController.java` |
| Cierre FASE 14 | `docs/audits/PASO-14.9-AUTHORIZATION-VERIFICATION.md` |

---

**Auditoría completada. Sin cambios de código.**
