# PASO 10.9 — AuthenticateIdentityUseCase

**Fecha:** 2026-06-02  
**Estado:** Implementado y validado (`./gradlew build`, `./gradlew :modules:identity-access-management:test`).

---

## Entregables

| Artefacto | Ubicación |
|-----------|-----------|
| Puerto inbound | `AuthenticateIdentityUseCase` |
| Implementación | `AuthenticateIdentityUseCaseImpl` |
| Command / Result | `AuthenticationCommand`, `AuthenticationResult` |
| Excepciones | `InvalidCredentialsException`, `IdentityNotAllowedToAuthenticateException` |
| Wiring | `IamModuleConfiguration.authenticateIdentityUseCase` |
| Unit tests | `AuthenticateIdentityUseCaseTest` (7 escenarios) |
| Integration tests | `AuthenticateIdentityUseCaseIT` (PostgreSQL + BCrypt real) |

---

## Flujo implementado

1. Validar `tenantId`, email y password no vacíos (`InvalidDomainValueException`).
2. `EmailAddress.of(email)`.
3. `IdentityRepository.findByTenantAndEmail`.
4. Vacío → `InvalidCredentialsException` (mensaje genérico).
5. `status != ACTIVE` → `IdentityNotAllowedToAuthenticateException`.
6. `PasswordHasher.matches` → false → `InvalidCredentialsException`.
7. Éxito → `AuthenticationResult` sin hash ni tokens.

---

## Tests

| Tipo | Clase | Casos |
|------|-------|-------|
| Unit | `AuthenticateIdentityUseCaseTest` | ACTIVE ok, password incorrecta, inexistente, 4 estados no-ACTIVE |
| IT | `AuthenticateIdentityUseCaseIT` | ACTIVE ok, password incorrecta, inexistente, PENDING post-registro, LOCKED/DISABLED/PASSWORD_RESET_REQUIRED |

---

## Fuera de alcance (confirmado)

JWT, refresh token, login HTTP, OAuth2, SecurityContext, UserDetailsService, AuthenticationManager, modificación `SecurityWebFilterChain`, sesiones, eventos de dominio, lockout tracker en use case.

---

## Riesgos pendientes

| Riesgo | Paso futuro |
|--------|-------------|
| Timing attacks (status antes que password) | Endurecer en login HTTP + constant-time policy |
| `AuthenticationNotPermittedException` vs nuevas excepciones | Unificar al exponer HTTP (401/403 mapping) |
| Sin audit de intentos fallidos | Failed Authentication Workflow |
| Login HTTP sin rate limit | PASO 11.2 |

---

## Specs actualizados

- `security-rules.md` §4.5 Authentication Flow
- `workflows.md` §4.6 Authenticate Identity Workflow
- `testing-strategy.md` §6.7 Authentication Integration Tests
