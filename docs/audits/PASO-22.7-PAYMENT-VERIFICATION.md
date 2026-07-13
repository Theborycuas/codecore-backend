# PASO 22.7 — Payment Verification

**Entregable:** `PaymentVerificationIT` — 8 verificaciones E2E HTTP sobre stack completo (IAM + Org + Patient + Item + Encounter-ref + Billing + Payments).

| # | Verificación | Resultado |
|---|--------------|-----------|
| 1 | Journey completo create → list → get → void | ✅ |
| 2 | READ_ONLY no puede crear payment | ✅ 403 |
| 3 | Cross-tenant access | ✅ 404 |
| 4 | Invoice inexistente al crear | ✅ 404 |
| 5 | MANAGER puede void per matriz RBAC | ✅ 200 |
| 6 | OpenAPI documenta `payments-administration` | ✅ |
| 7 | Sin JWT | ✅ 401 |
| 8 | Crear payment contra Invoice DRAFT (no ISSUED) | ✅ 404 |

## Test configuration

`PaymentAdministrationVerificationTestConfiguration` → `PaymentAdminIntegrationTestConfiguration` — mínimo necesario para poder crear + emitir una Invoice y liquidarla:

- IAM completo (auth, tenants, roles) — mismo bloque que `InvoiceAdminIntegrationTestConfiguration`
- `BillingModuleConfiguration` + `InvoiceAdminController` + `R2dbcInvoiceRepository`/`R2dbcInvoiceAdminQueryRepository`/`R2dbcInvoiceReferenceAdapter`
- `OrganizationModuleConfiguration` + `PatientModuleConfiguration` + `InventoryModuleConfiguration` (issuer/bill-to Invoice) · `R2dbcEncounterReferenceAdapter` (bean requerido por `InvoiceAdministrationUseCaseImpl`, sin superficie HTTP Encounter)
- `PaymentModuleConfiguration` + `PaymentAdminController`

## Resultado

```
PaymentVerificationIT → 8/8 verde
```

## Siguiente

**PASO 22.8 — Payments Closeout.**
