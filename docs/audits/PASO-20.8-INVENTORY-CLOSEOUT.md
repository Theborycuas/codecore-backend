# PASO 20.8 — Inventory Closeout (Item)

**Veredicto:** **FASE 20 — Inventory (Item slice): ✅ CERRADA**

Item queda como BC estable del Core Platform: identidad inventariable *intentionally small*, API admin, ReferencePort listo para Stock / Billing / consumo clínico.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-20.7](PASO-20.7-ITEM-VERIFICATION.md) · [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Cierre formal de **FASE 20 — Inventory** (slice Item). Sin nuevas capacidades de negocio; superficie de consumo y documentación para Stock / Billing / packs.

---

## Entregables

| Área | Detalle |
|------|---------|
| OpenAPI | `InventoryOpenApiConfiguration` — grupo `inventory-administration` |
| Endpoint docs | `GET /v3/api-docs/inventory-administration` |
| Paths | `/api/v1/inventory/items` (+ archive, activate) |
| ReferencePort | `ItemReferencePort` + `R2dbcItemReferenceAdapter` (ADR-013) |
| Guía | [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) |
| Verificación port | `ItemReferencePortIT` + contract test |
| ROADMAP | FASE 20 Item slice → **✅ Cerrada** · siguiente **Stock** (mismo BC) o **Billing** |

---

## Superficie entregada (FASE 20)

| Capa | Entregable |
|------|------------|
| Dominio | Aggregate `Item` + VOs (ADR-016 frozen) |
| Persistencia | Schema `inventory` · V24 · R2DBC |
| Auth | `item:create\|read\|update\|archive` · V25 |
| HTTP | Soft admin API espejo Patient |
| Contract | `ItemId` · `ItemPermissionCatalog` · `ItemReferencePort` |
| Reference ports in | `OrganizationReferencePort` (primary org opcional) |
| Tests | Domain · use case · persistence IT · admin IT · verification 8/8 · reference port IT |

**Permisos:** 4 · **Migraciones:** V24–V25 · **ADRs:** 016 (frozen), 013 (patrón)

---

## Documentación de fase

| Documento | Propósito |
|-----------|-----------|
| [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) | Modelo Item — **congelado** |
| [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) | Reference Contracts |
| [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) | Guía consumidores |
| [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md) | Item como consumer de Org |
| PASO-20.0 … PASO-20.7 | Trazabilidad implementación |

---

## Criterio de cierre FASE 20 (PASO 20.0 §12)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Item según ADR-016 (*intentionally small*) | ✅ |
| 2 | Org solo por IDs + Reference Ports | ✅ |
| 3 | Verification E2E verde | ✅ 20.7 |
| 4 | ROADMAP FASE 20 Item slice ✅ · siguiente Stock / Billing | ✅ |
| 5 | Ningún aggregate IAM / Org / Patient / Appointment / Encounter modificado (salvo seeds IAM) | ✅ |
| 6 | `ItemReferencePort` + guía publicados | ✅ |

---

## Explicitamente fuera (post-20 Item)

Stock · qty · Movements · price · UoM · BOM · lotes · Warehouse · Office en Item · Encounter/Patient refs · POS/WMS · vertical packs · eventos cross-BC · org-scoped RBAC · DELETE HTTP

---

## Próximo

**Stock (mismo BC Inventory)** — crece **alrededor** de Item vía `ItemId` + `ItemReferencePort` (+ `OfficeId` para locus) **sin reabrir** ADR-016 ni FASE 16–19.

Alternativa de producto: **FASE 21 — Billing** puede consumir `ItemId` para material lines sin esperar Stock completo.

Los módulos de negocio no modifican Item; consumen `ItemReferencePort` cuando necesiten validar catálogo ACTIVE.

---

## Veredicto

**FASE 20 — Inventory (Item slice): ✅ CERRADA**

Identidad inventariable entregada, verificada E2E, documentada y publicada para consumo a largo plazo. CodeCore queda fortalecido como Core Platform multi-vertical:

```text
IAM → Organization → Patient → Appointment → Encounter → Inventory (Item)
 CLOSED     CLOSED      CLOSED     CLOSED        CLOSED         CLOSED (Item)
                                                              ↘ Stock → Movements / Billing lines / packs
```

---

## Referencias

- [PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md](PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md) — patrón de cierre  
- [PASO-20.7-ITEM-VERIFICATION.md](PASO-20.7-ITEM-VERIFICATION.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
