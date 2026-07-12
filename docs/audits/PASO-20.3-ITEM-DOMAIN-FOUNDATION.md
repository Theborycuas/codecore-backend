# PASO 20.3 — Item Domain Foundation

**Item** is the inventoriable identity of something that can be stocked, moved, or consumed within a Tenant — intentionally small, frozen by ADR-016, and ready for Stock / Billing / clinical consumption without growing into an Inventory God Aggregate.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Dominio puro (sin persistencia / HTTP / use cases)  
**Dependencias:** [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-20.2](PASO-20.2-INVENTORY-REFERENCE-READINESS.md)

---

## One-sentence rule (aggregates importantes)

| Aggregate | Frase |
|-----------|--------|
| Patient | La identidad clínica registral del sujeto de cuidado. |
| Appointment | El compromiso planificado de atención. |
| Encounter | El episodio de atención que ocurrió. |
| **Item** | **La identidad inventariable de algo que puede stockearse, moverse o consumirse bajo un Tenant.** |

Si un aggregate deja de caber en una frase clara, suele estar asumiendo demasiadas responsabilidades.

---

## Objetivo

Implementar el foundation del Aggregate Root `Item` **exactamente** como ADR-016 Accepted — sin rediseñar, sin infraestructura real.

---

## Módulo Gradle

```text
modules/inventory-management/
  inventory-domain          ← Aggregate + VOs + exceptions + tests
  inventory-application     ← solo puertos de salida (placeholder de use cases)
  inventory-contract        ← publica ItemId vía api(domain); sin ReferencePort aún
  inventory-infrastructure  ← placeholder
```

Registrado en `settings.gradle.kts`. **No** cableado aún en `codecore-api` (persistencia/HTTP posteriores).

---

## Modelo implementado

```text
Item
  ├── ItemId                              (hard identity)
  ├── TenantId                            (required, immutable)
  ├── ItemDisplayName                     (required)
  ├── ItemCode?                           (optional SKU / material code)
  ├── PrimaryOrganizationId?              (UUID ref — not stock ownership)
  └── ItemStatus                          ACTIVE | ARCHIVED
```

**Behaviors:** `create` · `rename` · `assignCode` · `clearCode` · `assignPrimaryOrganization` · `removePrimaryOrganization` · `archive` · `activate` · `reconstitute`

**Mutaciones de catálogo** solo en estado `ACTIVE`.  
Validación ACTIVE+tenant de `PrimaryOrganizationId` → application + `OrganizationReferencePort` (pasos siguientes), no el aggregate.  
Soft-unique `(tenantId, code)` → application + `ItemQueryPort` (persistencia 20.4 / admin 20.6).

### Explicitamente ausente (ADR-016 §3)

Stock qty · StockMovement · Price · BOM · Lot · Office · Supplier · UoM · Encounter · Patient · Appointment · Identity · Membership · POS

---

## Application / Contract / Infrastructure

| Capa | Entregable 20.3 |
|------|-----------------|
| Application | `ItemRepository`, `ItemQueryPort` (incl. code existence helpers) — sin use cases ni servicios |
| Contract | `InventoryContractMarker` + `api(inventory-domain)` — **sin** `ItemReferencePort` |
| Infrastructure | `InventoryInfrastructurePlaceholder` |

---

## Tests

**27** tests de dominio (todos verdes):

| Suite | Cobertura |
|-------|-----------|
| `ItemTest` (16) | create, tenant immutable, rename/code/org, archive/activate, transiciones inválidas, reconstitución, API sin concerns stock/precio/clínicos |
| `ItemValueObjectTest` (11) | igualdad IDs, display name / code trim+límites, status enum congelado |

```bash
./gradlew :modules:inventory-management:inventory-domain:test
```

---

## Fuera de alcance

Persistencia · Flyway · R2DBC · HTTP · controllers · use cases de escritura · `item:*` permissions · `ItemReferencePort` · wiring `codecore-api`

---

## Siguiente paso

**PASO 20.4 — Item Persistence** ✅ — [PASO-20.4](PASO-20.4-ITEM-PERSISTENCE.md). Siguiente: **20.5 Authorization Contract**.

---

## Referencias

- [ADR-016-ITEM-DOMAIN-MODEL.md](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md)  
- [PASO-20.2-INVENTORY-REFERENCE-READINESS.md](PASO-20.2-INVENTORY-REFERENCE-READINESS.md)  
- [PASO-17.3-PATIENT-DOMAIN-FOUNDATION.md](PASO-17.3-PATIENT-DOMAIN-FOUNDATION.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
