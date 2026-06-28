# PASO 16.3.1 — Organization Administration API Audit

**Fecha:** 2026-06-22  
**Estado:** ✅ Auditoría cerrada — listo para 16.4  
**Tipo:** Diseño previo (sin código)  
**Dependencias:** [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md](PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md) · ADR-008 · ADR-010

---

## Objetivo

Anticipar decisiones de diseño antes del primer paso HTTP (16.4): controllers, DTOs, use cases, queries, paginación y autorización — donde suelen aparecer errores estructurales.

**Alcance 16.4:** solo **Organization** admin API. Office y StaffAssignment quedan en 16.5+ / 16.7+.

---

## Evidencia revisada

| Fuente | Relevancia |
|--------|------------|
| `Organization.java` | `code` final; `rename`, `archive`, `activate`; sin delete |
| `OrganizationQueryPort` | `findByIdAndTenantId`, `findAllByTenantId`, `countByTenantId` |
| PASO-16.3 matriz RBAC | ADMIN archive ✓; MANAGER solo `organization:read` |
| ADR-008 | `/api/v1/{module}/**`, JWT tenant, `@RequiresPermission`, paginación IAM |
| PASO-15.0.1 | `OwnershipPolicy` — solo jerarquía persona↔persona IAM |
| PASO-15.1 audit | Patrón `PageQuery`, membership/tenant-first, 404 cross-tenant |

---

## Respuestas obligatorias

### 1 — ¿OrganizationCode será editable?

**No.**

| Evidencia | Implicación |
|-----------|-------------|
| `code` es campo `final` en el aggregate | Identificador funcional inmutable tras creación |
| `UNIQUE (tenant_id, code)` en `org.organization` | Cambiar code = riesgo de ruptura de referencias futuras (Office, StaffAssignment, integraciones) |
| `OrganizationCode` normalizado en factory | Solo se establece en `POST` create |

**API 16.4:**

- `CreateOrganizationRequest`: incluye `code` + `name`
- `UpdateOrganizationRequest`: **solo `name`** (y opcionalmente `status` vía activate — ver §4)
- Intentar cambiar `code` → **ignorar campo** o **400** si se envía explícitamente (preferir **400** con mensaje claro — fail fast)

**Reactivación:** si se necesita otro code, crear nueva organization y archivar la anterior (patrón SaaS).

---

### 2 — ¿Organization archive afecta Offices?

**En 16.4: no** (Office no existe aún).

**Regla objetivo (16.5 / 16.8 — documentar ahora, implementar después):**

| Opción | Veredicto |
|--------|-----------|
| Cascade archive automático de offices | ⏳ Evaluar en 16.8 |
| **Bloquear archive si existen offices ACTIVE** | ✅ **Recomendado** para 16.5+ |
| Archive org dejando offices ACTIVE huérfanos | ❌ Rechazado — inconsistencia operativa |

**Decisión:** `Organization.archive()` permanece **local al aggregate** (16.1). En **16.5+**, `ArchiveOrganizationUseCase` validará:

```text
IF EXISTS office WHERE organization_id = :id AND status = ACTIVE
  → 409 Conflict ("Archive offices first")
ELSE
  → organization.archive()
```

Cascade archive masivo de offices queda como **mejora opcional post-16.8**, no default.

---

### 3 — ¿Organization archive afecta StaffAssignments?

**En 16.4: no** (StaffAssignment no existe).

**Regla objetivo (16.7 / 16.8):**

| Comportamiento | Decisión |
|----------------|----------|
| Delete físico de assignments | ❌ No |
| Auto-delete assignments al archivar org | ❌ No — pierde trazabilidad |
| Assignments existentes | Permanecen **históricos**; excluidos de queries operativas ACTIVE |
| Nuevos assignments sobre org ARCHIVED | **Bloqueados** en use case (409) |

Archive de organization **no muta** filas staff_assignment; filtra en lectura operativa.

---

### 4 — ¿GET archived se muestra o se oculta?

**Se muestra — no se oculta por permiso.**

| Operación | Comportamiento |
|-----------|----------------|
| `GET /organizations/{id}` | Devuelve org **ACTIVE o ARCHIVED** si pertenece al tenant JWT → **404** solo si id inexistente o **otro tenant** |
| `GET /organizations` (list) | Default: **`status=ACTIVE`** |
| Query param | `status=ACTIVE \| ARCHIVED \| ALL` (default `ACTIVE`) |

**Rationale:** `organization:read` no distingue archived; ocultar archived rompería auditoría y reactivación (`activate`). IAM no oculta roles inactivos del GET by id — mismo patrón.

**Reactivación:** `POST /organizations/{id}/activate` o `PUT` con transición de estado — permiso `organization:update` (ADMIN+) o endpoint dedicado protegido por `organization:update`.

---

### 5 — ¿Listados paginados desde el inicio?

**Sí.**

Reutilizar patrón FASE 15:

```text
GET /api/v1/org/organizations?page=0&size=20&sort=createdAt,desc&status=ACTIVE
```

| Elemento | Convención |
|----------|------------|
| Paginación | `page`, `size` (max `PageQuery.MAX_SIZE`) |
| Sort permitido | `code`, `name`, `status`, `createdAt`, `updatedAt` |
| Respuesta | `PagedOrganizationResponse` (content + totalElements + page + size) |
| Implementación | `OrganizationAdminQueryRepository` + SQL tenant-scoped (no solo `Flux` en memoria) |

**No** listar cross-tenant. **No** paginación diferida a fase posterior.

---

### 6 — ¿Soft delete únicamente?

**Sí — lifecycle soft únicamente.**

| Operación HTTP | Dominio | Permiso |
|----------------|---------|---------|
| Crear | `Organization.create` | `organization:create` |
| Renombrar | `rename` | `organization:update` |
| Archivar | `archive` | `organization:archive` |
| Reactivar | `activate` | `organization:update` |
| Delete físico | **No existe** | **No endpoint DELETE** |

**16.4:** no exponer `DELETE /organizations/{id}`. Archivar ≠ IAM `user:delete` (offboarding). Permiso sembrado es `organization:archive`, no `delete`.

---

### 7 — ¿Cross-tenant protection?

**Sí — obligatorio, sin excepciones.**

Capas (defensa en profundidad):

```text
1. JWT tenantId → AuthorizationContext
2. TenantOperationalGuard (tenant ACTIVE)
3. @RequiresPermission("organization:*")
4. Repository/query SIEMPRE filtra tenant_id
5. findByIdAndTenantId → empty = 404 (no 403 — anti-enumeración)
```

**Regla:** nunca aceptar `tenantId` en body/query del cliente. Tenant solo desde JWT (ADR-008, ADR-003).

Cross-tenant create con `tenantId` spoof → imposible (ignorar body tenantId; usar JWT).

---

### 8 — ¿Ownership aplica aquí o no?

**No — OwnershipPolicy no aplica a Organization.**

| Concepto | Aplica a | Organization |
|----------|----------|--------------|
| `OwnershipPolicy` (PASO-15.0.1) | Jerarquía **persona → persona** (OWNER vs ADMIN vs MANAGER) | ❌ |
| RBAC `@RequiresPermission` | Recurso + acción | ✅ Única barrera en 16.4 |

Organization es **estructura de negocio**, no actor IAM. No hay target "usuario más privilegiado que otro" al archivar una clínica.

**MANAGER no puede archivar** porque **no tiene** `organization:archive` (matriz 16.3), no por ownership.

---

### 9 — ¿ADMIN puede archivar?

**Sí.**

Matriz 16.3: `organization:archive` ✓ para **ADMIN** y **OWNER**.

Endpoint:

```text
POST /api/v1/org/organizations/{id}/archive
@RequiresPermission("organization:archive")
```

Alternativa aceptable: `PUT` con body `{ "status": "ARCHIVED" }` — preferir **POST /archive** explícito (acción de dominio clara, OpenAPI legible).

---

### 10 — ¿MANAGER puede ver organizaciones archivadas?

**Sí — si las solicita explícitamente.**

| Escenario | MANAGER |
|-----------|---------|
| `GET /organizations` (default `status=ACTIVE`) | Solo ACTIVE |
| `GET /organizations?status=ARCHIVED` | ✅ Ve archivadas (tiene `organization:read`) |
| `GET /organizations?status=ALL` | ✅ Ve ambas |
| `GET /organizations/{id}` archivada | ✅ 200 con `status: ARCHIVED` |
| Archivar / crear / renombrar org | ❌ Sin permisos |

MANAGER opera **offices y staff** bajo orgs existentes; debe **leer** estructura completa para contexto operativo, incluido histórico archivado.

---

## Resumen de decisiones 16.4

| Tema | Decisión |
|------|----------|
| Code editable | **No** |
| Archive → offices | **No en 16.4**; bloqueo si ACTIVE offices en 16.5+ |
| Archive → staff | **No mutación**; bloqueo nuevas asignaciones en 16.7+ |
| Archived en GET | **Visible**; list default ACTIVE + filtro |
| Paginación | **Sí desde 16.4** |
| Delete físico | **No** |
| Cross-tenant | **404 + tenant filter** |
| Ownership | **No aplica** |
| ADMIN archive | **Sí** |
| MANAGER ve archived | **Sí** (con filtro o GET by id) |

---

## Contradicciones detectadas

| Ítem | Estado |
|------|--------|
| ADR-010 menciona `organization:delete` | **Supersedido** por 16.3 (`organization:archive`) — no action required en ADR hasta cierre 16.10 |
| ADR-007 | **Compatible** — RBAC membership-scoped sin cambios |
| Dominio `code` final vs API update | **Alineado** — API no expone mutación de code |

---

## Blueprint API 16.4 (referencia implementación)

### Rutas

| Método | Path | Permiso |
|--------|------|---------|
| `GET` | `/api/v1/org/organizations` | `organization:read` |
| `GET` | `/api/v1/org/organizations/{id}` | `organization:read` |
| `POST` | `/api/v1/org/organizations` | `organization:create` |
| `PUT` | `/api/v1/org/organizations/{id}` | `organization:update` |
| `POST` | `/api/v1/org/organizations/{id}/archive` | `organization:archive` |
| `POST` | `/api/v1/org/organizations/{id}/activate` | `organization:update` |

### DTOs mínimos

```json
// CreateOrganizationRequest
{ "code": "DENTAL_NORTE", "name": "Dental Norte" }

// UpdateOrganizationRequest
{ "name": "Dental Norte Renovado" }

// OrganizationResponse
{
  "id": "uuid",
  "code": "DENTAL_NORTE",
  "name": "Dental Norte",
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Use cases (`port.in`)

| Use case | Responsabilidad |
|----------|-----------------|
| `ListOrganizationsUseCase` | Paginación + filtro status + tenant |
| `GetOrganizationUseCase` | By id + tenant → 404 |
| `CreateOrganizationUseCase` | Validar unicidad code; guard ACTIVE tenant |
| `UpdateOrganizationUseCase` | Rename; rechazar code en body |
| `ArchiveOrganizationUseCase` | `archive()`; futuro: guard offices |
| `ActivateOrganizationUseCase` | `activate()` |

### Paquetes (hexagonal — org module)

```text
interfaces.http.admin.*
application.port.in.*
application.admin.*Impl
application.query.*          ← OrganizationAdminQueryRepository
infrastructure.persistence.* ← extensión query + existente R2dbcOrganizationRepository
```

### Wiring

- Registrar controllers en `organization-infrastructure` o `codecore-api` import `OrganizationModuleConfiguration` extendido
- Reutilizar `AuthorizationContextWebFilter` / `@RequiresPermission` desde IAM (dependencia platform-security)
- **No** modificar IAM aggregates

---

## Tests mínimos 16.4 (preview)

| Tipo | Escenarios |
|------|------------|
| Unit | Use cases: rename, archive, code immutability |
| IT WebFlux | CRUD journey, pagination, status filter, cross-tenant 404 |
| IT RBAC | MANAGER read archived ✓, MANAGER archive 403, ADMIN archive ✓ |

---

## Veredicto

**Sin bloqueantes arquitectónicos para 16.4.**

Decisiones cerradas; cascade office/staff diferido conscientemente a 16.5+ / 16.7+ / 16.8 con reglas ya definidas.

**Próximo paso:** **PASO 16.4 — Organization Administration API**

---

## Referencias

- [PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md](PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md)
- [PASO-16.2-ORGANIZATION-PERSISTENCE.md](PASO-16.2-ORGANIZATION-PERSISTENCE.md)
- [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md](PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md)
- [ADR-008](../architecture/ADR-008-IAM-ADMINISTRATION-API.md)
- [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)
