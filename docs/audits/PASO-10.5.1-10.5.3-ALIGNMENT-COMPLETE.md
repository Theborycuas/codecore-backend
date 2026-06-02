# PASO 10.5.1 – 10.5.3 — Alineamiento dominio ↔ persistencia (completado)

**Fecha:** 2026-06-02  
**Estado:** Implementado. `build` OK. Flyway V3 OK. `bootRun` OK (puerto alternativo si 8080 ocupado).

---

## Cambios realizados

### Fase 1 — `IdentityStatus` única fuente de verdad
- Eliminados `enabled`, `locked` del aggregate.
- Métodos de dominio y validación usan solo `IdentityStatus`.
- Añadido `markEmailVerified()` (transición `PENDING_VERIFICATION` → `ACTIVE`).

### Fase 2 — Email-first
- Eliminado `Username` VO y campo en `Identity`.
- Eliminados `findByTenantAndUsername` / `existsByTenantAndUsername` del puerto y adapter.
- Eliminadas queries SQL `regexp_replace` / `deriveUsername`.

### Fase 3 — V3 Flyway
- `V3__cleanup_iam_user.sql`: `DROP first_name, last_name`.
- Seguro: columnas siempre null en runtime; perfil → user-management.

### Fase 4 — `email_verified` (proyección, no maestro)
- **Maestro:** `IdentityStatus` / columna `status`.
- **Proyección:** `email_verified` — fórmula en `EmailVerifiedProjection`, CHECK en BD (V4).
- Entidad: `getEmailVerifiedProjection()` / `setEmailVerifiedProjection()` (no `isEmailVerified()`).
- Mapper: escribe vía `EmailVerifiedProjection`; `toDomain` no lee la columna.
- Dominio: `isEmailVerified()` solo como vista de `status()` — documentado en JavaDoc.

### Fase 5 — Mapper isomórfico
- Sin derivaciones artificiales.
- `email` y `normalized_email` = `EmailAddress.value()`.
- Round-trip de status y `email_verified` probado en IT.

### Fase 6 — Multi-módulo R2DBC
- Removido `spring.r2dbc.properties.schema: iam` global.
- `@Table(schema = "iam", name = "iam_user")` en entidad.

---

## Verificación

```bash
./gradlew build
./gradlew :modules:identity-access-management:test
./gradlew :apps:codecore-api:bootRun --args="--server.port=8081"
```

Flyway log: `Successfully applied ... version "3 - cleanup iam user"`.

---

## Riesgos pendientes (conscientes)

- Metadatos `Credential` (`passwordChangedAt`, etc.) no persistidos — V4.
- `CredentialId` = `IdentityId` (1:1 embebido) — documentado.
- Datos legacy con `email_verified` incoherente con `status` — corregidos en próximo `save`.
