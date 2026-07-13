# PASO 21.5 — Invoice Authorization Contract

**Invoice** entra al contrato de permisos de la plataforma: 5 nuevos permisos (`invoice:create|read|update|issue|void`), `IamPermissionCatalog.ALL` pasa de **44 → 49**, `SystemRoleTemplate` extendido — espejo Item (FASE 20.5).

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Autorización (contrato + seed + wiring IAM)  
**Dependencias:** [PASO-21.4](PASO-21.4-INVOICE-PERSISTENCE.md) · [ADR-017 §12](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md)

---

## Entregable

### Contrato — `billing-contract`

`com.codecore.billing.contract.authorization.InvoicePermissionCatalog`:

```java
INVOICE_CREATE = "invoice:create"
INVOICE_READ   = "invoice:read"
INVOICE_UPDATE = "invoice:update"
INVOICE_ISSUE  = "invoice:issue"
INVOICE_VOID   = "invoice:void"

ALL              = {create, read, update, issue, void}   // 5
INVOICE_READ_ONLY = {read}                                // 1
```

Deliberadamente **sin** `invoice:pay`, `invoice:post`, `invoice:tax` — pertenecen a Payments / Accounting / Tax (BCs futuros, ADR-017 §2 fuera de alcance).

### Flyway — `V27__seed_invoice_authorization_contract.sql`

- Inserta los 5 permisos en `iam.permission` (idempotente, `WHERE NOT EXISTS`).
- Backfill de `iam.role_permission` para tenants pre-existentes (`system_role = TRUE`), matriz:

| Rol | create | read | update | issue | void |
|-----|:-:|:-:|:-:|:-:|:-:|
| OWNER | ✅ | ✅ | ✅ | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ |
| MANAGER | ✅ | ✅ | ✅ | ✅ | ✅ |
| USER | — | ✅ | — | — | — |
| READ_ONLY | — | ✅ | — | — | — |

### Wiring IAM (`identity-access-management`)

- `IamPermissionCatalog`: añade `INVOICE_CREATE/READ/UPDATE/ISSUE/VOID` + `INVOICE_ALL` / `INVOICE_READ_ONLY` / `INVOICE_PLATFORM_ALL` (= `INVOICE_ALL`) + `MANAGER_INVOICE` (= `INVOICE_ALL`); `ALL` = `union(IAM_FOUNDATION, ORGANIZATION_PLATFORM_ALL, PATIENT_PLATFORM_ALL, APPOINTMENT_PLATFORM_ALL, ENCOUNTER_PLATFORM_ALL, ITEM_PLATFORM_ALL, INVOICE_PLATFORM_ALL)` → **49** permisos.
- `SystemRoleTemplate`: `OWNER=ALL`; `ADMIN` añade `INVOICE_PLATFORM_ALL`; `MANAGER` añade `MANAGER_INVOICE`; `USER`/`READ_ONLY` añaden `INVOICE_READ_ONLY`.
- `build.gradle.kts` (`identity-access-management`): `implementation(project(":modules:billing-management:billing-contract"))`.

---

## Tests (11/11 verdes)

| Suite | Tests | Verifica |
|-------|-------|----------|
| `InvoicePermissionCatalogTest` (billing-contract) | 3 | `ALL` tiene 5 elementos, `READ_ONLY` = `{read}`, códigos exactos `invoice:*` |
| `InvoiceAuthorizationSeedMigrationIT` (IAM) | 1 | Migra hasta V26, siembra tenant+roles pre-V27, corre V27 → `iam.permission` = **49**, `invoice:*` = 5, versión aplicada = `"27"`; re-ejecución del seed es idempotente (no duplica); backfill correcto por rol (OWNER/ADMIN/MANAGER=5, USER/READ_ONLY=1) |
| `SystemRoleTemplateTest` | (existente, actualizado) | `IamPermissionCatalog.ALL` `hasSize(49)`; `OWNER` incluye todos los `invoice:*`; `USER`/`READ_ONLY` solo `invoice:read` |
| `IamPermissionCatalogTest` / otros contadores | (existente, actualizado) | Cualquier assertion `hasSize(44)` migrada a `hasSize(49)` |

```bash
./gradlew :modules:billing-management:billing-contract:test
./gradlew :modules:identity-access-management:test --tests "*Permission*" --tests "*SystemRole*" --tests "*Invoice*" --tests "*ItemAuthorization*"
# BUILD SUCCESSFUL
```

### Deviación anotada

Se auditó el repo completo por asserts hardcodeados `44` relacionados al conteo de permisos globales — todos los que referencian `IamPermissionCatalog.ALL` ya estaban actualizados a `49` en el working tree heredado. No se encontraron regresiones `44` pendientes. Ver PASO-21.7 §"Fallos IAM pre-existentes corregidos" — se detectaron y corrigieron (fuera del modelo Invoice, sin ADR nuevo) dos bugs latentes de configuración de test IAM no relacionados con Billing, descubiertos al correr la suite amplia solicitada por el enunciado.

## Checklist

- [x] `InvoicePermissionCatalog` con 5 permisos, sin pay/post/tax
- [x] V27 seed 5 permisos + backfill matriz OWNER/ADMIN/MANAGER=todos, USER/READ_ONLY=read
- [x] `IamPermissionCatalog.ALL` 44 → 49
- [x] `SystemRoleTemplate` extendido para los 5 roles
- [x] `InvoiceAuthorizationSeedMigrationIT` nuevo, idempotencia verificada
- [x] `identity-access-management/build.gradle.kts` con `implementation(billingContract)`

## Siguiente paso

**PASO 21.5.1 — Invoice Administration API (audit) / PASO 21.6 — implementación**.
