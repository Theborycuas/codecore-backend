# PASO 22.6 — Payment Administration API

**Entregable:** `PaymentAdminController` + `PaymentModuleConfiguration` + `PaymentHttpExceptionHandler` + OpenAPI — HTTP completo sobre 22.3/22.4/22.5.

| Elemento | Valor |
|----------|-------|
| Controller | `PaymentAdminController` — `GET /api/v1/payments` · `GET /{id}` · `POST /` · `POST /{id}/void` |
| Permisos | `@RequiresPermission("payment:read\|create\|void")` |
| DTOs | `CreatePaymentRequest` · `PaymentResponse` · `PagedPaymentResponse` |
| Exception handler | `PaymentHttpExceptionHandler` (`@RestControllerAdvice` scoped a `com.codecore.payment.interfaces.http`) |
| Configuration | `PaymentModuleConfiguration` (`@EnableR2dbcRepositories` + import `PaymentAdministrationConfiguration`/`PaymentOpenApiConfiguration`) · `PaymentAdministrationConfiguration` (wiring use cases + `IamTenantContextAccessor` + `InvoiceReferencePort` bean externo) |
| OpenAPI | `PaymentOpenApiConfiguration` — grupo `payments-administration` sobre `/api/v1/payments/**` |

## Wiring `codecore-api`

- `implementation(projects.modules.paymentManagement.paymentInfrastructure)`
- `scanBasePackages` += `"com.codecore.payment"`

## Unit tests

```
PaymentAdministrationUseCaseImplTest — 5/5 (create+issued invoice, reject+non-issued invoice, void sin revalidar port, void not-found, re-void rejected)
```

## Siguiente

**PASO 22.7 — Payment Verification.**
