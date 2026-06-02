# PASO 10.8 — HTTP Adapter RegisterIdentity

**Fecha:** 2026-06-02  
**Estado:** Completado

Auditoría previa: [`PASO-10.8-HTTP-ADAPTER-AUDIT.md`](PASO-10.8-HTTP-ADAPTER-AUDIT.md)

---

## Implementado

| Artefacto | Ubicación |
|-----------|-----------|
| `RegisterIdentityRequest` | `interfaces.http.dto` |
| `RegisterIdentityResponse` | `interfaces.http.dto` |
| `RegisterIdentityController` | `interfaces.http` — `POST /api/v1/identities` |
| `IamHttpExceptionHandler` | 409 / 400 dominio |
| `RegisterIdentityControllerIT` | WebTestClient + Testcontainers |
| `IamHttpIntegrationTestConfiguration` | test support |

## HTTP

| Código | Condición |
|--------|-----------|
| 201 | Registro OK |
| 409 | `IdentityAlreadyExistsException` |
| 400 | Bean Validation o `InvalidDomainValueException` |

## Dependencias (`identity-access-management/build.gradle.kts`)

- `platform-webflux`
- `spring-boot-starter-webflux`
- `spring-boot-starter-validation`

## Verificación

```bash
./gradlew :modules:identity-access-management:test
./gradlew build
```

**Resultado:** BUILD SUCCESSFUL — **18 tests** módulo IAM (incl. 3 `RegisterIdentityControllerIT`).

## Documentación actualizada

- `codecore-specifications/.../api-contracts.md` §4.5
- `codecore-specifications/.../testing-strategy.md` §6.6

## Riesgos pendientes

- `codecore-api` + `platform-security` puede exigir auth en runtime (configurar `permitAll` para registro en paso futuro).
- Path `/api/v1/identities` vs blueprint base `/api/v1/iam` — documentado.

## Fuera de alcance (confirmado)

JWT, login, OAuth, `@PreAuthorize`, error framework genérico, campos de perfil.
