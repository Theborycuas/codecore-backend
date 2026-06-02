# PASO 10.7 — Persistence Integration Tests (entregable)

**Fecha:** 2026-06-02  
**Estado:** Completado — `BUILD SUCCESSFUL`

Auditoría previa: [`PASO-10.7-PERSISTENCE-INTEGRATION-TESTS-AUDIT.md`](PASO-10.7-PERSISTENCE-INTEGRATION-TESTS-AUDIT.md)

---

## 1. Auditoría (resumen)

Ver documento de auditoría. Conclusión: **seguro implementar 10.7** con actualización doc mínima.

---

## 2. Cambios en documentación (specifications)

| Archivo | Cambio |
|---------|--------|
| `testing-strategy.md` | §6.5 — Testcontainers, clases IT, reglas verificadas |
| `repositories.md` | Nota email-first; username ops no implementadas |

**No modificados:** `aggregates.md`, `entities.md`, `overview.md` (desfase conocido; paso futuro).

---

## 3. Dependencias

Sin nuevas dependencias Gradle (ya existían `testcontainers`, `flyway`, `postgresql`, `spring-boot-starter-test`).

---

## 4. Clases creadas / refactorizadas

| Clase | Acción |
|-------|--------|
| `AbstractPostgresIntegrationTest` | **Creada** — container + Flyway + R2DBC props |
| `RegisterIdentityUseCaseIT` | **Creada** — E2E use case + PostgreSQL |
| `R2dbcIdentityRepositoryIT` | **Refactor** — extiende abstract (sin duplicar container) |

---

## 5. Tests implementados (`RegisterIdentityUseCaseIT`)

| Test | Valida |
|------|--------|
| `shouldRegisterIdentityAndPersistRow` | Registro OK + fila JDBC + Flyway/schema/tabla |
| `shouldRejectDuplicateEmailInSameTenant` | `IdentityAlreadyExistsException` |
| `shouldAllowSameEmailInDifferentTenants` | Multi-tenant |
| `shouldRoundTripThroughRepositoryAfterRegistration` | save → load (StepVerifier, sin `.block()`) |

---

## 6. Build y tests

```bash
./gradlew :modules:identity-access-management:test
./gradlew build
```

**Resultado:** BUILD SUCCESSFUL (15 tests módulo IAM, incl. unit + IT).

---

## 7. Riesgos pendientes

| Riesgo | Nivel |
|--------|-------|
| Blueprints `entities.md` / `aggregates.md` con username, locked, enabled | Medio |
| Session/JWT/Redis IT aún no implementados | Esperado |
| Unificar `PasswordHasher` / `CredentialHashingPort` | Bajo |

---

## 8. Fuera de alcance (confirmado)

Controllers, JWT, Kafka, mocks de repositorio, H2, PostgreSQL local manual, frameworks de test propios.
