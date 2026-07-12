# PASO 18.5 — Appointment Authorization Contract

**Cuatro permisos transversales** definen el lenguaje de autorización de Scheduling — estables, sin verticales, listos para cualquier producto sobre CodeCore.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Auditoría:** [PASO-18.5-…-AUDIT.md](PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT-AUDIT.md)  
**Dependencias:** ADR-007 · ADR-014 · PASO-17.5 · PASO-18.4

---

## Contrato sembrado

| Código | Uso |
|--------|-----|
| `appointment:create` | Crear compromiso planificado |
| `appointment:read` | Consultar / listar |
| `appointment:update` | Reschedule / reasignar refs / **complete** |
| `appointment:cancel` | Anular compromiso |

**Ausentes por diseño:** `delete` · `complete` (dedicado) · `restore` · `slot` · `availability` · verbos verticales (`chair`, `surgery`, …).

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / cancel | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform:** ALL **36** · OWNER 36 · ADMIN 33 · MANAGER 20 · USER 6 · READ_ONLY 8.

---

## Artefactos

| Artefacto | Ubicación |
|-----------|-----------|
| `AppointmentPermissionCatalog` | `appointment-contract/.../authorization/` |
| `IamPermissionCatalog` + `SystemRoleTemplate` | IAM (depende de `appointment-contract`) |
| Flyway | `V21__seed_appointment_authorization_contract.sql` |
| Tests | Catalog · SystemRoleTemplate · AppointmentAuthorizationSeedMigrationIT |

Patrón: **espejo exacto de PASO 17.5 / 16.3** (insert idempotente + backfill `role_permission`).

---

## Validación Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform  

---

## Fuera de alcance

HTTP · `@RequiresPermission` en controllers · use cases · `AppointmentReferencePort`

---

## Siguiente paso

**PASO 18.5.1 — Appointment Admin API Audit** — ✅ [PASO-18.5.1](PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md). Siguiente: **18.6 Administration API**.

---

## Referencias

- [PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT-AUDIT.md](PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT-AUDIT.md)  
- [PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md)  
- [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
