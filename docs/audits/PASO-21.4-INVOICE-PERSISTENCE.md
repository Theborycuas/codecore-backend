# PASO 21.4 — Invoice Persistence

**Invoice** persiste en el schema `billing` (dos tablas, un solo aggregate): `billing.invoice` + `billing.invoice_line`, con R2DBC + Testcontainers PostgreSQL, espejo Item (FASE 20.4).

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Persistencia (Flyway + R2DBC) — sin HTTP, sin RBAC  
**Dependencias:** [PASO-21.3](PASO-21.3-INVOICE-DOMAIN-FOUNDATION.md) · [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md)

---

## Entregable

### Flyway — `V26__create_invoice_tables.sql`

Schema `billing` (nuevo). Dos tablas:

| Tabla | Notas |
|-------|-------|
| `billing.invoice` | `invoice_id` PK; `tenant_id`, `issuer_organization_id` NOT NULL; `bill_to_patient_id` / `bill_to_organization_id` nullable con `ck_invoice_bill_to_xor` (`<>` entre `IS NOT NULL`); `ck_invoice_bill_to_org_not_issuer`; `invoice_number` nullable + índice único parcial `(tenant_id, invoice_number) WHERE invoice_number IS NOT NULL`; `currency` `CHAR(3)` con `ck_invoice_currency_iso`; `status` con `ck_invoice_status IN ('DRAFT','ISSUED','VOIDED')` |
| `billing.invoice_line` | `line_id` PK; `invoice_id` FK **intra-aggregate** (`ON DELETE CASCADE` — no es FK cross-BC); `description` NOT NULL con check no-blank; `amount_minor BIGINT` con `ck_invoice_line_amount_positive` (`> 0`); `currency CHAR(3)`; `item_id` / `encounter_id` nullable **sin FK** (ADR-013 — referencias lógicas a otros BCs) |

Índices: `tenant_id`, `status`, `(tenant_id, status)`, `(tenant_id, issuer_organization_id)` en `invoice`; `invoice_id`, `tenant_id`, `(tenant_id, item_id)`, `(tenant_id, encounter_id)` en `invoice_line`.

**Sin FK cross-BC** — `tenant_id`, `issuer_organization_id`, `bill_to_patient_id`, `bill_to_organization_id`, `item_id`, `encounter_id` son referencias lógicas validadas por ReferencePorts en la capa de aplicación (21.6), no por constraints SQL.

### R2DBC (`billing-infrastructure`)

| Componente | Responsabilidad |
|------------|------------------|
| `InvoiceEntity` / `InvoiceLineEntity` | `Persistable<UUID>` — filas `billing.invoice` / `billing.invoice_line` |
| `InvoiceMapper` | Mapeo isomórfico `Invoice`+líneas ↔ entidades; el aggregate siempre se lee/escribe completo |
| `SpringDataInvoiceRepository` / `SpringDataInvoiceLineRepository` | `ReactiveCrudRepository` + queries derivadas (`existsByTenantIdAndInvoiceNumber…`, `findAllByInvoiceId`, …) |
| `R2dbcInvoiceRepository` | Implementa `InvoiceRepository` + `InvoiceQueryPort`. **`save`**: upsert de la fila `invoice`, luego `deleteAllByInvoiceId` + `saveAll` de las líneas — full-replace, sin updates parciales de línea (ADR-017 §8/§9) |
| `R2dbcInvoiceAdminQueryRepository` | Lectura paginada/filtrada para `InvoiceAdminQueryRepository` (21.6) |

### Test infra copiada (espejo Item)

`billing-infrastructure/src/test/.../testsupport/`: `AbstractPostgresIntegrationTest` (Testcontainers `postgres:16-alpine` estático + Flyway `classpath:db/migration` desde `apps/codecore-api`), `InvoicePersistenceTestConfiguration`, `BillingPersistenceTestApplication` — mismo patrón que `inventory-management`.

---

## Tests — `R2dbcInvoiceRepositoryIT` (10/10 verdes)

| Test | Verifica |
|------|----------|
| `shouldPersistAndFindByIdWithLines` | Save + read round-trip con líneas |
| `shouldPersistOrganizationBillToAndVoidLifecycle` | Bill-to Organization + `voidInvoice()` persistido |
| `shouldFullyReplaceLinesOnUpdate` | `save` reemplaza el set de líneas completo |
| `shouldReportExistsByIdAndTenant` | Aislamiento por tenant en `existsByIdAndTenantId` |
| `shouldCountAndFindByTenantIdAndStatus` | Filtro por status + conteo |
| `shouldIsolateCrossTenantReads` | `findByIdAndTenantId` cross-tenant → vacío |
| `shouldEnforceSoftUniqueInvoiceNumberWithinSameTenant` | Índice único parcial dispara `DuplicateKeyException` |
| `shouldAllowSameInvoiceNumberInDifferentTenants` | Soft-unique es **por tenant** |
| `shouldAllowMultipleInvoicesWithoutNumber` | Múltiples `NULL` permitidos (índice parcial) |
| `shouldReportInvoiceNumberExistenceHelpers` | `existsByTenantIdAndInvoiceNumber(ExcludingId)` |

```bash
./gradlew :modules:billing-management:billing-infrastructure:test --tests "*R2dbcInvoiceRepositoryIT*"
# BUILD SUCCESSFUL — 10 tests
```

---

## Checklist

- [x] Schema `billing` nuevo — sin tocar `inventory` / `encounter` / `patient` / `org` / `iam`
- [x] `invoice` + `invoice_line` sin FK cross-BC (ADR-013)
- [x] Bill-to xor + bill-to-org≠issuer + status + amount>0 reforzados también en SQL (defensa en profundidad)
- [x] Índice único parcial soft-unique `(tenant_id, invoice_number)`
- [x] Aggregate persistido/leído siempre completo (Invoice + líneas)
- [x] 10/10 ITs verdes con Testcontainers PostgreSQL real

## Siguiente paso

**PASO 21.5 — Invoice Authorization Contract**.
