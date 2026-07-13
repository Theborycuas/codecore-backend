# PASO 21.6 — Invoice Administration API

**Invoice** expone su ciclo de vida completo vía HTTP reactivo: `billing-application` (comandos + use cases + validación multi-port) y `billing-infrastructure` (controller + DTOs + exception handler + configuración de módulo/OpenAPI) — espejo Encounter (multi-port) + Item (shape administrativa), FASE 19.6/20.6.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Aplicación + HTTP  
**Dependencias:** [PASO-21.5.1](PASO-21.5.1-INVOICE-ADMINISTRATION-API-AUDIT.md) · [PASO-21.5](PASO-21.5-INVOICE-AUTHORIZATION-CONTRACT.md)

---

## Entregable

### Application (`billing-application`)

**Comandos** (`com.codecore.billing.application.command`): `CreateInvoiceCommand` / `UpdateInvoiceCommand` (issuer, billToPatientId?, billToOrganizationId?, invoiceNumber?, currency, `List<InvoiceLineDraft>`); `InvoiceLineDraft` (description, amountMinor, itemId?, encounterId?).

**Puertos entrantes** (`port.in`): `ListInvoicesUseCase` / `GetInvoiceUseCase` / `CreateInvoiceUseCase` / `UpdateInvoiceUseCase` / `IssueInvoiceUseCase` / `VoidInvoiceUseCase`.

**`InvoiceAdministrationUseCaseImpl`** — implementa los 6 puertos, con validación de referencias multi-BC en escritura (`create`, `update`, `issue`; **`voidInvoice` no revalida** — transición local):

```text
validateWriteRefs(tenantId, issuer, billTo, lines):
  1. OrganizationReferencePort.existsActiveByIdAndTenant(issuer) → false: InvoiceReferenceNotFoundException
  2. bill-to Patient  → PatientReferencePort.existsActiveByIdAndTenant      → false: InvoiceReferenceNotFoundException
     bill-to Organization → OrganizationReferencePort.existsActiveByIdAndTenant → false: InvoiceReferenceNotFoundException
  3. por cada línea:
     - itemId presente     → ItemReferencePort.existsActiveByIdAndTenant        → false: InvoiceReferenceNotFoundException
     - encounterId presente → EncounterReferencePort.findLinkableByIdAndTenant  → vacío: InvoiceReferenceNotFoundException
                                                                                  → presente + bill-to Patient + patientId≠view.patientId: InvoicePatientMismatchException
```

Soft-unique `invoiceNumber` vía `InvoiceQueryPort.existsByTenantIdAndInvoiceNumber(ExcludingId)` → `DuplicateInvoiceNumberException` (409). Todo el flujo de escritura envuelto en `TransactionalOperator` (R2DBC reactive tx).

Bridging de VOs: los tipos locales de Invoice (`OrganizationId`, `BillToPatientId`, `ItemId`, `EncounterId`, `TenantId`) se convierten por UUID a los VOs de cada contrato consumido (`organization-contract`, `patient-contract`, `inventory-contract`, `encounter-contract`).

### HTTP (`billing-infrastructure`)

| Método | Path | Permiso | Use case |
|--------|------|---------|----------|
| GET | `/api/v1/billing/invoices` | `invoice:read` | `ListInvoicesUseCase` |
| GET | `/api/v1/billing/invoices/{id}` | `invoice:read` | `GetInvoiceUseCase` |
| POST | `/api/v1/billing/invoices` | `invoice:create` (201) | `CreateInvoiceUseCase` |
| PUT | `/api/v1/billing/invoices/{id}` | `invoice:update` | `UpdateInvoiceUseCase` |
| POST | `/api/v1/billing/invoices/{id}/issue` | `invoice:issue` | `IssueInvoiceUseCase` |
| POST | `/api/v1/billing/invoices/{id}/void` | `invoice:void` | `VoidInvoiceUseCase` |

**List query params:** `page` (0), `size` (20), `sort` (`createdAt,desc`), `status` (**default `DRAFT`**; también `ISSUED`/`VOIDED`/`ALL`), `q?`, `issuerOrganizationId?`, `billToPatientId?`, `billToOrganizationId?`.

**DTOs** (`interfaces.http.admin.dto`): `CreateInvoiceRequest` / `UpdateInvoiceRequest` (+ `InvoiceLineRequest`), `InvoiceResponse` (+ `InvoiceLineResponse`, incluye `totalAmountMinor` derivado), `PagedInvoiceResponse`. Ninguno expone `tenantId`.

**`InvoiceHttpExceptionHandler`** (`@RestControllerAdvice(basePackages = "com.codecore.billing.interfaces.http")`, `@Order(HIGHEST_PRECEDENCE)`):

| Excepción | Status |
|-----------|--------|
| `InvoiceNotFoundException` | 404 |
| `InvoiceReferenceNotFoundException` | 404 |
| `DuplicateInvoiceNumberException` | 409 |
| `InvoicePatientMismatchException` | 409 |
| `DuplicateKeyException` (defensa en profundidad — índice único parcial SQL) | 409 |
| `InvalidDomainValueException` | 400 |
| `InvalidInvoiceStateException` | 400 |
| `AuthorizationDeniedException` | 403 |
| `IllegalArgumentException` | 400 |

Sin JWT → 401 (filtro de seguridad IAM, no de este handler).

**Configuración:** `BillingModuleConfiguration` (beans de dominio/application: mapper, repos, use case), `BillingAdministrationConfiguration` (wiring del controller + adapters), `BillingOpenApiConfiguration` (grupo OpenAPI `billing-administration`, `x-permission` vía `@RequiresPermission`).

### Gradle

- `billing-application` → `implementation(billingDomain, organizationContract, patientContract, encounterContract, inventoryContract)` + `reactor` + `spring-boot-starter-data-r2dbc`.
- `billing-infrastructure` → billing-domain/application/contract + esos mismos contratos + `identityAccessManagement` + infra de org/patient/encounter/inventory (solo para ITs) + `platform` + `springdoc`.
- `identity-access-management` → `implementation(billingContract)`.
- `codecore-api` → `implementation(billingInfrastructure)`; `scanBasePackages` incluye `"com.codecore.billing"`.

---

## Tests

| Suite | Tests | Cobertura |
|-------|-------|-----------|
| `InvoiceAdministrationUseCaseTest` (billing-application) | 14 | create/update/issue/void felices; issuer/bill-to/item/encounter inválidos → `InvoiceReferenceNotFoundException`; mismatch encounter↔patient → `InvoicePatientMismatchException`; invoiceNumber duplicado → `DuplicateInvoiceNumberException`; transición inválida delega en el dominio; list con filtros/paginación |

```bash
./gradlew :modules:billing-management:billing-application:test
# BUILD SUCCESSFUL — 14 tests
```

HTTP end-to-end (controller + exception handler + seguridad + persistencia real) se cubre en **PASO-21.7** (`InvoiceVerificationIT`) en lugar de un `InvoiceAdminControllerIT` aislado con `WebTestClient` mockeado — mismo criterio que Item/Encounter: la superficie HTTP de Invoice depende de IAM + 4 ReferencePorts reales, por lo que la integración completa (Testcontainers) es más representativa que un slice test.

---

## Checklist

- [x] 6 use cases + `InvoiceAdministrationUseCaseImpl` con validación multi-port (Organization × 2, Patient, Item, Encounter)
- [x] `voidInvoice` no revalida referencias (transición local, ADR-017 §11)
- [x] Soft-unique `invoiceNumber` en escritura (create + update, excluyendo self)
- [x] Controller + DTOs + exception handler sin exponer `tenantId`
- [x] Default `status=DRAFT` en el listado; filtros adicionales soportados
- [x] `BillingModuleConfiguration` / `BillingAdministrationConfiguration` / `BillingOpenApiConfiguration` — grupo `billing-administration`
- [x] Gradle: application/infrastructure/IAM/codecore-api con las dependencias exactas pedidas

## Siguiente paso

**PASO 21.7 — Invoice Verification**.
