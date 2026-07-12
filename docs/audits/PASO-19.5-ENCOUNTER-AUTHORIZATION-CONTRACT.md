# PASO 19.5 — Encounter Authorization Contract

**Cuatro permisos transversales** definen el lenguaje de autorización de Clinical Records — estables, sin verticales, listos para cualquier producto sobre CodeCore.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Auditoría:** [PASO-19.5-…-AUDIT.md](PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT-AUDIT.md)  
**Dependencias:** ADR-007 · ADR-015 · PASO-18.5 · PASO-19.4

---

## Contrato sembrado

| Código | Uso |
|--------|-----|
| `encounter:create` | Abrir / registrar episodio de atención ocurrido |
| `encounter:read` | Consultar / listar |
| `encounter:update` | Ajustar refs / tiempo / **complete** |
| `encounter:cancel` | Anular episodio |

**Ausentes por diseño:** `delete` · `complete` (dedicado) · `restore` · `note` · `soap` · verbos verticales (`surgery`, `hygiene`, `odontogram`, …).

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / cancel | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform:** ALL **40** · OWNER 40 · ADMIN 37 · MANAGER 24 · USER 7 · READ_ONLY 9.

---

## Artefactos

| Artefacto | Ubicación |
|-----------|-----------|
| `EncounterPermissionCatalog` | `encounter-contract/.../authorization/` |
| `IamPermissionCatalog` + `SystemRoleTemplate` | IAM (depende de `encounter-contract`) |
| Flyway | `V23__seed_encounter_authorization_contract.sql` |
| Tests | Catalog · SystemRoleTemplate · EncounterAuthorizationSeedMigrationIT |

Patrón: **espejo exacto de PASO 18.5 / 17.5** (insert idempotente + backfill `role_permission`).

---

## Validación Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform  

---

## Fuera de alcance

HTTP · `@RequiresPermission` en controllers · use cases · `EncounterReferencePort`

---

## Siguiente paso

**PASO 19.5.1 — Encounter Admin API Audit** — ✅ [PASO-19.5.1](PASO-19.5.1-ENCOUNTER-ADMINISTRATION-API-AUDIT.md). Siguiente: **19.6 Administration API**.

---

## Referencias

- [PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT-AUDIT.md](PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT-AUDIT.md)  
- [PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT.md](PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT.md)  
- [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md) · [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- Migración: `V23__seed_encounter_authorization_contract.sql`  
