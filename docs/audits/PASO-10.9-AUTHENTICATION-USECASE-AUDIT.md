# PASO 10.9 — AuthenticateIdentityUseCase (auditoría previa)

**Fecha:** 2026-06-02  
**Alcance:** solo capa application — sin JWT, HTTP, SecurityWebFilterChain, sesiones.

---

## 1. Alineamiento specs ↔ código existente

| Spec | Estado previo | Notas |
|------|---------------|-------|
| `security-rules.md` §4.2 | Alineado | Estados no autenticables: LOCKED, DISABLED, PENDING_VERIFICATION, PASSWORD_RESET_REQUIRED |
| `workflows.md` §4 | Parcial | Flujo completo incluye JWT/sesión — PASO 10.9 recorta a credential + status |
| `repositories.md` | Alineado | `findByTenantAndEmail` ya implementado |
| `aggregates.md` / `Identity` | Alineado | `validateAuthenticationEligibility()` existe; use case validará `ACTIVE` explícitamente |
| `testing-strategy.md` | Gap | Sin IT de autenticación — se añade en 10.9 |

---

## 2. Clases a crear

| Clase | Paquete |
|-------|---------|
| `AuthenticateIdentityUseCase` | `application.port.in` |
| `AuthenticateIdentityUseCaseImpl` | `application` |
| `AuthenticationCommand` | `application.command` |
| `AuthenticationResult` | `application.dto` |
| `InvalidCredentialsException` | `domain.exception` |
| `IdentityNotAllowedToAuthenticateException` | `domain.exception` |
| `AuthenticateIdentityUseCaseTest` | `test` |
| `AuthenticateIdentityUseCaseIT` | `test` |

**Reutilización:** `IdentityRepository`, `PasswordHasher`, `EmailAddress`, `InvalidDomainValueException`, `IamModuleConfiguration`.

**No crear:** JWT, tokens, `LoginAttemptTracker` en este paso, HTTP adapter, mapeo de `AuthenticationNotPermittedException` (use case usa excepciones nuevas según requisito).

---

## 3. Flujo acordado

```
AuthenticationCommand
  → validar tenantId / email / password (blank)
  → EmailAddress.of(email)
  → IdentityRepository.findByTenantAndEmail
  → vacío → InvalidCredentialsException (anti-enumeración)
  → status != ACTIVE → IdentityNotAllowedToAuthenticateException
  → PasswordHasher.matches → false → InvalidCredentialsException
  → AuthenticationResult (identityId, tenantId, email, status)
```

---

## 4. Riesgos detectados

| Riesgo | Mitigación |
|--------|------------|
| Enumeración de cuentas | Mismo mensaje/excepción para inexistente y password incorrecta |
| Exposición de hash | `AuthenticationResult` sin credential |
| IT con hash fake en repo tests | IT usa `BCryptPasswordHasher` real al persistir |
| Duplicar `AuthenticationNotPermittedException` | Excepciones dedicadas para contrato del use case (tests 4–7) |

---

## 5. Fuera de alcance (confirmado)

JWT, Refresh Token, Login HTTP, OAuth2, SecurityContext, UserDetailsService, AuthenticationManager, modificación `SecurityWebFilterChain`, eventos, audit trail, lockout counter.
