# PASO 22.0.1 — Payment Aggregate Audit (DDD Estratégico)

**Payment** es el registro de liquidación aplicada a una Invoice — *intentionally small*, multi-vertical, primer Aggregate Root del BC **Payments**.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado (solo arquitectura)  
**Dependencias:** [PASO-22.0](PASO-22.0-PAYMENTS-FOUNDATION-PLANNING.md) · ADR-017 · ADR-013 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Checklist política (§8)

| # | Ítem | ✓ |
|---|------|---|
| 1–12 | Aggregate / ownership / invariants / lifecycle / permissions / IDs / BC / ADRs / escala | ✅ |

**Veredicto:** listo para **ADR-018**.

---

## Decisiones irreversibles

| Decisión | Elección |
|----------|----------|
| Naturaleza | Registro de importe **aplicado** hacia liquidar una Invoice ISSUED |
| Root | **`Payment`** |
| Tenant | Obligatorio e inmutable |
| Invoice | **`InvoiceId` obligatorio** — ACTIVE claim = ISSUED vía port en **create** |
| Dinero | `Money` (currency + amountMinor > 0) — propio del Payment |
| Currency vs Invoice | **No** exigir match en v1 (port boolean no expone currency); read-model futuro / evolución port just-in-time |
| Method | `paymentMethodCode?` opcional ≤32, opaco (CASH/CARD/TRANSFER…) — sin PSP |
| Lifecycle | `RECORDED` → `VOIDED` — create entra en RECORDED; **sin DRAFT**; **sin update de contenido** |
| void | RECORDED → VOIDED; **no** revalida Invoice port |
| Physical delete | Prohibido |
| Overpayment / saldo Invoice | **Fuera** — reporting / proyección; Invoice no gana `PAID` |
| PSP / capture intents | **Fuera** — adapters de pack |
| Refund | Aggregate futuro alrededor de Payment |
| Permisos | `payment:create\|read\|void` |
| Módulo / schema / HTTP | `payment-management` · `payments` · `/api/v1/payments` |

---

## One-sentence

> **Payment** = el registro de que un importe se aplicó hacia la liquidación de una Invoice bajo un Tenant.

---

## Invariantes (normativas)

1. Exactamente un `TenantId`, inmutable.  
2. Status ∈ {`RECORDED`, `VOIDED`}.  
3. `InvoiceId` siempre presente; en create → Invoice ISSUED en tenant.  
4. `amountMinor > 0`; currency ISO 4217.  
5. Contenido inmutable tras create; solo `void`.  
6. Payment **no** muda Invoice ni embede Tax/GL/PSP.  
7. Cross-tenant imposible.  
8. Única liquidación-record role en Core — no “DentalPayment” paralelo.

---

## Permanencia

> **Payment is intentionally small.**

Payments/Tax/GL/Subscription/CRM no viven dentro. Ampliar Payment con captura PSP completa o saldo de Invoice = violación.

---

## Prep. ADR-018

Congelar: definición, permanence, InvoiceId + port, Money, lifecycle RECORDED/VOIDED, permisos, freeze rule.

## Siguiente

**PASO 22.1 — ADR-018**.
