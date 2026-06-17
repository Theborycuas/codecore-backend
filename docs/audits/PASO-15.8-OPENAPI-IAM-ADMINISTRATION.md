# PASO 15.8 — OpenAPI (IAM Administration)

**Fecha:** 2026-06-17  
**Alcance:** Contrato OpenAPI 3 para toda la superficie `/api/v1/iam/**` (ADR-008 §7).  
**Prerequisitos:** PASO 15.0–15.7 (controllers y DTOs estables).

---

## 1. Resumen ejecutivo

| Ítem | Estado |
|------|--------|
| springdoc-openapi (WebFlux) | ✅ |
| Grupo `iam-administration` | ✅ |
| Esquema seguridad JWT Bearer | ✅ |
| Extensión `x-permission` por operación | ✅ |
| Swagger UI (dev) | ✅ |
| Deshabilitado en `prod` | ✅ |
| Tests contrato OpenAPI | ✅ 5 tests |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| Librería | `springdoc-openapi-starter-webflux` 2.8.9 |
| Generación | Runtime desde controllers + DTOs (no YAML manual) |
| Alcance documentado | Solo `/api/v1/iam/**` (grupo dedicado) |
| Permisos RBAC | `GlobalOperationCustomizer` lee `@RequiresPermission` → `x-permission` |
| Auth en contrato | `bearer-jwt` global; `X-Tenant-Id` como esquema API key opcional |
| Swagger UI | Público en dev (`permitAll`); deshabilitado en `application-prod.yml` |
| Bootstrap/auth legacy | Fuera del grupo IAM (no documentados en 15.8) |

---

## 3. Endpoints de documentación

| Recurso | URL (dev) |
|---------|-----------|
| OpenAPI JSON (grupo IAM) | `GET /v3/api-docs/iam-administration` |
| Índice grupos | `GET /v3/api-docs` |
| Swagger UI | `GET /swagger-ui.html` |

### Exportar contrato

```bash
curl -s http://localhost:8080/v3/api-docs/iam-administration > iam-administration-openapi.json
```

---

## 4. Superficie documentada (22 operaciones)

| Tag | Rutas |
|-----|-------|
| Administration | `GET /administration/status` |
| Users | CRUD `/users`, `/users/{id}` |
| Memberships | CRUD `/memberships`, `/memberships/{id}` |
| Membership Roles | `GET/PUT /memberships/{membershipId}/roles` |
| Roles | CRUD `/roles`, `/roles/{id}` |
| Role Permissions | `GET/PUT /roles/{roleId}/permissions` |
| Permissions | `GET /permissions`, `/permissions/{id}` |
| Tenants | `GET/PUT /tenants/current` |

Cada operación incluye extensión `x-permission` con el código V13 requerido.

---

## 5. Archivos principales

| Área | Archivos |
|------|----------|
| Config OpenAPI | `IamOpenApiConfiguration.java` |
| Customizer RBAC | `RequiresPermissionOperationCustomizer.java` (`GlobalOperationCustomizer`) |
| Tags HTTP | `@Tag` en los 8 controllers admin |
| Dependencias | `libs.versions.toml`, `identity-access-management/build.gradle.kts`, `codecore-api/build.gradle.kts` |
| Seguridad UI | `PlatformSecurityAutoConfiguration`, `PublicApiPaths` |
| Properties | `application.yml`, `application-prod.yml` |
| Tests | `IamAdministrationOpenApiTest`, `RequiresPermissionOperationCustomizerTest` |

---

## 6. Tests ejecutados (solo 15.8)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.interfaces.http.openapi.*"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `IamAdministrationOpenApiTest` | 4 | ✅ |
| `RequiresPermissionOperationCustomizerTest` | 1 | ✅ |

---

## 7. Próximo paso

**15.9 — IAM Administration Verification** (E2E HTTP completo, cierre FASE 15).
