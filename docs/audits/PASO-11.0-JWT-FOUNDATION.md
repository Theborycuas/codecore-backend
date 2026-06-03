# PASO 11.0 — JWT Foundation

**Fecha:** 2026-06-02  
**Estado:** Implementado — `./gradlew build` OK.

---

## Decisiones arquitectónicas

1. **Puerto `TokenProvider` en application.port.out** — el caso de uso no conoce JJWT ni HS256.
2. **`JwtTokenProvider` en IAM infrastructure** — evita que `platform-security` dependa de IAM para implementar un puerto del bounded context.
3. **`JwtProperties` con `@ConfigurationProperties("security.jwt")`** — secret, issuer, expiration externalizados en `application.yml`.
4. **`AuthenticationResponse`** sustituye la salida pública de autenticación (`accessToken`, `tokenType`, `expiresIn`); `AuthenticationResult` (10.9) queda sin uso en el puerto inbound.
5. **Claims mínimos** — `sub`, `email`, `status`, `iss`, `iat`, `exp`; sin `tenantId`, roles ni permissions (PASO 11.2+ / authorization).

---

## Entregables

| Artefacto | Ubicación |
|-----------|-----------|
| `TokenProvider` | `application.port.out` |
| `AccessTokenClaims`, `IssuedAccessToken`, `AuthenticationResponse` | `application.dto` |
| `JwtProperties` | `infrastructure.security.config` |
| `JwtTokenProvider` | `infrastructure.security` |
| `IamAuthenticationConfiguration` | `configuration` — beans JWT + `AuthenticateIdentityUseCase` (separado de `IamModuleConfiguration` para IT slices) |
| Tests | `JwtTokenProviderTest`, `AuthenticateIdentityUseCaseTest` (actualizado), `AuthenticateIdentityUseCaseIT` (JWT real) |

---

## Configuración runtime

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:...}
    issuer: codecore-api
    expiration: 900s
```

---

## Verificación hexagonal

| Capa | JWT |
|------|-----|
| Domain | Sin dependencias |
| Application | Solo `TokenProvider` + DTOs |
| Infrastructure | JJWT + properties |

---

## Fuera de alcance (11.0)

Validación JWT en filter, refresh token, login HTTP, OAuth2, `AuthenticationManager`, `UserDetailsService`, tenant en claims.

---

## Riesgos pendientes

- Rotación de secret / JWKS — futuro
- Validación en `SecurityWebFilterChain` — PASO 11.2+
- `tenantId` en claims — decisión explícita post multi-tenant JWT
