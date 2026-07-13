# PASO 22.3 — Payment Domain Foundation

**Entregable:** Módulo `payment-management` (domain, application, infrastructure, contract) en Gradle; Aggregate `Payment` + VOs — ADR-018 implementado al pie de la letra.

| Elemento | Valor |
|----------|-------|
| Package | `com.codecore.payment` |
| Aggregate | `Payment` — `create` → `RECORDED`; `voidPayment` `RECORDED` → `VOIDED` |
| VOs | `PaymentId` · `TenantId` · `InvoiceId` (referencia local) · `Money` · `PaymentMethodCode` · `PaymentStatus` |
| Exceptions | `PaymentDomainException` · `InvalidDomainValueException` · `InvalidPaymentStateException` · `PaymentNotFoundException` · `InvoiceNotFoundException` |
| Tests | 20 domain tests (`PaymentTest` + `PaymentValueObjectTest`) |

## Invariantes verificadas

- `TenantId` / `InvoiceId` inmutables tras `create` (getters constantes; sin setters).
- `Money.amountMinor > 0`; ISO 4217 `currency`.
- `PaymentMethodCode` opcional, opaco, ≤ 32 chars, sin interpretar.
- `voidPayment()` rechaza re-void (`InvalidPaymentStateException`).
- Reflexión: **no** existen `addRefund`, `postToLedger`, `capturePsp`, `markInvoicePaid` en la API pública de `Payment` — confirmado en `shouldNeverExposeRefundLedgerPspOrInvoiceMutationConcernsInPublicApi()`.

## Gradle

`settings.gradle.kts` — bloque `payment-management:{payment-domain,payment-application,payment-infrastructure,payment-contract}` justo después del bloque `billing-management`.

`payment-domain/build.gradle.kts` — solo `codecore.spring-boot-library` (sin Spring en `main`, mismo patrón que `billing-domain`).

## Resultado

```
:modules:payment-management:payment-domain:test → 20/20 verde
```

## Siguiente

**PASO 22.4 — Payment Persistence.**
