# CodeCore — Inventory Architecture Review (FASE 20)

**Fecha:** 2026-07-12  
**Tipo:** Revisión arquitectónica post-cierre (sin modificar ADRs · sin reabrir fase · sin código de corrección)  
**Alcance:** FASE 20 — Inventory (`Item` slice) ya cerrada (PASO 20.8)  
**Pregunta:** ¿Inventory (Item) quedó como bounded context reutilizable del Core Platform, o como embrión de WMS / catálogo ERP / vertical?

**Autoridad de contraste:** [CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md) · [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md) · [ADR-016](ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [INVENTORY-CONSUMPTION-GUIDE.md](INVENTORY-CONSUMPTION-GUIDE.md) · PASO 20.0 → 20.8

---

## Executive Summary

**Sí — Inventory (slice Item) es Core Platform, no un WMS y no un vertical.**  
`Item` permanece *intentionally small*, es **independiente** de Patient / Appointment / Encounter, consume Organization **solo** por ID + `OrganizationReferencePort`, publica `ItemReferencePort`, y no incorpora qty, price, BOM, lotes, warehouse, purchasing, clinical consumption ni verbos dentales/retail.

No se encontró un error arquitectónico **crítico** que exija reabrir FASE 20 ni corregir el modelo antes de Stock / Billing.

> **Veredicto operativo: A) La siguiente fase puede comenzar sin cambios en Inventory (Item).**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| DDD | **9.5** | Aggregate Root correcto; freeze ADR-016 §3 explícito anti–God Catalog |
| Hexagonal | **9** | Ports in/out claros; adapter R2DBC del ReferencePort; Org solo por contract |
| Modular Monolith | **9** | Schema `inventory` · `inventory-management` · HTTP `/api/v1/inventory/**` |
| Core Platform | **9.5** | Agnóstico vertical; primer BC no clínico sin arrastrar WMS |
| Desacoplamiento | **9** | Sin deps clinical/scheduling/encounter; SQL solo `inventory.item` |
| Reutilización | **9** | Identidad inventariable sirve ERP/Retail/Hospital/Dental/Vet/Lab/Manufactura |
| Consistencia | **9** | Espejo Patient soft-entity (API, lifecycle, closeout, guía, port) |
| Escalabilidad | **8.5** | Modelo listo para Stock/Movements alrededor; riesgo God-BC si Purchase/WMS se meten sin disciplina |
| Mantenibilidad | **9** | Superficie pequeña; verification 8/8; guide de consumo |
| Visión a largo plazo | **9.5** | Item-first (no Stock-first) + freeze permanente es la decisión de plataforma más valiosa de FASE 20 |
| **Global FASE 20** | **9.2** | Misma disciplina que Records/Scheduling; diversifica el Core sin contaminarlo |

Comparado con revisión Clinical Records 19 (**9.2**) y Scheduling 18 (**9.1**): FASE 20 **mantiene** la disciplina y **prueba** que el patrón soft-registry + ReferencePorts funciona fuera del dominio clínico.

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir FASE 20 / ADR-016? | **No** |
| ¿Cambiar el Aggregate Root? | **No** — Item es el root correcto |
| ¿Bloquear Stock / Billing? | **No** |
| ¿Deuda P0 en Inventory? | **Ninguna** |
| Opción | **A) Siguiente fase sin cambios en Item** |

---

## Respuestas a las 11 preguntas de auditoría

### 1 — Bounded Context

**Inventory es un BC coherente** como *hogar* de identidad inventariable + futuros aggregates de stock/movimiento — **no** como “todo lo que suena a almacén”.

| Evidencia | Lectura |
|-----------|---------|
| Schema `inventory` · módulo `inventory-management` · HTTP `/api/v1/inventory/items` | Límites claros |
| Independiente de `clinical` / `scheduling` / `records` | Sin fusión clínica |
| Consume Org solo vía contract | Downstream correcto |
| Item slice cerrado; Stock diferido explícitamente | Evita God Aggregate en v1 |

**¿Item debería vivir en otro BC (p. ej. Catalog)?**

Alternativa legítima en literatura DDD: *Catalog* ≠ *Inventory*. Se consideró y rechazó en ADR-016 (Product/Sku naming; Stock-first). Critica honesta:

| Opción | Fortalece Core hoy? |
|--------|---------------------|
| Separar `catalog` BC solo para Item | **No ahora** — más módulos sin invariante nuevo; Stock seguiría necesitando el mismo `ItemId` |
| Mantener Item en Inventory + Stock mismo BC | **Sí** — un BC de recursos inventariables con roots separados (Item → Stock → Movement) |

**Riesgo real (disciplina, no reopen):** el nombre “Inventory” invita a meter Purchase, Warehouse Master, MRP, Receiving, Cycle Count… en el mismo módulo hasta volverse **God Bounded Context**. Mitigación: ADR-016 §2–§3 + guía — Procurement / Warehouse / Pricing fuera o como aggregates **nuevos** con audit propio, no “features de Item”.

**¿Preparado para Stock / Movements / Reservations / Receipts / Transfers / Adjustments / Consumption / Warehouse?**

| Aggregate futuro | ¿Puede crecer alrededor de Item sin modificarlo? | Condición |
|------------------|--------------------------------------------------|-----------|
| Stock | **Sí** | `ItemId` + `OfficeId` + qty; port ACTIVE |
| Movements / Adjustments / Transfers | **Sí** | Ledger sobre Stock/ItemId; no engordar Item |
| Reservations | **Sí** | Aggregate propio; Item solo identidad |
| Purchase Receipts | **Cuidado** | Suele ser **Procurement**, no Inventory — no forzar en este BC |
| Warehouse (master) | **Cuidado** | Locus operativo ≈ Office (ADR-011); Warehouse Master aparte si no es Office |
| Consumption (clínico) | **Sí** | Consumer clínico referencia `ItemId`; no vive en Item |

**Conclusión §1:** BC correcto para el slice entregado. El peligro no es Item; es **inflar Inventory** con procurement/WMS sin nuevos límites.

---

### 2 — Aggregate Root

**`Item` es el Aggregate Root correcto.** Stock-first hubiera dejado cantidades huérfanas sin identidad estable (ADR-016 §4) — rechazo correcto.

**Superficie real** (`Item.java`):

- Identidad: `ItemId`, `TenantId` (inmutable)
- Catálogo: `displayName`, `code?`, `primaryOrganizationId?`
- Lifecycle: `ACTIVE` ↔ `ARCHIVED`
- Mutaciones de catálogo solo en `ACTIVE`

| Pregunta | Respuesta |
|----------|-----------|
| ¿Suficientemente pequeño? | **Sí** — espejo soft-registry Patient, aún más delgado |
| ¿God Aggregate en formación? | **No** — tests niegan qty/price/BOM/office/clinical methods |
| ¿Mejor candidato? | **No** — Product/Sku sesga comercio; Stock no es identidad |
| ¿Invariantes ausentes *realmente* importantes? | UoM diferido a Stock — **explícito**, no omisión accidental. Soft-unique `code` en DB (parcial) — suficiente para Core v1 |

---

### 3 — Responsabilidades

**Item solo representa identidad inventariable.** Contaminación revisada:

| Responsabilidad ajena | ¿En Item hoy? | ¿Debería salir? |
|-----------------------|---------------|-----------------|
| Pricing / tax | No | N/A — correcto fuera |
| Purchasing / supplier / PO | No | N/A |
| Manufacturing / BOM | No | N/A |
| Rich Catalog media / SEO | No | N/A |
| Billing unit price | No | N/A |
| Clinical / Encounter / Patient | No | N/A |
| Warehouse qty / Office locus | No | N/A — Stock futuro |
| `PrimaryOrganizationId?` | Sí | **No** — espejo Patient; custodial grouping, **no** ownership de stock (ADR-016 / ADR-011) |

Nada en el modelo entregado “sobra” como responsabilidad de Pricing/Purchasing/Manufacturing/Billing/Clinical/Warehouse.

**Matiz crítico:** `PrimaryOrganizationId` es la pieza más fácil de **malinterpretar** como “el stock pertenece a esta org”. La documentación lo niega; el código no lo refuerza con un nombre tipo `CustodialOrganizationId`. No justifica reopen — sí disciplina en Stock (ownership/qty vive en Stock+Office).

---

### 4 — ReferencePorts / ADR-013

**Limpios en el write path.**

| Dirección | Evidencia |
|-----------|-----------|
| Inventory → Org | `OrganizationReferencePort.existsActiveByIdAndTenant` en create/update |
| Inventory → Patient/Appointment/Encounter | **Ausente** — correcto |
| Repos Org / SQL `org.*` desde Inventory | **No** |
| Stock/Billing → Inventory (publicado) | `ItemReferencePort.existsActiveByIdAndTenant` |

| ¿Violación ADR-013? | **No** en el camino principal |

**Olores (no P0):**

| Olor | Severidad |
|------|-----------|
| `inventory-contract` hace `api(inventory-domain)` → consumidor *puede* importar aggregate `Item` | P2 plataforma (mismo patrón Patient) |
| `ItemQueryPort.existsByTenantIdAndCode*` implementado pero **no usado** en create/update (unicidad vía índice + 409) | P2 higiene / javadoc stale |
| `activate` no revalida primary org ACTIVE | P2 invariante blando (espejo típico soft-entity) |

Ninguno es Repository cross-BC ni acoplamiento clínico.

---

### 5 — Consistencia con fases anteriores

| Aspecto | Org / Patient / Appointment / Encounter | Item | ¿Paridad? |
|---------|------------------------------------------|------|-----------|
| Intentionally small + ADR freeze | ✅ | ✅ ADR-016 | ✅ |
| Soft ACTIVE/ARCHIVED | Patient / Org | ✅ | ✅ |
| Admin API list/get/create/update + lifecycle POST | ✅ | archive/activate | ✅ |
| Sin `tenantId` en JSON | ✅ | ✅ | ✅ |
| Cross-tenant → 404 | ✅ | ✅ 20.7 | ✅ |
| ReferencePort closeout + guía | ✅ | ✅ 20.8 | ✅ |
| Verification E2E 8 checks | ✅ | ✅ | ✅ |
| Permisos 4 neutros | ✅ | `item:*` | ✅ |
| Multi-ReferencePort stress | Appointment/Encounter | Solo Org (correcto para catálogo) | ✅ delta dominio |

**¿Se desvió?** **No.** Inventory es el primer BC **no clínico** y **copia el patrón** Patient soft-registry en lugar de inventar un estilo ERP. Eso fortalece el Core.

Inconsistencia HTTP preexistente (Patient 400 vs Appointment/Encounter 409 en state inválido): Item usa **400** para `InvalidItemState` — alineado a Patient, no a Appointment. Deuda de **plataforma**, no de Inventory (P2).

---

### 6 — Reutilización multi-vertical

Definición operativa: **identidad inventariable de algo que puede stockearse, moverse o consumirse bajo un Tenant.**

| Vertical | ¿Sirve sin modificar Item? | Nota |
|----------|----------------------------|------|
| ERP / Manufactura | **Sí** | Item = material/SKU identidad; BOM/MRP fuera |
| Retail | **Sí** | `code` opcional; price/POS fuera |
| Hospital | **Sí** | Consumibles/equipamiento identidad; lots futuros fuera |
| Dental | **Sí** | Sin “DentalSupply” paralelo |
| Veterinaria | **Sí** | Idem |
| Laboratorio | **Sí** | Reactivos = Item; expiry/lot = agregados posteriores |

Ningún “no” duro. Vertical packs **no** deben crear roots paralelos (ADR-016 §2).

---

### 7 — API

| Aspecto | Estado |
|---------|--------|
| `/api/v1/inventory/items` | ✅ |
| Soft POST archive/activate (no DELETE) | ✅ |
| Default list `ACTIVE` · filtros `q`/`code`/`primaryOrganizationId` | ✅ |
| Activate → `item:update` | ✅ |
| Sin `tenantId` en response | ✅ |
| OpenAPI `inventory-administration` | ✅ |
| Verification 8/8 | ✅ |
| Bodies de error vacíos en handler | P2 higiene (también visto en otros BCs) |

**Inconsistencia material vs Patient/Org:** ninguna estructural. Item es más delgado (sin demographics) — correcto.

---

### 8 — Authorization

| Código | ¿Pertenece a Item? |
|--------|--------------------|
| `item:create\|read\|update\|archive` | **Sí** |
| `item:activate` dedicado | **No** — mapea a `:update` (correcto) |
| `item:stock` / `item:adjust` / `item:transfer` | **Ausentes** — correcto; serán de Stock/Movements |
| Verbos verticales / BOM / price | **Ausentes** — correcto |

Permisos **suficientemente pequeños**. No hay permisos de más ni robados a aggregates futuros.

---

### 9 — Consumo (`INVENTORY-CONSUMPTION-GUIDE`)

| Criterio ADR-013 | ¿Guía alineada? |
|------------------|-----------------|
| Solo `inventory-contract` | ✅ |
| Validar ACTIVE vía port | ✅ |
| No SQL `inventory.item` desde otro BC | ✅ |
| No importar aggregate | ✅ (texto) |
| Recetas Stock / Billing / packs | ✅ sin engordar Item |

**Riesgo residual:** el contract `api`s domain completo — un consumidor descuidado *puede* acoplarse al aggregate pese a la guía. Mitigación = review de PRs consumidores + eventual thin-contract (P2 plataforma, no reopen Item).

La guía **no** invita a acoplamiento; refuerza el freeze.

---

### 10 — Evolución alrededor de Item

| Aggregate / concepto | ¿Independiente sin tocar Item? |
|----------------------|--------------------------------|
| Stock | **Sí** — root nuevo; consume port |
| Movements / Adjustments / Transfers | **Sí** |
| Reservations | **Sí** |
| Warehouse Master | **Sí si ≠ Office**; si locus = Office, no hace falta Warehouse en Item |
| Purchase / Receipts | **Preferible BC Procurement** — no “feature de Inventory” automática |
| Consumption clínico | **Sí** — consumer + `ItemId` |

**Regla operativa:** si la feature responde *how much / where / what moved / what price / what lot / what was purchased or consumed*, **no** pertenece a Item. Si alguien propone `ALTER TABLE inventory.item ADD quantity…` → **violación ADR-016**, no “evolución natural”.

---

### 11 — Riesgos futuros (síntesis)

| Riesgo | Tipo | ¿En código hoy? |
|--------|------|-----------------|
| God Aggregate Item | Core Platform | **No** — freeze + tests |
| God Bounded Context Inventory | Core Platform | **No aún** — riesgo de disciplina post-20 |
| Verticalización (DentalSupply, …) | Core Platform | **No** |
| Acoplamiento clínico | ADR-013 | **No** |
| Duplicidad Product/Sku paralelo | DDD | **Prohibido** por ADR; vigilancia en packs |
| Hexagonal bleed (R2DBC en application) | Hexagonal | P2 patrón Core |
| Contract over-exposure | ADR-013 pureza | P2 patrón Core |

---

## Contaminación (código `inventory-*` main)

| Concepto | ¿Presente como tipo/tabla/API? |
|----------|--------------------------------|
| Qty / Stock balance | **No** |
| Price / cost / tax | **No** |
| BOM / kit / recipe | **No** |
| Lot / serial / expiry | **No** |
| Warehouse / Office en Item | **No** |
| Patient / Encounter / Appointment | **No** |
| Purchase order / supplier | **No** |
| Verbos dental/retail/WMS en permisos | **No** |
| SQL `clinical.*` / `scheduling.*` / `records.*` / `org.*` | **No** |

---

## Deuda técnica (solo real)

### P0 — Crítica

**Ninguna.**

### P1 — Alta

**Ninguna atribuible al modelo Item entregado.**

| Ítem residual (disciplina) | Nota |
|----------------------------|------|
| Tentación God-BC al meter Purchase/WMS en `inventory-management` sin audit | No es defecto del código 20.x; es el principal riesgo **después** del closeout |

No bloquea Stock ni Billing.

### P2 — Baja (higiene / consistencia)

| Ítem | Evidencia | Acción futura (no ahora) |
|------|-----------|---------------------------|
| Unicidad `code` solo en DB; ports `existsBy…Code` muertos + javadoc stale | Application / `ItemQueryPort` | Usar port en write **o** eliminar métodos muertos al tocar el módulo |
| `activate` sin revalidar primary org ACTIVE | Use case | Solo si un invariante de producto lo exige; no es Core blocker |
| `inventory-contract` expone domain completo | `build.gradle.kts` `api(domain)` | Thin-contract platform-wide si duele |
| `spring-boot-starter-data-r2dbc` en `inventory-application` | Gradle | Quitar al tocar el módulo (patrón Patient/Appointment) |
| HTTP error bodies vacíos | `ItemHttpExceptionHandler` | Política platform web |
| `Invalid*State` 400 (Item/Patient) vs 409 (Appointment/Encounter) | Handlers | Alinear política HTTP global |

Si se exige una frase: **no hay deuda P0/P1 de modelo; lo listado es P2 o riesgo de disciplina futura.**

---

## Aspectos destacados

1. **Item-first, no Stock-first** — evita cantidades sin identidad; decisión de plataforma correcta.  
2. **ADR-016 §3 permanence** — anti–God Catalog explícito y testeable.  
3. **Primer BC no clínico** con el mismo ritual (audit → ADR → domain → persist → auth → API audit → API → verification → closeout + port + guía).  
4. **Cero acoplamiento** a la tríada clínica — Inventory no necesita Encounter para existir.  
5. **Permisos y API** alineados a Patient soft-entity sin inventar semántica WMS.  
6. **`ItemReferencePort` mínimo** (boolean ACTIVE) — ADR-013 discipline, no query API gorda.

---

## ¿Debe modificarse algo antes de FASE 21 / Stock?

| Acción | ¿Obligatoria? |
|--------|---------------|
| Reabrir ADR-016 | **No** |
| Mover Item a otro BC | **No** |
| Añadir UoM / price / Office a Item “para Stock” | **No — prohibido** |
| Corregir P2 (ports muertos, thin-contract, …) | **No** antes de continuar |
| Publicar más métodos en `ItemReferencePort` “por si acaso” | **No** — just-in-time cuando Stock/Billing lo demuestren |

**Puede comenzar la siguiente fase sin cambios:** **Sí.**

Stock (mismo BC) o Billing (FASE 21) consumen `ItemId` + `ItemReferencePort` **sin** tocar Item.

---

## Opciones de cierre

| Opción | Criterio | ¿Aplica? |
|--------|----------|----------|
| **A) Siguiente fase sin cambios en Inventory (Item)** | Sin P0/P1 de modelo; BC slice cerrado; port + guía listos | **✅ Elegida** |
| B) Corregir solo críticos antes de seguir | Existiría P0/P1 real | No — no hay |
| C) Reabrir parcialmente FASE 20 / ADR-016 | Evidencia fuerte de modelo roto | No — ADR-016 intacto y correcto |
| D) Separar Catalog BC antes de Stock | Preferencia estética DDD | No — no fortalece el Core hoy |

---

## Conclusión final

FASE 20 entrega lo que el Core necesitaba tras la tríada clínica: un **recurso inventariable estable**, vertical-agnóstico, *intentionally small*, con contrato de consumo para Stock, Movements y material lines — **sin** nacer como WMS ni como catálogo ERP.

CodeCore como Core Platform queda **más fuerte**: el patrón Organization → soft registry → ReferencePort → Administration API → Verification → Closeout funciona **fuera** de lo clínico. El riesgo dominante ya no está en Item; está en **no traicionar el freeze** cuando lleguen qty, lots, compras y warehouses.

**Siguiente paso:** iniciar **Stock** (aggregate independiente en Inventory) y/o **Billing** consumiendo contratos existentes. **No reabrir** FASE 16–20 Item · **No modificar** ADR-016.

---

## Referencias (lectura; no modificadas por esta revisión)

- [ADR-016-ITEM-DOMAIN-MODEL.md](ADR-016-ITEM-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [INVENTORY-CONSUMPTION-GUIDE.md](INVENTORY-CONSUMPTION-GUIDE.md)  
- [PASO-20.8-INVENTORY-CLOSEOUT.md](../audits/PASO-20.8-INVENTORY-CLOSEOUT.md)  
- [PASO-20.7-ITEM-VERIFICATION.md](../audits/PASO-20.7-ITEM-VERIFICATION.md)  
- [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](../audits/PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md)  
- [PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md](../audits/PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md)  
- [CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md)  
- [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md)  
- [ROADMAP.md](ROADMAP.md)  
