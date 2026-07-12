# PASO 20.5 — Item Authorization Contract

**Cuatro permisos transversales** definen el lenguaje de autorización de Inventory (Item) — estables, sin verticales, listos para cualquier producto sobre CodeCore.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Auditoría:** [PASO-20.5-…-AUDIT.md](PASO-20.5-ITEM-AUTHORIZATION-CONTRACT-AUDIT.md)  
**Dependencias:** ADR-007 · ADR-016 · PASO-17.5 · PASO-19.5 · PASO-20.4

---

## Contrato sembrado

| Código | Uso |
|--------|-----|
| `item:create` | Alta de identidad inventariable |
| `item:read` | Consultar / listar |
| `item:update` | Corregir registro + **activate** |
| `item:archive` | Soft-retire |

**Ausentes por diseño:** `delete` · `activate` (dedicado) · `restore` · `stock` · `adjust` · `bom` · `price` · verbos verticales.

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / archive | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform:** ALL **44** · OWNER 44 · ADMIN 41 · MANAGER 28 · USER 8 · READ_ONLY 10.

---

## Artefactos

| Artefacto | Ubicación |
|-----------|-----------|
| `ItemPermissionCatalog` | `inventory-contract/.../authorization/` |
| `IamPermissionCatalog` + `SystemRoleTemplate` | IAM (depende de `inventory-contract`) |
| Flyway | `V25__seed_item_authorization_contract.sql` |
| Tests | Catalog · SystemRoleTemplate · ItemAuthorizationSeedMigrationIT |

Patrón: **espejo exacto de PASO 17.5 / 19.5** (insert idempotente + backfill `role_permission`).

---

## Validación Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Retail / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform  

---

## Fuera de alcance

HTTP · `@RequiresPermission` en controllers · use cases · `ItemReferencePort`

---

## Siguiente paso

**PASO 20.5.1 — Item Admin API Audit** ✅ — [PASO-20.5.1](PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md). Siguiente: **20.6 Administration API**.

---

## Referencias

- [PASO-20.5-ITEM-AUTHORIZATION-CONTRACT-AUDIT.md](PASO-20.5-ITEM-AUTHORIZATION-CONTRACT-AUDIT.md)  
- [PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md) · [PASO-19.5](PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT.md)  
- [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md) · [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- Migración: `V25__seed_item_authorization_contract.sql`  
