# ADR-018 — Payment Domain Model

**Status:** Accepted  
**Date:** 2026-07-12  
**Accepted:** 2026-07-12 (PASO 22.1)  
**Deciders:** CodeCore architecture (FASE 22.1)  
**Relates to:** ADR-003 · ADR-007 · ADR-013 · ADR-017 · [PASO-22.0.1-PAYMENT-AGGREGATE-AUDIT.md](../audits/PASO-22.0.1-PAYMENT-AGGREGATE-AUDIT.md) · [BILLING-CONSUMPTION-GUIDE.md](BILLING-CONSUMPTION-GUIDE.md)

---

## Context

FASE 21 closed **Billing** (`Invoice`, ADR-017) with `InvoiceReferencePort.existsIssuedByIdAndTenant`. CodeCore must introduce **Payments** so products can record that money was applied toward an ISSUED commercial claim — **without** embedding `PAID` on Invoice, without a PSP God Aggregate, and without Tax/GL/Subscription.

---

## Decision

### 1. Bounded context

**Payments** — downstream of Billing; independent of Accounting, Tax, Subscription, Inventory.

Gradle: `payment-management` · Schema: `payments` · HTTP: `/api/v1/payments`

### 2. What Payment is

> **The record that an amount was applied toward settling an Invoice within a Tenant.**

It is the **only** Core Aggregate Root owning that liquidation-record role. Vertical packs must not invent parallel “DentalPayment” / “PosTender” roots.

**Not:** PSP capture orchestration · Refund · Tax · GL posting · Invoice balance / `PAID` · Subscription charge.

### 3. Permanence — intentionally small

Payment may contain only: identity, tenant, `InvoiceId`, money, optional opaque method code, recorded timestamp, status `RECORDED`|`VOIDED`, and intrinsic invariants.

Everything else lives elsewhere.

### 4. Lifecycle

```text
(create) → RECORDED → void → VOIDED
```

- No DRAFT; content immutable after create.  
- `void` does not revalidate Invoice port.  
- No physical delete; no un-void in v1.

### 5. References

| Ref | Rule |
|-----|------|
| `TenantId` | Required, immutable |
| `InvoiceId` | Required; create validates `InvoiceReferencePort.existsIssuedByIdAndTenant` |

No Patient/Org/Item/Encounter/Office on Payment v1.

### 6. Money & method

- `Money`: ISO currency + `amountMinor` > 0  
- Currency match with Invoice **not** enforced in v1 (boolean port)  
- Optional `paymentMethodCode` ≤ 32, opaque  

### 7. Permissions

`payment:create` · `payment:read` · `payment:void`

### 8. Freeze

Changes to boundaries, lifecycle (incl. adding PENDING/CAPTURED as Core requirement without audit), or embedding Invoice `PAID` / Tax / GL require a **new ADR**.

---

## Consequences

**Positive:** Payments grow around Invoice without reopening Billing; multi-vertical liquidation record.  
**Deferred:** overpayment rules, currency coherence via richer port, Refund, PSP adapters.  
**Neutral:** `PaymentReferencePort` in closeout 22.8.

---

## Acceptance

**Accepted** PASO 22.1 (2026-07-12).
