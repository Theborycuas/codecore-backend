# PASO 24.3 — Audit Domain Foundation

**Entregable:** Módulo `audit-management` (domain) en Gradle; Aggregate `AuditEntry` + VOs — ADR-020 implementado al pie de la letra.

| Elemento | Valor |
|----------|-------|
| Package | `com.codecore.audit` |
| Aggregate | `AuditEntry` — `append`/`create` → immutable terminal |
| VOs | `AuditEntryId` · `TenantId` · `MembershipId` (local) · `ActionCode` · `ResourceType` · `ResourceId` · `AuditOutcome` |
| Exceptions | `AuditDomainException` · `InvalidDomainValueException` · `AuditEntryNotFoundException` · `ActorMembershipNotFoundException` |
| Tests | 18 domain tests (`AuditEntryTest` + `AuditValueObjectTest`) |

## Invariantes verificadas

- Append-only: sin update/void/delete/setPayload/metrics en API pública (reflexión).
- `ActionCode` / `ResourceType` no blank, ≤64.
- `outcome` default `SUCCESS` cuando null.
- `createdAt` = `occurredAt` en factory.

## Resultado

```
:modules:audit-management:audit-domain:test → verde
```

## Siguiente

**PASO 24.4 — Audit Persistence.**
