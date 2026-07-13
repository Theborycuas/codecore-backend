# PASO 24.6 — Audit Administration API

**Entregable:** Application + HTTP read-only + `AuditAppendPort` bean + productores Access/IAM.

| Elemento | Valor |
|----------|-------|
| Use cases | List / Get / Append (`AuditAdministrationUseCaseImpl`) |
| Actor | Si presente → ACTIVE via `IamMembershipReferencePort` |
| HTTP | `AuditAdminController` GET only · `AuditHttpExceptionHandler` |
| Producers | Invitation create/accept/revoke · PasswordReset request/complete (best-effort) |

## Resultado

Application unit tests append + actor validation verdes.

## Siguiente

**PASO 24.7 — Audit Verification.**
