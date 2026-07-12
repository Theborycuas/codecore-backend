# PASO 20.0.1 — Item Aggregate Audit (DDD Estratégico)

**Item** es la identidad inventariable — *intentionally small*, multi-vertical, y el primer Aggregate Root del BC **Inventory**.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado (solo arquitectura)  
**Tipo:** Auditoría obligatoria — Aggregate Root nuevo + Bounded Context Inventory  
**Dependencias:** [PASO-20.0](PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) · ADR-003 · ADR-007 · ADR-010 · ADR-011 · ADR-013 · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Objetivo

Definir el modelo correcto del Aggregate Root **`Item`** para que CodeCore, como Core Platform, registre identidades inventariables en dental, veterinaria, hospital, laboratorio, retail, manufactura, ERP de servicios y verticales futuros — **sin** acoplar Inventory a un producto concreto ni convertir Item en un ERP / catálogo e-commerce / línea de stock.

**Sin código. Sin tablas. Sin endpoints. Sin migraciones. Sin qty / movimientos / precios / BOM / lotes.**

---

## Checklist política (§8) — verdicto previo

| # | Ítem | ✓ | Nota |
|---|------|---|------|
| 1 | Aggregate Root identificado | ✅ | `Item` |
| 2 | Ownership definido | ✅ | BC Inventory |
| 3 | Invariantes definidas | ✅ | § Invariantes |
| 4 | Lifecycle definido | ✅ | § Lifecycle |
| 5 | Estados definidos | ✅ | `ACTIVE` · `ARCHIVED` |
| 6 | Permisos definidos | ✅ | Borrador `item:*` |
| 7 | Relaciones solo mediante IDs | ✅ | § Referencias |
| 8 | Bounded Context correcto | ✅ | Inventory (≠ Org, ≠ Clinical, ≠ Billing) |
| 9 | No rompe ADR vigentes | ✅ | 003/006/007/010–015 intactos; BCs cerrados |
| 10 | Escalable multi-tenant | ✅ | |
| 11 | Escalable multi-organization | ✅ | Catálogo tenant-scoped; org opcional |
| 12 | Escalable millones de registros | ✅ | Aggregate delgado; sin hijos de stock |

**Veredicto:** checklist en verde → **ADR-016 Accepted** ([PASO-20.1](PASO-20.1-ITEM-MODEL-CONTRACT.md)).

---

## Decisiones irreversibles (resumen ejecutivo)

| Decisión | Elección |
|----------|----------|
| Naturaleza | **Identidad inventariable** (catálogo stockable) — no Stock, no Product comercial, no Asset fijo |
| Aggregate Root | **`Item`** — **única** identidad inventariable de ese “qué” dentro del Tenant |
| BC propietario | **Inventory** |
| Tenant | **Siempre** — `TenantId` obligatorio e **inmutable** |
| Scope de catálogo | **Tenant-scoped** — un catálogo por tenant (no un Item por Organization por defecto) |
| Organization | **`OrganizationId` opcional** — agrupación / custodia administrativa (espejo `Patient.primaryOrganizationId`); **no** ownership de stock |
| Office | **Fuera de Item** — locus de cantidad → aggregate `Stock` futuro (`ItemId` + `OfficeId`) |
| Unicidad dura | Solo **`ItemId` (UUID)** |
| Código humano | **Opcional** — soft-unique por tenant si presente (SKU / material code) |
| Nombre | **`displayName` obligatorio** (etiqueta operativa) |
| Unidad de medida | **Fuera de v1** — diferida a Stock / Unit catalog si hace falta |
| Precio / costo | **Fuera** — Billing / Pricing |
| Qty / lotes / movimientos | **Fuera** — Stock / StockMovement |
| Lifecycle v1 | `ACTIVE` / `ARCHIVED` — **sin delete físico** |
| Clinical / Encounter link | **Fuera de Item** — consumo futuro referencia `ItemId` |
| Módulo / schema (propuesta) | `inventory-management` · schema `inventory` |
| HTTP (propuesta) | `/api/v1/inventory/items` |

Borrador formal: **ADR-016 — Item Domain Model** — ✅ **Accepted** en [PASO-20.1](PASO-20.1-ITEM-MODEL-CONTRACT.md).

---

## 1. Naturaleza del Aggregate

### ¿Qué representa Item?

**Item es la identidad inventariable de algo que puede stockearse, moverse o consumirse dentro de un Tenant.**

| Interpretación | ¿Es? | Por qué |
|----------------|------|---------|
| ¿La ficha / catálogo de “qué es esto”? | **Sí** | Definición operativa |
| ¿Cuántas unidades hay en un office? | **No** | Eso es `Stock` |
| ¿Una entrada/salida? | **No** | Eso es `StockMovement` |
| ¿Un producto en venta / e-commerce? | **No como tipo** | Puede *usarse* como vendible; el root no es “Product” |
| ¿Un activo fijo (equipo, sillón)? | **No** | Asset — modelo distinto |
| ¿Algo dental / retail específico? | **No** | Core Platform |

**Regla de exclusividad:** dentro del Tenant, el **“qué inventariable”** vive en Inventory como `Item`. Ningún vertical debe crear un “DentalSupply” / “RetailSku” / “LabReagent” paralelo en el Core que duplique ese rol — los verticales **referencian** `ItemId` o extienden por packs.

**Intentionally small:** Item no crece con cantidades, precios, proveedores, BOM, lotes, imágenes de catálogo ni reglas de consumo clínico.

---

## 2. Aggregate Root

### ¿Por qué Item debe ser Aggregate Root?

- Boundary transaccional de la **identidad inventariable** (alta, corrección, archivo).  
- Ciclo de vida propio (`ACTIVE` / `ARCHIVED`).  
- `ItemId` estable para Stock, Movements, Billing de materiales, consumo clínico futuro.  
- Sus invariantes **no** incluyen consistencia de cantidades ni de precios (política §5).

### Principio de permanencia (para ADR-016)

> **Item is intentionally small.**

Decisión **permanente** (no limitación de FASE 20): Item solo representa la identidad inventariable + invariantes propias. Stock, movements, pricing, procurement, BOM, lotes y consumo clínico viven en **otros** aggregates. Embebidos en Item = violación arquitectónica (**God Aggregate**).

### ¿Por qué no Stock?

Stock responde “¿cuánto hay dónde?”. Nace **después** de saber qué es el Item. Muchos Stocks por Item (un office cada uno). Meter catálogo dentro de Stock → God Aggregate.

### ¿Por qué no Product / Sku?

Sesgo comercio/retail. No todo lo inventariable se vende. `code` opcional cubre SKU humano sin renombrar el root.

### ¿Por qué no Warehouse?

Locus. En Core v1, **`Office`** (Organization Management) es el locus de stock futuro. Warehouse propio = sobreingeniería hasta evidencia de que Office no basta.

### ¿Por qué no InventoryItem (nombre)?

En ERPs clásicos “Inventory Item” = línea de stock o balance. El nombre **`Item`** evita esa ambigüedad en el Core.

---

## 3. Ownership

| Rol | Actor / BC |
|-----|------------|
| **Propietario del modelo** | Bounded Context **Inventory** (`inventory-management`) |
| **Quién crea** | Membership con `item:create` |
| **Quién modifica** | `item:update` |
| **Quién archiva** | `item:archive` |
| **Quién reactiva** | `item:update` (espejo Patient/Org `activate` → `:update`) — cerrar en 20.5 |
| **Quién solo consulta** | `item:read` |
| **Quién NO es dueño** | IAM, Organization, Clinical Foundation, Scheduling, Clinical Records, Billing |

**Borrador permisos (20.5):**  
`item:read` · `item:create` · `item:update` · `item:archive`  

**No** verbos verticales (`item:dental-supply`, `item:sku-import`, `item:bom`).  
**No** `item:stock` / `item:adjust` — eso será del aggregate Stock futuro.

Organization **no** posee Item. Solo puede ser referenciada opcionalmente.

---

## 4. Bounded Context

| Pregunta | Respuesta |
|----------|-----------|
| ¿BC? | **Inventory** |
| ¿Parte de Organization? | **No** — Org no es catálogo de materiales |
| ¿Parte de Billing? | **No** — Billing referencia Item |
| ¿Parte de Clinical Records? | **No** — consumo clínico referencia Item; no vive aquí |
| ¿Módulo Gradle? | `modules/inventory-management/` (propuesta) |
| ¿Schema? | `inventory` (propuesta) — **no** meter catálogo en `org` ni `clinical` ni `records` |

```text
org.*        → Organization / Office / StaffAssignment     CLOSED
clinical.*   → Patient                                     CLOSED
scheduling.* → Appointment                                 CLOSED
records.*    → Encounter                                   CLOSED
inventory.*  → Item (catalog) · Stock? · Movement?         FASE 20
```

Inventory puede crecer después con `Stock` / `StockMovement` **en el mismo BC** (mismo patrón Org→Office), siempre referenciando `ItemId` — **sin engordar Item**.

---

## 5. Tenant

| Pregunta | Respuesta |
|----------|-----------|
| ¿Item pertenece al Tenant? | **Sí** |
| ¿Siempre? | **Sí** |
| ¿Puede cambiar de Tenant? | **Nunca** |

`TenantId` se fija en `create` y es inmutable. Cross-tenant → 404.

**Invariante:** un “mismo material físico conceptual” en otro tenant es **otro** `ItemId` (otro registro SaaS), no una migración del aggregate.

---

## 6. Scope: tenant-scoped vs org-scoped (cerrado aquí)

| Opción | Pros | Contras |
|--------|------|---------|
| **Tenant-scoped** (elegida) | Un catálogo compartido multi-org; un `ItemId` estable para todo el tenant; espejo Patient registry | Requiere disciplina de quién edita el catálogo (RBAC) |
| Org-scoped obligatorio | Catálogos aislados por unidad | Duplicación de Items entre orgs hermanas; peor para ReferencePorts y Billing |

**Decisión:** Item es **tenant-scoped**.

`OrganizationId` **opcional** = agrupación / custodia administrativa por defecto (espejo `PrimaryOrganizationId` de Patient):

- Ausente → Item de catálogo general del tenant.  
- Presente → debe ser Organization **ACTIVE** en el tenant (`OrganizationReferencePort`).  
- **No** significa “el stock solo existe en esa org”.  
- **No** implica RBAC org-scoped (ADR-007 intacto).

Archived Organization **bloquea nuevos** enlaces de `OrganizationId` en create/update; Items históricos siguen legibles.

---

## 7. Office

| Pregunta | Respuesta |
|----------|-----------|
| ¿Item referencia Office? | **No** |
| ¿Dónde vive el locus? | Aggregate **`Stock` futuro**: `ItemId` + `OfficeId` + cantidad |

**Por qué:** el catálogo describe *qué*; la ubicación describe *dónde hay cuánto*. Mezclarlos en Item fuerza N copias del mismo material por office o campos qty ilegítimos.

Alineado con Patient audit: Office fuera del registry; solo en aggregates operativos.

---

## 8. Identidad humana (código / nombre)

| Mecanismo | Rol en CodeCore |
|-----------|-----------------|
| **`ItemId` (UUID)** | **Única clave de identidad dura** |
| `displayName` | **Obligatorio** — etiqueta operativa (recepción / almacén / factura) |
| `code` (SKU / material code) | **Opcional** — soft-unique por `(tenantId, code)` si presente |
| Descripción larga / rich text | **Fuera de v1** — no inflar Item |
| Barras / GTIN / HS code | External identifiers tipados futuros — no hardcodear en v1 |

| Opción | Decisión |
|--------|----------|
| Solo UUID | Clave primaria |
| `code` único obligatorio | **No** — muchos tenants empiezan sin SKU |
| `displayName` único | **No** — colisiones normales (“Guantes M”) |
| UUID + `code` soft-unique | **Adoptado** |

**Regla:** no asumir legislación ni esquema de SKU de un vertical.  
Si `code` está presente → unicidad `(tenantId, code)`; si ausente → OK.

---

## 9. Unidad de medida

| Opción | Decisión |
|--------|----------|
| Unit catalog BC ahora | **No** — sobreingeniería |
| `unitCode` obligatorio en Item | **No en v1** |
| Diferir UoM a Stock | **Sí** |

Stock futuro decidirá si la cantidad es adimensional, lleva `unitCode` opaco, o aparece un catálogo de unidades. **Item v1 no congela UoM.**

---

## 10. Lifecycle

| Estado | Significado |
|--------|-------------|
| `ACTIVE` | Disponible para nuevos Stock / Movements / referencias de escritura |
| `ARCHIVED` | Soft-retire; retención histórica; **no** delete físico |

```text
(create) → ACTIVE
              ├── archive  → ARCHIVED
              ├── activate → ACTIVE   (desde ARCHIVED)
              └── update (permanece ACTIVE; solo si ACTIVE)
```

| Comportamiento | Regla |
|----------------|-------|
| `update` de nombre/código/org | Solo desde `ACTIVE` |
| `archive` | ACTIVE → ARCHIVED |
| `activate` | ARCHIVED → ACTIVE → permiso `item:update` |
| Delete físico | **Prohibido en v1** |
| Reactivar | Permitido (espejo Patient/Org) — catálogo necesita reaparecer |

Archived Item **bloquea** nuevas vinculaciones de Stock / consumo / líneas nuevas (vía ReferencePort futuro `existsActive…`). Lecturas históricas OK.

---

## 11. Referencias (IDs only)

| ID | En Item | Cardinalidad | Semántica |
|----|---------|--------------|-----------|
| `TenantId` | Required | 1 | Aislamiento SaaS — **inmutable** |
| `OrganizationId` | Optional | 0..1 | Custodia / agrupación administrativa |
| `OfficeId` | — | — | **Prohibido en Item** |
| `PatientId` / `EncounterId` / `AppointmentId` | — | — | **Prohibido** |
| `StaffAssignmentId` / `MembershipId` / `IdentityId` | — | — | **Prohibido** |

Encounter / Billing / Stock **referencian** `ItemId`; Item **no** conoce esas colecciones.

---

## 12. ReferencePorts (consumo)

| Port | ¿Item v1? | Uso |
|------|-----------|-----|
| `OrganizationReferencePort.existsActiveByIdAndTenant` | Si `OrganizationId` presente | create / update |
| `OfficeReferencePort` | **No** | Stock futuro |
| Patient / Appointment / Encounter ports | **No** | Fuera |

Cancel de ports: N/A (soft-entity archive/activate).

**20.2:** documentar “ports Org listos; sin evolución de contract” salvo que 20.1 descubra un hueco — **no** inventar ports.

---

## 13. Invariantes (normativas para ADR-016)

1. Exactamente un `TenantId`, fijado en create — **nunca cambia**.  
2. Status ∈ {`ACTIVE`, `ARCHIVED`}.  
3. `archive` / mutaciones de registro solo desde estados permitidos (§ Lifecycle).  
4. `displayName` siempre presente (no blank).  
5. Si `code` presente → soft-unique por tenant.  
6. Si `OrganizationId` presente en escritura → Organization ACTIVE en tenant.  
7. Item **no** almacena cantidad, precio, proveedor, BOM, lote ni referencias clínicas.  
8. Item **no** transactionaliza Stock, Billing ni Encounter.  
9. `ItemId` nunca se reasigna.  
10. Cross-tenant access imposible.  
11. Dentro del Tenant, Item es la **única** identidad inventariable de ese rol — no duplicar con “Product”/“Sku” roots en el Core.

*(Permanencia “do not embed stock/pricing/vertical children” — §2.)*

---

## 14. Multi-organization

Un tenant multi-org comparte el **mismo** catálogo de Items (tenant-scoped).  
`OrganizationId` opcional no aísla el catálogo; solo etiqueta custodia.  
Stock futuro por `OfficeId` (office ∈ org) da el aislamiento operativo de cantidades.

---

## 15. Escalabilidad

Aggregate delgado (ids + name + optional code + status + timestamps).  
Sin colecciones hijas.  
Índices previstos (no diseñar SQL aquí): tenant, status, code (parcial), organization opcional.

Millones de SKUs: viable; búsqueda full-text / facets = read-model futuro, no el aggregate.

---

## 16. Permisos (borrador → 20.5)

| Código | Uso |
|--------|-----|
| `item:create` | Alta de identidad inventariable |
| `item:read` | Consulta / listado |
| `item:update` | Corregir registro + **activate** |
| `item:archive` | Soft-retire |

Matriz esperada (espejo Patient): OWNER/ADMIN/MANAGER = lifecycle; USER/READ_ONLY = read.

---

## 17. Módulo / schema / HTTP (propuestas — cerrar en pasos posteriores)

| Artefacto | Propuesta |
|-----------|-----------|
| Gradle | `modules/inventory-management/` (`inventory-domain` · `application` · `infrastructure` · `contract`) |
| Schema | `inventory` |
| HTTP | `/api/v1/inventory/items` |
| OpenAPI group | `inventory-administration` (closeout) |
| Permisos | `item:*` |
| ReferencePort | `ItemReferencePort` en 20.8 |

---

## 18. Qué queda explícitamente fuera (God Aggregate guard)

| Fuera | Dónde va |
|-------|----------|
| Stock quantity | Aggregate `Stock` |
| Adjust / transfer / receive | `StockMovement` |
| Price / tax / tariff | Billing / Pricing |
| Supplier / PO | Procurement |
| BOM / kit | Manufactura / pack |
| Lot / serial / expiry | Aggregate/flags posteriores |
| Images / attachments | Media |
| Encounter consumption | Clinical + ItemId ref |
| POS / cart | Retail pack |
| Asset / depreciation | Asset BC |

---

## 19. Comparación con Patient / Appointment / Encounter

| Aspecto | Patient | Appointment | Encounter | **Item** |
|---------|---------|-------------|-----------|----------|
| Rol | Quién (sujeto) | Planificado | Ocurrido | **Qué (inventariable)** |
| Lifecycle | ACTIVE/ARCHIVED | SCHEDULED/… | IN_PROGRESS/… | **ACTIVE/ARCHIVED** |
| Org | Opcional primary | Obligatoria denorm | Obligatoria denorm | **Opcional custodia** |
| Office | No | Opcional | Opcional | **No** (→ Stock) |
| Intentionally small | Sí | Sí | Sí | **Sí** |

Consistencia de plataforma: registry soft-entity (Patient/Org/Item) vs operational lifecycle (Appointment/Encounter).

---

## 20. Prep. ADR-016 — contenido mínimo

El ADR-016 debe congelar:

1. Definición one-sentence + exclusivity rule  
2. Permanencia *intentionally small*  
3. Campos/refs permitidos vs prohibidos  
4. Tenant-scoped + OrganizationId opcional  
5. Lifecycle ACTIVE/ARCHIVED  
6. Unicidad `ItemId` + soft-unique `code`  
7. Sin UoM / qty / price en v1  
8. Consumption via ReferencePorts only  
9. Consecuencias para Stock / Billing / vertical packs  

---

## Criterio de salida 20.0.1

- [x] Checklist §8 en verde  
- [x] Decisiones irreversibles tabuladas  
- [x] Tenant vs org scope cerrado  
- [x] Office / Stock / UoM / price fuera de Item  
- [x] Lifecycle soft-entity definido  
- [x] Borrador permisos  
- [x] Prep. ADR-016 listo  
- [x] Sin código / tablas / endpoints  

---

## Siguiente paso

**PASO 20.1 — Item Model ADR** ✅ — [PASO-20.1](PASO-20.1-ITEM-MODEL-CONTRACT.md). Siguiente: **20.2 Reference Readiness**.

---

## Referencias

- [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md)  
- [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md) · [PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md](PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ROADMAP.md](../architecture/ROADMAP.md)  
