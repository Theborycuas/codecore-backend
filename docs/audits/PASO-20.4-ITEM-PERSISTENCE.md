# PASO 20.4 — Item Persistence

**Item** queda persistido exactamente como ADR-016 lo congela: identidad inventariable tenant-scoped, sin FK cross-BC, listo para Stock / Billing / consumo clínico posteriores.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Persistencia R2DBC + Flyway  
**Dependencias:** [PASO-20.3](PASO-20.3-ITEM-DOMAIN-FOUNDATION.md) · [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Conectar el Aggregate `Item` a PostgreSQL siguiendo el patrón nativo CodeCore (Patient / Encounter). Sin HTTP, use cases, seguridad ni ReferencePorts propios.

---

## Decisiones

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Schema | `inventory` | BC Inventory (ADR-016) — no mezclar con `org` / `clinical` / `records` |
| Tabla raíz | `inventory.item` | Un row = un Item |
| Child tables | Ninguna | Aggregate single-row (ADR-016 intentionally small) |
| `tenant_id` | UUID obligatorio, sin FK a IAM | ADR-003 |
| `primary_organization_id` | UUID nullable, sin FK | ADR-011/013 — ReferencePort en escritura futura |
| `code` | VARCHAR(64) nullable | Soft-unique parcial por tenant |
| Soft-unique code | `UNIQUE (tenant_id, code) WHERE code IS NOT NULL` | ADR-016; múltiples NULL OK |
| Adapter | `R2dbcItemRepository` implementa `ItemRepository` + `ItemQueryPort` | Igual que Patient/Encounter |

### Por qué NO hay FK cross-BC

Misma filosofía que Patient V18 / Encounter V22: IDs lógicos + ReferencePorts en application; schemas ajenos no se acoplan.

---

## SQL (V24)

```text
inventory.item
  item_id PK
  tenant_id NOT NULL
  primary_organization_id NULL
  display_name NOT NULL (≤200, not blank)
  code NULL (≤64, not blank if present)
  status ACTIVE|ARCHIVED
  created_at, updated_at
```

**Índices:** tenant · status · (tenant, status) · (tenant, primary_organization) · **partial unique** `(tenant, code) WHERE code IS NOT NULL`.

**Ausente por diseño:** quantity · price · BOM · lot · office_id · supplier · UoM · clinical FKs.

---

## Infraestructura

```text
inventory-infrastructure
  configuration/InventoryModuleConfiguration
  persistence/entity/ItemEntity
  persistence/mapper/ItemMapper
  persistence/repository/SpringDataItemRepository
  persistence/repository/R2dbcItemRepository
```

`codecore-api` escanea `com.codecore.inventory` y depende de `inventory-infrastructure`.

---

## Tests (`R2dbcItemRepositoryIT`)

| Caso | Cubierto |
|------|----------|
| save + findById | ✓ |
| exists / existsByIdAndTenant | ✓ |
| countByTenant + findByTenant | ✓ |
| cross-tenant isolation | ✓ |
| code / primaryOrganization null / non-null | ✓ |
| findByPrimaryOrganization | ✓ |
| findByStatus (ACTIVE / ARCHIVED) | ✓ |
| soft-unique code same tenant → DuplicateKey | ✓ |
| same code different tenants OK | ✓ |
| multiple NULL codes OK | ✓ |
| existsByTenantIdAndCode (+ excludingId) | ✓ |
| update without duplicating row | ✓ |

**12/12** ITs verdes.

```bash
./gradlew :modules:inventory-management:inventory-infrastructure:test --tests "*R2dbcItemRepositoryIT"
```

---

## Fuera de alcance

HTTP · Controllers · OpenAPI · Use cases · Permissions · `ItemReferencePort` · Stock · Eventos

---

## Siguiente paso

**PASO 20.5 — Item Authorization Contract** ✅ — [PASO-20.5](PASO-20.5-ITEM-AUTHORIZATION-CONTRACT.md). Siguiente: **20.5.1 Admin API Audit**.

---

## Referencias

- [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-20.3](PASO-20.3-ITEM-DOMAIN-FOUNDATION.md)  
- [PASO-17.4](PASO-17.4-PATIENT-PERSISTENCE.md) · [PASO-19.4](PASO-19.4-ENCOUNTER-PERSISTENCE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- Migración: `V24__create_item_table.sql`  
