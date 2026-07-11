# PASO 17.5 — Patient Authorization Contract

**Cuatro permisos transversales** definen el lenguaje de autorización del Clinical Foundation — estables, sin verticales, listos para cualquier producto sobre CodeCore.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Auditoría:** [PASO-17.5-…-AUDIT.md](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT-AUDIT.md)  
**Dependencias:** ADR-007 · ADR-012 · PASO-16.3 · PASO-17.4

---

## Contrato sembrado

| Código | Uso |
|--------|-----|
| `patient:create` | Registrar identidad clínica |
| `patient:read` | Consultar / listar |
| `patient:update` | Corregir registro + `activate` |
| `patient:archive` | Soft-retire |

**Ausentes por diseño:** `delete` · `merge` · `restore` · `anonymize` · `export` · `link-identity` · verbos verticales.

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / archive | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform:** ALL **32** · OWNER 32 · ADMIN 29 · MANAGER 16 · USER 5 · READ_ONLY 7.

---

## Artefactos

| Artefacto | Ubicación |
|-----------|-----------|
| `PatientPermissionCatalog` | `patient-contract/.../authorization/` |
| `IamPermissionCatalog` + `SystemRoleTemplate` | IAM (depende de `patient-contract`) |
| Flyway | `V19__seed_patient_authorization_contract.sql` |
| Tests | Catalog · SystemRoleTemplate · PatientAuthorizationSeedMigrationIT |

Patrón: **espejo exacto de PASO 16.3** (insert idempotente + backfill `role_permission`).

---

## Validación Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform  

---

## Fuera de alcance

HTTP · `@RequiresPermission` en controllers · use cases · `PatientReferencePort`

---

## Siguiente paso

**PASO 17.5.1 — Patient Admin API Audit** (antes de HTTP 17.6).

---

## Referencias

- [PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT-AUDIT.md](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT-AUDIT.md)  
- [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md](PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md)  
- [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
