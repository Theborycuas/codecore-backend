# PASO 22.0 — Payments Foundation Planning

**Payments** es el siguiente bounded context **nuevo** del Core: registra **que se aplicó dinero hacia la liquidación de una reclamación comercial (Invoice)** — sin PSP God Aggregate, sin Tax, sin GL, sin reabrir Billing / Invoice.

**Fecha:** 2026-07-12  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Definición de FASE 22 · Bounded Context Payments  
**Dependencias:** FASE 21 Invoice cerrada · ADR-017 · ADR-013 · [BILLING-CONSUMPTION-GUIDE.md](../architecture/BILLING-CONSUMPTION-GUIDE.md) · [CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md)

---

## Quick path

1. BC = **Payments** · primer Aggregate Root = **`Payment`**  
2. Consume **solo** `InvoiceId` + `InvoiceReferencePort.existsIssuedByIdAndTenant`  
3. Lifecycle: `RECORDED` → `VOIDED` · **sin** tocar Invoice (`PAID` prohibido)

**Sin Accounting · Tax · Subscription · ERP · Event Bus.**

---

## 0. ¿Payments es el siguiente BC correcto?

| Alternativa | ¿Ahora? | Motivo |
|-------------|---------|--------|
| **Payments** | **Sí** | Consumidor normativo de `InvoiceReferencePort`; cierra *se adeuda → se liquida* |
| Stock | Paralelo | Continuación Inventory; no valida el closeout de Billing |
| Platform / Invitations | No | IAM-adjacent; no cadena económica |
| Tax / Accounting | No | Otro BC; contaminaría Payments |

**Veredicto:** Payments es el siguiente BC nuevo correcto.

```text
Invoice (ISSUED) → Payment (RECORDED) → (Refund/PSP adapters / reporting)
     CLOSED            FASE 22
```

**Regla de oro:** no modificar Invoice, Billing schema, ni ADR-017.

---

## 1. BC · Aggregate · One-sentence

| Decisión | Valor |
|----------|-------|
| BC | **Payments** |
| Root | **`Payment`** |
| One-sentence | El registro de que un importe se aplicó hacia la liquidación de una Invoice bajo un Tenant |
| Schema / módulo | `payments` · `payment-management` |
| HTTP | `/api/v1/payments` |

**No es:** captura PSP completa · refund engine · asiento contable · impuesto · suscripción · estado `PAID` en Invoice.

---

## 2. Qué consume / publica

| Consume | Port |
|---------|------|
| `TenantId` | JWT |
| `InvoiceId` | `InvoiceReferencePort.existsIssuedByIdAndTenant` en create |

| Publica (closeout) | Para |
|--------------------|------|
| `PaymentId` | Refunds / reporting / packs |
| `PaymentReferencePort` | p. ej. `existsRecordedByIdAndTenant` |

---

## 3. Plan de pasos

| Paso | Nombre |
|------|--------|
| **22.0** | Foundation Planning |
| **22.0.1** | Payment Aggregate Audit |
| **22.1** | ADR-018 Accepted |
| **22.2** | Reference Readiness (Invoice port — sin evolución esperada) |
| **22.3** | Domain Foundation |
| **22.4** | Persistence (V28) |
| **22.5** | Authorization (`payment:*` + V29) |
| **22.5.1** | Admin API Audit |
| **22.6** | Administration API |
| **22.7** | Verification |
| **22.8** | Closeout + guía |

---

## Checklist

- [x] Payments desafiado vs Stock / Platform / Tax  
- [x] Root = Payment  
- [x] Sin modificar Invoice  
- [x] Plan 22.0 → 22.8  

## Siguiente

**PASO 22.0.1 — Payment Aggregate Audit**.
