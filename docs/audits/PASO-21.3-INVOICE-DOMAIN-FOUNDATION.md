# PASO 21.3 — Invoice Domain Foundation

**Invoice** existe como aggregate puro (dominio sin framework): VOs + `Invoice` + `InvoiceLine` + excepciones + 38 tests de dominio, espejo Item (FASE 20.3).

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Dominio (ADR-017) — sin persistencia, sin HTTP, sin ports  
**Dependencias:** [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md) · [PASO-21.2](PASO-21.2-BILLING-REFERENCE-READINESS.md)

---

## Entregable

**Módulo:** `modules/billing-management/billing-domain` (framework-free; `codecore.spring-boot-library` solo alinea el classpath de test).

### Value Objects (`com.codecore.billing.domain.valueobject`)

| VO | Regla |
|----|-------|
| `InvoiceId` | UUID surrogate; `generate()` |
| `InvoiceLineId` | UUID surrogate; `generate()` |
| `TenantId` | Referencia lógica a `iam.tenant`; inmutable en `Invoice` tras `create` |
| `OrganizationId` | Emisor — referencia lógica a `org.organization` |
| `BillToPatientId` | Deudor clínico — referencia lógica a `patient.patient` |
| `BillToOrganizationId` | Deudor B2B — referencia lógica a `org.organization` |
| `ItemId` | Línea de material opcional — referencia lógica a `inventory.item` |
| `EncounterId` | Línea de origen clínico opcional — referencia lógica a `encounter.encounter` |
| `InvoiceNumber` | Opcional, no-blank, máx 64, trim; soft-unique per tenant (aplicación + persistencia) |
| `LineDescription` | No-blank, máx 500, trim |
| `Money` | `currency` ISO 4217 (regex `^[A-Z]{3}$`, normalizado a mayúsculas) + `amountMinor` (long, estrictamente > 0) |
| `InvoiceStatus` | `DRAFT` \| `ISSUED` \| `VOIDED` — sin `PAID` |
| `BillTo` | Factory `patient(...)` / `organization(...)` / `of(...)` (reconstitución defensiva) — exactamente uno de Patient/Organization |

### Aggregate root — `Invoice`

- Campos: `id`, `tenantId` (inmutable), `issuerOrganizationId`, `billTo`, `invoiceNumber?`, `lines` (≥ 1), `status`, `createdAt`, `updatedAt`.
- `create(...)` → `DRAFT` + valida invariantes.
- `updateContent(...)` — full replace (PUT), solo permitido en `DRAFT`.
- `issue()` — `DRAFT → ISSUED`, re-valida invariantes de contenido.
- `voidInvoice()` — `DRAFT|ISSUED → VOIDED`; sin re-validación de ReferencePorts; sin un-void.
- `currency()` / `totalAmountMinor()` — derivados de las líneas (moneda única; total = suma de `amountMinor`).
- Invariantes (`validateInvariants`): bill-to Organization ≠ issuer; ≥ 1 línea; todas las líneas comparten la misma `currency`.

### Entidad interna — `InvoiceLine`

- `id`, `description` (`LineDescription`), `amount` (`Money`), `itemId?`, `encounterId?`.
- Inmutable — `Invoice.updateContent` reemplaza la lista completa.
- **Sin** `quantity`, `unitOfMeasure`, `unitPrice`, desglose de impuestos, `StockId` ni `AppointmentId`.

### Excepciones (`com.codecore.billing.domain.exception`)

| Excepción | Uso |
|-----------|-----|
| `InvoiceDomainException` | Base |
| `InvalidDomainValueException` | VO inválido / invariante de contenido |
| `InvalidInvoiceStateException` | Transición de lifecycle inválida |
| `InvoiceNotFoundException` | Invoice no encontrado en el tenant |
| `InvoiceReferenceNotFoundException` | ReferencePort devuelve `false`/vacío (application, 21.6) |
| `InvoicePatientMismatchException` | Línea `EncounterId` no coincide con bill-to Patient (application, 21.6) |
| `DuplicateInvoiceNumberException` | `invoiceNumber` duplicado en el tenant (application, 21.6) |

---

## Tests de dominio (38/38 verdes)

| Suite | Tests | Cobertura |
|-------|-------|-----------|
| `InvoiceTest` | 16 | create (Patient/Organization bill-to), bill-to org = issuer rechazado, ≥1 línea, mismatch de moneda, total = suma, update solo DRAFT, issue DRAFT→ISSUED (+ rechazo si no DRAFT), void DRAFT\|ISSUED→VOIDED (+ rechazo doble void), inmutabilidad de `tenantId`, reconstitución, **test negativo de reflexión** |
| `InvoiceValueObjectTest` | 22 | Igualdad por valor de todos los VO id-like, trims, longitudes, moneda ISO 4217, `amountMinor > 0`, `BillTo` factory/`of` xor, enum `InvoiceStatus` congelado |

### Test negativo (anti God Aggregate)

`shouldNeverExposePaymentsTaxLedgerStockOrSubscriptionConcernsInPublicApi` — reflexión sobre `Invoice.class.getDeclaredMethods()` **niega**: `addPayment`, `recordPayment`, `pay`, `calculateTax`, `applyTax`, `deductStock`, `adjustStock`, `addSubscription`, `assignPlan`, `postToLedger`, `post`, `toJournalEntry`.

```bash
./gradlew :modules:billing-management:billing-domain:test
# BUILD SUCCESSFUL — 38 tests
```

---

## Checklist

- [x] `Invoice` + `InvoiceLine` según ADR-017 (frozen) — sin desviaciones
- [x] VOs id-like locales (sin depender de domain ajeno)
- [x] Excepciones de dominio + aplicación preparadas
- [x] 38 tests verdes incluyendo test negativo de reflexión
- [x] Sin Spring, sin R2DBC, sin HTTP en este módulo

## Siguiente paso

**PASO 21.4 — Invoice Persistence**.
