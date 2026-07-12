# Inventory Consumption Guide

**Audience:** Developers building modules after FASE 20 Item slice (Stock, Movements, Billing material lines, clinical consumption packs, …)  
**Authority:** [ADR-016](ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
**Status:** Vigente desde PASO 20.8 — Inventory **Item slice cerrada**

---

## Mental model

FASE 20 no es “CRUD de stock”. FASE 20 entrega un **bounded context estable** (slice Item): la identidad inventariable del catálogo stockable (*intentionally small*).

```text
Tenant (IAM)
 └── Item                        ← inventoriable identity (intentionally small)
      ├── displayName            ← operational label
      ├── code?                  ← optional soft-unique SKU-like key per tenant
      ├── primaryOrganizationId? ← optional custodial grouping (NOT stock ownership)
      └── status                 ← ACTIVE ↔ ARCHIVED
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Cuál es la identidad inventariable? | `ItemId` |
| ¿Puedo abrir Stock / consumir / facturar material sobre este Item? | `ItemReferencePort.existsActiveByIdAndTenant` |
| ¿Dónde “pertenece” el Item? | Tenant (obligatorio) · primary org (opcional, agrupación) |
| ¿Cuánta cantidad hay / en qué office? | **No en Item** — futuro aggregate `Stock` (`OfficeId` + qty) |
| ¿Precio / UoM / BOM / lote? | **No en Item** — aggregates / BCs posteriores |

---

## Decision tree (30 seconds)

```text
Need to store a link to an inventoriable catalog entry?
  → Store ItemId on your aggregate + tenantId

Need to validate item exists and is ACTIVE at write time?
  → ItemReferencePort (`inventory-contract`, ADR-013 / ADR-016)
  → Never ItemRepository, never SQL against inventory.item from another BC

Need display name / code for UI labels?
  → Prefer your own read model / Query Port later — do not import Item aggregate

Need quantity, warehouse location, lot, or price?
  → Wrong — grow around Item via Stock / Pricing / Movements (ADR-016 §3)
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:inventory-management:inventory-contract"))
```

Only. Never `inventory-application` or `inventory-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `ItemId` + `tenantId` | `@Autowired R2dbcItemRepository` |
| Validate via `ItemReferencePort` | `SELECT * FROM inventory.item` outside Inventory |
| Filter by JWT `tenantId` | Accept client-sent `tenantId` as authority |
| Treat Item as inventoriable identity | Put qty / price / BOM / Encounter fields on Item |

---

## Contract surface (Item slice closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `ItemId` | `inventory-contract` → domain VO | Hard identity |
| `ItemPermissionCatalog` | `inventory-contract` | `item:create\|read\|update\|archive` |
| `ItemReferencePort` | `inventory-contract` | ACTIVE + tenant existence check |
| `R2dbcItemReferenceAdapter` | `inventory-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface ItemReferencePort {
    Mono<Boolean> existsActiveByIdAndTenant(ItemId itemId, TenantId tenantId);
}
```

Archived items → `false` (blocks **new** Stock / consumption / material links; historical reads may need a separate method later if a consumer invariant requires it).

---

## Module recipes

### Stock (same Inventory BC — next)

**Owns:** quantity at an operational locus  
**References:** `ItemId` (required), typically `OfficeId` (ADR-011 location rule)

```text
Stock
  itemId              ← validate ACTIVE via ItemReferencePort
  officeId            ← where quantity lives
  quantity…
```

**Never:** embed Item displayName/code as source of truth; never put qty on Item.

### Billing material lines (FASE 21+)

**Owns:** charges that reference catalog materials  
**References:** `ItemId` when inventoriable; never owns Item lifecycle; **never** invents unit price from Item alone (ADR-016 — no price on Item).

### Clinical consumption / packs (vertical)

**Owns:** procedure kits / consumption events  
**References:** `ItemId` (+ future Stock); never grow Item for dental/vet-specific fields.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `ItemReferencePort` in `inventory-contract` |
| **Frontend / mobile** | `/api/v1/inventory/items` for admin; business APIs return embedded `itemId` |
| **Reporting** | Separate read models — not ad-hoc joins into `inventory` from other BCs |

Item Administration API is for **tenant admins**, not for Stock/Billing module internals.

---

## OpenAPI

Grupo springdoc: **`inventory-administration`**

```text
GET /v3/api-docs/inventory-administration
```

Paths: `/api/v1/inventory/items` (+ archive/activate).

---

## Testing consumers

- Mock `ItemReferencePort` in unit / module tests  
- Do **not** load full Inventory infrastructure unless E2E  
- Testcontainers `inventory` schema only in cross-BC ITs  

---

## Checklist before merging a consumer of Item

| # | Item |
|---|------|
| 1 | Gradle depends only on `inventory-contract` |
| 2 | Aggregates store `ItemId`, not Item entity |
| 3 | No SQL against `inventory.item` outside Inventory module |
| 4 | Write-time ACTIVE check via `ItemReferencePort` |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | Archived item blocks **new** links |
| 7 | ADR-016 consulted — do not grow Item for qty/price/vertical needs |

---

## Related documents

- [ADR-016 — Item Domain Model](ADR-016-ITEM-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)
- [PASO-20.8-INVENTORY-CLOSEOUT.md](../audits/PASO-20.8-INVENTORY-CLOSEOUT.md)
- [PASO-20.7-ITEM-VERIFICATION.md](../audits/PASO-20.7-ITEM-VERIFICATION.md)
