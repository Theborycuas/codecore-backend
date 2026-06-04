# PASO 12.9 — Tenant Context

**Fecha:** 2026-06-04

---

## 1. Objetivo

Exponer el tenant autenticado durante el ciclo de vida de una request HTTP, sin consultar base de datos ni modificar login, registro, repositorios ni emisión JWT.

---

## 2. Auditoría

Documento completo: [`PASO-12.9-TENANT-CONTEXT-AUDIT.md`](PASO-12.9-TENANT-CONTEXT-AUDIT.md)

---

## 3. Diseño elegido

### 3.1 Comparativa

| Estrategia | WebFlux | Observación |
|------------|---------|-------------|
| ThreadLocal | ❌ | Hilos del pool no correlacionan 1:1 con request; propagación rota en `publishOn` / `subscribeOn` |
| MDC | ❌ | Misma limitación; prohibido como fuente de tenant |
| Spring `SecurityContext` | ⚠️ | No poblado por el filtro JWT actual; modelo paralelo a `AuthenticationContext` |
| Singleton mutable | ❌ | Condiciones de carrera entre requests |
| **Reactor Context + puerto** | ✅ | Mismo patrón que `AuthenticationContext` (PASO 11.x) |

### 3.2 Decisión

- **Puerto:** `TenantContext` (`application.port.out`)
- **Implementación:** `ReactorTenantContext` (`@Component`)
- **Fuente:** `AuthenticationContext` → `AuthenticatedPrincipal.tenantId()` (claim JWT ya validado)
- **Errores:** `TenantContextUnavailableException` con `Reason` (`NOT_AUTHENTICATED`, `TENANT_CLAIM_ABSENT`)

### 3.3 Justificación técnica (no ThreadLocal)

WebFlux ejecuta el pipeline en pocos hilos compartidos; el contexto de request viaja en el `Context` de Reactor asociado al `Mono`/`Flux`, no al hilo. ThreadLocal no se propaga a operadores asíncronos ni a callbacks encadenados, lo que produciría tenant incorrecto o `null` intermitente.

---

## 4. Implementación

### 4.1 Archivos nuevos

| Archivo | Rol |
|---------|-----|
| `application/port/out/TenantContext.java` | Contrato `Mono<TenantId> currentTenant()` |
| `domain/exception/TenantContextUnavailableException.java` | Error explícito (sin null) |
| `interfaces/http/security/ReactorTenantContext.java` | Adaptador Reactor Context |
| `ReactorTenantContextTest.java` | 5 casos (4 de aceptación + legacy validado) |

### 4.2 Sin cambios

Login, registro, `AuthenticateIdentityUseCase`, `RegisterIdentityUseCase`, repositorios, Flyway, `JwtTokenProvider`, `JwtAuthenticationWebFilter`, `MeResponse`.

### 4.3 Flujo completo

```
HTTP Request + Bearer JWT
  → JwtAuthenticationWebFilter
  → JwtTokenValidator → AuthenticatedPrincipal (tenantId opcional en DTO)
  → AuthenticationContext.write(reactorContext, principal)
  → [futuros use cases] TenantContext.currentTenant()
       → ReactorTenantContext
       → Mono<TenantId> o TenantContextUnavailableException
```

Preparación para 13.x:

```
JWT → AuthenticationContext → TenantContext → Application Layer
```

---

## 5. Comportamiento de errores

| Situación | `Reason` | Resultado |
|-----------|----------|-----------|
| Sin principal en Context | `NOT_AUTHENTICATED` | `Mono.error(...)` |
| Principal sin `tenantId` (JWT legado) | `TENANT_CLAIM_ABSENT` | `Mono.error(...)` |
| JWT con `tenantId` válido | — | `Mono.just(TenantId)` |

No se devuelve `null` ni `Optional.empty()` desde `currentTenant()`.

---

## 6. Tests

| # | Caso | Método |
|---|------|--------|
| 1 | JWT válido → tenant recuperable | `shouldResolveTenantWhenJwtContainsTenantId` |
| 2 | JWT sin tenantId → error controlado | `shouldFailWhenJwtHasNoTenantIdClaim` |
| 3 | Request sin autenticación → error controlado | `shouldFailWhenRequestIsNotAuthenticated` |
| 4 | Tenant = claim JWT | `shouldResolveTenantMatchingJwtClaim` |
| + | Token legado validado sin claim | `shouldFailForLegacyTokenValidatedWithoutTenantClaim` |

### Ejecución rápida

```bash
./gradlew :modules:identity-access-management:compileJava :modules:identity-access-management:compileTestJava
./gradlew :modules:identity-access-management:test --tests "com.codecore.iam.interfaces.http.security.ReactorTenantContextTest"
```

Validación final del paso: `./gradlew build`

---

## 7. Criterios de aceptación

| Criterio | Estado |
|----------|--------|
| `TenantContext` obtiene tenant desde JWT (vía principal) | ✅ |
| No consulta BD | ✅ |
| Compatible WebFlux / Reactor | ✅ |
| Sin ThreadLocal / MDC / singleton mutable | ✅ |
| Sin cambios visibles login/registro | ✅ |
| BUILD SUCCESSFUL | Ver §6 |

---

## 8. Referencias

- [`PASO-12.9-TENANT-CONTEXT-AUDIT.md`](PASO-12.9-TENANT-CONTEXT-AUDIT.md)
- [`PASO-12.8-JWT-TENANT-CLAIM.md`](PASO-12.8-JWT-TENANT-CLAIM.md)
