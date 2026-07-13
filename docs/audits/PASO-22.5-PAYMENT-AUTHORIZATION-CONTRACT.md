# PASO 22.5 — Payment Authorization Contract

**Entregable:** `PaymentPermissionCatalog` (3 permisos) + `IamPermissionCatalog` extendido + `SystemRoleTemplate` actualizado + Flyway V29.

| Elemento | Valor |
|----------|-------|
| Permisos | `payment:create` · `payment:read` · `payment:void` |
| Migración | `V29__seed_payment_authorization_contract.sql` — idempotente (`NOT EXISTS`), backfill `role_permission` |
| `IamPermissionCatalog.ALL` | **49 → 52** |

## Matriz RBAC

| Rol | `payment:create` | `payment:read` | `payment:void` |
|-----|:---:|:---:|:---:|
| OWNER | ✅ | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ |
| MANAGER | ✅ | ✅ | ✅ |
| USER | ❌ | ✅ | ❌ |
| READ_ONLY | ❌ | ✅ | ❌ |

Mismo patrón que `invoice:*` (V27) — sin refund/capture/ledger.

## Tests actualizados

- `SystemRoleTemplateTest` — `PAYMENT_PLATFORM_ALL` (3), `ALL` = 52, matrices OWNER/ADMIN/MANAGER/USER/READ_ONLY.
- `PaymentAuthorizationSeedMigrationIT` (nuevo, espejo `InvoiceAuthorizationSeedMigrationIT`) — idempotencia V29 + backfill tenant pre-existente.
- `EXPECTED_TOTAL_PERMISSION_COUNT` 49→52 en **todos** los `*AuthorizationSeedMigrationIT` (Invoice, Encounter, Appointment, Patient, Organization, Item, base `AuthorizationSeedMigrationIT`); `appliedMigrationVersion()` 27→29.

## Gradle

`identity-access-management` → `implementation(projects.modules.paymentManagement.paymentContract)`.

## Resultado

```
:modules:identity-access-management:test --tests "*SystemRoleTemplateTest*" --tests "*AuthorizationSeedMigrationIT*" → 8/8 verde
```

## Siguiente

**PASO 22.5.1 — Payment Admin API Audit.**
