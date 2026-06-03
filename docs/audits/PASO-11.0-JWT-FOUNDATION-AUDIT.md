# PASO 11.0 — JWT Foundation (auditoría de ubicación)

**Fecha:** 2026-06-02  
**Alcance:** emisión de access token JWT tras autenticación exitosa — sin validación en filter, refresh, login HTTP.

---

## 1. Ubicación de archivos (decisión)

| Artefacto | Módulo | Paquete | Motivo |
|-----------|--------|---------|--------|
| `TokenProvider` | `identity-access-management` | `application.port.out` | Puerto outbound del caso de uso; sin dependencia JWT |
| `AccessTokenClaims` | IAM | `application.dto` | Entrada al puerto sin tipos de librería JWT |
| `IssuedAccessToken` | IAM | `application.dto` | Salida del puerto (token + metadata) |
| `AuthenticationResponse` | IAM | `application.dto` | Contrato público del `AuthenticateIdentityUseCase` |
| `JwtProperties` | IAM | `infrastructure.security.config` | `@ConfigurationProperties` `security.jwt.*` |
| `JwtTokenProvider` | IAM | `infrastructure.security` | Adaptador que implementa `TokenProvider` |
| Tests `JwtTokenProviderTest` | IAM | `test/.../infrastructure/security` | Adapter aislado |
| Tests `AuthenticateIdentityUseCaseTest` | IAM | `test/.../application` | Mock de `TokenProvider` |

**No ubicar en `platform-security`:** ese módulo no debe depender del bounded context IAM para implementar un puerto definido en IAM (evitar inversión de dependencias entre módulos de negocio y plataforma).

**Validación JWT en `SecurityWebFilterChain`:** PASO 11.2+ (fuera de 11.0).

---

## 2. Dependencias

| Librería | Módulo | Uso |
|----------|--------|-----|
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | IAM | Firma HS256 en `JwtTokenProvider` |
| `spring-boot-starter-validation` | IAM (ya presente) | `@Validated` en properties |

**Excluido:** OAuth2, Authorization Server, Nimbus vía Spring Security OAuth.

---

## 3. Cambios en casos de uso existentes

| Cambio | Detalle |
|--------|---------|
| `AuthenticateIdentityUseCase` | Retorna `Mono<AuthenticationResponse>` |
| `AuthenticateIdentityUseCaseImpl` | Inyecta `TokenProvider`; emite JWT solo tras password + status OK |
| `AuthenticationResult` | Se mantiene sin uso en API pública (legacy 10.9); sustituido por `AuthenticationResponse` en el puerto inbound |
| `IamModuleConfiguration` | `@EnableConfigurationProperties(JwtProperties.class)`; bean use case + `JwtTokenProvider` |

---

## 4. Claims y configuración

| Claim / campo | Valor |
|---------------|-------|
| `sub` | `identityId` (UUID string) |
| `email` | email normalizado |
| `status` | `IdentityStatus.name()` |
| `iss` | `security.jwt.issuer` |
| `iat` / `exp` | calculados en adapter |

**Prohibido en 11.0:** `tenantId`, `roles`, `permissions`, `organizationId`.

---

## 5. Riesgos y mitigaciones

| Riesgo | Mitigación 11.0 |
|--------|------------------|
| Secret débil en local | Default largo en `application.yml`; prod vía `JWT_SECRET` |
| Secret en logs | No loguear token ni secret en adapter |
| Rotación de claves | Fuera de alcance; documentar en 11.x |

---

## 6. Verificación DDD + hexagonal

- Dominio sin imports JWT.
- Application depende de `TokenProvider` (interfaz).
- Infrastructure implementa firma y properties.
- Tests unitarios: use case con mock; adapter con secret de test.
