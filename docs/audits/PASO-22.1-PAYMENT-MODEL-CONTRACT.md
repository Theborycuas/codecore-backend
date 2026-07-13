# PASO 22.1 — Payment Model Contract (ADR-018)

**Entregable:** [ADR-018](../architecture/ADR-018-PAYMENT-DOMAIN-MODEL.md) **Accepted**.

| Elemento | Valor |
|----------|-------|
| Definición | Record that an amount was applied toward settling an Invoice |
| Estados | `RECORDED` · `VOIDED` |
| Refs | `TenantId` · `InvoiceId` |
| Ports | `InvoiceReferencePort.existsIssuedByIdAndTenant` |
| Permisos | `payment:create\|read\|void` |
| HTTP / schema | `/api/v1/payments` · `payments` |

## Siguiente

**PASO 22.2 — Payments Reference Readiness**.
