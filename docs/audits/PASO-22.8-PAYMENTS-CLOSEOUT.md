# PASO 22.8 — Payments Closeout

**Veredicto:** **FASE 22 — Payments: ✅ CERRADA**

Payment queda como BC estable del Core Platform: registro de liquidación *intentionally small*, API admin, ReferencePort listo para Refunds / PSP adapters / Accounting / reporting — **sin** PSP God Aggregate, Tax, GL ni Subscriptions.

**Fecha:** 2026-07-12
**Estado:** ✅ Completado
**Dependencias:** [PASO-22.7](PASO-22.7-PAYMENT-VERIFICATION.md) · [ADR-018](../architecture/ADR-018-PAYMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Cierre formal de **FASE 22 — Payments**. Sin nuevas capacidades de negocio; superficie de consumo y documentación para Refunds / PSP adapters / packs.

---

## Entregables

| Área | Detalle |
|------|---------|
| OpenAPI | `PaymentOpenApiConfiguration` — grupo `payments-administration` |
| Endpoint docs | `GET /v3/api-docs/payments-administration` |
| Paths | `/api/v1/payments` (+ `/{id}`, `/{id}/void`) |
| ReferencePort | `PaymentReferencePort.existsRecordedByIdAndTenant` + `R2dbcPaymentReferenceAdapter` (ADR-013) |
| Guía | [PAYMENTS-CONSUMPTION-GUIDE.md](../architecture/PAYMENTS-CONSUMPTION-GUIDE.md) |
| Verificación port | `PaymentReferencePortIT` + `PaymentReferencePortContractTest` |
| ROADMAP | FASE 22 Payments → **✅ Cerrada** · siguiente **Stock** y/o **Platform** |
| Architecture Review | [CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md) — veredicto **A**, 9.2/10 |

---

## Superficie entregada (FASE 22)

| Capa | Entregable |
|------|------------|
| Dominio | Aggregate `Payment` + VOs (ADR-018 frozen) |
| Persistencia | Schema `payments` · V28 · R2DBC |
| Auth | `payment:create\|read\|void` · V29 · `ALL` 49→52 |
| HTTP | Settlement-record admin API (RECORDED / VOIDED) |
| Contract | `PaymentId` · `PaymentPermissionCatalog` · `PaymentReferencePort` |
| Reference ports usados | `InvoiceReferencePort.existsIssuedByIdAndTenant` (create) |
| Tests | Domain 20 · use case 5 · persistence IT 6 · verification 8/8 · reference port IT + contract |

**Permisos:** 3 · **Migraciones:** V28–V29 · **ADRs:** 018 (frozen), 013 (patrón)

---

## Documentación de fase

| Documento | Propósito |
|-----------|-----------|
| [ADR-018](../architecture/ADR-018-PAYMENT-DOMAIN-MODEL.md) | Modelo Payment — **congelado** |
| [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) | Reference Contracts |
| [PAYMENTS-CONSUMPTION-GUIDE.md](../architecture/PAYMENTS-CONSUMPTION-GUIDE.md) | Guía consumidores |
| [BILLING-CONSUMPTION-GUIDE.md](../architecture/BILLING-CONSUMPTION-GUIDE.md) | Payment como consumer de Invoice |
| PASO-22.0 … PASO-22.7 | Trazabilidad implementación |

---

## Criterio de cierre FASE 22 (PASO 22.0 §Checklist)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Payment según ADR-018 (*intentionally small*) | ✅ |
| 2 | Invoice consumido solo por ID + `InvoiceReferencePort` | ✅ |
| 3 | Verification E2E verde | ✅ 22.7 (8/8) |
| 4 | ROADMAP FASE 22 ✅ · siguiente Stock / Platform | ✅ |
| 5 | Ningún aggregate IAM / Org / Patient / Appointment / Encounter / Item / Invoice modificado (salvo seeds IAM) | ✅ |
| 6 | `PaymentReferencePort` + guía publicados | ✅ |
| 7 | Ningún Refund / PSP capture / Tax / GL / Subscription embebido | ✅ |
| 8 | Ningún `PAID` agregado a Invoice | ✅ |

---

## Explícitamente fuera (post-22 Payments)

Refund · PSP capture orchestration · Tax engine · GL / Journal · Subscriptions / Plans / Seats · Invoice `PAID` · overpayment rules · currency-coherence port · un-void · DELETE HTTP · org-scoped RBAC · event bus preventivo

---

## Próximo

**Stock** (continuación Inventory, FASE 20) puede avanzar — no bloquea ni sustituye Platform Services.

**Platform Services** (FASE 22 histórico del ROADMAP: Invitations, password recovery ADR-009, Subscriptions SaaS) queda disponible en paralelo.

Los módulos de negocio no modifican Payment; consumen `PaymentReferencePort` cuando necesiten validar un settlement **RECORDED** (p. ej. Refunds).

---

## Veredicto

**FASE 22 — Payments: ✅ CERRADA**

Registro de liquidación entregado, verificado E2E, documentado y publicado para consumo a largo plazo. CodeCore queda fortalecido como Core Platform multi-vertical:

```text
IAM → Organization → Patient → Appointment → Encounter → Item → Invoice → Payment
 CLOSED     CLOSED      CLOSED     CLOSED        CLOSED      CLOSED   CLOSED   CLOSED (Payments)
                                                                                ↘ Refunds / PSP adapters / Accounting
```

---

## Referencias

- [PASO-21.8-BILLING-CLOSEOUT.md](PASO-21.8-BILLING-CLOSEOUT.md) — patrón de cierre
- [PASO-22.7-PAYMENT-VERIFICATION.md](PASO-22.7-PAYMENT-VERIFICATION.md)
- [ROADMAP.md](../architecture/ROADMAP.md)
