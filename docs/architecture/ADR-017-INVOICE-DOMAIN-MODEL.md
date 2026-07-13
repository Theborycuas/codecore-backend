# ADR-017 — Invoice Domain Model

**Status:** Accepted  
**Date:** 2026-07-12  
**Accepted:** 2026-07-12 (PASO 21.1)  
**Deciders:** CodeCore architecture (FASE 21.1)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-010 · ADR-011 · ADR-012 · ADR-013 · ADR-015 · ADR-016 · [PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md](../audits/PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md) · [PASO-21.0-BILLING-FOUNDATION-PLANNING.md](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Context

FASE 20 closed **Inventory** (`Item`, ADR-016). Clinical and resource BCs remain closed. CodeCore must introduce **Billing** as the first **economic** bounded context — without reopening IAM, Organization, Patient, Appointment, Encounter, or Inventory, and without merging SaaS **Subscriptions** into Billing (DEVELOPMENT-POLICY).

PASO-21.0.1 audited `Invoice` as the first Aggregate Root of Billing: commercial claim that an amount is owed, issuer Organization, bill-to Patient **xor** Organization, internal lines, lifecycle `DRAFT` / `ISSUED` / `VOIDED` — **without** `PAID`, tax engine, GL, or Stock.

A recurring failure mode is the **God Billing module**: invoice + payments + tax + ledger + subscriptions + CRM stuffed into one aggregate until the Core becomes an ERP. This ADR freezes a **deliberately small** Invoice so Dental, Veterinary, Hospital, Lab, Retail, Manufacturing, and B2B ERP share one **commercial claim** foundation **without** that fate.

---

## Decision

### 1. Bounded context

**Billing** — downstream of IAM and Organization; optionally consumes Patient, Encounter, Item via ReferencePorts; independent of Accounting, Payments, Tax, Subscription, Inventory Stock.

Gradle module: `billing-management`  
SQL schema: `billing`  
HTTP surface (shape deferred to PASO 21.5.1): `/api/v1/billing/invoices`

### 2. What Invoice is

`Invoice` is:

> **The commercial claim that an amount is owed by a bill-to party to an issuing organization within a Tenant.**

It is the **only** Aggregate Root in the Core that owns that role within a Tenant. Vertical packs must not introduce parallel roots (“DentalInvoice”, “RetailReceipt”, “HospitalClaim”, …). Downstream aggregates **reference** `InvoiceId` when needed.

It is **not**:

| Not Invoice | Belongs instead |
|-------------|-----------------|
| Payment / PSP capture | Payments BC / aggregate |
| `PAID` / balance status | Payments / reporting projection |
| Tax calculation / fiscal stamp | Tax / compliance packs |
| General ledger / journal | Accounting |
| Price list / fee schedule | Pricing |
| Subscription / plan / seat | Subscription (policy — not this BC) |
| Customer / CRM account | CRM — use Patient \| Organization bill-to |
| Stock deduction | Inventory Stock / Movement |
| Appointment origin | Prefer Encounter on line |
| Credit note | Future aggregate around Invoice |

### 3. Permanence principle — Invoice is intentionally small

> **Invoice is intentionally small.**

This is a **permanent architectural decision**, not a temporary limitation of FASE 21.

`Invoice` may contain **only**:

- commercial claim identity and status  
- issuer organization + exactly one bill-to (Patient **xor** Organization)  
- optional human `invoiceNumber`  
- currency + line amounts (already resolved money)  
- optional per-line `ItemId` / `EncounterId`  
- invariants intrinsic to the commercial claim  

**Everything else lives elsewhere.** Expanding Invoice with payments, tax engines, GL, subscriptions, stock, or CRM is an architecture violation.

### 4. Why Invoice is the Aggregate Root

| Criterion | Rationale |
|-----------|-----------|
| Transaction boundary | Owns draft content, issue, void |
| Own lifecycle | `DRAFT` → `ISSUED` → `VOIDED` — independent of payment |
| Stable ID | `InvoiceId` for Payments, CreditNotes, reporting, packs |
| Single-aggregate invariants | Does **not** transactionalize Payments, Accounting, or Stock |

Charge-first was considered and deferred: lines are internal entities in v1; Charge may appear later if line invariants demand a separate root.

### 5. Ownership

| Concern | Owner BC |
|---------|----------|
| **Invoice** | **Billing** |
| Issuer / bill-to org structure | Organization Management — IDs only |
| Patient bill-to | Clinical Foundation — IDs only |
| Encounter / Item on lines | Clinical Records / Inventory — IDs only |

### 6. Identity and money

- Hard identity: `InvoiceId` (UUID)  
- Optional `invoiceNumber` — soft-unique per tenant if present  
- `Money`: ISO 4217 `currency` + `amountMinor` (`long`); **one currency per Invoice**; line amounts > 0; total = sum of lines > 0  
- No FX; no negative lines in v1 (credits → future CreditNote)

### 7. Bill-to

Exactly one of:

- `billToPatientId` — clinical / self-pay  
- `billToOrganizationId` — B2B; **must differ** from issuer `organizationId`  

Guest / anonymous retail bill-to is **out of v1** (future BillingAccount only with new audit+ADR).

### 8. Lines (internal entities)

Each line: stable line id · non-blank `description` · `Money` · optional `ItemId` · optional `EncounterId`.  
No quantity / UoM / unit price / tax breakdown / StockId / AppointmentId.  
Invoice must have **≥ 1** line.

### 9. Lifecycle

```text
(create) → DRAFT → issue → ISSUED → void → VOIDED
              └── void → VOIDED
```

- Content mutations only in `DRAFT`  
- `issue` revalidates ReferencePorts  
- `void` does not revalidate ports  
- No physical delete; no un-void; no `PAID`  

### 10. Invariants (normative)

1. Exactly one `TenantId`, immutable.  
2. Status ∈ {`DRAFT`, `ISSUED`, `VOIDED`}.  
3. Content mutations only from `DRAFT`; `issue` only from `DRAFT`; `void` from `DRAFT`\|`ISSUED`.  
4. Bill-to XOR Patient \| Organization; issuer always present; bill-to Org ≠ issuer.  
5. ≥ 1 line; description non-blank; amountMinor > 0; single currency; total = sum > 0.  
6. Optional refs validated ACTIVE/linkable on write/`issue` via ReferencePorts.  
7. If bill-to Patient and line has Encounter → encounter.patientId must match bill-to.  
8. No payments, tax breakdown, GL, stock, subscriptions on Invoice.  
9. `InvoiceId` never reassigned; cross-tenant access impossible.  
10. Sole commercial-claim role in Tenant — no parallel Invoice roots in Core.

### 11. Reference Ports (ADR-013)

| Port | When |
|------|------|
| `OrganizationReferencePort.existsActiveByIdAndTenant` | Issuer always; bill-to Org if present |
| `PatientReferencePort.existsActiveByIdAndTenant` | Bill-to Patient |
| `ItemReferencePort.existsActiveByIdAndTenant` | Line ItemId |
| `EncounterReferencePort.findLinkableByIdAndTenant` | Line EncounterId (+ patient coherence) |

No Stock / Payment / Tax / Appointment ports for Invoice v1.

### 12. Permissions (seeded in PASO 21.5)

`invoice:read` · `invoice:create` · `invoice:update` · `invoice:issue` · `invoice:void`

No vertical verbs. No `invoice:pay` / `invoice:post` / `invoice:tax`.

### 13. Multi-organization

Issuer is one Organization per Invoice. Bill-to may be Patient (tenant-scoped) or another Organization. No org-scoped RBAC (ADR-007).

---

## Consequences

### Positive

- Stable `InvoiceId` for Payments without embedding payment state  
- Multi-vertical bill-to without CRM  
- First cross-BC consumer of `ItemReferencePort` outside Inventory  
- Resists Billing-ERP God module  

### Negative / deferred

- Guest retail bill-to unsupported in v1  
- No tax / multi-currency / credit notes  
- Lines cannot express qty without description text  

### Neutral

- HTTP / seeds deferred to 21.5 / 21.5.1  
- `InvoiceReferencePort` in closeout 21.8  

---

## Alternatives considered

| Alternative | Rejected because |
|-------------|------------------|
| Charge-first | No commercial document; worse retail/ERP DX |
| BillingAccount / Customer | CRM premature |
| Only Patient bill-to | Clinical verticalization |
| `PAID` on Invoice | Embeds Payments |
| Subscriptions in Billing | Violates DEVELOPMENT-POLICY split |
| Stock-before-Billing | Stock is Inventory continuation, not economic BC |
| Appointment on Invoice | Dual clinical origin; use Encounter |

---

## Compatibility check

| Document | Impact |
|----------|--------|
| ADR-003 / 006 / 007 | Unchanged |
| ADR-010 / 011 | Org as issuer / bill-to via ports |
| ADR-012 / 015 / 016 | Unchanged — consume ports only |
| ADR-013 | Normative multi-port consumption |
| DEVELOPMENT-POLICY | Billing ≠ Subscription respected |

**No existing ADR is modified.**

---

## Freeze rule

The Invoice domain model defined by this ADR is **frozen** as of PASO **21.1**.

Any change to Aggregate Root boundaries, bill-to model, lifecycle (including adding `PAID`), embedding payments/tax/GL/stock, or the §3 permanence principle **requires** a new architecture audit and a **new ADR**.

Implementation steps 21.2+ must implement this contract — they must not reopen it.

---

## Acceptance

**Accepted** in PASO **21.1** (2026-07-12).  
Evidence: this ADR · [PASO-21.0.1](../audits/PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md) · [PASO-21.0](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md).

---

## References

- [PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md](../audits/PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md)  
- [PASO-21.0-BILLING-FOUNDATION-PLANNING.md](../audits/PASO-21.0-BILLING-FOUNDATION-PLANNING.md)  
- [ADR-016](ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-015](ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ROADMAP.md](ROADMAP.md)  
