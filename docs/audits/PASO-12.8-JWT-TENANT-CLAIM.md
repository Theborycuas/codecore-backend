# PASO 12.8 — JWT Tenant Claim

**Fecha:** 2026-06-03

---

## 1. Objetivo

Incluir `tenantId` como claim del JWT emitido en login, preservarlo en validación y mantener compatibilidad con tokens legados sin ese claim.

**Fuera de alcance:** Tenant Context, autorización multi-tenant, cambios en filtros HTTP, login/registro HTTP, eliminación de `iam_user.tenant_id`.

---

## 2. Auditoría previa

Documento completo: [`PASO-12.8-JWT-TENANT-CLAIM-AUDIT.md`](PASO-12.8-JWT-TENANT-CLAIM-AUDIT.md)

### Respuestas resumidas

| Pregunta | Respuesta |
|----------|-----------|
| ¿Dónde se construye el JWT? | `JwtTokenProvider.generateAccessToken()` |
| ¿Qué claims existían? | `iss`, `sub`, `email`, `status`, `iat`, `exp` |
| ¿Cómo se valida? | `JwtTokenValidator` → `AuthenticatedPrincipal` |
| ¿Se copian todos los claims? | No; solo `sub`, `email`, `status` (ahora también `tenantId` opcional) |
| ¿DTO intermedio? | `AccessTokenClaims` (emisión), `AuthenticatedPrincipal` (post-validación) |
| ¿Riesgo tokens existentes? | Bajo si `tenantId` es opcional en validación |

---

## 3. Implementación

### 3.1 Claims antes / después

**Antes (12.7):**

```json
{
  "iss": "codecore",
  "sub": "<identity-uuid>",
  "email": "user@example.com",
  "status": "ACTIVE",
  "iat": 1717430400,
  "exp": 1717431300
}
```

**Después (12.8):**

```json
{
  "iss": "codecore",
  "sub": "<identity-uuid>",
  "email": "user@example.com",
  "status": "ACTIVE",
  "tenantId": "<tenant-uuid>",
  "iat": 1717430400,
  "exp": 1717431300
}
```

Sin cambios en: expiración, issuer, subject, email, status. No se añadieron `roles`, `aud` ni `permissions`.

### 3.2 Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `AccessTokenClaims` | Campo `String tenantId` |
| `JwtTokenProvider` | `.claim("tenantId", claims.tenantId())`; rechaza emisión si `tenantId` blank |
| `JwtTokenValidator` | `parseTenantId()` → `Optional<TenantId>` en `AuthenticatedPrincipal` |
| `AuthenticatedPrincipal` | Campo `Optional<TenantId> tenantId` |
| `AuthenticateIdentityUseCaseImpl` | Pasa `tenantId.value().toString()` del tenant autenticado (sin recalcular ni consultar otra tabla) |

**Sin modificar:** `JwtAuthenticationWebFilter`, `AuthenticatedPrincipalAuthorizationManager`, `MeResponse`, `AuthResponse`, `MembershipRepository`, Flyway, registro.

### 3.3 Flujo login (12.8)

```
AuthenticationCommand(tenantId, email, password)
  → findByTenantAndEmail(tenantId, email)
  → password + membership ACTIVE
  → AccessTokenClaims(subject, email, status, tenantId)   ← tenant del comando
  → JwtTokenProvider → JWT con claim tenantId
  → AuthenticationResponse (sin cambio de contrato HTTP)
```

### 3.4 Flujo validación

```
Bearer token
  → JwtTokenValidator.validate()
  → AuthenticatedPrincipal(identityId, email, status, Optional<tenantId>)
  → JwtAuthenticationWebFilter → Reactor Context (sin cambio de filtro)
```

---

## 4. Compatibilidad con tokens legados

| Situación | Comportamiento |
|-----------|----------------|
| Token **sin** claim `tenantId` | Autenticación **válida**; `AuthenticatedPrincipal.tenantId()` → `Optional.empty()` |
| Token **con** `tenantId` blank/null | Tratado como ausente → `Optional.empty()` |
| Token **con** `tenantId` UUID válido | `Optional.of(TenantId)` |
| Emisión nueva | `tenantId` obligatorio en `AccessTokenClaims` (login siempre conoce el tenant) |

**Estrategia de transición:** tokens emitidos antes de 12.8 siguen funcionando hasta expirar. Nuevos tokens incluyen `tenantId`. Consumidores que necesiten tenant deben comprobar `tenantId().isPresent()` hasta que todos los tokens activos hayan rotado.

---

## 5. Tests

### 5.1 Casos de aceptación

| # | Caso | Clase / método |
|---|------|----------------|
| 1 | Login exitoso → JWT contiene `tenantId` | `AuthenticateIdentityUseCaseIT` — parseo JJWT del token emitido |
| 2 | JWT validado → `tenantId` recuperable | `JwtTokenValidatorTest.shouldValidateValidToken`, `JwtTokenValidationIT.shouldRoundTripIssuedTokenToAuthenticatedPrincipal` |
| 3 | Token legado sin `tenantId` sigue válido | `JwtTokenValidatorTest.shouldValidateLegacyTokenWithoutTenantId`, `JwtAuthenticationWebFilterTest.shouldAcceptLegacyTokenWithoutTenantIdClaim` |
| 4 | `tenantId` emitido = tenant autenticado | `AuthenticateIdentityUseCaseTest.shouldAuthenticateActiveIdentityAndIssueAccessToken`, `JwtTokenProviderTest.shouldIssueBearerTokenWithRequiredClaims` |

### 5.2 Ejecución

Validación rápida (solo JWT, sin Testcontainers):

```bash
./gradlew :modules:identity-access-management:test --tests "com.codecore.iam.infrastructure.security.JwtToken*" --tests "com.codecore.iam.application.AuthenticateIdentityUseCaseTest" --tests "com.codecore.iam.interfaces.http.security.JwtAuthenticationWebFilterTest"
```

Validación final del paso:

```bash
./gradlew build
```

### 5.3 Lock `output.bin` en Windows

Si `./gradlew build` falla con `Unable to delete ... output.bin`:

1. `./gradlew --stop` (libera daemon que mantiene el archivo abierto)
2. Borrar `modules/identity-access-management/build/test-results`
3. Reintentar `./gradlew build`

**Fix permanente (buildSrc):** el hook `beforeTask` en `codecore.java-conventions` ahora usa `testTask.project` (antes apuntaba al proyecto raíz y no limpiaba el submódulo IAM).

---

## 6. Criterios de aceptación

| Criterio | Estado |
|----------|--------|
| JWT emitido contiene `tenantId` | ✅ |
| Validación preserva `tenantId` en `AuthenticatedPrincipal` | ✅ |
| Tokens antiguos sin `tenantId` siguen válidos | ✅ |
| BUILD SUCCESSFUL | `./gradlew build` tras liberar lock (ver §4) |
| Sin cambios visibles en login/registro HTTP | ✅ |

---

## 7. Referencias

- [`PASO-12.8-JWT-TENANT-CLAIM-AUDIT.md`](PASO-12.8-JWT-TENANT-CLAIM-AUDIT.md)
- [`PASO-12.7-TRANSACTIONAL-REGISTRATION.md`](PASO-12.7-TRANSACTIONAL-REGISTRATION.md)
- [`PASO-12.6-TRANSACTIONAL-CONSISTENCY-AUDIT.md`](PASO-12.6-TRANSACTIONAL-CONSISTENCY-AUDIT.md)
