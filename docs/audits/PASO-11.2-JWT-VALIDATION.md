# PASO 11.2 — JWT Validation Foundation

**Fecha:** 2026-06-03  
**Estado:** Implementado — `./gradlew build` OK.

---

## Decisiones arquitectónicas

1. **`TokenValidator`** (puerto outbound) — application sin tipos JJWT.
2. **`JwtTokenValidator`** — misma `JwtProperties` y clave HS256 que `JwtTokenProvider`.
3. **`AuthenticatedPrincipal`** — `identityId`, `email`, `status` (sin tenant/roles).
4. **Excepciones de dominio** — `InvalidTokenException`, `ExpiredTokenException`; JJWT encapsulado en infrastructure.
5. **Sin filtros HTTP** ni `SecurityWebFilterChain` en este paso.

---

## Claims soportados

| Claim | Validación |
|-------|------------|
| `sub` | → `IdentityId` |
| `email` | obligatorio |
| `status` | → `IdentityStatus` |
| `iss` | `requireIssuer` = `security.jwt.issuer` |
| `exp` | rechazo → `ExpiredTokenException` |
| Firma | HS256 con `security.jwt.secret` |

---

## Validaciones realizadas

- Firma HS256
- Issuer esperado
- Expiración (`exp`)
- Claims obligatorios presentes
- Prefijo `Bearer ` opcional al validar
- Token malformado / vacío → `InvalidTokenException`

---

## Tests

| Clase | Alcance |
|-------|---------|
| `JwtTokenValidatorTest` | Válido, expirado, firma, issuer, malformado, blank, Bearer |
| `JwtTokenValidationIT` | Round-trip Provider → Validator |

---

## Riesgos futuros

| Riesgo | Notas |
|--------|-------|
| Sin `tenantId` en JWT | Tenant debe venir de header/contexto en pasos posteriores |
| Sin roles/permissions | Authorization en otro bounded context |
| Rotación de claves | Un solo `secret`; sin JWKS |
| JWKS / OAuth2 | Fuera de alcance |
| Filtro reactivo + `SecurityContext` | PASO 11.3+ |

---

## Verificación hexagonal

- Dominio: excepciones + VOs en `AuthenticatedPrincipal`.
- Application: `TokenValidator`, `AuthenticatedPrincipal`.
- Infrastructure: `JwtTokenValidator` (único uso de JJWT para validar).
