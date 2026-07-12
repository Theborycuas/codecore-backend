# PASO 20.1 — Item Model Contract (ADR-016)

**Item** queda congelado: identidad inventariable, *intentionally small*, listo para que FASE 20 implemente sin reabrir el dominio.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Aceptación ADR (sin código)  
**Dependencias:** [PASO-20.0.1](PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md) · [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md)

---

## Entregable

| Artefacto | Estado |
|-----------|--------|
| [ADR-016 — Item Domain Model](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) | **Accepted** |

Sin tablas · sin HTTP · sin persistencia · sin módulos Gradle en este paso.

---

## Contrato congelado (resumen)

| Elemento | Valor |
|----------|-------|
| Definición | Inventoriable identity of something that can be stocked, moved, or consumed within a Tenant |
| Principio | **Item is intentionally small** (permanente) |
| Scope | **Tenant-scoped** catalog |
| Estados | `ACTIVE` · `ARCHIVED` |
| Identidad | `ItemId` (UUID) · `displayName` required · `code` optional soft-unique per tenant |
| Refs | `TenantId` · `PrimaryOrganizationId?` |
| Prohibido | `OfficeId` · qty · price · BOM · lots · clinical IDs · UoM en v1 |
| Ports | `OrganizationReferencePort` (si primary org presente) |
| Schema / módulo (objetivo) | `inventory` · `inventory-management` |
| HTTP (objetivo) | `/api/v1/inventory/items` |
| Permisos (borrador) | `item:read` · `item:create` · `item:update` · `item:archive` |

---

## Checklist de aceptación

- [x] Checklist política §8 verde (20.0.1)  
- [x] Permanencia §3 en ADR  
- [x] Lifecycle soft-entity congelado  
- [x] Tenant vs org scope cerrado (tenant-scoped + PrimaryOrganizationId opcional)  
- [x] Compatibilidad ADR-003/006/007/010–015 sin modificar ADRs previos  
- [x] Freeze rule publicada  

---

## Siguiente paso

**PASO 20.2 — Inventory Reference Readiness** ✅ — [PASO-20.2](PASO-20.2-INVENTORY-REFERENCE-READINESS.md). Siguiente: **20.3 Domain Foundation**.

---

## Referencias

- [ADR-016-ITEM-DOMAIN-MODEL.md](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md)  
- [PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md](PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md)  
- [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
