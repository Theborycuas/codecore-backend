# PASO 12.8 — JWT Tenant Claim (auditoría previa)

**Fecha:** 2026-06-03  
**Alcance:** Auditoría obligatoria antes de implementación.

---

## 1. Artefactos analizados

| Artefacto | Rol |
|-----------|-----|
| `JwtTokenProvider` | Emisión JWT (HS256, JJWT) — implementa `TokenProvider` |
| `JwtTokenValidator` | Validación JWT — implementa `TokenValidator` |
| `JwtProperties` | `secret`, `issuer`, `expiration` (`security.jwt.*`) |
| `AccessTokenClaims` | DTO application para emisión (sin tipos JWT) |
| `AuthenticatedPrincipal` | DTO post-validación |
| `AuthenticateIdentityUseCaseImpl` | Construye `AccessTokenClaims` tras login |
| `JwtAuthenticationWebFilter` | Valida Bearer → `AuthenticatedPrincipal` en Reactor Context |
| `PlatformSecurityAutoConfiguration` | Security WebFlux (no JWT claims) |
| `AuthenticatedPrincipalAuthorizationManager` | Solo comprueba presencia de principal |

**No existe** `SecurityConfig` dedicado en IAM; seguridad HTTP vive en `platform-security`.

---

## 2. Preguntas de auditoría

### 1. ¿Dónde se construye actualmente el JWT?

**Único punto de emisión:** `JwtTokenProvider.generateAccessToken(AccessTokenClaims)`.

Invocado desde `AuthenticateIdentityUseCaseImpl` tras validar credenciales y membership ACTIVE.

```40:48:codecore-backend/modules/identity-access-management/src/main/java/com/codecore/iam/infrastructure/security/JwtTokenProvider.java
        String accessToken = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(claims.subject())
                .claim("email", claims.email())
                .claim("status", claims.status())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
```

### 2. ¿Qué claims existen hoy?

| Claim | Tipo JWT | Origen |
|-------|----------|--------|
| `iss` | issuer | `JwtProperties.issuer` |
| `sub` | subject | `identityId` (UUID string) |
| `email` | custom | email normalizado |
| `status` | custom | `IdentityStatus.name()` |
| `iat` / `exp` | estándar | expiración configurada |

**No existen hoy:** `tenantId`, `roles`, `permissions`, `aud`.

### 3. ¿Cómo se valida el JWT?

`JwtTokenValidator.validate(String)`:

1. Normaliza prefijo `Bearer `
2. `Jwts.parser().verifyWith(signingKey).requireIssuer(...).parseSignedClaims`
3. Mapea a `AuthenticatedPrincipal` en `toPrincipal(Claims)`

Errores → `InvalidTokenException` / `ExpiredTokenException`.

Filtro HTTP: `JwtAuthenticationWebFilter` delega en `TokenValidator` (sin lógica extra de claims).

### 4. ¿Los claims se copian íntegramente durante validación?

**No.** Solo se extraen **`sub`, `email`, `status`** — campos obligatorios en `toPrincipal`. Claims adicionales en el JWT se ignoran (no fallan).

Implicación 12.8: hay que **mapear explícitamente** `tenantId` en `toPrincipal` si debe sobrevivir validación.

### 5. ¿Existe DTO intermedio para claims?

**Sí:**

| DTO | Dirección |
|-----|-----------|
| `AccessTokenClaims` | Emisión (application → infrastructure) |
| `AuthenticatedPrincipal` | Post-validación (infrastructure → application/HTTP) |

No hay DTO intermedio “JWT payload” en dominio; acoplamiento JJWT limitado a infrastructure.

### 6. ¿Hay riesgo de romper tokens existentes?

| Escenario | Riesgo |
|-----------|--------|
| Tokens emitidos **antes** de 12.8 (sin `tenantId`) | **Bajo** si validación **no exige** `tenantId` |
| Tokens emitidos **después** de 12.8 | Incluirán `tenantId` |
| Cambiar `AccessTokenClaims` (nuevo campo) | Solo afecta emisión; validación backward-compatible si `tenantId` opcional |
| `MeResponse` / login HTTP | Sin cambio de contrato si no se expone `tenantId` en API |
| Filtros / autorización | Sin cambio si no se usa `tenantId` en authz |

**Estrategia de transición:** `tenantId` **opcional** en validación → `AuthenticatedPrincipal.tenantId()` como `Optional<TenantId>`.

---

## 3. Flujo login actual (sin tenant en JWT)

```
AuthenticationCommand(tenantId, email, password)
  → findByTenantAndEmail(tenantId, email)
  → password + membership ACTIVE
  → AccessTokenClaims(subject, email, status)   ← tenantId NO incluido
  → JwtTokenProvider → JWT
  → AuthenticationResponse (token only)
```

El `tenantId` del comando **se pierde** tras emitir el token.

---

## 4. Cambios propuestos (Fase 2)

| Artefacto | Cambio |
|-----------|--------|
| `AccessTokenClaims` | + `String tenantId` |
| `JwtTokenProvider` | + `.claim("tenantId", ...)` |
| `JwtTokenValidator` | Mapear `tenantId` opcional → `AuthenticatedPrincipal` |
| `AuthenticatedPrincipal` | + `Optional<TenantId> tenantId()` |
| `AuthenticateIdentityUseCaseImpl` | Pasar `command.tenantId()` al emitir (sin recalcular) |

**Sin cambiar:** filtros HTTP, autorización, `MeResponse`, registro, Flyway, membership repos.

---

## 5. Tests propuestos

| Caso | Clase |
|------|-------|
| Login → JWT contiene `tenantId` | `AuthenticateIdentityUseCaseIT` / `JwtTokenProviderTest` |
| Validación → `tenantId` recuperable | `JwtTokenValidatorTest` / `JwtTokenValidationIT` |
| Token legado sin `tenantId` válido | `JwtTokenValidatorTest` |
| `tenantId` emitido = tenant autenticado | `AuthenticateIdentityUseCaseTest` / IT |

---

## 6. Decisión

Proceder con Fase 2: enriquecer JWT en **único punto** (`JwtTokenProvider` + input `AccessTokenClaims`), validación **backward-compatible**, `tenantId` disponible en `AuthenticatedPrincipal` sin Tenant Context.

---

## 7. Referencias

- `PASO-11.0-JWT-FOUNDATION.md`
- `PASO-11.2-JWT-VALIDATION.md`
- `PASO-12.6-TRANSACTIONAL-CONSISTENCY-AUDIT.md` (JWT sin tenantId — deuda resuelta en 12.8)
