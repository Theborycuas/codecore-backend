# PASO 21.0.1 — Invoice Aggregate Audit (DDD Estratégico)

**Invoice** es la reclamación comercial de un importe adeudado — *intentionally small*, multi-vertical, y el primer Aggregate Root del BC **Billing**.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado (solo arquitectura)  
**Tipo:** Auditoría obligatoria — Aggregate Root nuevo + Bounded Context Billing  
**Dependencias:** [PASO-21.0](PASO-21.0-BILLING-FOUNDATION-PLANNING.md) · ADR-003 · ADR-007 · ADR-010 · ADR-011 · ADR-012 · ADR-013 · ADR-015 · ADR-016 · [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) · [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](../architecture/CLINICAL-RECORDS-CONSUMPTION-GUIDE.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Objetivo

Definir el modelo correcto del Aggregate Root **`Invoice`** para que CodeCore, como Core Platform, registre reclamaciones comerciales en dental, veterinaria, hospital, laboratorio, retail, ERP B2B, manufactura (cobro de trabajo) y verticales futuros — **sin** acoplar Billing a un producto concreto ni convertir Invoice en ERP / Tax Engine / Payments / CRM / Subscription.

**Sin código. Sin tablas. Sin endpoints. Sin migraciones. Sin Payments / Tax / GL / Stock / Subscriptions.**

---

## Checklist política (§8) — verdicto previo

| # | Ítem | ✓ | Nota |
|---|------|---|------|
| 1 | Aggregate Root identificado | ✅ | `Invoice` |
| 2 | Ownership definido | ✅ | BC Billing |
| 3 | Invariantes definidas | ✅ | § Invariantes |
| 4 | Lifecycle definido | ✅ | § Lifecycle |
| 5 | Estados definidos | ✅ | `DRAFT` · `ISSUED` · `VOIDED` |
| 6 | Permisos definidos | ✅ | Borrador `invoice:*` |
| 7 | Relaciones solo mediante IDs | ✅ | § Referencias |
| 8 | Bounded Context correcto | ✅ | Billing (≠ Accounting, ≠ Inventory, ≠ Clinical, ≠ Subscription) |
| 9 | No rompe ADR vigentes | ✅ | 003/006/007/010–016 intactos; BCs cerrados |
| 10 | Escalable multi-tenant | ✅ | |
| 11 | Escalable multi-organization | ✅ | Emisor org-scoped; bill-to Patient \| Org |
| 12 | Escalable millones de registros | ✅ | Aggregate delgado; líneas mínimas |

**Veredicto:** checklist en verde → listo para **ADR-017** ([PASO-21.1](PASO-21.1-INVOICE-MODEL-CONTRACT.md) — siguiente).

---

## Decisiones irreversibles (resumen ejecutivo)

| Decisión | Elección |
|----------|----------|
| Naturaleza | **Reclamación comercial** de importe adeudado — no Payment, no asiento, no tarifa, no stock |
| Aggregate Root | **`Invoice`** — documento comercial; **única** reclamación de ese claim dentro del Tenant |
| BC propietario | **Billing** |
| Tenant | **Siempre** — `TenantId` obligatorio e **inmutable** |
| Emisor | **`OrganizationId` obligatorio** — quién emite; ACTIVE vía `OrganizationReferencePort` |
| Bill-to (deudor) | **Exactamente uno** de: `PatientId` **o** `OrganizationId` (bill-to) — ver §6 |
| Bill-to anónimo / guest retail | **Fuera de v1** — no CRM; no `BillingAccount` aún |
| Emisor ≠ bill-to org | Si bill-to es Organization → **debe diferir** del emisor |
| Líneas | **Entidades internas** del Invoice (no Charge AR en v1) — mínimas |
| Línea | `description` + `Money` (+ `ItemId?` + `EncounterId?`) — **sin** qty/UoM/Stock |
| Appointment | **Prohibido** en Invoice v1 — origen clínico = Encounter |
| Stock | **Prohibido** |
| Dinero | `currency` (ISO 4217) + `amountMinor` (enteros); **una** currency por Invoice; total = suma de líneas |
| Unicidad dura | Solo **`InvoiceId` (UUID)** |
| Número humano | **Opcional** — soft-unique por tenant si presente (`invoiceNumber`) |
| Lifecycle v1 | `DRAFT` → `ISSUED` → `VOIDED` — **sin** `PAID` · **sin** delete físico |
| Mutaciones de contenido | Solo en `DRAFT` |
| Payments / Tax / GL / Subscription | **Fuera** |
| Módulo / schema (propuesta) | `billing-management` · schema `billing` |
| HTTP (propuesta) | `/api/v1/billing/invoices` |

Borrador formal: **ADR-017 — Invoice Domain Model** — a aceptar en PASO 21.1.

---

## 1. Naturaleza del Aggregate

### ¿Qué representa Invoice?

**Invoice es la reclamación comercial de que un importe es adeudado por una parte (bill-to) a una organización emisora, bajo un Tenant.**

| Interpretación | ¿Es? | Por qué |
|----------------|------|---------|
| ¿El documento / claim de “se debe este dinero”? | **Sí** | Definición operativa |
| ¿El cobro / captura PSP? | **No** | Payments |
| ¿El asiento contable? | **No** | Accounting |
| ¿La tarifa / price list? | **No** | Pricing — Invoice lleva importes **ya resueltos** |
| ¿Una cantidad en almacén? | **No** | Stock |
| ¿Una nota clínica? | **No** | Puede *referenciar* Encounter en línea |
| ¿Una suscripción SaaS? | **No** | Subscription |
| ¿Algo dental / retail específico? | **No** | Core Platform |

**Regla de exclusividad:** dentro del Tenant, la **reclamación comercial de importe adeudado** vive en Billing como `Invoice`. Ningún vertical debe crear un “DentalInvoice” / “RetailReceipt” / “HospitalClaim” paralelo en el Core que duplique ese rol — los verticales **referencian** `InvoiceId` o extienden por packs.

**Intentionally small:** Invoice no crece con pagos, impuestos calculados, asientos, suscripciones, CRM customer, ni deducción de stock.

---

## 2. Aggregate Root

### ¿Por qué Invoice debe ser Aggregate Root?

- Boundary transaccional del **documento comercial** (alta en borrador, emisión, anulación).  
- Ciclo de vida propio (`DRAFT` / `ISSUED` / `VOIDED`).  
- `InvoiceId` estable para Payments, CreditNotes, reporting, packs.  
- Sus invariantes **no** incluyen liquidación de cobro ni posting contable (política §5).

### Principio de permanencia (para ADR-017)

> **Invoice is intentionally small.**

Decisión **permanente** (no limitación de FASE 21): Invoice solo representa la reclamación comercial + invariantes propias. Payments, Tax, GL, Pricing engines, Subscriptions, Stock y CRM viven en **otros** aggregates/BCs. Embebidos en Invoice = violación arquitectónica (**God Aggregate**).

### ¿Por qué no Charge primero?

Charge es unidad atómica útil, pero **sin documento** retail/ERP reinventan el agrupador y Payments anclan mal. Charge puede ser **segundo** aggregate si las líneas ganan invariantes propias. v1: líneas internas mínimas bajo Invoice.

### ¿Por qué no Payment?

Payment responde “¿qué se **cobró**?”. Necesita un claim estable (`InvoiceId`) — espejo Stock-needs-Item.

### ¿Por qué no BillingAccount / Customer?

Identidad de deudor genérico → **CRM**. v1 reutiliza `PatientId` \| `OrganizationId` existentes. Guest/anónimo retail → diferido (§6).

### ¿Por qué no Subscription / TaxDocument / LedgerEntry?

Descartados en [PASO-21.0](PASO-21.0-BILLING-FOUNDATION-PLANNING.md) — otro BC o compliance local.

---

## 3. Ownership

| Rol | Actor / BC |
|-----|------------|
| **Propietario del modelo** | Bounded Context **Billing** (`billing-management`) |
| **Quién crea** | Membership con `invoice:create` |
| **Quién modifica borrador** | `invoice:update` |
| **Quién emite** | `invoice:issue` |
| **Quién anula** | `invoice:void` |
| **Quién solo consulta** | `invoice:read` |
| **Quién NO es dueño** | IAM, Organization, Patient, Appointment, Encounter, Inventory, Accounting, Payments |

**Borrador permisos (21.5):**  
`invoice:read` · `invoice:create` · `invoice:update` · `invoice:issue` · `invoice:void`

**No** mapear `issue`/`void` a solo `:update` — son transiciones de documento con semántica comercial distinta (espejo `appointment:cancel` / `encounter:cancel`, no espejo soft-entity archive).

**No** verbos verticales (`invoice:dental-claim`, `invoice:pos-ticket`, `invoice:subscribe`).  
**No** `invoice:pay` / `invoice:post` / `invoice:tax` — Payments / Accounting / Tax.

Organization **no** posee Invoice. Es **emisora** (y opcionalmente bill-to B2B) por referencia.

---

## 4. Bounded Context

| Pregunta | Respuesta |
|----------|-----------|
| ¿BC? | **Billing** |
| ¿Parte de Accounting? | **No** — Accounting consume hechos; no vive aquí |
| ¿Parte de Inventory? | **No** — solo `ItemId?` en línea |
| ¿Parte de Clinical Records? | **No** — solo `EncounterId?` en línea |
| ¿Parte de Subscription? | **No** — política: Billing ≠ Subscription |
| ¿Módulo Gradle? | `modules/billing-management/` (propuesta) |
| ¿Schema? | `billing` (propuesta) — **no** meter facturas en `org` / `clinical` / `records` / `inventory` |

```text
org.*        → Organization / Office / StaffAssignment     CLOSED
clinical.*   → Patient                                     CLOSED
scheduling.* → Appointment                                 CLOSED
records.*    → Encounter                                   CLOSED
inventory.*  → Item                                        CLOSED
billing.*    → Invoice (+ Payment? / CreditNote? después)  FASE 21
```

Billing puede crecer después con `Payment` / `CreditNote` **alrededor** de `InvoiceId` — **sin engordar Invoice** con estados de cobro.

---

## 5. Tenant

| Pregunta | Respuesta |
|----------|-----------|
| ¿Invoice pertenece al Tenant? | **Sí** |
| ¿Siempre? | **Sí** |
| ¿Puede cambiar de Tenant? | **Nunca** |

`TenantId` se fija en `create` y es inmutable. Cross-tenant → 404.

---

## 6. Bill-to (deudor) — cerrado aquí

### Problema

Solo `PatientId` **verticaliza** el Core hacia salud. Solo “string nombre” destruye integridad referencial. `BillingAccount` / Customer ahora = **CRM prematuro**.

### Opciones

| Opción | Pros | Contras |
|--------|------|---------|
| Solo Patient | Simple clínico | Falla ERP/Retail/B2B |
| Solo Organization bill-to | B2B limpio | Falla self-pay clínico |
| **Patient \| Organization (exactamente uno)** | Cubre clínico + B2B sin CRM | Guest retail anónimo diferido |
| BillingAccount nuevo | Máxima flexibilidad | God-CRM en FASE 21 |

### Decisión

**Bill-to = exactamente uno de:**

| Variante | Campo | Uso |
|----------|-------|-----|
| Paciente / sujeto de cuidado | `billToPatientId` | Self-pay, paciente facturable |
| Organización deudora | `billToOrganizationId` | B2B, empresa cliente, otra unidad facturable |

**Invariantes:**

1. Exactamente uno de `{billToPatientId, billToOrganizationId}` presente — nunca ambos, nunca ninguno.  
2. Emisor `organizationId` (issuer) **siempre** presente.  
3. Si bill-to es Organization → `billToOrganizationId ≠ organizationId` (issuer).  
4. En escritura (create/update DRAFT + al `issue`):  
   - Patient → `PatientReferencePort.existsActiveByIdAndTenant`  
   - Bill-to Org → `OrganizationReferencePort.existsActiveByIdAndTenant`  
   - Issuer Org → idem  

### Explicitamente diferido

| Caso | Tratamiento |
|------|-------------|
| Guest / walk-in retail sin Patient ni Org cliente | **Fuera de v1** — futuro `BillingAccount` o Party **solo** con audit+ADR; no inventar en Invoice ahora |
| “Cash customer” genérico | Pack/tenant config — no hardcodear en Core |
| Employer pays + Encounter clínico | Permitido: bill-to Org + líneas con `EncounterId?` — **sin** forzar match Patient↔bill-to |

---

## 7. Emisor (issuer)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Quién emite? | **`OrganizationId` obligatorio** (denormalizado) |
| ¿Validación? | ACTIVE en tenant vía `OrganizationReferencePort` en create/update DRAFT y en `issue` |
| ¿Office emisor? | **No en v1** — locus comercial no es el claim |
| ¿Staff que emitió? | **No en v1** — auditoría de actor = platform/audit trail futuro, no el aggregate |

---

## 8. Líneas (entities internas — no Aggregate Root)

### Decisión

**Líneas viven dentro del Invoice** como entidades del aggregate (colección).  
**No** Charge AR en v1.

Si más adelante aparecen invariantes de Charge independientes (posting parcial, rebill, insurance split), un audit+ADR puede introducir `Charge` **sin** reabrir la one-sentence de Invoice (Invoice agruparía Charges por ID).

### Forma mínima de línea

| Campo | ¿v1? | Nota |
|-------|------|------|
| Identidad de línea | Sí | `InvoiceLineId` (UUID) o identidad estable dentro del aggregate |
| `description` | **Obligatorio** | Texto comercial no blank |
| `Money` (line amount) | **Obligatorio** | Importe de línea **ya resuelto** (no unit×qty×tax) |
| `ItemId` | **Opcional** | Material/catálogo — ACTIVE vía `ItemReferencePort` si presente |
| `EncounterId` | **Opcional** | Origen clínico — ver §9 |
| `quantity` / UoM | **No en v1** | Invita Stock/Pricing; packs pueden poner qty en `description` |
| Unit price | **No** | Solo amount de línea |
| Tax breakdown | **No** | Tax Engine fuera |
| AppointmentId / StockId | **No** | Prohibidos |

### Cardinalidad

- Invoice **DRAFT/ISSUED** debe tener **≥ 1** línea.  
- Vaciar líneas solo posible volviendo a estado inválido — prohibido al `issue`.  
- Total Invoice = **suma exacta** de `amountMinor` de líneas (misma currency).

---

## 9. Orígenes clínicos / materiales

| Ref | ¿Dónde? | Regla |
|-----|---------|-------|
| `EncounterId` | **Solo en línea**, opcional | Si presente → validar linkable/existencia vía `EncounterReferencePort` (forma exacta en 21.2 según port actual) |
| `AppointmentId` | **Prohibido** | Origen clínico = Encounter (plan ≠ cobro) |
| `ItemId` | **Solo en línea**, opcional | ACTIVE vía `ItemReferencePort` |
| `StockId` / qty on hand | **Prohibido** | Billing ≠ Inventory |
| Coherencia Patient | Si bill-to es **Patient** y línea tiene `EncounterId` → `encounter.patientId` **debe** coincidir con bill-to (application, espejo Encounter↔Appointment) | |
| Coherencia Org bill-to | Si bill-to es **Organization** → **no** exigir match Patient del Encounter | Employer/B2B pay |

**No** `EncounterId` obligatorio en cabecera: una Invoice puede agrupar varias líneas/encuentros o ser puramente comercial sin episodio.

---

## 10. Dinero

| Decisión | Valor |
|----------|-------|
| Representación | VO `Money`: `currency` (ISO 4217, 3 letras) + `amountMinor` (`long`, enteros) |
| Currency del Invoice | **Una** por documento — fijada en create; todas las líneas la comparten |
| FX / multi-currency | **Fuera de v1** |
| Negativos | **Prohibidos** en líneas v1 — créditos = `CreditNote` futuro |
| Cero | Line amount **> 0**; total **> 0** |
| Tax incluido/excluido | **No modelar** — amount es el hecho comercial |

---

## 11. Identidad humana (número de factura)

| Mecanismo | Rol |
|-----------|-----|
| **`InvoiceId` (UUID)** | **Única clave dura** |
| `invoiceNumber` | **Opcional** — soft-unique por `(tenantId, invoiceNumber)` si presente |
| Secuencia fiscal legal | **Fuera** — compliance local / packs; no congelar en Core |

No asumir legislación de numeración de un país o vertical.

---

## 12. Lifecycle

| Estado | Significado |
|--------|-------------|
| `DRAFT` | Borrador editable; **no** es aún reclamación emitida para Payments |
| `ISSUED` | Reclamación comercial **efectiva**; contenido inmutable |
| `VOIDED` | Anulada; retención histórica; **no** delete físico |

```text
(create) → DRAFT
              ├── update (líneas / bill-to / issuer mientras DRAFT)
              ├── issue  → ISSUED
              └── void   → VOIDED   (desde DRAFT o ISSUED)

ISSUED → void → VOIDED
VOIDED → (terminal; sin reactivate / unvoid en v1)
```

| Comportamiento | Regla |
|----------------|-------|
| `update` contenido | Solo desde `DRAFT` |
| `issue` | DRAFT → ISSUED; revalidar ports de refs presentes; ≥1 línea; total > 0 |
| `void` | DRAFT \| ISSUED → VOIDED |
| Delete físico | **Prohibido en v1** |
| `PAID` / `PARTIALLY_PAID` / `OVERDUE` | **Prohibidos** — Payments / reporting |
| Reactivar VOIDED | **Prohibido en v1** — emitir nueva Invoice |
| Enmendar ISSUED | **Prohibido** — void + nueva, o CreditNote futuro |

**ReferencePort futuro (closeout):** p. ej. `existsIssuedByIdAndTenant` — Payments solo liquidan `ISSUED` (no DRAFT/VOIDED).

---

## 13. Referencias (IDs only)

### Cabecera

| ID | En Invoice | Cardinalidad | Semántica |
|----|------------|--------------|-----------|
| `TenantId` | Required | 1 | Aislamiento SaaS — **inmutable** |
| `OrganizationId` (issuer) | Required | 1 | Emisor |
| `billToPatientId` | XOR | 0..1 | Deudor clínico |
| `billToOrganizationId` | XOR | 0..1 | Deudor B2B |
| `OfficeId` / `StaffAssignmentId` | — | — | **Prohibido en v1** |
| `AppointmentId` | — | — | **Prohibido** |
| `MembershipId` / `IdentityId` | — | — | **Prohibido** como deudor |

### Línea

| ID | En línea | Cardinalidad |
|----|----------|--------------|
| `ItemId` | Optional | 0..1 |
| `EncounterId` | Optional | 0..1 |

Payments / Accounting **referencian** `InvoiceId`; Invoice **no** conoce colecciones de pagos ni asientos.

---

## 14. ReferencePorts (consumo)

| Port | ¿Invoice v1? | Uso |
|------|--------------|-----|
| `OrganizationReferencePort.existsActiveByIdAndTenant` | **Sí** | Issuer siempre; bill-to Org si aplica — create/update DRAFT + `issue` |
| `PatientReferencePort.existsActiveByIdAndTenant` | Si bill-to Patient | create/update DRAFT + `issue` |
| `ItemReferencePort.existsActiveByIdAndTenant` | Si línea tiene Item | create/update DRAFT + `issue` |
| `EncounterReferencePort` | Si línea tiene Encounter | create/update DRAFT + `issue` — **21.2** confirma método (`findLinkable` vs existencia) |
| `AppointmentReferencePort` | **No** | |
| `OfficeReferencePort` / Staff | **No** | |
| Stock / Payment / Tax ports | **No** | No existen |

`void` **no** revalida ports (espejo cancel Encounter).

**21.2:** documentar readiness; evolucionar Encounter port **solo** si el método actual no cubre el invariante de enlace — **no** inventar ports nuevos “por si acaso”.

---

## 15. Invariantes (normativas para ADR-017)

1. Exactamente un `TenantId`, fijado en create — **nunca cambia**.  
2. Status ∈ {`DRAFT`, `ISSUED`, `VOIDED`}.  
3. Mutaciones de contenido solo desde `DRAFT`; `issue` solo desde `DRAFT`; `void` desde `DRAFT`\|`ISSUED`.  
4. Exactamente un bill-to: Patient **xor** Organization.  
5. Issuer `OrganizationId` siempre presente; si bill-to Org → distinto del issuer.  
6. ≥ 1 línea; cada línea con `description` no blank y `Money` con `amountMinor > 0`.  
7. Una sola `currency` por Invoice; total = suma de líneas; total > 0.  
8. Si refs opcionales presentes en escritura/`issue` → ports ACTIVE/linkable según §14.  
9. Si bill-to Patient + línea con Encounter → patient del Encounter = bill-to.  
10. Invoice **no** almacena pagos, impuestos desglosados, asientos, stock ni suscripciones.  
11. Invoice **no** transactionaliza Payments, Accounting ni Inventory.  
12. `InvoiceId` nunca se reasigna.  
13. Cross-tenant access imposible.  
14. Dentro del Tenant, Invoice es la **única** reclamación comercial de ese rol — no duplicar con “DentalInvoice”/“Receipt” roots en el Core.

*(Permanencia “do not embed payments/tax/GL/stock/CRM” — §2.)*

---

## 16. Multi-organization

Un tenant multi-org: cada Invoice tiene **un emisor** (`OrganizationId`).  
Bill-to puede ser Patient (compartido tenant-scoped) u otra Organization del tenant.  
**No** implica RBAC org-scoped (ADR-007 intacto).  
Listados admin filtran por tenant (+ filtros org opcionales en API — 21.5.1).

---

## 17. Escalabilidad

Aggregate: cabecera + N líneas delgadas (ids + description + money + optional refs).  
Sin hijos de pagos/impuestos.  
Índices previstos (no SQL aquí): tenant, status, issuer org, bill-to patient/org, issuedAt, invoiceNumber parcial.

Millones de facturas: viable; aging/statements = read-model futuro, no el aggregate.

---

## 18. Permisos (borrador → 21.5)

| Código | Uso |
|--------|-----|
| `invoice:create` | Alta en `DRAFT` |
| `invoice:read` | Consulta / listado |
| `invoice:update` | Editar contenido en `DRAFT` |
| `invoice:issue` | DRAFT → ISSUED |
| `invoice:void` | → VOIDED |

Matriz esperada: OWNER/ADMIN/MANAGER = lifecycle completo; USER/READ_ONLY = read (cerrar en 21.5).

---

## 19. Módulo / schema / HTTP (propuestas — cerrar en pasos posteriores)

| Artefacto | Propuesta |
|-----------|-----------|
| Gradle | `modules/billing-management/` (`billing-domain` · `application` · `infrastructure` · `contract`) |
| Schema | `billing` |
| HTTP | `/api/v1/billing/invoices` (+ `/{id}/issue`, `/{id}/void`) |
| OpenAPI group | `billing-administration` (closeout) |
| Permisos | `invoice:*` |
| ReferencePort | `InvoiceReferencePort` en 21.8 |

---

## 20. Qué queda explícitamente fuera (God Aggregate / God BC guard)

| Fuera | Dónde va |
|-------|----------|
| Payment / PSP capture | Payments (aggregate/BC posterior) |
| `PAID` status | Payments / proyección |
| Tax engine / fiscal stamp | Tax / compliance pack |
| GL / Journal | Accounting |
| PriceList / FeeSchedule | Pricing |
| Subscription / Plan / Seat | Subscription (política) |
| Customer / BillingAccount | CRM — solo si audit futuro lo exige |
| Stock deduction | Inventory Stock / Movement |
| Appointment link | Prohibido — usar Encounter |
| CreditNote / Refund doc | Aggregate posterior |
| Insurance Claim / RCM | Vertical pack |
| POS cart / Sales Order | Commerce |
| Guest bill-to anónimo | Diferido (§6) |

---

## 21. Comparación con fases anteriores

| Aspecto | Encounter | Item | **Invoice** |
|---------|-----------|------|-------------|
| Rol | Ocurrido clínico | Qué inventariable | **Reclamación comercial** |
| Lifecycle | IN_PROGRESS/… | ACTIVE/ARCHIVED | **DRAFT/ISSUED/VOIDED** |
| Org | Obligatoria | Opcional custodia | **Obligatoria emisora** |
| Soft-entity reactivate | N/A / activate | Sí | **No** (VOIDED terminal) |
| Hijos | No | No | **Líneas mínimas** |
| Intentionally small | Sí | Sí | **Sí** |

Consistencia: Invoice sigue el patrón **documento operativo** (como Encounter), no soft-registry (Patient/Item).

---

## 22. Prep. ADR-017 — contenido mínimo

El ADR-017 debe congelar:

1. Definición one-sentence + exclusivity rule  
2. Permanencia *intentionally small*  
3. Issuer + bill-to XOR (Patient \| Organization) + guest diferido  
4. Líneas internas mínimas (description + Money + Item?/Encounter?)  
5. Money (currency + amountMinor); sin tax/FX  
6. Lifecycle DRAFT/ISSUED/VOIDED sin PAID  
7. Ports de consumo (Org, Patient?, Item?, Encounter?)  
8. Permisos `invoice:create|read|update|issue|void`  
9. Fuera: Payments, Accounting, Tax, Stock, Subscription, CRM, Appointment  
10. Freeze rule (cambios → nuevo ADR)

---

## 23. Criterio de salida de este paso

| Criterio | Estado |
|----------|--------|
| Checklist §8 verde | ✅ |
| Hipótesis 21.0 cerradas (bill-to, líneas, lifecycle, dinero, refs) | ✅ |
| Prep. ADR-017 listo | ✅ |
| Sin código / tablas / endpoints | ✅ |
| BCs cerrados intactos | ✅ |

---

## Siguiente paso

**PASO 21.1 — Invoice Model Contract (ADR-017)** — congelar el modelo de este audit como **Accepted**.

---

## Referencias

- [PASO-21.0-BILLING-FOUNDATION-PLANNING.md](PASO-21.0-BILLING-FOUNDATION-PLANNING.md)  
- [PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md](PASO-20.0.1-ITEM-AGGREGATE-AUDIT.md) · [PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md](PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md)  
- [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) · [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](../architecture/CLINICAL-RECORDS-CONSUMPTION-GUIDE.md) · [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ROADMAP.md](../architecture/ROADMAP.md)  
