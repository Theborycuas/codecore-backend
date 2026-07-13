# CodeCore — Billing Architecture Review (FASE 21)

**Fecha:** 2026-07-12  
**Tipo:** Revisión arquitectónica post-cierre (sin modificar ADRs · sin reabrir fase · sin código de corrección)  
**Alcance:** FASE 21 — Billing (`Invoice` slice) ya cerrada (PASO 21.8)  
**Pregunta:** ¿Billing (Invoice) quedó como bounded context reutilizable del Core Platform, o como embrión de ERP / Tax / Payments / SaaS Subscription?

**Autoridad de contraste:** [CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md) · [CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md) · [ADR-017](ADR-017-INVOICE-DOMAIN-MODEL.md) · [ADR-016](ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-015](ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [BILLING-CONSUMPTION-GUIDE.md](BILLING-CONSUMPTION-GUIDE.md) · PASO 21.0 → 21.8

---

## Executive Summary

**Sí — Billing (slice Invoice) es Core Platform, no un ERP y no un vertical.**  
`Invoice` permanece *intentionally small*, cierra el arco económico del Core (*quién / plan / ocurrió / inventariable → se adeuda*), consume Organization / Patient / Item / Encounter **solo** por IDs + ReferencePorts, publica `InvoiceReferencePort.existsIssuedByIdAndTenant`, y no incorpora Payments, `PAID`, Tax, GL, Subscription, CRM, Stock ni Pricing.

No se encontró un error arquitectónico **crítico** que exija reabrir FASE 21 ni corregir el modelo antes de Payments.

> **Veredicto operativo: A) La siguiente fase puede comenzar sin cambios en Billing (Invoice).**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| DDD | **9.5** | Aggregate Root correcto; freeze ADR-017 §3 anti–God Billing |
| Hexagonal | **9** | Ports in/out claros; adapter ReferencePort; consumers vía contract |
| Modular Monolith | **9** | Schema `billing` · `billing-management` · HTTP `/api/v1/billing/**` |
| Core Platform | **9.5** | Primer BC económico; agnóstico vertical; Billing ≠ Subscription respetado |
| Desacoplamiento | **9** | Sin repos/SQL cross-BC; 4 ReferencePorts en write path |
| Reutilización | **9** | Bill-to Patient xor Org cubre clínico + B2B/ERP/Retail sin CRM |
| Consistencia | **8.5** | Misma disciplina de closeout; leve delta HTTP (`tenantId` en response) |
| Escalabilidad | **9** | Modelo listo para Payments alrededor; riesgo God-BC si Tax/GL/Sub se meten sin límites |
| Mantenibilidad | **9** | Superficie pequeña; verification 8/8; guía de consumo |
| Visión a largo plazo | **9.5** | Separar claim (`Invoice`) de liquidación (`Payment`) es la decisión de plataforma más valiosa de FASE 21 |
| **Global FASE 21** | **9.2** | Misma nota que Inventory/Records; stress-test multi-port + cadena económica |

Comparado con Inventory 20 (**9.2**) y Clinical Records 19 (**9.2**): FASE 21 **mantiene** la disciplina y **demuestra** que el patrón ReferencePorts escala a un BC económico multi-origen sin contaminar BCs cerrados.

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir FASE 21? | **No** |
| ¿Modificar ADR-017? | **No** |
| ¿Puede comenzar FASE 22 (Payments)? | **Sí** |
| ¿Deuda P0 en Billing? | **Ninguna** |
| Opción | **A) Siguiente fase sin cambios en Invoice** |

---

## 1. Bounded Context

**Billing está correctamente delimitado** como *hogar* de la reclamación comercial + futuros aggregates de documento (CreditNote) — **no** como “todo lo financiero”.

| Evidencia | Lectura |
|-----------|---------|
| Schema `billing` · módulo `billing-management` · HTTP `/api/v1/billing/invoices` | Límites claros |
| Independiente de Accounting / Payments / Tax / Subscription | Política + ADR-017 |
| Consume Org/Patient/Item/Encounter solo vía contracts | Downstream correcto |
| Invoice slice cerrado; Payment diferido explícitamente | Evita God Aggregate en v1 |

### ¿Hay responsabilidad que no debería estar aquí?

| Responsabilidad | ¿En Invoice hoy? | ¿Debería salir? |
|-----------------|------------------|-----------------|
| Payments / `PAID` | No | N/A — correcto fuera |
| Tax / fiscal stamp | No | N/A |
| GL / Journal | No | N/A |
| Subscription / seats | No | N/A — FASE Platform / Subscription |
| CRM Customer | No | N/A — bill-to Patient\|Org |
| Stock qty | No | N/A |
| Líneas con `ItemId?` / `EncounterId?` | Sí | **No** — refs opcionales de origen; no ownership clínico/inventario |
| `Money` ya resuelto en línea | Sí | **No** — hecho comercial; no es Pricing engine |

Nada en el modelo entregado “sobra” como responsabilidad de Payments/Tax/GL/CRM.

**Riesgo real (disciplina, no reopen):** el nombre “Billing” invita a meter Tax, GL, Subscriptions SaaS y Payments “dentro del mismo módulo” hasta **God Bounded Context**. Mitigación: ADR-017 §3 + DEVELOPMENT-POLICY (Billing ≠ Subscription) + guía — Payment/CreditNote como aggregates **nuevos** con audit propio.

**Conclusión §1:** BC correcto. El peligro no es Invoice; es **inflar Billing** sin nuevos límites.

---

## 2. Aggregate `Invoice`

**Sigue siendo intentionally small.** No hay indicios de God Aggregate en código.

**Superficie real** (`Invoice.java` + `InvoiceLine`):

- Identidad: `InvoiceId`, `TenantId` (inmutable)
- Emisor: `OrganizationId` obligatorio
- Bill-to: Patient **xor** Organization (≠ issuer)
- Contenido: `invoiceNumber?`, líneas (`description` + `Money` + `ItemId?` + `EncounterId?`)
- Lifecycle: `DRAFT` → `ISSUED` → `VOIDED` (también DRAFT → VOIDED)
- Mutaciones de contenido solo en `DRAFT`

| Pregunta | Respuesta |
|----------|-----------|
| ¿Intentionally small? | **Sí** — alineado ADR-017 §3 |
| ¿God Aggregate en formación? | **No** — tests niegan pay/tax/ledger/stock/subscription API |
| ¿Líneas engordan el root? | **No en v1** — entities internas mínimas; Charge-first diferido con criterio |
| ¿Invariantes ausentes *realmente* importantes? | Guest bill-to / qty en línea — **explícitamente diferidos**, no omisiones accidentales |

---

## 3. Integración (ReferencePorts)

### Consumo (Invoice → providers)

| Port | Uso | ¿Suficiente? |
|------|-----|--------------|
| `OrganizationReferencePort.existsActiveByIdAndTenant` | Issuer + bill-to Org | ✅ |
| `PatientReferencePort.existsActiveByIdAndTenant` | Bill-to Patient | ✅ |
| `ItemReferencePort.existsActiveByIdAndTenant` | Línea material | ✅ |
| `EncounterReferencePort.findLinkableByIdAndTenant` | Línea clínica + `patientId` | ✅ |

| Prohibido verificado | Estado |
|----------------------|--------|
| Repos Org/Patient/Encounter/Item en application | **Ausente** |
| SQL `org.*` / `clinical.*` / `records.*` / `inventory.*` | **Ausente** |
| FK cross-BC en V26 | **Ausente** (solo FK intra-aggregate línea→invoice) |
| Revalidación de ports en `void` | **Omitida** — correcto (espejo cancel Encounter) |

**Coherencia Patient↔Encounter** vive en application (mismatch → excepción dedicada). Correcto bajo ADR-013.

**Acoplamiento oculto:** no a nivel de persistencia/repos. Olor de plataforma (no reopen): `billing-contract` hace `api(billing-domain)` → consumidor *puede* importar aggregate; mismo patrón Patient/Item/Encounter.

**Conclusión §3:** ReferencePorts **suficientes**; sin evolución requerida para Payments v1. Sin acoplamiento oculto que rompa ADR-013.

---

## 4. Reutilización multi-vertical

Definición operativa: **reclamación comercial de que un importe es adeudado por una parte a una organización emisora bajo un Tenant.**

| Vertical | ¿Sirve sin modificar Invoice? | Nota |
|----------|-------------------------------|------|
| Dental | **Sí** | Bill-to Patient + líneas con Encounter/Item opcionales |
| Veterinaria | **Sí** | Idem |
| Hospital | **Sí** | Claim genérico; RCM/seguro = packs / BC posterior |
| Laboratorio | **Sí** | Servicios + materiales por ItemId |
| ERP / B2B | **Sí** | Bill-to Organization (≠ issuer) |
| Retail | **Sí** con límite | Guest/anónimo **fuera de v1** — no bloquea Core; packs pueden exigir Patient/Org mínimo hasta BillingAccount futuro |

Ningún “no” duro. Vertical packs **no** deben crear roots paralelos (ADR-017 §2).

---

## 5. Consumo futuro — Payments

¿Payments podrá construirse **solo** con `InvoiceId` + `InvoiceReferencePort` **sin reabrir** Billing?

| Necesidad Payments v1 típica | ¿Cubierta? |
|------------------------------|------------|
| Validar claim liquidable | ✅ `existsIssuedByIdAndTenant` |
| Anclar pago a documento | ✅ almacenar `InvoiceId` + `tenantId` |
| Rechazar DRAFT / VOIDED | ✅ port → `false` |
| Importe / moneda del claim | Read-model propio o ampliación **just-in-time** del port (ADR-013) — **no** reabrir aggregate |

**Sí.** Payments crece **alrededor** de Invoice. Si mañana hace falta `findIssuedView…` (currency/total), se evoluciona el **contract** sin tocar ADR-017 del aggregate — mismo patrón Appointment `findLinkable` en 19.2.

Meter `PAID` en Invoice = **violación ADR-017**, no “evolución natural”.

---

## 6. Arquitectura — hallazgos

### Contaminación (`billing-*` main)

| Concepto | ¿Presente? |
|----------|------------|
| Payment / PSP / `PAID` | **No** |
| Tax / GL / Subscription | **No** |
| CRM Customer / BillingAccount | **No** |
| Stock / qty / Appointment | **No** |
| Verbos verticales en permisos | **No** |
| SQL cross-schema | **No** |

### Olores reales (no inventados)

| Olor | Severidad |
|------|-----------|
| `InvoiceHttpExceptionHandler` → bodies vacíos | P2 (patrón Encounter/Item) |
| `billing-contract` `api(domain)` over-exposure | P2 plataforma |
| `spring-boot-starter-data-r2dbc` en `billing-application` (solo tipa `TransactionalOperator`) | P2 plataforma |
| `tenantId` en `InvoiceResponse` (Item/Patient suelen omitirlo en JSON) | P2 inconsistencia HTTP menor |
| Guest retail bill-to ausente | Diferido consciente — no deuda de modelo |

### Sobreingeniería

**No material.** Sin Event Bus, sin Charge AR prematuro, sin Pricing engine, sin ports “por si acaso” (Appointment/Office/Stock no inventados).

---

## 7. Deuda técnica

### P0 — Crítica

**Ninguna.**

### P1 — Alta

**Ninguna atribuible al modelo Invoice entregado.**

| Ítem residual (disciplina) | Nota |
|----------------------------|------|
| Tentación God-BC (Tax/GL/Subscription/Payments dentro de Invoice) | No es defecto del código 21.x; es el riesgo **después** del closeout |

No bloquea Payments.

### P2 — Baja (higiene / consistencia)

| Ítem | Evidencia | Acción futura (no ahora) |
|------|-----------|---------------------------|
| HTTP error bodies vacíos | `InvoiceHttpExceptionHandler` | Política platform web |
| Contract `api(domain)` | `billing-contract/build.gradle.kts` | Thin-contract platform-wide si duele |
| R2DBC starter en application | Gradle | Quitar al tocar el módulo |
| `tenantId` en response JSON | `InvoiceResponse` | Alinear con Patient/Item si se toca API admin |
| Guest bill-to | ADR-017 §7 | Solo con audit+ADR si un producto lo exige |

**¿Resolver algo antes de FASE 22?** **No.**

---

## 8. Roadmap — ¿FASE 22 Payments?

### Alternativas

| Candidato | ¿Ahora? | Motivo |
|-----------|---------|--------|
| **Payments** | **Sí — siguiente BC económico** | Único consumidor claro e inmediato de `InvoiceReferencePort`; cierra *se adeuda → se liquida* sin reabrir Billing |
| Stock (Inventory) | Paralelo válido | Continuación del **mismo** BC Inventory; **no** sustituye Payments ni consume Invoice |
| Platform Services (Invitations) | No como prioridad Core tras Billing | IAM-adjacent; no alarga la cadena de negocio publicada |
| Tax / Accounting / Subscription | No | Violan la separación que FASE 21 acaba de probar |
| CreditNote antes que Payment | No | Menos universal; Payment es el consumidor normativo del port ISSUED |

### Veredicto roadmap

**Sí — continuar con Payments como siguiente Bounded Context de negocio** (consumiendo `InvoiceId` + `InvoiceReferencePort`) **sin reabrir** FASE 16–21.

Stock puede avanzar **en paralelo** como slice Inventory; no es “FASE 22” en el sentido de BC nuevo del ecosistema económico.

Si el ROADMAP histórico numera “22 = Platform Services”, la recomendación de **Core Platform** es: **no dejar Payments a la espera de Invitations** — Payments fortalece el corazón; Platform no valida el closeout de Billing.

---

## Aspectos destacados

1. **Claim ≠ liquidation** — freeze sin `PAID` es la defensa anti-ERP más importante de FASE 21.  
2. **Bill-to Patient xor Org** — evita verticalizar el Core a “solo clínica” y evita CRM prematuro.  
3. **Primer consumidor económico multi-port** (Org + Patient + Item + Encounter) sin reabrir providers.  
4. **Primer consumidor cross-BC real de `ItemReferencePort`** fuera de Inventory.  
5. **Permisos dedicados `issue`/`void`** — semántica de documento, no soft-entity archive.  
6. **Closeout completo** — port mínimo boolean + guía alineada ADR-013.

---

## Riesgos residuales (disciplina)

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Meter `PAID` / balance en Invoice “porque Payments lo necesita” | Alta si se viola | ADR-017 freeze + consumption guide |
| Meter Subscriptions SaaS en Billing | Alta | DEVELOPMENT-POLICY · BC/fase aparte |
| Convertir `InvoiceReferencePort` en query API gorda | Media | ADR-013: boolean primero; view solo just-in-time |
| Guest bill-to → Customer God object | Media | Audit+ADR; no “string nombre” en Invoice |

Ninguno justifica opción B o C hoy.

---

## Opciones de cierre

| Opción | Criterio | ¿Aplica? |
|--------|----------|----------|
| **A) Payments / siguiente fase sin cambios en Billing** | Sin P0/P1 de modelo; port + guía listos | **✅ Elegida** |
| B) Corregir solo críticos antes de seguir | Existiría P0/P1 real | No — no hay |
| C) Reabrir parcialmente FASE 21 / ADR-017 | Evidencia fuerte de modelo roto | No — ADR-017 intacto y correcto |

---

## Checklist de auditoría (respuestas)

| # | Pregunta | Respuesta |
|---|----------|-----------|
| 1 | ¿BC bien delimitado? | **Sí** |
| 2 | ¿Intentionally small / sin God Aggregate? | **Sí** |
| 3 | ¿ReferencePorts suficientes / sin acoplamiento oculto? | **Sí** / **Sin acoplamiento oculto material** |
| 4 | ¿Reutilizable multi-vertical? | **Sí** (guest retail diferido, consciente) |
| 5 | ¿Payments sin reabrir Billing? | **Sí** |
| 6 | ¿Sobreingeniería / God BC hoy? | **No** |
| 7 | ¿P0/P1 antes de FASE 22? | **Ninguno** |
| 8 | ¿FASE 22 = Payments correcto? | **Sí** (Stock en paralelo; Platform no bloquea) |
| — | ¿Modificar ADR-017? | **No** |
| — | ¿Reabrir FASE 21? | **No** |
| — | ¿Puede comenzar FASE 22? | **Sí** |

---

## Conclusión ejecutiva

FASE 21 entrega lo que el Core necesitaba tras Inventory: un **documento comercial estable**, vertical-agnóstico, *intentionally small*, con contrato de consumo para liquidación — **sin** nacer como ERP, pasarela de pagos, motor fiscal ni SaaS seats.

CodeCore como Core Platform queda **más fuerte**: la cadena  
`Organization → Patient → Appointment → Encounter → Item → Invoice`  
está cerrada y publicada. El riesgo dominante ya no está en Invoice; está en **no traicionar el freeze** cuando lleguen Payments, Tax y Subscriptions.

**Siguiente paso:** iniciar **Payments** consumiendo contratos existentes. **No reabrir** FASE 16–21 Invoice · **No modificar** ADR-017.

---

## Referencias (lectura; no modificadas por esta revisión)

- [ADR-017-INVOICE-DOMAIN-MODEL.md](ADR-017-INVOICE-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [BILLING-CONSUMPTION-GUIDE.md](BILLING-CONSUMPTION-GUIDE.md)  
- [PASO-21.8-BILLING-CLOSEOUT.md](../audits/PASO-21.8-BILLING-CLOSEOUT.md)  
- [PASO-21.7-INVOICE-VERIFICATION.md](../audits/PASO-21.7-INVOICE-VERIFICATION.md)  
- [PASO-21.0-BILLING-FOUNDATION-PLANNING.md](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md)  
- [CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md)  
- [ROADMAP.md](ROADMAP.md)  
