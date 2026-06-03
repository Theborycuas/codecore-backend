# PASO 11.2 — JWT Validation Foundation (auditoría previa)

**Fecha:** 2026-06-03

---

## 1. Estado actual (11.0 / 11.1)

| Componente | Ubicación | Emisión |
|------------|-----------|---------|
| `TokenProvider` | `application.port.out` | Puerto outbound |
| `JwtTokenProvider` | `infrastructure.security` | HS256, `JwtProperties` |
| `JwtProperties` | `infrastructure.security.config` | `secret`, `issuer`, `expiration` |
| `AuthenticationResponse` | `application.dto` | `accessToken`, `tokenType`, `expiresIn` (HTTP login) |

**Claims emitidos hoy (`JwtTokenProvider`):**

| Claim | Valor |
|-------|-------|
| `sub` | `identityId` (UUID string) |
| `email` | email normalizado |
| `status` | `IdentityStatus.name()` |
| `iss` | `security.jwt.issuer` |
| `iat` / `exp` | ahora + `expiration` |

**No emitidos:** `tenantId`, `roles`, `permissions`.

---

## 2. Validación issuer / expiration hoy

| Validación | En emisión | En validación (11.2) |
|------------|------------|----------------------|
| `exp` | Sí (builder) | Sí (`parseSignedClaims` + `ExpiredJwtException`) |
| `iss` | Sí (builder) | Sí (`requireIssuer` en parser) |
| Firma HS256 | Sí (`signWith`) | Sí (`verifyWith` misma clave) |

Hasta 11.2 **no existía** validación en runtime (solo tests de emisión parsean manualmente con JJWT en `JwtTokenProviderTest`).

---

## 3. Ubicación propuesta (11.2)

| Artefacto | Paquete |
|-----------|---------|
| `TokenValidator` | `application.port.out` |
| `AuthenticatedPrincipal` | `application.dto` |
| `InvalidTokenException` | `domain.exception` |
| `ExpiredTokenException` | `domain.exception` |
| `JwtTokenValidator` | `infrastructure.security` |
| `JwtTokenValidatorTest` | `test/.../infrastructure/security` |
| `JwtTokenValidationIT` | `test/.../infrastructure/security` |

**Sin cambios:** `SecurityWebFilterChain`, `TokenProvider`, login HTTP, filtros.

**Wiring:** `@Component` en `JwtTokenValidator` (mismo patrón que `JwtTokenProvider`).

---

## 4. Mapeo de errores JJWT → dominio

| Condición | Excepción dominio |
|-----------|-----------------|
| Token expirado | `ExpiredTokenException` |
| Firma inválida, issuer, malformado, claims faltantes | `InvalidTokenException` |

JJWT no sale de `infrastructure.security`.

---

## 5. Riesgos documentados (11.2)

1. JWT sin `tenantId` — validación no puede aislar tenant solo por token.
2. Sin roles/permissions en claims.
3. Rotación de claves / JWKS fuera de alcance.
4. Filtro HTTP / `SecurityContext` — paso posterior.
