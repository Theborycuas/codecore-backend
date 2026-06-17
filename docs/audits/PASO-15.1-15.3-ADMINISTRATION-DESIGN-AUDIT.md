# PASO 15.1–15.3 — Administration Design Audit

**Fecha:** 2026-06-17  
**Alcance:** Auditoría funcional conjunta previa a implementación de User, Membership y Role Administration (FASE 15.1 → 15.3).  
**Restricción:** Solo análisis y diseño. **Sin cambios de código** en este paso.

**Fuentes revisadas:** `ROADMAP.md`, ADR-006, ADR-007, ADR-008, `PASO-14.9.1-RBAC-OPERABILITY-AUDIT.md`, `PASO-15.0-IAM-ADMINISTRATION-FOUNDATION.md`, módulo `identity-access-management`.

---

## 1. Resumen ejecutivo

| Área | Estado actual | Trabajo requerido (15.1–15.3) |
|------|---------------|-------------------------------|
| **Users** | Bootstrap `POST /api/v1/identities`; dominio con mutadores de estado; sin listado ni admin HTTP | Use cases + controller + queries membership-anchored + `changeEmail` en dominio |
| **Memberships** | Dominio + `findByTenantId`; sin `findById` en puerto; sin HTTP | Use cases + `findByIdAndTenantId` + controller |
| **Roles** | Dominio + provisioning system; sin listado ni delete; protección `systemRole` en dominio | Use cases + `findByTenantId` + delete custom + controller |

**Conclusión:** La infraestructura FASE 10–14 es **suficiente como base**, pero **no** cubre listados admin, aislamiento membership-first en lecturas de Identity, ni capa HTTP/use case. Se requieren extensiones acotadas en puertos out, nuevos use cases `port.in`, DTOs HTTP y handlers 404 — **sin** tocar `AuthorizationService`, `AuthorizationContext`, JWT ni modelo RBAC.

---

## 2. User Administration (15.1)

### 2.1 Lectura — `GET /api/v1/iam/users` y `GET /api/v1/iam/users/{id}`

#### ¿Existe todo lo necesario hoy?

**No.**

| Capa | Existente | Faltante |
|------|-----------|----------|
| **Aggregate** | `Identity` — `id`, `email` (final), `status`, `credential`, `lastLoginAt`, timestamps | Campo **nombre** no existe (V3 eliminó `first_name`/`last_name`) |
| **Repository** | `findById(TenantId, IdentityId)`, `findByEmail`, `save`, `delete` | `findById(IdentityId)` global; **listado por tenant**; paginación |
| **Queries** | Ninguna query admin | `IdentityAdminQueryRepository` o método en puerto: identities con membership en tenant |
| **DTOs** | `RegisterIdentityResult`, `MeResponse` (JWT) | `UserResponse`, `PagedResponse<UserResponse>`, `UpdateUserRequest` |
| **HTTP** | `RegisterIdentityController` (`/identities`), `AuthenticationController` (`/auth/me`) | `IamUserAdminController` bajo `/api/v1/iam/users` |
| **Use case** | `RegisterIdentityUseCase` (bootstrap) | `ListUsersUseCase`, `GetUserUseCase`, `UpdateUserUseCase`, `DeactivateUserUseCase`, `CreateUserUseCase` (admin) |

#### Decisión de listado (membership-first, ADR-006)

El listado de “usuarios del tenant” **no** debe usar solo `iam_user.tenant_id` (columna legacy en transición 13.5). Fuente de verdad del pertenecimiento: `identity_tenant_membership`.

```sql
-- Query canónica propuesta (R2DBC custom)
SELECT u.*
FROM iam.iam_user u
INNER JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
WHERE m.tenant_id = :tenantId
  AND (:statusFilter IS NULL OR m.status = :statusFilter)
ORDER BY u.created_at DESC
LIMIT :size OFFSET :offset
```

`GET /users/{id}`:

1. Resolver `tenantId` desde `AuthorizationContext` (JWT).
2. Verificar membership: `membershipRepository.exists(identityId, tenantId)` o equivalente.
3. Cargar identity: `identityRepository.findById(IdentityId)` (**nuevo** método global).
4. Si membership ausente → `404` (no filtrar datos de otro tenant).

### 2.2 Actualización — email, status, nombre

| Campo | Dominio actual | Acción 15.1 |
|-------|----------------|-------------|
| **status** | `disable()`, `enable()`, `lockAccount()`, `unlockAccount()`, `markEmailVerified()`, `requirePasswordReset()` | **Reutilizar** mutadores existentes vía `UpdateUserUseCase`; mapear request → método de dominio según intención |
| **email** | `email` es `final`; sin `changeEmail()` | **Nuevo comportamiento de dominio:** `Identity.changeEmail(EmailAddress newEmail)` con validación de formato (value object) y regla de unicidad global delegada al use case (`existsByEmail`) antes de invocar dominio |
| **nombre** | No existe en aggregate ni schema | **Fuera de alcance 15.1–15.3**; requeriría migración + aggregate change (no solicitado en ROADMAP) |

**Anti anemic domain:** el use case orquesta transacción y unicidad; el aggregate ejecuta transiciones de estado y `changeEmail` con invariantes locales (p. ej. no cambiar email si `LOCKED` — definir en implementación).

`PUT /api/v1/iam/users/{id}` — cuerpo propuesto:

```json
{
  "status": "DISABLED | ACTIVE | LOCKED",
  "email": "optional@domain.com"
}
```

Permiso: `user:update`.

### 2.3 Eliminación — decisión obligatoria

| Opción | Evaluación | Decisión |
|--------|------------|----------|
| DELETE físico (`IdentityRepository.delete`) | Identity global (ADR-006); borrar fila afecta **todos** los tenants; sin FK membership→user | **Rechazado** como semántica de `user:delete` |
| Soft delete / disable | `Identity.disable()` → `DISABLED`; bloquea autenticación global | **Aceptado** |
| Solo baja de membership | Corresponde a `membership:delete` (15.2) | No confundir con `user:delete` |

**Decisión documentada:**

- `DELETE /api/v1/iam/users/{id}` con permiso `user:delete` ejecuta **`Identity.disable()`** (soft delete a nivel identidad).
- Respuesta: `204 No Content`.
- No elimina filas ni memberships; un usuario deshabilitado no autentica en ningún tenant.
- Si en el futuro se requiere “quitar del tenant sin deshabilitar globalmente”, usar **Membership Administration** (15.2).

`POST /api/v1/iam/users` (admin create, `user:create`):

- Misma transacción que `RegisterIdentityUseCaseImpl`: `Identity` + `IdentityTenantMembership`.
- Diferencias admin: status inicial configurable (`ACTIVE` vs `PENDING_VERIFICATION`); protegido con `@RequiresPermission("user:create")`.
- Bootstrap `POST /identities` permanece público hasta 15.7 (ADR-008).

### 2.4 Diagrama — User Administration

```mermaid
flowchart TB
    subgraph http [HTTP]
        C[IamUserAdminController]
    end

    subgraph app [Application]
        UC[List / Get / Create / Update / Deactivate User UseCases]
        CTX[AuthorizationContextAccessor]
    end

    subgraph domain [Domain]
        ID[Identity]
    end

    subgraph infra [Infrastructure]
        IR[IdentityRepository + AdminQuery]
        MR[MembershipRepository]
    end

    C -->|@RequiresPermission user:*| UC
    UC --> CTX
    UC -->|tenantId| MR
    UC -->|membership gate| ID
    UC --> IR
    ID -->|disable / enable / changeEmail| UC
```

---

## 3. Membership Administration (15.2)

### 3.1 Listado — `GET /api/v1/iam/memberships` y `GET /api/v1/iam/memberships/{id}`

| Capa | Existente | Faltante |
|------|-----------|----------|
| **Aggregate** | `IdentityTenantMembership` completo | — |
| **Repository** | `findByTenantId(TenantId)` → `Flux` | `findByIdAndTenantId(MembershipId, TenantId)`; paginación |
| **DTOs** | — | `MembershipResponse` (incl. `identityId`, opcional `identityEmail` en listado) |
| **HTTP / Use case** | — | `ListMembershipsUseCase`, `GetMembershipUseCase` |

Listado: `membershipRepository.findByTenantId(tenantId)` con paginación en capa query (nuevo método paginado o `LIMIT/OFFSET` en adapter).

GET por id: cargar por `membershipId` **y** validar `membership.tenantId == context.tenantId`; si no coincide → `404`.

### 3.2 Alta — `POST /api/v1/iam/memberships`

**¿Se crea Identity? ¿Se vincula existente? ¿Ambos?**

**Ambos casos**, con flujo explícito:

| Caso | Request | Flujo |
|------|---------|-------|
| **A — Vincular identity existente** | `{ "identityId": "uuid" }` o `{ "email": "user@x.com" }` | Resolver identity → `exists(identityId, tenantId)` false → `IdentityTenantMembership.create()` → `save` |
| **B — Identity nueva en el tenant** | `{ "email": "...", "password": "..." }` | Delegar a lógica de `CreateUserUseCase` (15.1) + membership ya creada en registro; **o** error `409` si email existe sin membership en tenant → sugerir caso A |

**Regla:** una sola membership por par `(identityId, tenantId)` — `UNIQUE` en V7.

Permiso: `membership:create`.

No asignar roles en 15.2 (reservado para 15.6).

### 3.3 Baja — significado de eliminar membership

| Opción | Evaluación | Decisión |
|--------|------------|----------|
| DELETE físico | `ON DELETE CASCADE` en `membership_role` (V12) | Posible en DB pero **no** semántica principal |
| INACTIVE (`deactivate()`) | Revoca acceso RBAC (`ACTIVE` membership requerida, ADR-007) | **Aceptado** como `membership:delete` |
| ARCHIVE | Sin modelo | **No introducir** |

**Decisión documentada:**

- `DELETE /api/v1/iam/memberships/{id}` → `membership.deactivate()` (`INACTIVE`).
- `PUT` con `membership:update` puede `activate()` / `deactivate()`.
- Hard delete de fila membership: **fuera de alcance 15.2** (evita pérdida de auditoría; CASCADE eliminaría `membership_role`).

### 3.4 Diagrama — Membership Administration

```mermaid
flowchart TB
    subgraph http [HTTP]
        MC[IamMembershipAdminController]
    end

    subgraph app [Application]
        MUC[List / Get / Create / Deactivate Membership UseCases]
        CUC[CreateUserUseCase - caso B]
    end

    subgraph domain [Domain]
        MEM[IdentityTenantMembership]
    end

    subgraph infra [Infrastructure]
        MR[MembershipRepository]
        IR[IdentityRepository]
    end

    MC -->|@RequiresPermission membership:*| MUC
    MUC -->|tenantId from context| MR
    MUC --> MEM
    MUC -->|caso B| CUC
    CUC --> IR
    MEM -->|create / activate / deactivate| MUC
```

---

## 4. Role Administration (15.3)

### 4.1 Crear — `POST /api/v1/iam/roles`

| Existente | Faltante |
|-----------|----------|
| `Role.create(tenantId, code, name, now)` | `CreateRoleUseCase`, validar `existsByTenantIdAndCode` → `409` |
| `RoleRepository.save` | Controller + `CreateRoleRequest` (`code`, `name`) |

Permiso: `role:create`. Solo roles **custom** (`systemRole = false`).

### 4.2 Editar — `PUT /api/v1/iam/roles/{id}`

| Operación | Dominio | System role |
|-----------|---------|-------------|
| Renombrar (`name`) | `Role.rename(RoleName)` | Bloqueado (`ensureMutable`) |
| Activar / desactivar | `activate()` / `deactivate()` | Bloqueado |

Flujo:

1. `roleRepository.findById(roleId)`.
2. Verificar `role.tenantId() == context.tenantId()` → si no, `404`.
3. Si `role.systemRole()` → `403 Forbidden` (antes de llamar dominio).
4. Aplicar cambios y `save`.

Permiso: `role:update`.

`code` es **inmutable** (final en aggregate) — no exponer cambio de código en API.

### 4.3 Eliminar — `DELETE /api/v1/iam/roles/{id}`

Efectos en tablas asociadas (V11, V12):

| Tabla | ON DELETE hacia `role` |
|-------|------------------------|
| `role_permission` | **CASCADE** — se eliminan asignaciones |
| `membership_role` | **RESTRICT** — impide delete si hay memberships con el rol |

**Comportamiento propuesto:**

1. Rechazar si `systemRole == true` → `403`.
2. Verificar tenant scope → `404` si otro tenant.
3. Si existe fila en `membership_role` para `roleId` → `409 Conflict` (rol en uso).
4. Si no hay referencias: **DELETE físico** del rol (CASCADE limpia `role_permission`).
5. Alternativa futura: solo `deactivate()` sin delete — **no** para 15.3; el ROADMAP pide “eliminar roles custom”.

Nuevo en infra: `RoleRepository.delete(RoleId)` + `MembershipRoleRepository.existsByRoleId(RoleId)` (o query count).

Permiso: `role:delete`.

### 4.4 System roles — OWNER, ADMIN, MANAGER, USER, READ_ONLY

Provisionados por `TenantSystemRolesProvisionerImpl` / `SystemRoleTemplate`.

| Operación | Comportamiento admin |
|-----------|---------------------|
| **Edición** (rename, activate, deactivate) | **403** — `systemRole=true`; dominio lanza `IllegalStateException` si se intenta |
| **Eliminación** | **403** — nunca exponer delete exitoso |
| **Renombrado de código** | Imposible (`RoleCode` final) |

Los system roles siguen **solo legibles** vía `GET` (incluidos en listado del tenant).

### 4.5 Lectura — listado y detalle

| Faltante | Acción |
|----------|--------|
| `Flux<Role> findByTenantId(TenantId)` | Añadir a `RoleRepository` + Spring Data |
| GET con aislamiento | `findById` + comparar `role.tenantId()` con JWT tenant |

Permiso list/detail: `role:read`.

### 4.6 Diagrama — Role Administration

```mermaid
flowchart TB
    subgraph http [HTTP]
        RC[IamRoleAdminController]
    end

    subgraph app [Application]
        RUC[List / Get / Create / Update / Delete Role UseCases]
    end

    subgraph domain [Domain]
        ROLE[Role]
    end

    subgraph infra [Infrastructure]
        RR[RoleRepository]
        MRR[MembershipRoleRepository]
        RPR[RolePermissionRepository]
    end

    RC -->|@RequiresPermission role:*| RUC
    RUC --> RR
    RUC --> ROLE
    RUC -->|delete pre-check| MRR
    ROLE -->|ensureMutable / create| RUC
    RR -->|CASCADE| RPR
```

---

## 5. Preguntas transversales

### 5.1 Tenant isolation — por endpoint

`tenantId` operativo = claim JWT, validado por `ReactorAuthorizationContextAccessor` → membership **ACTIVE** en ese tenant. Toda operación admin parte de `AuthorizationContextAccessor.current()`.

| Endpoint | Mecanismo concreto |
|----------|-------------------|
| `GET /iam/users` | Query JOIN `membership.tenant_id = :tenantId` desde contexto |
| `GET /iam/users/{id}` | `membershipRepository.exists(identityId, tenantId)` + `findById(identityId)` |
| `PUT/DELETE /iam/users/{id}` | Igual gate membership antes de mutar |
| `POST /iam/users` | `tenantId` del contexto (ignorar body tenant) |
| `GET /iam/memberships` | `findByTenantId(context.tenantId)` |
| `GET /iam/memberships/{id}` | `findByIdAndTenantId(id, context.tenantId)` |
| `POST /iam/memberships` | Membership creada con `tenantId` del contexto |
| `PUT/DELETE /iam/memberships/{id}` | Load + `membership.tenantId().equals(context.tenantId())` |
| `GET /iam/roles` | `findByTenantId(context.tenantId)` |
| `GET/PUT/DELETE /iam/roles/{id}` | `role.tenantId().equals(context.tenantId())` |
| `POST /iam/roles` | `Role.create(context.tenantId(), ...)` |

**Authorization:** `@RequiresPermission` en cada handler (FASE 14 AOP). Sin permiso → `403` (`AuthorizationDeniedException`).

**No** confiar en IDs de otro tenant: mismatch → **404** (no 403) para no revelar existencia cross-tenant.

### 5.2 Pagination — estrategia única

No existe patrón previo en el codebase. **Estrategia única** para Users, Memberships y Roles:

| Parámetro | Default | Máximo | Descripción |
|-----------|---------|--------|-------------|
| `page` | `0` | — | Índice 0-based |
| `size` | `20` | `100` | Tamaño de página |
| `sort` | recurso-dependiente | — | `field,direction` — p. ej. `createdAt,desc` |

**Campos sort permitidos:**

| Recurso | Campos |
|---------|--------|
| Users | `email`, `status`, `createdAt`, `lastLoginAt` |
| Memberships | `status`, `createdAt` |
| Roles | `code`, `name`, `status`, `createdAt` |

**Response envelope** (HTTP DTO, no compartido con application):

```json
{
  "content": [ "...ResourceResponse" ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

Implementación: queries R2DBC con `LIMIT/OFFSET` + `COUNT(*)` en el mismo tenant. Sin Spring Data `Pageable` en dominio.

### 5.3 Response contract — DTOs HTTP

No exponer aggregates. DTOs en `interfaces.http.admin.dto`:

#### `UserResponse`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | UUID | `IdentityId` |
| `email` | string | |
| `status` | enum string | `IdentityStatus` |
| `lastLoginAt` | ISO-8601 / null | |
| `createdAt` | ISO-8601 | |
| `updatedAt` | ISO-8601 | |

#### `MembershipResponse`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | UUID | `MembershipId` |
| `identityId` | UUID | |
| `tenantId` | UUID | Siempre igual al contexto |
| `status` | `ACTIVE` \| `INACTIVE` | |
| `identityEmail` | string | Solo en listado (join opcional) |
| `createdAt` / `updatedAt` | ISO-8601 | |

#### `RoleResponse`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | UUID | |
| `tenantId` | UUID | |
| `code` | string | |
| `name` | string | |
| `status` | `ACTIVE` \| `INACTIVE` | |
| `systemRole` | boolean | |
| `createdAt` / `updatedAt` | ISO-8601 | |

Mappers estáticos o métodos package-private en DTOs — **sin** DTOs compartidos entre capas application e interfaces.

### 5.4 Error contract

Reutilizar `IamHttpExceptionHandler` (cuerpo vacío, consistente con FASE 11–15.0).

| HTTP | Cuándo | Excepción / origen |
|------|--------|-------------------|
| **400** | Validación request / dominio | `InvalidDomainValueException`, `IllegalArgumentException` (handler nuevo) |
| **401** | Sin JWT | Spring Security (no handler IAM) |
| **403** | Sin permiso RBAC | `AuthorizationDeniedException` |
| **403** | Mutación system role | `SystemRoleImmutableException` (nueva) o `IllegalStateException` mapeada |
| **404** | Recurso no encontrado en tenant | `IdentityNotFoundException`, `MembershipNotFoundException` (nueva), `RoleNotFoundException` (nueva) |
| **409** | Email duplicado / membership duplicada / código rol duplicado / rol con assignments | `IdentityAlreadyExistsException`, conflictos de negocio dedicados |
| **423** | Identity locked (auth) | Ya existente en auth flows |

**Extensiones mínimas 15.1–15.3:**

- Handlers `@ExceptionHandler` para not-found (404).
- Handler `IllegalStateException` con mensaje “System roles cannot be modified” → 403.
- Mantener **sin** JSON body (alineado con handlers actuales).

---

## 6. Matriz HTTP — permisos (ADR-008)

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/users` | `user:read` |
| GET | `/api/v1/iam/users/{id}` | `user:read` |
| POST | `/api/v1/iam/users` | `user:create` |
| PUT | `/api/v1/iam/users/{id}` | `user:update` |
| DELETE | `/api/v1/iam/users/{id}` | `user:delete` |
| GET | `/api/v1/iam/memberships` | `membership:read` |
| GET | `/api/v1/iam/memberships/{id}` | `membership:read` |
| POST | `/api/v1/iam/memberships` | `membership:create` |
| PUT | `/api/v1/iam/memberships/{id}` | `membership:update` |
| DELETE | `/api/v1/iam/memberships/{id}` | `membership:delete` |
| GET | `/api/v1/iam/roles` | `role:read` |
| GET | `/api/v1/iam/roles/{id}` | `role:read` |
| POST | `/api/v1/iam/roles` | `role:create` |
| PUT | `/api/v1/iam/roles/{id}` | `role:update` |
| DELETE | `/api/v1/iam/roles/{id}` | `role:delete` |

Constantes: `IamPermissionCatalog` + seeds V13 (sin cambios).

---

## 7. Extensiones de puertos (resumen implementación)

### 7.1 IdentityRepository / query

```text
+ Mono<Identity> findById(IdentityId identityId)
+ Flux<Identity> findAllByTenantId(TenantId tenantId, PageQuery page)   // o puerto IdentityAdminQueryRepository
+ Mono<Long> countByTenantId(TenantId tenantId)
```

### 7.2 MembershipRepository

```text
+ Mono<IdentityTenantMembership> findByIdAndTenantId(MembershipId id, TenantId tenantId)
+ Flux<...> findByTenantId(TenantId tenantId, PageQuery page)   // paginado
+ Mono<Long> countByTenantId(TenantId tenantId)
```

### 7.3 RoleRepository

```text
+ Flux<Role> findByTenantId(TenantId tenantId, PageQuery page)
+ Mono<Role> findByTenantIdAndId(TenantId tenantId, RoleId roleId)
+ Mono<Long> countByTenantId(TenantId tenantId)
+ Mono<Void> delete(RoleId roleId)
```

### 7.4 MembershipRoleRepository

```text
+ Mono<Boolean> existsByRoleId(RoleId roleId)   // pre-delete check
```

### 7.5 Dominio

```text
Identity.changeEmail(EmailAddress newEmail)   // 15.1
MembershipNotFoundException, RoleNotFoundException   // 15.2 / 15.3
```

### 7.6 Sin tocar (restricción usuario)

- `AuthorizationService` / `AuthorizationServiceImpl`
- `AuthorizationContext` / `ReactorAuthorizationContextAccessor` / `AuthorizationContextWebFilter`
- `TenantContext` / JWT pipeline
- `IdentityTenantMembership` modelo RBAC (`assignRole` / seeds)
- Tablas V9–V13 (sin migración nueva en 15.1–15.3)

---

## 8. Plan de implementación (orden estricto)

### 15.1 User Administration

1. Dominio: `changeEmail`, `IdentityNotFoundException` → 404 handler.
2. Puertos + R2DBC: listado membership-anchored, `findById(IdentityId)`.
3. Use cases: List, Get, Create (admin), Update, Deactivate.
4. `IamUserAdminController` + DTOs + `IamAdminApiPaths.USERS`.
5. Tests: `IamUserAdminControllerIT` (200/401/403/404/409).
6. Doc: `PASO-15.1-USER-ADMINISTRATION.md`.

### 15.2 Membership Administration

1. Puerto `findByIdAndTenantId` + paginación.
2. Use cases: List, Get, Create (link + create-vía-user), Update, Deactivate.
3. `IamMembershipAdminController`.
4. Tests: `IamMembershipAdminControllerIT`.
5. Doc: `PASO-15.2-MEMBERSHIP-ADMINISTRATION.md`.

### 15.3 Role Administration

1. Puerto `findByTenantId`, `delete`, `existsByRoleId`.
2. Use cases: List, Get, Create, Update, Delete (custom only).
3. `IamRoleAdminController`.
4. Tests: `IamRoleAdminControllerIT` + system role 403 + delete con assignment 409.
5. Doc: `PASO-15.3-ROLE-ADMINISTRATION.md`.

**Cada paso:** solo tests del paso; no suite completa IAM (~115).

---

## 9. Resultado esperado al cerrar 15.3 (verificación)

| # | Capacidad | Endpoint | Permiso |
|---|-----------|----------|---------|
| 1 | Consultar usuarios | GET `/iam/users` | `user:read` |
| 2 | Modificar usuarios | PUT `/iam/users/{id}` | `user:update` |
| 3 | Desactivar usuarios | DELETE `/iam/users/{id}` | `user:delete` |
| 4 | Consultar memberships | GET `/iam/memberships` | `membership:read` |
| 5 | Crear memberships | POST `/iam/memberships` | `membership:create` |
| 6 | Desactivar memberships | DELETE `/iam/memberships/{id}` | `membership:delete` |
| 7 | Consultar roles | GET `/iam/roles` | `role:read` |
| 8 | Crear roles | POST `/iam/roles` | `role:create` |
| 9 | Editar roles | PUT `/iam/roles/{id}` | `role:update` |
| 10 | Eliminar roles custom | DELETE `/iam/roles/{id}` | `role:delete` |

Evidencia: tests IT por paso + audits `PASO-15.x` con comando Gradle y resultado.

---

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| `iam_user.tenant_id` legacy vs membership-first | List/get users siempre anclados a membership del JWT tenant |
| `user:delete` deshabilita identidad global | Documentado; usar membership deactivate para baja por tenant |
| Delete rol con assignments | Pre-check `membership_role` → 409 |
| Mutación system role | 403 en use case antes de dominio |
| Paginación sin precedente | `PageQuery` value object en application; un solo envelope HTTP |

---

## 11. Estado del paso previo

| Ítem | Estado |
|------|--------|
| Auditoría conjunta 15.1–15.3 | ✅ Este documento |
| Implementación 15.1 | ⏳ Pendiente |
| Implementación 15.2 | ⏳ Pendiente |
| Implementación 15.3 | ⏳ Pendiente |

**Siguiente acción:** implementar **15.1 User Administration** según sección 8.
