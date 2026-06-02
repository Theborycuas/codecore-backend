# PASO 10.7 — Auditoría documentación vs código (pre-implementación)

**Fecha:** 2026-06-02  
**Fuentes:** `codecore-specifications/module-blueprints/identity-access-management/`

---

## Alineado

| Tema | Documentación | Implementación |
|------|---------------|----------------|
| Aggregate `Identity` | `aggregates.md` | `Identity.java` |
| Estados de ciclo de vida | ACTIVE, LOCKED, DISABLED, PENDING_VERIFICATION, PASSWORD_RESET_REQUIRED | `IdentityStatus` enum |
| Entidad `Credential` embebida | `entities.md` | `Credential` en aggregate |
| Unicidad por tenant | `aggregates.md` §4.4 | `UNIQUE (tenant_id, normalized_email)` V2 |
| Email-first auth | `AuthenticateCommand` + alineamiento 10.5 | Sin `Username` en dominio/puerto |
| `IdentityRepository` tenant-aware | `repositories.md` | `existsByTenantAndEmail`, `findByTenantAndEmail` |
| Registro → verificación pendiente | `security-rules` / estados | `PENDING_VERIFICATION` en `RegisterIdentityUseCase` |
| Testcontainers + PostgreSQL | `testing-strategy.md` §6.2 | `R2dbcIdentityRepositoryIT` (parcial) |
| StepVerifier / reactivo | `testing-strategy.md` §7 | Tests existentes |
| Perfil fuera de IAM | `overview.md` §2 | `first_name`/`last_name` eliminados V3 |
| Password nunca en claro persistido | `security-rules`, VOs | `PasswordHasher` + `password_hash` |

---

## No alineado

| Tema | Documentación | Código actual | Severidad |
|------|---------------|---------------|-----------|
| `username` en Identity | `entities.md`, `aggregates.md` (email/username) | Eliminado (10.5.2 email-first) | **Medio** — doc desactualizada |
| `locked` / `enabled` atributos | `entities.md` | Solo `IdentityStatus` | **Medio** — doc desactualizada |
| `findByUsername` / `existsByTenantAndUsername` | `repositories.md` §4.3 | Eliminados del puerto | **Medio** |
| Tabla `iam_user` vs modelo entidad | No documentada en blueprints | V2–V4 Flyway | **Bajo** — doc de persistencia pendiente |
| `email_verified` proyección | No documentada | `EmailVerifiedProjection` + V4 CHECK | **Bajo** |
| `RegisterIdentityUseCase` | No en workflows (no hay workflow “register” explícito) | Implementado 10.6 | **Bajo** — gap doc funcional |
| JWT / Session / Redis IT | `testing-strategy.md` §6.3 obligatorio | No implementado aún | **Esperado** — fuera de 10.7 |
| `CredentialHashingPort` vs `PasswordHasher` | `value-objects` / ports blueprint | Dos puertos coexisten | **Bajo** — deuda 10.6 |

---

## Riesgo

| ID | Descripción | Nivel |
|----|-------------|-------|
| R1 | Doc menciona username; código email-only | Medio |
| R2 | IT de registro E2E no existía antes de 10.7 | Alto → mitigado con `RegisterIdentityUseCaseIT` |
| R3 | AI implementa `findByUsername` por doc vieja | Medio |
| R4 | JWT/session tests marcados MUST en strategy pero no existen | Bajo (futuro) |

---

## Recomendación (corrección documental mínima)

1. **testing-strategy.md** — añadir § implementado (Testcontainers, alcance 10.7).
2. **repositories.md** — nota breve: operaciones username deprecadas; email-first.
3. **entities.md** / **aggregates.md** — actualización mayor; **diferir** a paso de sincronización global (no reescribir en 10.7).

**No modificar** workflows, events, api-contracts en este paso.

---

## Reglas a verificar en IT (10.7)

- Unicidad `tenant_id + normalized_email`
- Mismo email en otro tenant permitido
- Status inicial `PENDING_VERIFICATION` tras registro
- Flyway: schema `iam`, tabla `iam_user`, historial migraciones
- Round-trip use case → repository → PostgreSQL
- Sin mocks en `IdentityRepository` / use case bajo test

---

## Contradicciones dominio ↔ persistencia ↔ use case

| Punto | Estado post 10.5–10.6 |
|-------|------------------------|
| Ciclo de vida | **Resuelto** — solo `status` |
| Email canónico | **Resuelto** — `EmailAddress` ↔ `normalized_email` |
| Verificación email | **Resuelto** — proyección `email_verified`, maestro `status` |
| Username | **Resuelto en código** — doc aún no |

**Conclusión:** Seguro proceder con PASO 10.7. Actualización doc mínima en `testing-strategy.md` + nota en `repositories.md`.
