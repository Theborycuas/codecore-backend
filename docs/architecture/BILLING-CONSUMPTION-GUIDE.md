# Billing Consumption Guide

**Audience:** Developers building modules after FASE 21 Invoice slice (Payments, CreditNotes, Reporting, Accounting bridge, vertical packs, …)  
**Authority:** [ADR-017](ADR-017-INVOICE-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
**Status:** Vigente desde PASO 21.8 — Billing **Invoice slice cerrada**

---

## Mental model

FASE 21 no es “ERP de facturación”. FASE 21 entrega un **bounded context estable** (slice Invoice): la reclamación comercial de un importe adeudado (*intentionally small*).

```text
Tenant (IAM)
 └── Invoice                     ← commercial claim (intentionally small)
      ├── issuerOrganizationId   ← who issues
      ├── billTo                 ← Patient XOR Organization (≠ issuer)
      ├── invoiceNumber?         ← optional soft-unique human number
      ├── lines[]                ← description + Money (+ ItemId? / EncounterId?)
      └── status                 ← DRAFT → ISSUED → VOIDED
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Cuál es la reclamación comercial? | `InvoiceId` |
| ¿Puedo liquidar / registrar un pago sobre esta Invoice? | `InvoiceReferencePort.existsIssuedByIdAndTenant` |
| ¿Quién emite / quién debe? | Issuer Org · bill-to Patient \| Org |
| ¿Está pagada? | **No en Invoice** — Payments / proyección |
| ¿Impuesto / asiento / suscripción? | **No en Invoice** — Tax / Accounting / Subscription |

---

## Decision tree (30 seconds)

```text
Need to store a link to a commercial claim?
  → Store InvoiceId on your aggregate + tenantId

Need to validate invoice exists and is ISSUED at write time (e.g. Payment)?
  → InvoiceReferencePort (`billing-contract`, ADR-013 / ADR-017)
  → Never InvoiceRepository, never SQL against billing.invoice from another BC

Need labels / line details for UI?
  → Prefer your own read model / Query Port later — do not import Invoice aggregate

Need payment capture, tax engine, GL posting, or SaaS seats?
  → Wrong — grow around Invoice via Payments / Tax / Accounting / Subscription (ADR-017 §3)
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:billing-management:billing-contract"))
```

Only. Never `billing-application` or `billing-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `InvoiceId` + `tenantId` | `@Autowired R2dbcInvoiceRepository` |
| Validate via `InvoiceReferencePort` | `SELECT * FROM billing.invoice` outside Billing |
| Filter by JWT `tenantId` | Accept client-sent `tenantId` as authority |
| Treat Invoice as commercial claim | Put `PAID`, tax, GL, Stock, Subscription on Invoice |

---

## Contract surface (Invoice slice closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `InvoiceId` | `billing-contract` → domain VO | Hard identity |
| `InvoicePermissionCatalog` | `billing-contract` | `invoice:create\|read\|update\|issue\|void` |
| `InvoiceReferencePort` | `billing-contract` | ISSUED + tenant existence check |
| `R2dbcInvoiceReferenceAdapter` | `billing-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface InvoiceReferencePort {
    Mono<Boolean> existsIssuedByIdAndTenant(InvoiceId invoiceId, TenantId tenantId);
}
```

`DRAFT` / `VOIDED` → `false` (blocks **new** Payment links; historical reads may need a separate method later if a consumer invariant requires it).

---

## Module recipes

### Payments (next logical consumer)

**Owns:** liquidation / capture against a claim  
**References:** `InvoiceId` (required ISSUED)

```text
Payment
  invoiceId           ← validate ISSUED via InvoiceReferencePort
  amount…
```

**Never:** embed Invoice lines as source of truth; never set Invoice status to `PAID`.

### CreditNote (same BC or adjacent — later)

**Owns:** credit against a prior claim  
**References:** `InvoiceId` (typically ISSUED or VOIDED historical)

### Accounting bridge / Reporting

**Owns:** export / statements  
**References:** `InvoiceId`; never owns Invoice lifecycle; never turns Billing into the GL.

### Vertical packs (Dental / Retail / Hospital)

**Owns:** presentation rules, fiscal adapters local  
**References:** `InvoiceId`; never create parallel “DentalInvoice” roots in the Core.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `InvoiceReferencePort` in `billing-contract` |
| **Frontend / mobile** | `/api/v1/billing/invoices` for admin; business APIs return embedded `invoiceId` |
| **Reporting** | Separate read models — not ad-hoc joins into `billing` from other BCs |

Invoice Administration API is for **tenant admins**, not for Payments module internals.

---

## OpenAPI

Grupo springdoc: **`billing-administration`**

```text
GET /v3/api-docs/billing-administration
```

Paths: `/api/v1/billing/invoices` (+ issue/void).

---

## Testing consumers

- Mock `InvoiceReferencePort` in unit / module tests  
- Do **not** load full Billing infrastructure unless E2E  
- Testcontainers `billing` schema only in cross-BC ITs  

---

## Checklist before merging a consumer of Invoice

| # | Item |
|---|------|
| 1 | Gradle depends only on `billing-contract` |
| 2 | Aggregates store `InvoiceId`, not Invoice entity |
| 3 | No SQL against `billing.invoice` outside Billing module |
| 4 | Write-time ISSUED check via `InvoiceReferencePort` |
| 5 | Tenant filter on every query (ADR-003) |
| 6 | DRAFT / VOIDED blocks **new** payment links |
| 7 | ADR-017 consulted — do not grow Invoice for pay/tax/GL/Subscription |

---

## Related documents

- [ADR-017 — Invoice Domain Model](ADR-017-INVOICE-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [INVENTORY-CONSUMPTION-GUIDE.md](INVENTORY-CONSUMPTION-GUIDE.md) · [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](CLINICAL-RECORDS-CONSUMPTION-GUIDE.md)
- [PASO-21.8-BILLING-CLOSEOUT.md](../audits/PASO-21.8-BILLING-CLOSEOUT.md)
- [PASO-21.7-INVOICE-VERIFICATION.md](../audits/PASO-21.7-INVOICE-VERIFICATION.md)
