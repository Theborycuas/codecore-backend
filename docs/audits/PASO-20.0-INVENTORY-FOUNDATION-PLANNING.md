# PASO 20.0 — Inventory Foundation Planning

**Inventory** es el siguiente bounded context del Core: introduce **qué puede inventariarse** bajo el tenant — sin facturar, sin consumir clínicamente y sin reabrir IAM / Organization / Patient / Appointment / Encounter.

**Fecha:** 2026-07-12  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Definición de FASE 20 · Bounded Context Inventory  
**Dependencias:** FASE 19 cerrada · ADR-011 · ADR-013 · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md) · [CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. BC = **Inventory** · primer Aggregate Root = **`Item`** (identidad inventariable / catálogo stockable)  
2. Consumo **solo** IDs + ReferencePorts (**Organization** opcional · **Office no** en Item v1)  
3. Siguiente: **PASO 20.5.1** Admin API Audit ✅ · siguiente **20.6** Administration API

**Sin código. Sin tablas. Sin endpoints. Sin DTOs. Sin stock qty / movimientos / precios / BOM.**

---

## Objetivo

1. Declarar **FASE 20 — Inventory** tras el cierre de Clinical Records.  
2. Nombrar el BC con rigor y elegir el **primer Aggregate Root** (uno solo).  
3. Fijar el plan de pasos (espejo FASE 17 / 18 / 19).  
4. Dejar explícito qué **no** es este BC (Billing, clinical consumption, ERP manufacturing completo, retail POS, God Catalog).  
5. **Desafiar** si Inventory es realmente el siguiente paso — y concluir con evidencia.

---

## 0. ¿Inventory es el siguiente BC correcto?

### Alternativas consideradas

| Alternativa | ¿Adoptar ahora? | Motivo |
|-------------|-----------------|--------|
| **Inventory** (ROADMAP) | **Sí** | Primer BC **no clínico** tras la tríada Patient · Appointment · Encounter. Prueba Office/Organization como locus operativo de **recursos materiales**. Sirve Dental, Vet, Hospital, Lab, Retail, Manufactura, ERP de servicios **sin** sesgo clínico. |
| Clinical Documents / Notes | No como FASE 20 | Continúa el eje clínico; estrecha el Core hacia “producto de salud”. Mejor **después** de un BC de recursos genéricos, o en paralelo de producto — no antes de diversificar el Core. |
| Billing & Subscriptions | No como FASE 20 | Mezcla dos mundos (seats SaaS vs factura operativa). Puede vivir sin stock; Inventory **no** puede vivir sin catálogo. Billing consume `OrganizationId` / `EncounterId` / futuro `ItemId` — orden correcto: Item antes que líneas de material facturables. |
| Availability / Slots | No | Capacidad de agenda — Scheduling adyacente; no demuestra Org como almacén/locus de stock. |
| Platform Services (Invitations) | No | IAM-adjacent (FASE 22); no dominio de negocio multi-vertical. |
| Product Catalog (BC separado) | No v1 | En Core temprano, **catálogo stockable + inventario** son un solo BC; partir Catalog vs Inventory ahora es sobreingeniería. |

### Por qué no reordenar a Notes o Billing

| Si eligiéramos… | Coste para el Core |
|-----------------|--------------------|
| Notes primero | Refuerza solo verticales clínicos; no aporta BC de recursos |
| Billing primero | Factura sin `ItemId` estable para materiales; mezcla Subscription SaaS con commerce |
| Inventory después de Billing | Líneas de material huérfanas o strings libres — peor contrato |

Reviews 18/19: **FASE 20 puede comenzar sin cambios**. El ROADMAP (PASO 17.0) ya colocó Inventory tras el núcleo clínico por dependencia en `OfficeId` / `OrganizationId`, **independiente** del flujo clínico diario.

**Veredicto:** **Inventory sigue siendo el siguiente Bounded Context correcto.**

```text
IAM → Organization → Patient → Appointment → Encounter → Inventory → (Stock / Movements / Billing / packs)
 CLOSED     CLOSED      CLOSED     CLOSED        CLOSED      FASE 20
```

**Regla de oro FASE 20:** no modificar aggregates ni schemas de IAM, Organization, Patient, Appointment ni Encounter.

---

## 1. Bounded Context — nombre

| Candidato | ¿Adoptar? | Motivo |
|-----------|-----------|--------|
| **Inventory** | **Sí** | Nombre estratégico del ROADMAP: dominio de **recursos inventariables** del tenant |
| Catalog / Product Catalog | No como nombre de fase | Describe el *primer* aggregate (`Item`), no el BC completo (stock + movements vendrán después) |
| Warehouse Management / WMS | No | Sesgo logístico/almacén; Office suele bastar como locus v1 |
| Materials Management | Débil | Sesgo hospitalario/manufactura |
| Stock | No | Es cantidad en ubicación — aggregate **posterior**, no el BC entero |

**Decisión:** el BC se llama **Inventory**.

Es el bounded context donde el Core registra **qué cosas pueden inventariarse** y, más adelante, **cuánto hay dónde** y **cómo se movió** — **sin** convertirse en un ERP vertical ni en un catálogo e-commerce gordo.

```text
Organization / Office (dónde opera / dónde puede haber stock)  → Org Management   CLOSED
Item (qué puede inventariarse)                               → Inventory        FASE 20  ← este BC
Stock / Movement (cuánto / qué cambió)                       → mismo BC         después del Item
```

---

## 2. Primer Aggregate Root: `Item`

### Análisis de candidatos (uno solo gana)

| Candidato | ¿Root de Inventory v1? | Motivo |
|-----------|------------------------|--------|
| **`Item`** | **Sí** | Frontera transaccional de la **identidad inventariable** (catálogo stockable). Parallel de `Patient` en Clinical Foundation: sin “qué es”, no hay stock ni movimiento coherente. Reutilizable en dental (insumos), vet (fármacos/alimentación), hospital (fungibles), lab (reactivos), retail (SKU), manufactura (material), ERP servicios (repuestos). |
| `Product` | **No** | Sesgo comercio/venta; no todo lo inventariable se “vende”. |
| `Sku` | **No** | Sesgo retail; el código puede ser atributo de Item, no el root. |
| `Stock` / `StockLevel` / `InventoryBalance` | **No (v1)** | Cantidad en un locus (`OfficeId` / warehouse). **Necesita** `ItemId`. Segundo aggregate del BC — no el primero. |
| `StockMovement` / `InventoryTransaction` | **No (v1)** | Ledger de cambios; necesita Item (+ Stock). Como Encounter necesita Patient. |
| `Warehouse` | **No (v1)** | Locus de stock. En Core v1, **`Office`** (Org) suele ser el locus; Warehouse propio = sobreingeniería hasta que Office no baste. |
| `Lot` / `Batch` / `Serial` | **No** | Complejidad de trazabilidad; flags/aggregate posterior. |
| `Bom` / `Recipe` | **No** | Manufactura / dental lab — vertical o BC posterior. |
| `Supplier` / `PurchaseOrder` | **No** | Procurement — otro BC o fase. |
| `Asset` / `FixedAsset` | **No** | Activo fijo ≠ inventario consumible; no mezclar. |
| `InventoryItem` (nombre) | **No como nombre** | En muchos ERPs significa “línea de stock”, no el catálogo — confunde. Preferir **`Item`**. |

### Por qué no `Stock` primero (aunque “Inventory” suene a cantidad)

| Si eligiéramos Stock primero… | Riesgo |
|-------------------------------|--------|
| Cantidades sin identidad estable de qué | Strings / códigos huérfanos; imposible ReferencePort limpio |
| Meter catálogo “dentro” del Stock | God Aggregate (qty + name + price + supplier + lot) |
| Office + qty anónimo | No sirve Billing/consumo clínico posterior con `ItemId` |

**Elegido:** **`Item`**.

Detalle irreversible: **PASO 20.0.1** (Aggregate Audit → prep. ADR-016).

---

## 3. One-sentence rule

> **Item** = la identidad inventariable de algo que puede stockearse, moverse o consumirse bajo un tenant.

| Interpretación | ¿Es? |
|----------------|------|
| ¿La ficha / catálogo de “qué es esto”? | **Sí** |
| ¿Cuántas unidades hay en un office? | **No** — eso es `Stock` (aggregate futuro del mismo BC) |
| ¿Una entrada/salida / transferencia? | **No** — eso es `StockMovement` (futuro) |
| ¿Un precio / tarifa / impuesto? | **No** — Pricing / Billing |
| ¿Un producto dental / SKU de tienda? | **No como tipo** — eso es un *uso* del Item genérico |
| ¿Un activo fijo (sillón, equipo)? | **No** — Asset (otro modelo) |

Patrón de frases del Core:

| Aggregate | Una frase |
|-----------|-----------|
| Organization | Unidad estructural del negocio bajo el tenant |
| Office | Locus físico/lógico donde opera el negocio |
| Patient | Identidad clínica registral del sujeto de cuidado |
| Appointment | Compromiso **planificado** de atención en el tiempo |
| Encounter | Episodio de atención que **ocurrió** |
| **Item** | Identidad **inventariable** de algo stockable bajo el tenant |

---

## 4. Por qué NO son Aggregate Root (ahora)

| Concepto | Por qué no es root del BC v1 |
|----------|------------------------------|
| Stock / Balance | Cantidad en ubicación — segundo aggregate |
| StockMovement | Evento de cambio — tercero (o segundo si se elige ledger-first; **no** en Core v1) |
| Warehouse | Locus; usar `OfficeId` hasta evidencia en contrario |
| Lot / Batch / Serial | Trazabilidad avanzada |
| Price / PriceList | Comercial — Billing/Pricing |
| Supplier / Vendor | Procurement |
| PurchaseOrder / GoodsReceipt | Compras |
| BOM / Kit / Bundle | Composición — manufactura / packs |
| SalesOrder / POS line | Retail/commerce |
| Clinical consumption (Encounter link) | Consumo clínico referencia `ItemId`; **no** vive dentro de Item |
| Attachment / Image gallery | Media — ownership distinto |

**Regla:** si responde *“cuánto hay / qué se movió / a qué precio / de qué proveedor / cómo se fabrica / qué es dental”* en lugar de *“qué identidad inventariable existe en este tenant”*, **no** pertenece dentro de `Item`.

---

## 5. Qué consume (solo IDs + ReferencePorts)

| Referencia | ¿En Item v1? | Validación (ADR-013) |
|------------|--------------|----------------------|
| `TenantId` | **Obligatoria** (inmutable) | JWT / TenantContext |
| `OrganizationId` | **Opcional** (custodia / catálogo por unidad de negocio) — **cerrar en 20.0.1** | `OrganizationReferencePort.existsActiveByIdAndTenant` si presente |
| `OfficeId` | **No en Item** | El stock futuro referencia Office; el catálogo no es “un office” |
| `PatientId` / `AppointmentId` / `EncounterId` | **No** | Inventory no es clínico |
| `StaffAssignmentId` / `MembershipId` / `IdentityId` | **No** | Quién cuenta/mueve stock = operación futura, no identidad del Item |

| Prohibido |
|-----------|
| Embed de Organization / Office |
| SQL a `org.*` / `clinical.*` / `scheduling.*` / `records.*` / `iam.*` desde este BC |
| Dependencia Gradle a `*-domain` / `*-infrastructure` ajenos (salvo wiring app) |
| Inventar ports Patient/Encounter “por si el material es clínico” |

Alineado con ADR-011 · ADR-013 · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md).

**Hipótesis a cerrar en 20.0.1:** Item **tenant-scoped** con `OrganizationId` opcional (espejo `Patient.primaryOrganizationId`) vs Item **obligatoriamente** org-scoped. No decidir tabla aquí — solo el trade-off.

---

## 6. Qué publica (más adelante)

| Artefacto | ¿Cuándo? | Para quién |
|-----------|----------|------------|
| `ItemId` | Desde domain/contract | Stock, Movements, Billing lines, clinical consumption, packs |
| `ItemReferencePort` | Closeout FASE 20 | `existsActiveByIdAndTenant` (o equivalente soft-status) |
| Guía de consumo | Closeout | Billing, Stock, vertical packs |

---

## 7. Qué NO pertenece a este BC

| Fuera | Por qué |
|-------|---------|
| IAM / Authentication | Plataforma cerrada |
| Organization / Office / StaffAssignment ownership | FASE 16 cerrada — solo IDs |
| Patient / Appointment / Encounter | FASE 17–19 cerradas — no mezclar clínico en catálogo |
| Billing / Invoices / Subscriptions | FASE 21 |
| Pricing engine | Comercial — no Item |
| Procurement / MRP / WMS completo | Futuro / packs |
| POS / e-commerce cart | Vertical retail |
| Notes / SOAP / Labs clínicos | Clinical Records adyacente |
| Notifications / Workflows | Platform / otros BC |
| Event bus “por si acaso” | Solo si un invariante eventual lo exige |
| Organization-scoped RBAC | ADR-007 intacto |
| Product packs Dental / Vet / Hospital / Retail | Componen el Core; no viven dentro de Item |

---

## 8. Cómo evitar el God Aggregate “Inventory”

Fracaso típico: un solo aggregate `InventoryItem` con qty, lotes, precios, proveedores, BOM, imágenes y reglas dentales.

| Defensa | Aplicación en FASE 20 |
|---------|----------------------|
| One-sentence rule | Solo identidad inventariable |
| Soft-entity likely | `ACTIVE` / `ARCHIVED` (cerrar en 20.0.1) — no DELETE físico |
| Stock separado | Aggregate futuro `Stock` (`ItemId` + `OfficeId` + qty) |
| Movements separados | Aggregate/ledger futuro |
| Sin precio en Item | Billing/Pricing |
| Sin EncounterId en Item | Consumo clínico referencia Item, no al revés |
| Tests negativos | Negar `addStock`, `setPrice`, `addBom`, `addLot` en domain tests (como Encounter negó SOAP) |

**Permanencia (a congelar en ADR-016):** *Item is intentionally small.*

---

## 9. FASE 20 — estructura completa

**Bounded context:** Inventory  
**Primer Aggregate Root:** `Item`  
**Módulo Gradle (propuesta):** `modules/inventory-management/` (domain · application · infrastructure · contract) — nombre final en 20.0.1  
**Schema SQL (propuesta):** `inventory` — decisión en 20.0.1  
**HTTP (cerrar en audit API):** `/api/v1/inventory/items` — decisión en 20.5.1  

### Pasos (espejo 17 / 18 / 19)

| Paso | Nombre | Objetivo | Auditoría / ADR | Entregable |
|------|--------|----------|-----------------|------------|
| **20.0** | Inventory Foundation Planning | Este documento + ROADMAP | Este PASO | Plan FASE 20 |
| **20.0.1** | Item Aggregate Audit | Checklist política; lifecycle; refs; *intentionally small*; tenant vs org scope | **Obligatoria** | Modelo + prep. ADR-016 |
| **20.1** | Item Model ADR | Congelar modelo irreversible | **ADR-016** | ADR Accepted |
| **20.2** | Inventory Reference Readiness | Verificar Org ports; **sin** ports “por si acaso” | Contract check | Ports listos para escritura |
| **20.3** | Item Domain Foundation | Aggregate + VOs + tests | — | Dominio puro |
| **20.4** | Item Persistence | Flyway + R2DBC + ITs | — | Schema `inventory` |
| **20.5** | Item Authorization Contract | `item:*` (o `inventory-item:*`) + seed | Mínima | Catalog + Flyway |
| **20.5.1** | Item Admin API Audit | HTTP/DTO/paginación/lifecycle | **Obligatoria** | Contrato HTTP |
| **20.6** | Item Administration API | Use cases + controller + ITs | — | `/api/v1/inventory/items` |
| **20.7** | Item Verification | E2E: refs, RBAC, tenant, OpenAPI | — | `ItemVerificationIT` |
| **20.8** | Inventory Closeout (slice Item) | Guía consumo + `ItemReferencePort` + ROADMAP | — | Slice Item cerrado |

### ¿Stock en la misma FASE 20?

**No en el slice v1 (20.0–20.8).**  
Tras closeout de `Item`, el ROADMAP puede abrir **Stock** como siguiente aggregate del **mismo** BC (pasos 20.9+ o FASE 20b) — espejo de cómo Notes vendría *después* de Encounter, no dentro.

Meter Stock en 20.0–20.8 diluye el primer root y aumenta riesgo de God design.

### Auditorías / ADR obligatorios

| Trigger (política) | Paso |
|--------------------|------|
| Nuevo Aggregate Root `Item` | **20.0.1** |
| Nuevo módulo + BC Inventory | **20.0.1** |
| Nuevo ADR de modelo | **20.1 → ADR-016** |
| Shape Admin API | **20.5.1** |

---

## 10. ReferencePorts

| Port | ¿Crear en FASE 20? | Nota |
|------|-------------------|------|
| `OrganizationReferencePort` | **No** — ya publicado | Consumir si Item lleva `OrganizationId` |
| `OfficeReferencePort` | **No para Item v1** | Reservado para Stock futuro |
| Patient / Appointment / Encounter ports | **No** | Fuera de Inventory catalog |
| `ItemReferencePort` | **Sí — closeout 20.8** | Para Stock / Billing / consumo clínico / packs |
| Ports Stock / Movement | **No** | No hay esos aggregates aún |

---

## 11. Cómo fortalece el Core Platform

| Señal de plataforma | Cómo lo aporta Item |
|---------------------|---------------------|
| Diversificación post-clínico | Primer BC de **recursos** — Core ≠ solo salud |
| Vertical-agnóstico | Mismo Item para insumos, reactivos, SKUs, materiales, repuestos |
| Consumo Org sin reabrir | Stress-test ADR-011/013 fuera del grafo clínico |
| Superficie estable | `ItemId` para Stock, Billing de materiales, consumo en Encounter (futuro) |
| *Intentionally small* | Resiste ERP monolítico / catálogo e-commerce en el Core |

**No** optimiza Dental supplies. **No** es un WMS hospitalario. **No** es un e-commerce.  
Es el **núcleo de identidad inventariable** sobre el que esos productos construyen packs.

```text
IAM → Organization → (Patient → Appointment → Encounter)
         CLOSED              CLOSED CLINICAL STACK
                ↘
                  Inventory (Item) → Stock → Movements → Billing lines / packs
```

---

## 12. Criterio de cierre FASE 20 (slice Item)

1. `Item` según ADR-016 (frozen) — *intentionally small*.  
2. Consumo Organization (si aplica) **solo** por IDs + ReferencePorts.  
3. Verification E2E verde.  
4. ROADMAP FASE 20 slice Item ✅ · siguiente **Stock** (mismo BC) o **Billing** según plan post-closeout.  
5. Ningún aggregate de IAM / Organization / Patient / Appointment / Encounter modificado (salvo seeds IAM acotados).  
6. `ItemReferencePort` + guía de consumo publicados en closeout.

---

## Relación con reviews previos

| Fuente | Tratamiento en FASE 20 |
|--------|------------------------|
| Clinical Records Review — opción A | ✅ Arranque sin cambios estructurales |
| ROADMAP PASO 17.0 — Inventory tras clínico | ✅ Confirmado; primer root = Item no Stock |
| Org guide — Office como locus | ✅ Stock futuro usa Office; Item no |
| Eventing vacío | No implementar bus preventivo |

---

## Checklist

- [x] Inventory desafiado vs Notes / Billing / Catalog separado — **sigue siendo el siguiente BC**  
- [x] Primer Aggregate Root = **`Item`** (no Stock, no Product, no Warehouse)  
- [x] One-sentence rule definida  
- [x] Fuera de alcance explícito (qty, price, BOM, clinical, POS)  
- [x] Plan de pasos 20.0 → 20.8  
- [x] ADR-016 previsto  
- [x] Sin tablas / endpoints / permisos / DTOs en este paso  

---

## Siguiente paso

**PASO 20.0.1 — Item Aggregate Audit** ✅ · **PASO 20.1 — ADR-016** ✅.

**Siguiente:** **PASO 20.6 — Item Administration API**.

---

## Referencias

- [PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md](PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md)  
- [CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md)  
- [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md) · [PASO-18.0](PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · [PASO-19.0](PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
