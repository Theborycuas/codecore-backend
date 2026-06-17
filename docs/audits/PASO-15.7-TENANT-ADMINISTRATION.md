# PASO 15.7 — Tenant Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP para leer/actualizar el tenant del JWT y endurecimiento de endpoints bootstrap legacy.  
**Prerequisitos:** PASO-15.0 (ADR-008), seeds V13 (`tenant:read`, `tenant:update`).

---

## 1. Resumen ejecutivo

Séptima capa administrativa IAM: metadata del tenant actual (resuelto desde JWT) y cierre de la ventana bootstrap pública prevista en ADR-008.

| Ítem | Estado |
|------|--------|
| `GET /api/v1/iam/tenants/current` | ✅ |
| `PUT /api/v1/iam/tenants/current` | ✅ |
| `@RequiresPermission("tenant:read")` en GET | ✅ |
| `@RequiresPermission("tenant:update")` en PUT | ✅ |
| Bootstrap `POST /tenants` ya no público | ✅ |
| Bootstrap `POST /identities` ya no público | ✅ |
| Tests unitarios 15.7 | ✅ 8 tests |
| Tests IT 15.7 | ✅ escritos (requieren Docker/Testcontainers) |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| Scope tenant | Siempre el `tenantId` del `AuthorizationContext` (JWT) |
| Sin listado multi-tenant | Solo `/current`; no CRUD de otros tenants |
| PUT parcial | `name` y/o `status` (al menos uno requerido) |
| Rename | `Tenant.rename(TenantName)` en dominio |
| Status | `activate()` / `suspend()` / `disable()` según `TenantStatus` |
| Nombre duplicado | `TenantAlreadyExistsException` → 409 |
| Tenant inexistente | `TenantNotFoundException` → 404 |
| RBAC | OWNER: `tenant:update`; READ_ONLY: `tenant:read`; ADMIN: sin `tenant:*` |
| Bootstrap 15.7 | Quitar `permitAll` de `POST /tenants` y `POST /identities` en `PublicApiPaths` + `PlatformSecurityAutoConfiguration` |
| Alta de tenant/usuario | Tests y ops usan `CreateTenantUseCase` / `POST /api/v1/iam/users` (`user:create`) con JWT |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/tenants/current` | `tenant:read` |
| PUT | `/api/v1/iam/tenants/current` | `tenant:update` |

### Request PUT

```json
{
  "name": "Acme Corp",
  "status": "ACTIVE"
}
```

`status`: `ACTIVE` | `SUSPENDED` | `DISABLED`

### Response

```json
{
  "tenantId": "uuid",
  "name": "Acme Corp",
  "status": "ACTIVE",
  "createdAt": "2026-06-17T...",
  "updatedAt": "2026-06-17T..."
}
```

---

## 4. Endurecimiento bootstrap (ADR-008)

| Ruta legacy | Antes (≤15.6) | Desde 15.7 |
|-------------|---------------|------------|
| `POST /api/v1/tenants` | Público | JWT requerido (401 sin token) |
| `POST /api/v1/identities` | Público | JWT requerido (401 sin token) |
| `POST /api/v1/auth/login` | Público | Sin cambio |
| `GET /actuator/health` | Público | Sin cambio |

**Onboarding productivo recomendado:** provisioning interno de tenant + `POST /api/v1/iam/users` con `user:create`.

---

## 5. Archivos principales

| Área | Archivos |
|------|----------|
| Dominio | `Tenant.rename()`, `TenantNotFoundException` |
| Use cases | `TenantAdministrationUseCaseImpl`, `Get/UpdateAdminTenantUseCase` |
| Command / DTO | `UpdateAdminTenantCommand`, `AdminTenantView` |
| HTTP | `IamTenantAdminController`, `TenantResponse`, `UpdateTenantRequest` |
| Seguridad | `PublicApiPaths`, `PlatformSecurityAutoConfiguration` |
| Config | `IamAdministrationConfiguration` |
| Tests | `TenantAdministrationUseCaseTest`, `IamTenantAdminControllerIT`, `BootstrapEndpointsSecurityIT` |

---

## 6. Tests ejecutados (solo 15.7)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.TenantAdministrationUseCaseTest" \
  --tests "com.codecore.iam.domain.model.tenant.TenantTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamTenantAdminControllerIT" \
  --tests "com.codecore.iam.interfaces.http.BootstrapEndpointsSecurityIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `TenantAdministrationUseCaseTest` | 4 | ✅ |
| `TenantTest` (rename) | 4 | ✅ |
| `IamTenantAdminControllerIT` | 6 | ⏳ Docker |
| `BootstrapEndpointsSecurityIT` | 2 | ⏳ Docker |

### Cobertura HTTP (IT — diseñada)

- GET como READ_ONLY; PUT como OWNER
- 403 ADMIN sin `tenant:read`; 403 READ_ONLY en PUT
- 409 nombre duplicado; 401 sin JWT
- 401 bootstrap `POST /tenants` y `POST /identities` sin JWT

---

## 7. Próximo paso

**15.8 — OpenAPI** (documentar toda la superficie IAM admin).
