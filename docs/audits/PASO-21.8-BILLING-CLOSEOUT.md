# PASO 21.8 — Billing Closeout (Invoice)

**Veredicto:** **FASE 21 — Billing (Invoice slice): ✅ CERRADA**

Invoice queda como BC estable del Core Platform: reclamación comercial *intentionally small*, API admin, ReferencePort listo para Payments / CreditNotes / reporting — **sin** ERP, Tax, GL ni Subscriptions.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-21.7](PASO-21.7-INVOICE-VERIFICATION.md) · [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Cierre formal de **FASE 21 — Billing** (slice Invoice). Sin nuevas capacidades de negocio; superficie de consumo y documentación para Payments / packs.

---

## Entregables

| Área | Detalle |
|------|---------|
| OpenAPI | `BillingOpenApiConfiguration` — grupo `billing-administration` |
| Endpoint docs | `GET /v3/api-docs/billing-administration` |
| Paths | `/api/v1/billing/invoices` (+ issue, void) |
| ReferencePort | `InvoiceReferencePort` + `R2dbcInvoiceReferenceAdapter` (ADR-013) |
| Guía | [BILLING-CONSUMPTION-GUIDE.md](../architecture/BILLING-CONSUMPTION-GUIDE.md) |
| Verificación port | `InvoiceReferencePortIT` + contract test |
| ROADMAP | FASE 21 Invoice slice → **✅ Cerrada** · siguiente **Payments** y/o **Stock** |

---

## Superficie entregada (FASE 21)

| Capa | Entregable |
|------|------------|
| Dominio | Aggregate `Invoice` + `InvoiceLine` + VOs (ADR-017 frozen) |
| Persistencia | Schema `billing` · V26 · R2DBC |
| Auth | `invoice:create\|read\|update\|issue\|void` · V27 |
| HTTP | Document lifecycle admin API (DRAFT / ISSUED / VOIDED) |
| Contract | `InvoiceId` · `InvoicePermissionCatalog` · `InvoiceReferencePort` |
| Reference ports in | Org · Patient · Item · Encounter (`findLinkable`) |
| Tests | Domain · use case · persistence IT · verification 8/8 · reference port IT |

**Permisos:** 5 · **Migraciones:** V26–V27 · **ADRs:** 017 (frozen), 013 (patrón)

---

## Documentación de fase

| Documento | Propósito |
|-----------|-----------|
| [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md) | Modelo Invoice — **congelado** |
| [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) | Reference Contracts |
| [BILLING-CONSUMPTION-GUIDE.md](../architecture/BILLING-CONSUMPTION-GUIDE.md) | Guía consumidores |
| [INVENTORY-CONSUMPTION-GUIDE.md](../architecture/INVENTORY-CONSUMPTION-GUIDE.md) | Invoice como consumer de Item |
| [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](../architecture/CLINICAL-RECORDS-CONSUMPTION-GUIDE.md) | Invoice como consumer de Encounter |
| PASO-21.0 … PASO-21.7 | Trazabilidad implementación |

---

## Criterio de cierre FASE 21 (PASO 21.0 §12)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Invoice según ADR-017 (*intentionally small*) | ✅ |
| 2 | Org / Patient / Encounter / Item solo por IDs + ReferencePorts | ✅ |
| 3 | Verification E2E verde | ✅ 21.7 |
| 4 | ROADMAP FASE 21 Invoice slice ✅ · siguiente Payments / Stock | ✅ |
| 5 | Ningún aggregate IAM / Org / Patient / Appointment / Encounter / Item modificado (salvo seeds IAM) | ✅ |
| 6 | `InvoiceReferencePort` + guía publicados | ✅ |
| 7 | Ningún Payment / Tax / GL / Subscription / Stock embebido | ✅ |

---

## Explicitamente fuera (post-21 Invoice)

Payments · `PAID` · CreditNote · Tax engine · GL / Journal · Subscriptions / Plans / Seats · Pricing engine · CRM Customer · Stock · Appointment en Invoice · guest bill-to · un-void · DELETE HTTP · org-scoped RBAC · event bus preventivo

---

## Próximo

**Payments** — crece **alrededor** de Invoice vía `InvoiceId` + `InvoiceReferencePort.existsIssuedByIdAndTenant` **sin reabrir** ADR-017 ni FASE 16–21 Invoice.

Alternativa de producto: **Stock** (continuación Inventory) puede avanzar en paralelo — no bloquea ni sustituye Payments.

Los módulos de negocio no modifican Invoice; consumen el ReferencePort cuando necesiten validar una reclamación **ISSUED**.

---

## Veredicto

**FASE 21 — Billing (Invoice slice): ✅ CERRADA**

Reclamación comercial entregada, verificada E2E, documentada y publicada para consumo a largo plazo. CodeCore queda fortalecido como Core Platform multi-vertical:

```text
IAM → Organization → Patient → Appointment → Encounter → Item → Invoice
 CLOSED     CLOSED      CLOSED     CLOSED        CLOSED      CLOSED   CLOSED (Invoice)
                                                                      ↘ Payments / CreditNotes / packs
```

---

## Referencias

- [PASO-20.8-INVENTORY-CLOSEOUT.md](PASO-20.8-INVENTORY-CLOSEOUT.md) — patrón de cierre  
- [PASO-21.7-INVOICE-VERIFICATION.md](PASO-21.7-INVOICE-VERIFICATION.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
