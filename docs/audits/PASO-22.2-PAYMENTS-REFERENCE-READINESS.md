# PASO 22.2 — Payments Reference Readiness

**Veredicto:** `InvoiceReferencePort.existsIssuedByIdAndTenant` **ya es suficiente** — **sin evolución** de contract.

| Invariante | Port | ¿OK? |
|------------|------|------|
| Create Payment → Invoice ISSUED en tenant | `existsIssuedByIdAndTenant` | ✅ |
| Void | No revalidar | ✅ |

No Patient/Org/Item/Encounter/Stock ports.

**Listo para 22.3 Domain Foundation.**
