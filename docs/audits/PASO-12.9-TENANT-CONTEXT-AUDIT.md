# PASO 12.9 — Tenant Context (auditoría previa)

**Fecha:** 2026-06-04  
**Alcance:** Auditoría obligatoria antes de implementación.

---

## 1. Artefactos analizados

| Artefacto | Rol |
|-----------|-----|
| `JwtAuthenticationWebFilter` | Valida Bearer JWT; escribe principal en Reactor Context |
| `PlatformSecurityAutoConfiguration` | Cadena WebFlux; inserta filtro JWT en `AUTHENTICATION` |
| `JwtTokenValidator` | JWT → `AuthenticatedPrincipal` (incl. `Optional<TenantId> tenantId`) |
| `AuthenticatedPrincipal` | DTO post-validación |
| `AuthenticationContext` | Acceso request-scoped vía Reactor Context |
| `AuthenticatedPrincipalAuthorizationManager` | Autorización: principal presente en Context |
| `AuthenticationController` | `/me` usa `AuthenticationContext.currentPrincipal()` |
| `JwtTokenProvider` | Emisión JWT (sin cambios en 12.9) |

---

## 2. Preguntas de auditoría

### 1. ¿Dónde queda disponible actualmente el tenantId tras validar el JWT?

Cadena actual:

```
JwtAuthenticationWebFilter
  → TokenValidator.validate(token)
  → AuthenticatedPrincipal.tenantId() : Optional<TenantId>
  → AuthenticationContext.write(reactorContext, principal)
```

El `tenantId` **no** se expone en Spring `SecurityContext`. Vive en:

- `AuthenticatedPrincipal.tenantId()` (campo del DTO)
- Reactor Context bajo clave `codecore.iam.authenticated-principal`

Tokens legados sin claim → `Optional.empty()` (validación sigue OK; sin tenant en contexto).

### 2. ¿Qué objeto representa al usuario autenticado?

**`AuthenticatedPrincipal`** (application DTO):

| Campo | Origen JWT |
|-------|------------|
| `identityId` | `sub` |
| `email` | `email` |
| `status` | `status` |
| `tenantId` | `tenantId` (opcional) |

No se usa `UserDetails`, `JwtAuthenticationToken` ni `ReactiveSecurityContextHolder` para el principal de negocio.

### 3. ¿Existe acceso al Authentication actual?

**Parcialmente.** Spring Security WebFlux está habilitado (`@EnableWebFluxSecurity`), pero el flujo JWT **no** rellena `ReactiveSecurityContext` con el principal CodeCore.

`AuthenticatedPrincipalAuthorizationManager.check(Mono<Authentication> authentication, …)` **ignora** el parámetro `authentication` y lee solo `AuthenticationContext` (Reactor Context).

Conclusión: **no hay** acceso fiable al tenant vía `SecurityContext` hoy.

### 4. ¿Cómo fluye la información en WebFlux?

```
HTTP Request
  → WebFilterChain
  → JwtAuthenticationWebFilter (si ruta no pública)
  → chain.filter(exchange).contextWrite(AuthenticationContext.write)
  → Controller / AuthorizationManager
  → Mono.deferContextual / AuthenticationContext.currentPrincipal()
```

Propagación **implícita** en el mismo `Mono` chain; no hay variable global ni request attribute explícito para tenant.

### 5. ¿Se utiliza Reactor Context?

**Sí.** Patrón corporativo ya establecido en PASO 11.x:

- `AuthenticationContext.CONTEXT_KEY`
- `contextWrite` en filtro
- `Mono.deferContextual` en consumidores

Es el mecanismo correcto para WebFlux.

### 6. ¿Existe algún patrón corporativo ya implementado?

| Patrón | Ubicación | Propósito |
|--------|-----------|-----------|
| Reactor Context para principal | `AuthenticationContext` | Identidad autenticada |
| Puerto `TokenValidator` | application.port.out | Validación JWT |
| Filtro + autorización desacoplados | filter / `AuthenticatedPrincipalAuthorizationManager` | AuthN vs presencia de principal |

**No existe** aún `TenantContext` ni acceso application-level al tenant de la request.

---

## 3. Brecha (12.8 → 12.9)

| Capa | Conoce tenant hoy |
|------|-------------------|
| JWT claim | ✅ |
| `AuthenticatedPrincipal` | ✅ (opcional) |
| Reactor Context | ✅ (dentro del principal) |
| Use cases / repos | ❌ (tenant explícito en comandos) |
| Infraestructura común | ❌ |

---

## 4. Diseño recomendado (resumen)

| Opción | Veredicto |
|--------|-----------|
| ThreadLocal | ❌ incompatible con WebFlux |
| MDC | ❌ prohibido como fuente de tenant |
| Spring `SecurityContext` | ❌ no poblado hoy; acoplaría a modelo paralelo |
| Singleton mutable | ❌ prohibido |
| **Reactor Context + puerto `TenantContext`** | ✅ alinea con `AuthenticationContext` |

Implementación: leer `AuthenticatedPrincipal` desde Context (sin BD), error explícito si falta principal o `tenantId`.

---

## 5. Restricciones confirmadas (12.9)

Sin cambios en: login, registro, JWT emisión, repositorios, Flyway, use cases existentes.

---

## 6. Referencias

- [`PASO-12.8-JWT-TENANT-CLAIM.md`](PASO-12.8-JWT-TENANT-CLAIM.md)
- [`PASO-11.2-JWT-VALIDATION.md`](PASO-11.2-JWT-VALIDATION.md) (si existe)
- `AuthenticationContext.java`, `JwtAuthenticationWebFilter.java`
