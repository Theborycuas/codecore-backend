# CodeCore — Payments Architecture Review (FASE 22)

**Fecha:** 2026-07-12  
**Tipo:** Revisión arquitectónica post-cierre (sin modificar ADRs · sin reabrir fase · sin código de corrección)  
**Alcance:** FASE 22 — Payments (`Payment` slice) ya cerrada (PASO 22.8)  
**Pregunta:** ¿Payments quedó como bounded context reutilizable del Core Platform (registro de liquidación), o como embrión de PSP / Accounting / Tax / “Invoice PAID”?

**Autoridad de contraste:** [CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md) · [CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-INVENTORY-ARCHITECTURE-REVIEW-2026-07.md) · [ADR-018](ADR-018-PAYMENT-DOMAIN-MODEL.md) · [ADR-017](ADR-017-INVOICE-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [PAYMENTS-CONSUMPTION-GUIDE.md](PAYMENTS-CONSUMPTION-GUIDE.md) · PASO 22.0 → 22.8

---

## Executive Summary

**Sí — Payments (slice Payment) es Core Platform, no un gateway PSP y no un vertical.**  
`Payment` permanece *intentionally small*: registro de que un importe se aplicó hacia una `Invoice` ISSUED, lifecycle `RECORDED` → `VOIDED`, consume **solo** `InvoiceId` + `InvoiceReferencePort.existsIssuedByIdAndTenant`, publica `PaymentReferencePort.existsRecordedByIdAndTenant`, y **no** incorpora Refund, Tax, GL, Subscription, captura PSP ni mutación `PAID` en Billing.

No se encontró un error arquitectónico **crítico** que exija reabrir FASE 22 ni corregir el modelo antes de Stock / Platform.

> **Veredicto operativo: A) La siguiente fase puede comenzar sin cambios en Payments (Payment).**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| DDD | **9.5** | Aggregate Root correcto; freeze ADR-018 anti–PSP God Aggregate |
| Hexagonal | **9** | Ports in/out claros; adapter ReferencePort; create valida Invoice port |
| Modular Monolith | **9** | Schema `payments` · `payment-management` · HTTP `/api/v1/payments` |
| Core Platform | **9.5** | Claim ≠ liquidation demostrado en código; multi-vertical settlement record |
| Desacoplamiento | **9.5** | Sin repos/SQL/FK hacia `billing`; un solo ReferencePort de consumo |
| Reutilización | **9** | CASH/CARD/WIRE vía método opaco; sin enum de medios vertical |
| Consistencia | **8.5** | Misma disciplina de closeout; `tenantId` en response (como Invoice) |
| Escalabilidad | **9** | Refunds/PSP/Accounting crecen **alrededor**; riesgo = meter balance/`PAID` en Invoice |
| Mantenibilidad | **9** | Superficie mínima; verification 8/8; guía de consumo |
| Visión a largo plazo | **9.5** | Cierra el arco económico Core (*se adeuda → se registra liquidación*) sin ERP |
| **Global FASE 22** | **9.2** | Misma nota que Billing/Inventory/Records; stress-test claim→settlement |

Comparado con Billing 21 (**9.2**): FASE 22 **cumple** la promesa del review 21 §5 — Payments se construyó **solo** con el port ISSUED, sin reabrir ADR-017.

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir FASE 22? | **No** |
| ¿Modificar ADR-018? | **No** |
| ¿Puede comenzar Stock / Platform? | **Sí** |
| ¿Deuda P0 en Payments? | **Ninguna** |
| Opción | **A) Siguiente fase sin cambios en Payment** |

---

## 1. Bounded Context

**Payments está correctamente delimitado** como *hogar* del registro de liquidación (+ futuros aggregates de liquidación como Refund) — **no** como “todo el dinero del producto”.

| Evidencia | Lectura |
|-----------|---------|
| Schema `payments` · módulo `payment-management` · HTTP `/api/v1/payments` | Límites claros |
| Independiente de Accounting / Tax / Subscription / Inventory / Billing schema | Política + ADR-018 |
| Consume Invoice solo vía contract | Downstream correcto |
| Payment slice cerrado; Refund / PSP diferidos explícitamente | Evita God Aggregate en v1 |

### ¿Hay responsabilidad que no debería estar aquí?

| Responsabilidad | ¿En Payment hoy? | ¿Debería salir? |
|-----------------|------------------|-----------------|
| Invoice `PAID` / balance | No | N/A — correcto fuera (inferencia externa) |
| Refund | No | N/A — BC/aggregate futuro |
| PSP capture / webhook state | No | N/A — adapters futuros |
| Tax / fiscal stamp | No | N/A |
| GL / Journal | No | N/A |
| Subscription / seats | No | N/A — Platform / Subscription |
| Patient/Org/Item/Encounter en Payment | No | N/A — ancla solo Invoice |

Nada en el modelo entregado “sobra” como responsabilidad de Tax/GL/PSP/CRM.

**Riesgo real (disciplina, no reopen):** el nombre “Payments” invita a meter captura PSP, refunds, conciliación bancaria y “marcar Invoice PAID” hasta **God Aggregate / God BC**. Mitigación: ADR-018 §3 + guía — Refund/PSP/Accounting como **nuevos** límites con audit propio; nunca mutar Invoice.

**Conclusión §1:** BC correcto. El peligro no es Payment; es **inflar Payments** o **reabrir Billing** sin nuevos límites.

---

## 2. Aggregate `Payment`

**Sigue siendo intentionally small.** No hay indicios de God Aggregate en código.

**Superficie real** (`Payment.java`):

- Identidad: `PaymentId`, `TenantId` (inmutable)
- Ancla: `InvoiceId` (inmutable; validado ISSUED solo en create)
- Contenido: `Money` + `paymentMethodCode?` + `recordedAt`
- Lifecycle: create → `RECORDED` → void → `VOIDED`
- Sin DRAFT; sin update de contenido; sin un-void; sin DELETE físico

| Pregunta | Respuesta |
|----------|-----------|
| ¿Intentionally small? | **Sí** — alineado ADR-018 §3 |
| ¿God Aggregate en formación? | **No** — tests niegan refund/ledger/PSP/Invoice mutation API |
| ¿Overpayment / currency match Invoice? | **Diferidos conscientes** (boolean port) — no omisiones accidentales |
| ¿Invariantes ausentes *realmente* importantes? | Balance total Invoice — **explícitamente externo** |

---

## 3. Integración (ReferencePorts)

### Consumo (Payment → Billing)

| Port | Uso | ¿Suficiente? |
|------|-----|--------------|
| `InvoiceReferencePort.existsIssuedByIdAndTenant` | Create | ✅ — exactamente lo que predijo review Billing §5 |

| Prohibido verificado | Estado |
|----------------------|--------|
| `InvoiceRepository` / SQL `billing.*` en application main | **Ausente** |
| FK `payments.payment.invoice_id` → `billing.invoice` (V28) | **Ausente** (comentario explícito ADR-013) |
| Revalidación de port en `void` | **Omitida** — correcto (espejo void Invoice / cancel Encounter) |
| Ports Patient/Org/Item/Encounter en Payment | **Ausentes** — correcto; el claim ya ancla la parte |

**Acoplamiento tipado esperado:** `payment-application` adapta `payment.domain.*Id` → `billing.domain.*Id` porque el port ISSUED vive en `billing-contract` con tipos del provider (`api(domain)`). Mismo olor de plataforma que Patient/Invoice — **no** viola ADR-013 a nivel de persistencia.

**Publicación (closeout):** `PaymentReferencePort.existsRecordedByIdAndTenant` — superficie mínima boolean para Refunds / reporting futuros.

**Conclusión §3:** ReferencePorts **suficientes**; sin evolución requerida para Stock/Platform. Sin acoplamiento oculto que rompa ADR-013.

---

## 4. Reutilización multi-vertical

Definición operativa: **registro de que un importe se aplicó hacia la liquidación de una Invoice bajo un Tenant.**

| Vertical | ¿Sirve sin modificar Payment? | Nota |
|----------|-------------------------------|------|
| Dental / Vet / Hospital | **Sí** | Liquidación clínica vía Invoice ya emitida |
| Laboratorio | **Sí** | Idem |
| ERP / B2B | **Sí** | Invoice bill-to Org; Payment no conoce la parte |
| Retail / POS | **Sí** con límite | Método opaco (`CASH`/`CARD`); captura PSP / tip / change = fuera v1 |
| Manufactura | **Sí** | Claim comercial previo; settlement record genérico |

Ningún “no” duro. Vertical packs **no** deben crear roots paralelos (`DentalPayment`, `PosTender`) — ADR-018 §2.

---

## 5. Consumo futuro — Refunds / PSP / Accounting / Stock

| Necesidad típica | ¿Cubierta sin reabrir Payment? |
|------------------|--------------------------------|
| ¿Existe settlement efectivo? | ✅ `existsRecordedByIdAndTenant` |
| Anclar Refund a Payment | ✅ almacenar `PaymentId` + `tenantId` |
| Rechazar VOIDED | ✅ port → `false` |
| Captura PSP | Adapter / BC aparte — **no** campos en Payment v1 |
| “Invoice fully paid?” | Suma externa RECORDED vs total Invoice — **no** en Aggregate |
| Stock | **No consume Payment** — continúa Inventory |

Meter `PAID` en Invoice “porque Payments ya existe” = **violación ADR-017**, no evolución natural.

---

## 6. Arquitectura — hallazgos

### Contaminación (`payment-*` main)

| Concepto | ¿Presente? |
|----------|------------|
| Refund / capture / `PAID` | **No** |
| Tax / GL / Subscription | **No** |
| Patient/Org/Item/Encounter refs | **No** |
| SQL cross-schema / FK Billing | **No** |
| Event Bus | **No** |

### Olores reales (no inventados)

| Olor | Severidad |
|------|-----------|
| `PaymentHttpExceptionHandler` → bodies vacíos (patrón Invoice/Encounter) | P2 |
| `payment-contract` `api(domain)` over-exposure | P2 plataforma |
| `spring-boot-starter-data-r2dbc` en `payment-application` | P2 plataforma |
| `tenantId` en `PaymentResponse` | P2 consistencia HTTP (alineado Invoice) |
| Mapping VO Payment↔Billing en application | P2 tipado contract; esperado bajo `api(domain)` |

### Sobreingeniería

**No material.** Sin Event Bus, sin balance engine, sin enum rígido de medios de pago, sin ports “por si acaso” (Patient/Org/Office).

---

## 7. Deuda técnica

### P0 — Crítica

**Ninguna.**

### P1 — Alta

**Ninguna atribuible al modelo Payment entregado.**

| Ítem residual (disciplina) | Nota |
|----------------------------|------|
| Tentación PSP God Aggregate / Invoice `PAID` | Riesgo **después** del closeout, no defecto 22.x |

### P2 — Baja (higiene / consistencia)

| Ítem | Evidencia | Acción futura (no ahora) |
|------|-----------|---------------------------|
| HTTP error bodies vacíos | `PaymentHttpExceptionHandler` | Política platform web |
| Contract `api(domain)` | `payment-contract/build.gradle.kts` | Thin-contract platform-wide si duele |
| R2DBC starter en application | Gradle | Quitar al tocar el módulo |
| Currency/total Invoice en port | ADR-018 §6 | Solo just-in-time si un producto lo exige (ADR-013) |

**¿Resolver algo antes de Stock / Platform?** **No.**

---

## 8. Roadmap — ¿qué sigue?

### Alternativas

| Candidato | ¿Ahora? | Motivo |
|-----------|---------|--------|
| **Stock** (Inventory) | **Sí — paralelo válido** | Continúa FASE 20; no reabre Payments; no consume Invoice |
| **Platform Services** (FASE 23) | **Sí — IAM-adjacent** | Invitations / password recovery / Subscriptions SaaS |
| Refunds | No como inmediato | Menos universal que Stock/Platform; crece alrededor de PaymentReferencePort |
| Tax / Accounting / Subscription en Payments | No | Violan la separación que FASE 21–22 acaba de probar |
| Reabrir Invoice con `PAID` | **No** | Anti-patrón explícito |

### Veredicto roadmap

**Sí — congelar FASE 22** y avanzar a **Stock** y/o **Platform Services (FASE 23)** **sin reabrir** ADR-016…018 ni FASE 16–22.

---

## Aspectos destacados

1. **Claim ≠ liquidation** — demostrado de punta a punta (Billing frozen + Payments closed).  
2. **Un solo port de consumo** — `existsIssuedByIdAndTenant` bastó; sin engordar InvoiceReferencePort.  
3. **Sin `PAID` en Invoice** — defensa anti-ERP mantenida bajo presión de implementación.  
4. **Método de pago opaco** — evita verticalizar el Core a un catálogo de tenders.  
5. **Closeout completo** — `PaymentReferencePort` + guía alineada ADR-013.  
6. **Verificación viva** — Flyway V28–V29 + OpenAPI `payments-administration` + tests verdes.

---

## Riesgos residuales (disciplina)

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Meter `PAID` / balance en Invoice | Alta si se viola | ADR-017 + consumption guides |
| Convertir Payment en PSP state machine | Alta | ADR-018 freeze + Refunds/PSP BC aparte |
| Engordar `InvoiceReferencePort` a query API | Media | ADR-013: boolean primero; view just-in-time |
| Parallel “PosPayment” en packs | Media | ADR-018 §2 + guía |

Ninguno justifica opción B o C hoy.

---

## Opciones de cierre

| Opción | Criterio | ¿Aplica? |
|--------|----------|----------|
| **A) Siguiente fase sin cambios** | Sin P0/P1 de modelo; BC correcto | **Sí** |
| B) Corregir deuda antes de continuar | Habrá P1 de modelo | No |
| C) Reabrir ADR-018 / FASE 22 | Error de frontera o God Aggregate | No |

---

## Conclusión

FASE 22 entrega un **Bounded Context Payments cerrado y audit-ready**: settlement record *intentionally small*, desacoplado de Billing por IDs + ReferencePorts, listo para Refunds / PSP adapters / Accounting **alrededor** — no dentro.

**Congelar. No reabrir. Siguiente: Stock y/o Platform Services.**

Referencias: [PASO-22.8](../audits/PASO-22.8-PAYMENTS-CLOSEOUT.md) · [PAYMENTS-CONSUMPTION-GUIDE.md](PAYMENTS-CONSUMPTION-GUIDE.md) · [ADR-018](ADR-018-PAYMENT-DOMAIN-MODEL.md) · [CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md).
