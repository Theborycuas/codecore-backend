# PASO 21.0 — Billing Foundation Planning

**Billing** es el siguiente bounded context **nuevo** del Core: introduce **la reclamación comercial de un importe adeudado** bajo el tenant — sin contabilidad, sin cobros, sin motor fiscal, sin stock y sin reabrir IAM / Organization / Patient / Appointment / Encounter / Inventory (Item).

**Fecha:** 2026-07-12  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Definición de FASE 21 · Bounded Context Billing  
**Dependencias:** FASE 20 Item cerrada · ADR-011 · ADR-013 · ADR-016 · [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) · [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](../architecture/CLINICAL-RECORDS-CONSUMPTION-GUIDE.md) · [CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. BC = **Billing** · primer Aggregate Root = **`Invoice`** (reclamación comercial de importe adeudado)  
2. Consumo **solo** IDs + ReferencePorts (**Organization** obligatorio · **Patient / Encounter / Item** opcionales según invariantes 21.0.1 · **Stock no** en v1)  
3. Siguiente: **PASO 21.0.1** — Invoice Aggregate Audit  

**Sin código. Sin tablas. Sin endpoints. Sin DTOs. Sin Payments / Tax Engine / GL / Subscriptions / Stock.**

---

## Objetivo

1. **Desafiar** si Billing es realmente el siguiente paso — vs Stock, Notes, Platform, Procurement.  
2. Si sí: declarar **FASE 21 — Billing** y nombrar el BC con rigor.  
3. Elegir el **primer Aggregate Root** (uno solo).  
4. Fijar el plan de pasos (espejo FASE 17–20).  
5. Dejar explícito qué **no** es este BC (Accounting, ERP, Tax, Inventory, Payments, Clinical, CRM, Subscriptions SaaS).

---

## 0. ¿Billing es el siguiente BC correcto?

### Criterio de decisión (filosofía FASE 21+)

Un BC nuevo debe:

1. Consumir BCs previos **solo** vía IDs + ReferencePorts.  
2. Publicar contratos que **sí** tendrán consumidores claros.  
3. Alargar la **cadena lógica** del Core (no quedar aislado).

### Alternativas consideradas

| Alternativa | ¿Adoptar ahora? | Motivo |
|-------------|-----------------|--------|
| **Billing** (ROADMAP FASE 21) | **Sí** | Primer BC **económico** del Core. Cierra el arco operativo: *quién / dónde / plan / ocurrió / qué puede inventariarse* → **qué se adeuda**. Consume Org + (opcional) Patient / Encounter / Item. Publica `InvoiceId` para Payments / Reporting / packs. Sirve Dental, Vet, Hospital, Lab, Retail, ERP servicios, Manufactura (cobro de trabajo) **sin** sesgo clínico. |
| **Stock** (mismo BC Inventory) | **No como FASE 21** | Es el **siguiente aggregate de Inventory**, no un BC nuevo. Completa recursos materiales; **no** crea nodo económico del ecosistema. Billing **no necesita** qty en mano para emitir una reclamación comercial (servicios, honorarios, materiales por catálogo). Stock puede (y debe) llegar **después o en paralelo de producto**, sin bloquear Billing. |
| Clinical Documents / Notes | No | Continúa solo el eje clínico; estrecha el Core hacia “producto de salud”. Encounter ya publica port para Notes — Notes no desbloquea dinero. |
| Platform Services (Invitations, password) | No | IAM-adjacent (FASE 22); no dominio de negocio multi-vertical. |
| Procurement / Purchase | No | Cadena de suministro; no cierra el arco clínico→económico ni el retail de cobro al cliente. |
| Payments / PSP | No | **Cobra** lo adeudado; necesita `InvoiceId` (o Charge) estable primero — espejo Stock-needs-Item. |
| Pricing / FeeSchedule engine | No | Catálogo de precios; útil, pero no es la reclamación. Precio puede vivir en líneas de Invoice v1 como importe **ya resuelto**, sin motor. |
| Subscriptions (SaaS seats) | **No dentro de Billing** | [DEVELOPMENT-POLICY](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) ya separa **Billing** (finanzas operativas) de **Subscription** (modelo comercial SaaS). Mezclarlos = God BC. Subscriptions → Platform / fase aparte. |

### Por qué no Stock antes (aunque Inventory Review lo mencionara)

| Si eligiéramos Stock como “FASE 21”… | Coste para el Core |
|--------------------------------------|--------------------|
| Seguiríamos dentro de Inventory | No aparece BC económico; cadena `Encounter → dinero` sigue rota |
| Billing seguiría esperando | Productos SaaS (clínicos y no) facturan **servicios** sin stock |
| Item ya tiene consumidor potencial | Billing **es** el primer consumidor **cross-BC** real de `ItemReferencePort` (material lines); Stock es consumidor **intra-BC** |

Inventory Review (**opción A**): Stock **o** Billing pueden arrancar sin reabrir Item.  
Elegir Billing como **FASE 21** no cancela Stock: queda como continuación natural de Inventory (pasos propios), no como sustituto de la columna económica del Core.

### Por qué no reordenar a Notes o Platform

| Si eligiéramos… | Coste para el Core |
|-----------------|--------------------|
| Notes primero | Solo verticales clínicos; sin dinero; sin stress-test económico multi-port |
| Platform primero | Infraestructura de acceso; no dominio de negocio compartido |
| Billing después de Payments | Cobrar sin documento de adeudo — peor contrato |

### Veredicto

**Sí — Billing es el siguiente Bounded Context *nuevo* correcto.**

Stock es importante, pero es **evolución del BC Inventory**, no el siguiente BC del ecosistema.

```text
IAM → Organization → Patient → Appointment → Encounter → Inventory (Item)
 CLOSED     CLOSED      CLOSED     CLOSED        CLOSED         CLOSED
                                                              ↘ Stock (mismo BC, no FASE 21)
         ↘
           Billing (Invoice) → Payments / Reporting / packs
           FASE 21
```

**Regla de oro FASE 21:** no modificar aggregates ni schemas de IAM, Organization, Patient, Appointment, Encounter ni Inventory (Item). Seeds IAM de permisos `invoice:*` (o decididos) = cambio acotado permitido (espejo fases previas).

---

## 1. Bounded Context — nombre

| Candidato | ¿Adoptar? | Motivo |
|-----------|-----------|--------|
| **Billing** | **Sí** | Nombre estratégico ROADMAP + política: finanzas operativas del tenant (reclamaciones comerciales) |
| Invoicing | Débil como nombre de fase | Describe el *primer* aggregate; el BC crecerá (créditos, statements) |
| Accounting / Ledger / GL | **No** | Contabilidad financiera — otro BC |
| Revenue / RCM | **No** | Sesgo hospitalario USA |
| Commerce / Orders | **No** | Sesgo retail/e-commerce; Order ≠ Invoice |
| Accounts Receivable | Débil | Subconjunto; suena a módulo ERP completo |

**Decisión:** el BC se llama **Billing**.

Es el bounded context donde el Core registra **que existe una reclamación comercial de dinero adeudado** — **sin** convertirse en ERP, contabilidad, pasarela de pagos ni CRM.

```text
Organization (quién factura / unidad emisora)     → Org Management     CLOSED
Patient? (sujeto clínico facturable, opcional)   → Clinical Found.    CLOSED
Encounter? (episodio origen, opcional)           → Clinical Records   CLOSED
Item? (material en línea, opcional)              → Inventory          CLOSED
Invoice (reclamación comercial)                  → Billing            FASE 21  ← este BC
Payment (liquidación)                            → Payments / mismo BC después — NO v1
```

---

## 2. Primer Aggregate Root: `Invoice`

### Análisis de candidatos (uno solo gana)

| Candidato | ¿Root de Billing v1? | Motivo |
|-----------|----------------------|--------|
| **`Invoice`** | **Sí** | Frontera transaccional de la **reclamación comercial** (documento de importe adeudado). Universal en Dental, Vet, Hospital, Lab, Retail, ERP, Manufactura. `InvoiceId` estable para Payments / statements / packs. |
| `Charge` / `BillableCharge` | **No (v1)** | Unidad atómica de cobro — útil y puede ser **segundo** aggregate (agrupada por Invoice). Empezar solo con Charge deja sin documento comercial intercambiable; empeora retail/ERP. Si 21.0.1 demuestra que Invoice engorda, se puede **invertir** (Charge-first) vía ADR — no asumir ahora. |
| `BillingAccount` / `Customer` | **No (v1)** | Identidad de “quién paga” → deriva a **CRM**. Preferir refs a Patient / Organization existentes; cuenta de facturación solo si 21.0.1 prueba hueco real. |
| `PriceList` / `FeeSchedule` / `Tariff` | **No** | Pricing — motor aparte; Invoice lleva importes **ya determinados**. |
| `Payment` / `PaymentIntent` | **No** | Liquidación — necesita Invoice (o Charge) primero. |
| `CreditNote` / `Refund` | **No (v1)** | Documento posterior alrededor de Invoice. |
| `LedgerEntry` / `Journal` | **No** | Accounting. |
| `Subscription` / `Plan` / `Seat` | **No** | SaaS commercial — BC/fase aparte (política). |
| `Claim` (seguro) | **No** | Vertical seguros/salud; pack o BC posterior. |
| `TaxDocument` / `FiscalInvoice` | **No** | Tax / compliance local — fuera del Core genérico. |

### Por qué no `Charge` primero (aunque sea más “pequeño”)

| Si eligiéramos Charge primero… | Riesgo |
|--------------------------------|--------|
| Sin documento comercial | Retail/ERP esperan Invoice; packs reinventan el agrupador |
| Payments anclan a Charge | Multiplicidad de liquidaciones; peor DX multi-vertical |
| Invoice “después” siempre llega | Mejor congelar Invoice *intentionally small* que diferir el documento |

**Elegido:** **`Invoice`**.

Detalle irreversible: **PASO 21.0.1** (Aggregate Audit → prep. ADR-017).

### Hipótesis a cerrar en 21.0.1 (no decidir tablas aquí)

| Tema | Trade-off |
|------|-----------|
| **Emisor** | `OrganizationId` obligatorio (quién emite) — espejo Encounter |
| **Deudor / bill-to** | ¿Solo `PatientId`? (sesgo clínico) · ¿`OrganizationId` cliente B2B? · ¿exactamente uno de {Patient, Org}? · ¿BillingAccount futuro? — **debe servir ERP/Retail sin Patient** |
| **Líneas** | ¿Entidades internas del Invoice vs Charge aggregate separado? — preferir **líneas internas mínimas** en v1 si caben en one-sentence; partir si aparecen invariantes propias |
| **Orígenes opcionales** | `EncounterId?` · `AppointmentId?` · `ItemId?` en cabecera o línea — solo si invariante de escritura lo exige; **no** StockId |
| **Dinero** | Currency + amount (minor units) — VO; sin FX engine |
| **Lifecycle** | p. ej. `DRAFT → ISSUED → VOIDED` — **sin** `PAID` (Payments) |

---

## 3. One-sentence rule

> **Invoice** = la reclamación comercial de que un importe es adeudado por una parte a una organización emisora bajo un tenant.

| Interpretación | ¿Es? |
|----------------|------|
| ¿El documento / claim de “se debe este dinero”? | **Sí** |
| ¿El cobro / captura de tarjeta / PSP? | **No** — Payments |
| ¿El asiento contable / mayor? | **No** — Accounting |
| ¿La lista de precios / tarifa? | **No** — Pricing |
| ¿La cantidad en almacén? | **No** — Stock (Inventory) |
| ¿La nota clínica / episodio? | **No** — puede *referenciar* Encounter |
| ¿La suscripción SaaS del tenant? | **No** — Subscription |
| ¿Algo dental / retail específico? | **No como tipo** — eso es un *uso* del Invoice genérico |

Patrón de frases del Core:

| Aggregate | Una frase |
|-----------|-----------|
| Organization | Unidad estructural del negocio bajo el tenant |
| Patient | Identidad clínica registral del sujeto de cuidado |
| Appointment | Compromiso **planificado** de atención en el tiempo |
| Encounter | Episodio de atención que **ocurrió** |
| Item | Identidad **inventariable** de algo stockable bajo el tenant |
| **Invoice** | Reclamación **comercial** de un importe adeudado bajo el tenant |

---

## 4. Por qué NO son Aggregate Root (ahora)

| Concepto | Por qué no es root del BC v1 |
|----------|------------------------------|
| Payment / Refund capture | Liquidación — BC/aggregate posterior |
| Tax calculation / fiscal stamp | Tax Engine / compliance local |
| General Ledger / Journal | Accounting |
| PriceList / FeeSchedule | Pricing |
| Subscription / Plan / Seat | SaaS Subscription (política) |
| Customer / CRM Contact | CRM — usar Patient / Org refs |
| Insurance Claim | Vertical RCM |
| Sales Order / Cart / POS ticket | Commerce — Order ≠ Invoice |
| Stock deduction | Inventory Stock / Movement |
| Encounter / Clinical note | Clinical — solo ID opcional |
| CreditNote | Documento posterior alrededor de Invoice |
| Statement / Aging report | Read model / reporting |

**Regla:** si responde *“cómo se cobró / cómo se contabilizó / qué impuesto / qué plan SaaS / cuánto stock / qué escribió el clínico / quién es el lead CRM”* en lugar de *“qué reclamación comercial de importe adeudado existe en este tenant”*, **no** pertenece dentro de `Invoice`.

---

## 5. Qué consume (solo IDs + ReferencePorts)

| Referencia | ¿En Invoice v1? | Validación (ADR-013) |
|------------|-----------------|----------------------|
| `TenantId` | **Obligatoria** (inmutable) | JWT / TenantContext |
| `OrganizationId` (emisor) | **Obligatoria** (hipótesis 21.0.1) | `OrganizationReferencePort.existsActiveByIdAndTenant` |
| Bill-to (`PatientId` y/o otra parte) | **Cerrar en 21.0.1** — debe permitir **no clínico** | `PatientReferencePort` si Patient; Org port si B2B; **no** inventar Customer |
| `EncounterId` | **Opcional** | `EncounterReferencePort.findLinkable…` o existencia según audit — **solo si** línea/cabecera lo lleva |
| `AppointmentId` | **Opcional / probablemente no** | Evitar dualidad plan/ocurrido; preferir Encounter como origen clínico |
| `ItemId` | **Opcional** (línea de material) | `ItemReferencePort.existsActiveByIdAndTenant` |
| `OfficeId` / `StaffAssignmentId` | **No en v1** salvo audit lo exija | No engordar Invoice con locus operativo |
| `StockId` / qty on hand | **No** | Billing no es Inventory |
| `MembershipId` / `IdentityId` | **No** como deudor | Quién opera la API ≠ quién debe |

| Prohibido |
|-----------|
| Embed de Patient / Encounter / Item / Organization |
| SQL a `clinical.*` / `scheduling.*` / `records.*` / `inventory.*` / `org.*` / `iam.*` desde este BC |
| Dependencia Gradle a `*-domain` / `*-infrastructure` ajenos (salvo wiring app) |
| Inventar ports Stock / Payment / Tax “por si acaso” |
| Reabrir Encounter/Item para “campos de facturación” |

Alineado con ADR-011 · ADR-013 · guías Patient / Clinical Records / Inventory / Organization.

### Cómo se conecta sin romper BCs cerrados

| BC | Relación |
|----|----------|
| **IAM** | Tenant + JWT + permisos `invoice:*` (seed); Invoice ≠ Identity |
| **Organization** | Emisor (y posiblemente bill-to B2B) vía port |
| **Patient** | Bill-to clínico opcional vía port — Patient **no** gana campos de factura |
| **Appointment** | Preferible **no** enlazar en v1; cobro clínico ancla en Encounter |
| **Encounter** | Origen clínico opcional vía port — Encounter **no** embebe importes |
| **Inventory (Item)** | Línea material opcional vía `ItemId` + port — Item **no** gana precio |
| **Stock** | **No** en FASE 21 v1 |

---

## 6. Qué publica (más adelante)

| Artefacto | ¿Cuándo? | Para quién |
|-----------|----------|------------|
| `InvoiceId` | Desde domain/contract | Payments, CreditNotes, Reporting, packs |
| `InvoiceReferencePort` | Closeout FASE 21 | p. ej. `existsIssuedByIdAndTenant` (forma exacta en closeout) |
| Guía de consumo | Closeout | Payments, Accounting bridge, vertical packs |

**Consumidores claros (obligatorio por filosofía):**

| Consumidor | Uso de Invoice |
|------------|----------------|
| **Payments** (siguiente lógico) | Liquidar / registrar pago contra Invoice ISSUED |
| Reporting / statements | Saldos y documentos emitidos |
| Vertical packs | Reglas de presentación; no el modelo Core |
| Accounting (futuro) | Exportar hechos — **sin** que Billing sea el GL |

---

## 7. Qué NO pertenece a este BC

| Fuera | Por qué |
|-------|---------|
| IAM / Authentication | Plataforma cerrada |
| Organization / Office / Staff ownership | FASE 16 — solo IDs |
| Patient / Appointment / Encounter ownership | FASE 17–19 — solo IDs opcionales |
| Inventory Item / Stock / Movements | FASE 20 / continuación Inventory — solo `ItemId?` |
| **Payments / PSP / card capture** | BC o aggregate posterior |
| **Accounting / GL / Journal** | Otro BC |
| **Tax Engine / e-invoicing fiscal** | Compliance local / packs |
| **Subscriptions / Plans / Seats** | Subscription (política) — **no** FASE 21 |
| Pricing engine / dynamic tariffs | Pricing BC o aggregate posterior |
| CRM / Customer 360 | CRM |
| Insurance claims / RCM | Vertical |
| POS cart / Sales Order | Commerce |
| Notes / SOAP / clinical content | Clinical Records |
| Notifications | Platform |
| Event bus “por si acaso” | Solo si invariante eventual lo exige |
| Organization-scoped RBAC | ADR-007 intacto |
| Product packs Dental / Vet / Retail | Componen el Core; no viven dentro de Invoice |

---

## 8. Cómo evitar el God Aggregate / God BC “Billing ERP”

Fracaso típico: un solo módulo “Billing” con factura + cobros + impuestos + asientos + suscripciones + CRM + inventario.

| Defensa | Aplicación en FASE 21 |
|---------|----------------------|
| One-sentence rule | Solo reclamación comercial de importe adeudado |
| Sin estado `PAID` en Invoice v1 | Payments aparte |
| Sin tax engine | Importe ya resuelto; fiscal fuera |
| Sin GL | Accounting fuera |
| Sin Subscription | Fase/BC aparte (política) |
| Sin Stock | Inventory fuera |
| Sin Customer aggregate | Refs Patient/Org |
| Líneas mínimas | Partir a Charge si engordan |
| Tests negativos | Negar `addPayment`, `postToLedger`, `calculateTax`, `addSubscription`, `deductStock` en domain tests |
| Permanencia ADR | *Invoice is intentionally small* (congelar en ADR-017) |

---

## 9. FASE 21 — estructura completa

**Bounded context:** Billing  
**Primer Aggregate Root:** `Invoice`  
**Módulo Gradle (propuesta):** `modules/billing-management/` (domain · application · infrastructure · contract) — nombre final en 21.0.1  
**Schema SQL (propuesta):** `billing` — decisión en 21.0.1  
**HTTP (cerrar en audit API):** `/api/v1/billing/invoices` — decisión en 21.5.1  

### Pasos (espejo 17–20)

| Paso | Nombre | Objetivo | Auditoría / ADR | Entregable |
|------|--------|----------|-----------------|------------|
| **21.0** | Billing Foundation Planning | Este documento + ROADMAP | Este PASO | Plan FASE 21 |
| **21.0.1** | Invoice Aggregate Audit | Checklist política; lifecycle; bill-to; líneas; *intentionally small*; refs | **Obligatoria** | Modelo + prep. ADR-017 |
| **21.1** | Invoice Model ADR | Congelar modelo irreversible | **ADR-017** | ADR Accepted |
| **21.2** | Billing Reference Readiness | Verificar Org / Patient / Encounter / Item ports; evolucionar **solo** si 21.0.1 lo exige — **sin** ports “por si acaso” | Contract check | Ports listos para escritura |
| **21.3** | Invoice Domain Foundation | Aggregate + VOs + tests | — | Dominio puro |
| **21.4** | Invoice Persistence | Flyway + R2DBC + ITs | — | Schema `billing` |
| **21.5** | Invoice Authorization Contract | `invoice:*` (nombre exacto en audit) + seed | Mínima | Catalog + Flyway |
| **21.5.1** | Invoice Admin API Audit | HTTP/DTO/paginación/lifecycle | **Obligatoria** | Contrato HTTP |
| **21.6** | Invoice Administration API | Use cases + controller + ITs | — | `/api/v1/billing/invoices` |
| **21.7** | Invoice Verification | E2E: refs, RBAC, tenant, OpenAPI | — | `InvoiceVerificationIT` |
| **21.8** | Billing Closeout (slice Invoice) | Guía consumo + `InvoiceReferencePort` + ROADMAP | — | Slice Invoice cerrado |

### ¿Payments o Charge en la misma FASE 21?

**No en el slice v1 (21.0–21.8).**  
Tras closeout de `Invoice`, el ROADMAP puede abrir **Payment** (mismo BC o BC Payments) o **Charge** como aggregate posterior — espejo Stock tras Item / Notes tras Encounter.

Meter Payments o Tax en 21.0–21.8 diluye el primer root y convierte Billing en ERP.

### Auditorías / ADR obligatorios

| Trigger (política) | Paso |
|--------------------|------|
| Nuevo Aggregate Root `Invoice` | **21.0.1** |
| Nuevo módulo + BC Billing | **21.0.1** |
| Nuevo ADR de modelo | **21.1 → ADR-017** |
| Evolución ReferencePorts existentes (si aplica) | **21.2** (no reabrir ADR-012…016) |
| Shape Admin API | **21.5.1** |

---

## 10. ReferencePorts

| Port | ¿Crear / tocar en FASE 21? | Nota |
|------|---------------------------|------|
| `OrganizationReferencePort` | **Consumir** | Emisor (+ bill-to B2B si aplica) |
| `PatientReferencePort` | **Consumir si** bill-to Patient | Sin inventar Customer |
| `EncounterReferencePort` | **Consumir solo si** 21.0.1 exige enlace | Ya publicado (19.8) |
| `ItemReferencePort` | **Consumir solo si** líneas de material | Ya publicado (20.8) |
| `AppointmentReferencePort` | **Preferible no** en v1 | Evitar origen dual |
| `OfficeReferencePort` / Staff | **No** salvo audit | |
| Stock / Payment / Tax ports | **No** | No existen esos aggregates |
| `InvoiceReferencePort` | **Sí — closeout 21.8** | Para Payments / packs |

---

## 11. Cómo fortalece el Core Platform

| Señal de plataforma | Cómo lo aporta Invoice |
|---------------------|------------------------|
| Cadena lógica completa | Tras “ocurrió” e “inventariable”, aparece “se adeuda” — Core usable como producto SaaS |
| Consumidor cross-BC de Item | Primera prueba real de `ItemReferencePort` **fuera** de Inventory |
| Stress-test multi-port | Org + (Patient?) + (Encounter?) + (Item?) sin reabrir providers |
| Vertical-agnóstico | Misma Invoice para honorarios, materiales, retail, B2B servicios |
| Separación Subscription | Evita God “Billing & SaaS seats” |
| *Intentionally small* | Resiste ERP / RCM / Tax / Payments dentro del Core |

**No** optimiza Dental billing. **No** es un HIS de facturación hospitalaria. **No** es un POS. **No** es Stripe.  
Es el **núcleo de reclamación comercial** sobre el que esos productos construyen packs y pasarelas.

```text
IAM → Organization → Patient → Appointment → Encounter → Item
         CLOSED        CLOSED     CLOSED        CLOSED      CLOSED
                ↘                           ↗        ↗
                  Billing (Invoice) ←────────┴────────┘
                  FASE 21
                       → Payments / Accounting bridge / packs
```

---

## 12. Criterio de cierre FASE 21 (slice Invoice)

1. `Invoice` según ADR-017 (frozen) — *intentionally small*.  
2. Consumo Org / Patient / Encounter / Item (según modelo) **solo** por IDs + ReferencePorts.  
3. Verification E2E verde.  
4. ROADMAP FASE 21 slice Invoice ✅ · siguiente **Payments** y/o **Stock** (Inventory) según plan post-closeout — **sin** mezclar Subscriptions en Billing.  
5. Ningún aggregate de IAM / Organization / Patient / Appointment / Encounter / Item modificado (salvo seeds IAM acotados).  
6. `InvoiceReferencePort` + guía de consumo publicados en closeout.  
7. Ningún Payment / Tax / GL / Subscription / Stock embebido en Invoice.

---

## Relación con reviews y política previos

| Fuente | Tratamiento en FASE 21 |
|--------|------------------------|
| Inventory Review — Stock o Billing | ✅ Billing como **BC nuevo**; Stock queda continuación Inventory |
| DEVELOPMENT-POLICY — Billing ≠ Subscription | ✅ Subscriptions **fuera** de FASE 21 |
| ROADMAP — “Billing & Subscriptions” | ⚠️ **Corregir lectura:** FASE 21 = **Billing operativo (Invoice)**; Subscriptions ≠ este slice |
| Encounter / Item ports publicados | ✅ Consumo just-in-time en 21.2 según audit |
| Eventing vacío | No implementar bus preventivo |

---

## Checklist

- [x] Billing desafiado vs Stock / Notes / Platform / Payments / Procurement — **Billing es el siguiente BC nuevo**  
- [x] Stock explícitamente **no** bloquea ni sustituye FASE 21  
- [x] Primer Aggregate Root = **`Invoice`** (no Payment, no Charge-first asumido, no Subscription, no Customer)  
- [x] One-sentence rule definida  
- [x] Fuera de alcance explícito (Accounting, Tax, Payments, ERP, Inventory, Clinical, CRM, Subscriptions)  
- [x] Consumidores claros de `InvoiceId` (Payments, reporting, packs)  
- [x] Plan de pasos 21.0 → 21.8  
- [x] ADR-017 previsto  
- [x] Sin tablas / endpoints / permisos / DTOs en este paso  

---

## Siguiente paso

**PASO 21.0.1 — Invoice Aggregate Audit** ✅ · siguiente **PASO 21.1 — ADR-017**.

---

## Referencias

- [PASO-20.8-INVENTORY-CLOSEOUT.md](PASO-20.8-INVENTORY-CLOSEOUT.md)  
- [CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md)  
- [PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md](PASO-20.0-INVENTORY-FOUNDATION-PLANNING.md) · [PASO-19.0](PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) · [PASO-18.0](PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · [PASO-17.0](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)  
- [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) · [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](../architecture/CLINICAL-RECORDS-CONSUMPTION-GUIDE.md) · [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ROADMAP.md](../architecture/ROADMAP.md)  
