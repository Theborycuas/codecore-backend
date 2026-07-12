# ADR-016 — Item Domain Model

**Status:** Accepted  
**Date:** 2026-07-12  
**Accepted:** 2026-07-12 (PASO 20.1)  
**Deciders:** CodeCore architecture (FASE 20.1)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-010 · ADR-011 · ADR-012 · ADR-013 · ADR-014 · ADR-015 · [PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md](../audits/PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md) · [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Context

FASE 19 closed **Clinical Records** (`Encounter`, ADR-015). Clinical Foundation, Scheduling, and Organization Management remain closed. CodeCore must introduce **Inventory** as the first **non-clinical** resource bounded context after the Patient · Appointment · Encounter triad — without reopening IAM, Organization, Patient, Appointment, or Encounter.

PASO-20.0.1 audited `Item` as the first Aggregate Root of Inventory: tenant-scoped stockable catalog identity, optional administrative organization grouping, soft lifecycle `ACTIVE` / `ARCHIVED`.

A recurring failure mode in inventory systems is the **God Aggregate catalog line**: over years, quantity, lots, prices, suppliers, BOM, images, and vertical consumption rules are stuffed into “Item” / “InventoryItem” until the Core becomes a WMS or retail ERP. This ADR freezes a **deliberately small** Item so Dental, Veterinary, Hospital, Lab, Retail, Manufacturing, and future verticals share one **inventoriable identity** foundation **without** that fate.

---

## Decision

### 1. Bounded context

**Inventory** — downstream of IAM and Organization Management; independent of Clinical Foundation, Scheduling, and Clinical Records.

Gradle module (FASE 20 Item slice): `inventory-management`  
SQL schema: `inventory`  
HTTP surface (shape deferred to PASO 20.5.1): `/api/v1/inventory/items`

### 2. What Item is

`Item` is:

> **The inventoriable identity of something that can be stocked, moved, or consumed within a Tenant.**

It is the tenant-scoped **stockable catalog identity** (what can be inventoried — not how much, where, or at what price).

It is the **only** Aggregate Root in the Core that owns that role within a Tenant. Vertical packs must not introduce parallel roots (“DentalSupply”, “RetailSku”, “LabReagent”, …) that duplicate this role. Downstream aggregates **reference** `ItemId` when needed.

It is **not**:

| Not Item | Belongs instead |
|----------|-----------------|
| Stock quantity at a location | Future `Stock` aggregate (same BC) — `ItemId` + `OfficeId` + qty |
| Stock movement / ledger entry | Future `StockMovement` |
| Commercial “Product” / e-commerce SKU root | Optional `code` on Item; Product naming rejected |
| Fixed asset / depreciation | Future Asset BC |
| Price / tax / tariff | Billing / Pricing |
| Supplier / purchase order | Procurement |
| BOM / kit / recipe | Manufacturing / packs |
| Lot / batch / serial / expiry | Future aggregate or flags |
| Clinical consumption line | Future clinical + `ItemId` reference |
| POS cart / e-commerce | Retail packs |
| Organization / Office ownership | Organization Management — IDs only |

### 3. Permanence principle — Item is intentionally small

> **Item is intentionally small.**

This is a **permanent architectural decision**, not a temporary limitation of FASE 20.

`Item` exists **only** to represent the **inventoriable identity**. It may contain **only**:

- catalog identity and status  
- display name and optional human code  
- ID references required for that identity (tenant; optional primary organization)  
- invariants **intrinsic to the inventoriable identity** (tenant, status transitions, soft-unique code, organization rules on write)

**Everything else lives elsewhere** — in its own Aggregate Root — and **must never** be embedded, nested, or “conveniently” co-located inside `Item`.

| Belongs on Item | Must never live inside Item |
|-----------------|----------------------------|
| Inventoriable identity | Stock quantity / balances |
| `displayName`, optional `code` | Stock movements / adjustments |
| Lifecycle `ACTIVE` / `ARCHIVED` | Price / cost / tax |
| Optional `PrimaryOrganizationId` | Supplier / PO / MRP |
| Intrinsic catalog invariants | BOM / kits / recipes |
| | Lots / serials / expiry |
| | Images / rich catalog media |
| | Encounter / Patient / Appointment links |
| | Clinical consumption rules |
| | POS / cart / warehouse WMS rules |
| | Unit-of-measure catalog (deferred) |

**Rule for future contributors and agents:** if a feature answers *“how much / where / what moved / what price / what lot / what was consumed clinically?”* rather than *“what inventoriable thing is this in this tenant’s catalog?”*, it **does not** belong on `Item`. Add a new aggregate. Expanding Item “just this once” is an architecture violation.

This principle aligns with DEVELOPMENT-POLICY-FASE-16-PLUS (§4–§5–§9), ADR-012 permanence pattern (soft registry), and ADR-011 / ADR-013 consumption.

### 4. Why Item is the Aggregate Root

| Criterion | Rationale |
|-----------|-----------|
| Transaction boundary | Owns catalog identity, status, name/code, optional primary org |
| Own lifecycle | `ACTIVE` ↔ `ARCHIVED` — independent of stock quantities |
| Stable ID | `ItemId` for Stock, Movements, Billing material lines, clinical consumption |
| Single-aggregate invariants | Does **not** transactionalize Stock, Pricing, or clinical documents |

#### Why not Stock first

Stock answers *how much where*. It **needs** a stable `ItemId`. Catalog inside Stock → God Aggregate.

#### Why not Product / Sku

Commerce/retail bias. Not everything inventoriable is sold. Optional `code` covers human SKU without renaming the root.

#### Why not Warehouse

Locus. In Core v1, **`Office`** (Organization Management) is the future stock locus. A Warehouse aggregate is premature until Office is insufficient.

#### Why not InventoryItem (name)

In classic ERPs “Inventory Item” often means a stock line or balance. **`Item`** avoids that ambiguity in the Core.

### 5. Ownership

| Concern | Owner BC | Aggregate |
|---------|----------|-----------|
| **Item** (inventoriable identity) | **Inventory** | `Item` |
| Business structure | Organization Management | `Organization` |
| Physical / logical site | Organization Management | `Office` |
| Care-subject / planned / occurred care | Clinical Foundation / Scheduling / Clinical Records | Patient / Appointment / Encounter |
| Identity / Membership / RBAC | IAM | Identity, Membership, Role, … |
| Price / invoice | Billing (future) | — |
| Quantity at locus | Inventory (future) | `Stock` |

**Rule:** Inventory stores **IDs** of foreign aggregates and validates them via ReferencePorts (ADR-013). It never owns Organization or Office data. It never embeds clinical aggregates.

Who may mutate Item: Membership + `item:*` permissions (ADR-007; catalog seeded in PASO 20.5).

### 6. Identity & human keys

| Element | Rule |
|---------|------|
| `ItemId` (UUID) | **Hard** unique identity of the aggregate |
| `displayName` | **Required** — non-blank operational label |
| `code` | **Optional** human SKU / material code; when present, soft-unique per `(tenantId, code)` |
| Unit of measure | **Out of Item v1** — deferred to Stock / unit catalog if needed |
| GTIN / HS / typed external ids | Out of scope v1 (future optional typed identifiers) |
| Long description / rich text | Out of scope v1 |

`displayName` uniqueness is **not** required (collisions like “Gloves M” are normal).

### 7. Catalog scope — tenant-scoped

Item is **tenant-scoped**: one shared catalog per Tenant.

| Option | Decision |
|--------|----------|
| Tenant-scoped catalog | **Adopted** |
| Mandatory org-scoped Item | **Rejected** — duplicates Items across sibling orgs; weaker `ItemId` for Billing/Stock |

Do **not** create one Item row per organization for the same inventoriable thing.

### 8. References (IDs only)

Item maintains **only IDs**. It **never** loads foreign aggregates, **never** depends on foreign repositories, **never** runs SQL against other BC schemas, and **never** calls internal HTTP to other modules.

| ID | On Item | Cardinality | Semantics |
|----|---------|-------------|-----------|
| `TenantId` | Required | 1 | SaaS isolation — **immutable; never changes tenant** |
| `PrimaryOrganizationId` (`OrganizationId`) | Optional | 0..1 | Administrative **grouping / custodial default** — **not** ownership of stock |

**Naming:** use `PrimaryOrganizationId` in domain language (mirror ADR-012). Do **not** use “Home Organization” or imply “stock belongs only to this org.”

**Forbidden on Item:** `OfficeId`, `PatientId`, `AppointmentId`, `EncounterId`, `StaffAssignmentId`, `MembershipId`, `IdentityId`, nested stock/qty/lots/prices/BOM/suppliers.

#### Optional `PrimaryOrganizationId` (write-time)

When present on create / update while `ACTIVE`:

1. Organization **exists** and is **ACTIVE** in the same tenant (`OrganizationReferencePort`).  
2. Archived Organization **blocks new** links; historical Items remain readable.

Absence means a general tenant catalog entry (no custodial org label).

### 9. Lifecycle (frozen)

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Available for new Stock / Movements / write-time references |
| `ARCHIVED` | Soft-retired; retained for history |

```text
(create) → ACTIVE
              ├── archive  → ARCHIVED
              ├── activate → ACTIVE   (from ARCHIVED)
              └── update (remains ACTIVE; only while ACTIVE)
```

| Behavior | Rule |
|----------|------|
| `create` | Enters `ACTIVE`; `displayName` + optional `code` / `PrimaryOrganizationId` after port validation |
| `update` | Only from `ACTIVE`; re-validate ports on write when org present |
| `archive` | Only from `ACTIVE` → `ARCHIVED` |
| `activate` | Only from `ARCHIVED` → `ACTIVE` — maps to `item:update` |
| Physical delete | **Forbidden** in v1 |
| Tenant transfer | **Forbidden** |

Archived Item **blocks** new Stock links / consumption / material lines (via future `ItemReferencePort.existsActive…`). Historical reads OK.

### 10. Invariants (normative)

1. Exactly one `TenantId`, set at `create` — **never changes**.  
2. Status ∈ {`ACTIVE`, `ARCHIVED`}.  
3. `archive` / mutating `update` only from `ACTIVE`; `activate` only from `ARCHIVED`.  
4. `displayName` always present and non-blank.  
5. If `code` is present → soft-unique per tenant.  
6. If `PrimaryOrganizationId` is present on write → Organization ACTIVE in tenant.  
7. Item does **not** carry `OfficeId`, quantity, price, supplier, BOM, lot, or clinical references.  
8. Item does not transactionalize Stock, Billing, Encounter, or Organization consistency beyond write-time ReferencePort checks.  
9. `ItemId` is never reassigned.  
10. Cross-tenant access is impossible.  
11. Within the Tenant, Item is the **sole** inventoriable-identity role — no parallel Product/Sku roots in the Core.

*(Structural permanence — “do not embed stock/pricing/vertical children” — is stated in §3.)*

### 11. Reference Ports (ADR-013)

Item consumes **only** Reference Contracts — never provider repositories, never cross-BC SQL, never internal HTTP loopback.

| Port | Purpose |
|------|---------|
| `OrganizationReferencePort` | When `PrimaryOrganizationId` present: Organization exists and is ACTIVE in tenant |

`OfficeReferencePort`, Patient, Appointment, and Encounter ports are **not** required by Item v1.

Gradle: `inventory-application` depends on `organization-contract` only (for this port/ID) in the Item slice.

**PASO 20.2:** confirm Org ports are sufficient; **do not** invent Patient/Encounter ports “in case material is clinical.”

### 12. Permissions (seeded in PASO 20.5)

Catalog:

`item:read` · `item:create` · `item:update` · `item:archive`

`activate` maps to `item:update` (mirror Patient / Organization) — **no** vertical verbs (`item:dental-supply`, `item:sku-import`, `item:bom`).  
**No** `item:stock` / `item:adjust` — those belong to future Stock.

RBAC remains membership-scoped (ADR-007).

### 13. Multi-organization

One Tenant shares **one** Item catalog (tenant-scoped).  
`PrimaryOrganizationId` does **not** isolate the catalog; it only labels custodial grouping.  
Future Stock keyed by `OfficeId` (office ∈ org) provides operational quantity isolation.

---

## Consequences

### Positive

- Stable inventoriable ID for Stock, Movements, Billing material lines, and clinical consumption  
- **Permanent protection against an Item / Inventory God Aggregate** (§3)  
- First non-clinical resource BC after the care triad — diversifies the Core  
- Aligns with Patient soft-registry pattern (ACTIVE/ARCHIVED + optional primary org)  
- Aligns with ADR-011 location rule (`OfficeId` = where stock lives — on Stock, not Item)  
- Vertical-agnostic catalog foundation  

### Negative / deferred

- No UoM on Item v1 — Stock must resolve quantity semantics later  
- No price on Item — Billing cannot invent unit price from catalog alone  
- Stock / Movements require **new** aggregates later — higher short-term ceremony, lower long-term risk  
- Soft-unique `code` needs careful partial-index / null handling in persistence  

### Neutral

- Permission seed and HTTP shape deferred to 20.5 / 20.5.1  
- `ItemReferencePort` published in closeout 20.8  

---

## Alternatives considered

| Alternative | Rejected because |
|-------------|------------------|
| Stock as first Aggregate Root | Qty without stable inventoriable identity; orphan strings |
| Product / Sku naming | Commerce bias; not all inventoriable things are sold |
| Mandatory org-scoped Item | Duplicates catalog across sibling orgs |
| `OfficeId` on Item | Locus belongs on Stock; mirrors Patient anti-pattern |
| Embed qty / price / BOM / lots | **God Aggregate** — violates §3 |
| Grow Item “temporarily” then split | Temporary growth becomes permanent |
| Parallel DentalSupply / RetailSku roots in Core | Fractures inventoriable identity exclusivity |
| Unit-of-measure catalog in FASE 20 Item slice | Premature; defer to Stock |
| Clinical ports on Item write | Catalog is not clinical; consumption references Item outbound |

---

## Compatibility check

| Document | Impact |
|----------|--------|
| ADR-003 | Item always tenant-scoped; `TenantId` immutable |
| ADR-006 | Unchanged — Item ≠ Identity |
| ADR-007 | Unchanged — membership-scoped RBAC; `item:*` later |
| ADR-010 | Unchanged — Org / Office boundaries respected |
| ADR-011 | Item consumer: optional `PrimaryOrganizationId` only; `OfficeId` on future Stock |
| ADR-012 | Unchanged — Patient remains clinical registry; Item is inventoriable registry (parallel soft-entity pattern) |
| ADR-013 | Normative consumption via `OrganizationReferencePort` |
| ADR-014 / ADR-015 | Unchanged — clinical/scheduling BCs closed; Item does not reference them |
| ORGANIZATION-CONSUMPTION-GUIDE | Item recipe: optional primary org; Office on Stock later |
| DEVELOPMENT-POLICY-FASE-16-PLUS | §3 reinforces §§4–5–9 |

**No existing ADR is modified by this decision.**

---

## Freeze rule

The Item domain model defined by this ADR is **frozen** as of PASO **20.1**.

Any change to Aggregate Root boundaries, references (including adding `OfficeId` or quantity to Item), lifecycle, catalog scope (tenant vs org), or the §3 permanence principle **requires**:

1. A new architecture audit (DEVELOPMENT-POLICY-FASE-16-PLUS §2)  
2. A **new ADR**

Implementation steps 20.2+ must implement this contract — they must not reopen it.

---

## Acceptance

**Accepted** in PASO **20.1** (2026-07-12).  
Evidence: this ADR · [PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md](../audits/PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md) · [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) · [PASO-20.1-ITEM-MODEL-CONTRACT.md](../audits/PASO-20.1-ITEM-MODEL-CONTRACT.md).

---

## References

- [PASO-20.1-ITEM-MODEL-CONTRACT.md](../audits/PASO-20.1-ITEM-MODEL-CONTRACT.md)  
- [PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md](../audits/PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md)  
- [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md)  
- [ADR-012-PATIENT-DOMAIN-MODEL.md](ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ADR-015-ENCOUNTER-DOMAIN-MODEL.md](ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ROADMAP.md](ROADMAP.md)  
