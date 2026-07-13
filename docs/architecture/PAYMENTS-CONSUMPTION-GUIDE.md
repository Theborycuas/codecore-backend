# Payments Consumption Guide

**Audience:** Developers building modules after FASE 22 Payments (Refunds, PSP capture adapters, Accounting bridge, Reporting, vertical packs, …)
**Authority:** [ADR-018](ADR-018-PAYMENT-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-017](ADR-017-INVOICE-DOMAIN-MODEL.md)
**Status:** Vigente desde PASO 22.8 — Payments **cerrado**

---

## Mental model

FASE 22 no es "PSP / gateway de pagos". FASE 22 entrega un **bounded context estable**: el registro de que un importe se aplicó hacia la liquidación de una Invoice (*intentionally small*).

```text
Tenant (IAM)
 └── Invoice (ISSUED)             ← Billing (FASE 21, frozen)
      └── Payment                 ← settlement record (intentionally small)
           ├── invoiceId          ← must be ISSUED at create time
           ├── money              ← currency + amountMinor > 0
           ├── paymentMethodCode? ← opaque, ≤ 32 chars, uninterpreted
           └── status             ← RECORDED → VOIDED
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Se aplicó dinero hacia esta Invoice? | `PaymentReferencePort.existsRecordedByIdAndTenant` |
| ¿Puedo crear un Payment sobre esta Invoice? | `InvoiceReferencePort.existsIssuedByIdAndTenant` (usado internamente por Payments, no por consumidores) |
| ¿Cuánto se pagó / con qué medio? | `Money` / `paymentMethodCode` en `PaymentResponse` — opaco, sin PSP |
| ¿Está la Invoice `PAID`? | **No existe** — ni en Invoice ni en Payment; se infiere externamente (suma de `Payment` RECORDED) si un consumidor lo necesita |
| ¿Refund / captura PSP / asiento contable? | **No en Payments** — Refunds / PSP adapters / Accounting (BCs futuros) |

---

## Decision tree (30 seconds)

```text
Need to record that an amount was applied to an Invoice?
  → Own aggregate; call InvoiceReferencePort.existsIssuedByIdAndTenant at create
  → That IS Payment — do not create a parallel "DentalPayment" / "PosTender" root

Need to check whether a specific settlement record is still effective (not voided)?
  → PaymentReferencePort.existsRecordedByIdAndTenant (`payment-contract`, ADR-013/ADR-018)
  → Never PaymentRepository, never SQL against payments.payment from another BC

Need refund, PSP capture orchestration, tax, or GL posting?
  → Wrong — grow around Payment via a new BC (Refunds / PSP / Accounting), new ADR required

Need "is this Invoice fully paid"?
  → Compute externally (sum RECORDED Payments vs Invoice total) — not stored on Invoice or Payment
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:payment-management:payment-contract"))
```

Only. Never `payment-application` or `payment-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `PaymentId` + `tenantId` | `@Autowired R2dbcPaymentRepository` |
| Validate via `PaymentReferencePort` | `SELECT * FROM payments.payment` outside Payments |
| Filter by JWT `tenantId` | Accept client-sent `tenantId` as authority |
| Treat Payment as settlement record | Put `PAID` on Invoice, refund, tax, GL, PSP capture on Payment |

---

## Contract surface (Payments closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `PaymentId` | `payment-contract` → domain VO | Hard identity |
| `PaymentPermissionCatalog` | `payment-contract` | `payment:create\|read\|void` |
| `PaymentReferencePort` | `payment-contract` | RECORDED + tenant existence check |
| `R2dbcPaymentReferenceAdapter` | `payment-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface PaymentReferencePort {
    Mono<Boolean> existsRecordedByIdAndTenant(PaymentId paymentId, TenantId tenantId);
}
```

`VOIDED` → `false` — future consumers (e.g. Refunds) must only act against an effective settlement record.

---

## Module recipes

### Refunds (next logical consumer)

**Owns:** reversal against a prior settlement
**References:** `PaymentId` (typically RECORDED)

```text
Refund
  paymentId           ← validate RECORDED via PaymentReferencePort
  amount…
```

**Never:** mutate the original `Payment`; never reopen its status directly.

### PSP capture adapters (later)

**Owns:** gateway-specific capture/webhook orchestration, outside the Core
**References:** creates a `Payment` via the Administration API/use case once capture succeeds; never embeds PSP fields on `Payment` itself.

### Accounting bridge / Reporting

**Owns:** export / statements / GL posting
**References:** `PaymentId`; never owns Payment lifecycle; never turns Payments into the GL.

### Vertical packs (Dental / Retail / Hospital)

**Owns:** presentation rules, tender-type UX local
**References:** `PaymentId`; never create parallel payment roots in the Core.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `PaymentReferencePort` in `payment-contract` |
| **Frontend / mobile** | `/api/v1/payments` for admin; business APIs return embedded `paymentId` |
| **Reporting** | Separate read models — not ad-hoc joins into `payments` from other BCs |

Payment Administration API is for **tenant admins**, not for downstream module internals.

---

## OpenAPI

Grupo springdoc: **`payments-administration`**

```text
GET /v3/api-docs/payments-administration
```

Paths: `/api/v1/payments` (+ `/{id}`, `/{id}/void`).

---

## Testing consumers

- Mock `PaymentReferencePort` in unit / module tests
- Do **not** load full Payments infrastructure unless E2E
- Testcontainers `payments` schema only in cross-BC ITs

---

## Checklist before merging a consumer of Payment

| # | Item |
|---|------|
| 1 | Gradle depends only on `payment-contract` |
| 2 | Aggregates store `PaymentId`, not Payment entity |
| 3 | No SQL against `payments.payment` outside Payments module |
| 4 | Write-time RECORDED check via `PaymentReferencePort` |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | VOIDED blocks **new** dependent links (e.g. Refund) |
| 7 | ADR-018 consulted — do not grow Payment for refund/capture/tax/GL |

---

## Related documents

- [ADR-018 — Payment Domain Model](ADR-018-PAYMENT-DOMAIN-MODEL.md) — **frozen**
- [ADR-017 — Invoice Domain Model](ADR-017-INVOICE-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [BILLING-CONSUMPTION-GUIDE.md](BILLING-CONSUMPTION-GUIDE.md)
- [PASO-22.8-PAYMENTS-CLOSEOUT.md](../audits/PASO-22.8-PAYMENTS-CLOSEOUT.md)
- [PASO-22.7-PAYMENT-VERIFICATION.md](../audits/PASO-22.7-PAYMENT-VERIFICATION.md)
