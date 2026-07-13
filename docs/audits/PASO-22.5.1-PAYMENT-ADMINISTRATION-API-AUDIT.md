# PASO 22.5.1 — Payment Admin API Audit

**Obligatoria** (espejo `InvoiceAdminApiPaths` / `PatientAdminApiPaths`) — decisiones HTTP antes de escribir código en 22.6.

| Decisión | Valor |
|----------|-------|
| Base path | `/api/v1/payments` |
| Endpoints | `GET /` (list) · `GET /{id}` · `POST /` (create) · `POST /{id}/void` |
| Sin | `PUT` (no content update, ADR-018) · `DELETE` (no physical delete) |
| Default list `status` | `RECORDED` (cola de trabajo efectiva; espejo `status=DRAFT` en Invoice pero invertido a *lo vigente*) |
| Filtros list | `status=RECORDED\|VOIDED\|ALL` · `invoiceId` |
| `tenantId` en request body | **Nunca** — siempre resuelto del JWT vía `TenantContextAccessor` |
| DTOs | `CreatePaymentRequest` (sin id/tenant/status) · `PaymentResponse` · `PagedPaymentResponse` |
| Exceptions → HTTP | `PaymentNotFoundException`→404 · `InvoiceNotFoundException`→404 · `InvalidDomainValueException`→400 · `InvalidPaymentStateException`→400 · `AuthorizationDeniedException`→403 |
| OpenAPI group | `payments-administration` |

## Por qué default `status=RECORDED` y no `ALL`

Los administradores de tenant, en el 90% de los casos, buscan pagos **efectivos** (no anulados) — mismo criterio que Invoice usa `DRAFT` como cola de trabajo pendiente; en Payments no hay borrador, así que la vista por defecto es el registro vigente.

## Siguiente

**PASO 22.6 — Payment Administration API.**
