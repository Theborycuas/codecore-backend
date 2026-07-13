# PASO 22.4 — Payment Persistence

**Entregable:** Schema `payments`, tabla `payments.payment` (Flyway V28), adaptador R2DBC completo, capa de aplicación (`payment-application`).

| Elemento | Valor |
|----------|-------|
| Migración | `V28__create_payment_table.sql` |
| Tabla | `payments.payment` — sin FK a `billing.invoice` (ADR-013/ADR-018) |
| Índices | `tenant_id` · `status` · `(tenant_id, status)` · `(tenant_id, invoice_id)` |
| Check | `status IN ('RECORDED','VOIDED')` · `amount_minor > 0` · `currency ~ '^[A-Z]{3}$'` |
| Entidad | `PaymentEntity` (`Persistable<UUID>`) |
| Mapper | `PaymentMapper` — isomórfico `PaymentEntity` ↔ `Payment` |
| Repos | `SpringDataPaymentRepository` · `R2dbcPaymentRepository` (`PaymentRepository` + `PaymentQueryPort`) · `R2dbcPaymentAdminQueryRepository` (`DatabaseClient`, filtros status/invoiceId) |

## Application layer (`payment-application`)

- `CreatePaymentCommand`
- Use cases in: `ListPaymentsUseCase` · `GetPaymentUseCase` · `CreatePaymentUseCase` · `VoidPaymentUseCase`
- `PaymentAdministrationUseCaseImpl` — implementa los 4 in-ports; usa `InvoiceReferencePort` (dependencia a `billing-contract`) para validar en `create`; `TransactionalOperator` para atomicidad create/void.
- `void` **no** revalida el port (ADR-018 — mismo patrón que `voidInvoice` en Billing).

## Tests (Testcontainers PostgreSQL)

```
R2dbcPaymentRepositoryIT — 6/6 (persist, void lifecycle, exists, cross-tenant isolation, count/status, optional method code)
```

## Siguiente

**PASO 22.5 — Payment Authorization Contract.**
