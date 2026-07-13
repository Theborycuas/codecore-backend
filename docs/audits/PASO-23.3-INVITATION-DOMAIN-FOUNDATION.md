# PASO 23.3 — Invitation Domain Foundation

**Entregable:** Módulo `access-management` (domain, application, infrastructure, contract) en Gradle; Aggregate `Invitation` + VOs — ADR-019 implementado al pie de la letra.

| Elemento | Valor |
|----------|-------|
| Package | `com.codecore.access` |
| Aggregate | `Invitation` — `create` → `PENDING`; `accept` → `ACCEPTED`; `revoke` → `REVOKED`; `expire` → `EXPIRED` |
| VOs | `InvitationId` · `TenantId` · `EmailAddress` · `InvitationRoleCode` · `InvitationTokenHash` · `MembershipId` · `InvitationStatus` |
| Exceptions | `InvitationDomainException` · `InvalidDomainValueException` · `InvalidInvitationStateException` · `InvitationNotFoundException` · `ActiveMembershipAlreadyExistsException` · `PendingInvitationAlreadyExistsException` · … |
| Tests | 28 domain tests (`InvitationTest` + `InvitationValueObjectTest` + `InvitationNegativeApiTest`) |

## Invariantes verificadas

- `TenantId` / email / role / invitedBy inmutables tras `create`.
- `InvitationRoleCode` allow-list: ADMIN, MANAGER, USER, READ_ONLY — **nunca OWNER**.
- Raw token **nunca** en el aggregate; solo `InvitationTokenHash`.
- `accept` / `revoke` / `expire` rechazan transiciones ilegales (`InvalidInvitationStateException`).
- Reflexión: **no** existen StaffAssignment, Subscription, PasswordReset, ni email transport en la API pública de `Invitation`.

## Gradle

`settings.gradle.kts` — bloque `access-management:{access-domain,access-application,access-infrastructure,access-contract}`.

`access-domain/build.gradle.kts` — solo `codecore.spring-boot-library` (sin Spring en `main`).

## Resultado

```
:modules:access-management:access-domain:test → 28/28 verde
```

## Siguiente

**PASO 23.4 — Invitation Persistence.**
