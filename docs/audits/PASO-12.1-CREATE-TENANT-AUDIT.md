# PASO 12.1 — Create Tenant (auditoría previa)

**Fecha:** 2026-06-01

---

## 1. Patrones de referencia (RegisterIdentity)

| Elemento | Ubicación | Patrón |
|----------|-----------|--------|
| Command | `application.command.RegisterIdentityCommand` | Record inmutable |
| Use case port | `application.port.in.RegisterIdentityUseCase` | `Mono<Result> execute(command)` |
| Use case impl | `application.RegisterIdentityUseCaseImpl` | `Mono.defer`, validación, `exists*`, `save`, map DTO |
| Result DTO | `application.dto.RegisterIdentityResult` | VOs de dominio |
| Controller | `interfaces.http.RegisterIdentityController` | `@PostMapping`, `@ResponseStatus(CREATED)` |
| HTTP request | `interfaces.http.dto.RegisterIdentityRequest` | `@NotBlank` / `@NotNull` |
| HTTP response | `interfaces.http.dto.RegisterIdentityResponse` | Mapeo desde result |
| Duplicado | `IdentityAlreadyExistsException` | → 409 en `IamHttpExceptionHandler` |
| Inválido | `InvalidDomainValueException` | → 400 |
| Wiring | `IamModuleConfiguration` | `@Bean` use case |

---

## 2. Unicidad por nombre (V5)

| Capa | Estado actual |
|------|----------------|
| Flyway V5 | `name` NOT NULL, sin `UNIQUE` |
| Repositorio | Sin `existsByName` |

**Decisión 12.1:** añadir `existsByName(TenantName)` en el puerto + adapter, y migración **V6** con `UNIQUE (name)` para garantía en BD (alineado con `uq_iam_user_tenant_normalized_email`).

Comparación de nombres: valor persistido = `TenantName.of(raw)` (trim, case-sensitive).

---

## 3. Ubicación propuesta

| Artefacto | Paquete |
|-----------|---------|
| `TenantAlreadyExistsException` | `domain.exception` |
| `CreateTenantCommand` | `application.command` |
| `CreateTenantResponse` | `application.dto` |
| `CreateTenantUseCase` | `application.port.in` |
| `CreateTenantUseCaseImpl` | `application` |
| `CreateTenantController` | `interfaces.http` |
| `CreateTenantRequest` / `CreateTenantResponse` (HTTP) | `interfaces.http.dto` |
| Flyway V6 | `V6__tenant_name_unique.sql` |

---

## 4. HTTP y seguridad

| Método | Ruta | Seguridad |
|--------|------|-----------|
| `POST` | `/api/v1/tenants` | `permitAll` (bootstrap de tenants) |

Actualizar: `PlatformSecurityAutoConfiguration`, `PublicApiPaths`.

---

## 5. Riesgos (documentar en PASO-12.1)

1. Tenant sin miembros.
2. Sin FK / validación `Identity` ↔ `Tenant`.
3. JWT sin `tenantId`.
4. Tenant Context no implementado.
