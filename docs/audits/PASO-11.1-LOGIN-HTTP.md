# PASO 11.1 — Login HTTP

**Fecha:** 2026-06-02  
**Estado:** Implementado — `./gradlew build` OK.

---

## Decisiones arquitectónicas

1. **`AuthenticationController` dedicado** en `/api/v1/auth` — sin mezclar con `RegisterIdentityController`.
2. **Puerto inbound** `AuthenticateIdentityUseCase` — sin lógica de negocio ni JWT en el controller.
3. **`LoginRequest`** solo `email` + `password`; **tenant** obligatorio en header `X-Tenant-Id` (multi-tenant sin ampliar body).
4. **Respuesta** `AuthenticationResponse` (application DTO) — `accessToken`, `tokenType`, `expiresIn` emitidos por `TokenProvider` (11.0).
5. **`IdentityNotAllowedToAuthenticateException`** incluye `IdentityStatus` para mapeo HTTP sin jerarquía de excepciones.
6. **`platform-security`** — `POST /api/v1/auth/login` en `permitAll`.

---

## Rutas y DTOs

| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| POST | `/api/v1/auth/login` | Header `X-Tenant-Id` + `LoginRequest` JSON | `AuthenticationResponse` |

```json
// LoginRequest
{ "email": "user@example.com", "password": "********" }

// 200 OK
{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 900 }
```

---

## Códigos HTTP

| Código | Condición |
|--------|-----------|
| 200 | Autenticación exitosa |
| 400 | Validación `@Valid`, header tenant ausente |
| 401 | `InvalidCredentialsException` |
| 403 | Cuenta no autenticable (DISABLED, PENDING_VERIFICATION, PASSWORD_RESET_REQUIRED, etc.) |
| 423 | `IdentityStatus.LOCKED` |

Cuerpos de error vacíos (sin stack, sin nombres de clase).

---

## Tests

| Clase | Tipo |
|-------|------|
| `AuthenticationControllerTest` | Unit (`@WebFluxTest`, use case mock) |
| `AuthenticationControllerIT` | HTTP + PostgreSQL real + JWT real |

---

## Riesgos pendientes

| Riesgo | Paso futuro |
|--------|-------------|
| Tenant solo por header | Documentar en API pública; alternativa query/path |
| PENDING_VERIFICATION → 403 genérico | Endpoint verify-email |
| Rate limiting login | 11.2+ / API gateway |
| Refresh token / logout | 11.1 refresh / workflows |

---

## Verificación hexagonal

- Controller → `AuthenticateIdentityUseCase` (puerto in).
- JWT solo en `JwtTokenProvider` vía use case.
- Sin `AuthenticationManager`, OAuth2, `UserDetailsService`.
