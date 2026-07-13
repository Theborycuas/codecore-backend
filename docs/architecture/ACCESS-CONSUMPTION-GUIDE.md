# Access Consumption Guide

**Audience:** Developers building modules after FASE 23 Access (Invitation consumers, onboarding UX, vertical packs, future Subscription seat policy, …)
**Authority:** [ADR-019](ADR-019-INVITATION-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-006](ADR-006-IDENTITY-GLOBAL-MEMBERSHIP.md)
**Status:** Vigente desde PASO 23.8 — Access (Invitation) **cerrado**

---

## Mental model

FASE 23 Access no es “IAM completo” ni “SaaS billing”. Entrega un **bounded context estable**: la intención de otorgar Membership en un Tenant a un email (*intentionally small*).

```text
Tenant (IAM)
 └── Invitation                    ← join-intent (intentionally small)
      ├── invitedEmail             ← immutable
      ├── invitedRoleCode          ← ADMIN|MANAGER|USER|READ_ONLY (≠ OWNER)
      ├── invitedByMembershipId    ← ACTIVE inviter at create
      ├── tokenHash                ← raw token never stored
      └── status                   ← PENDING → ACCEPTED | REVOKED | EXPIRED
           └── accept → TenantAccessProvisionPort → Membership (IAM owns Membership)
```

| Pregunta | Dónde mirar |
|----------|-------------|
| ¿Hay invite PENDING para este id+tenant? | `InvitationReferencePort.existsPendingByIdAndTenant` |
| ¿Puedo crear / revoke invites? | Admin API + `invitation:create\|read\|revoke` |
| ¿Cómo entra el usuario? | `POST …/accept` con token (público) → IAM provision |
| ¿Quién es dueño de Membership? | **IAM** — Invitation solo guarda `resultingMembershipId` set-once |
| ¿Subscription / seats / StaffAssignment? | **No en Access** — BCs / tracks futuros |

---

## Decision tree (30 seconds)

```text
Need to invite a user into a Tenant by email?
  → That IS Invitation — do not create parallel "DentalInvite" / "OrgInvite" roots

Need to check whether a specific invite is still PENDING?
  → InvitationReferencePort.existsPendingByIdAndTenant (`access-contract`, ADR-013/ADR-019)
  → Never InvitationRepository, never SQL against access.invitation from another BC

Need to provision Membership / Identity?
  → Wrong place to own it — IAM via TenantAccessProvisionPort (Access uses it; consumers don't reinvent)

Need seats, plans, org-scoped invites, or custom roles?
  → Wrong — Subscription BC / new ADR; never grow Invitation for commercial SaaS
```

---

## Dependency rules

### Gradle

```kotlin
implementation(project(":modules:access-management:access-contract"))
```

Only. Never `access-application` or `access-infrastructure`.

### Code

| ✅ Do | ❌ Don't |
|-------|----------|
| Store `InvitationId` + `tenantId` | `@Autowired R2dbcInvitationRepository` |
| Validate via `InvitationReferencePort` | `SELECT * FROM access.invitation` outside Access |
| Filter by JWT `tenantId` on admin APIs | Accept client-sent `tenantId` as authority |
| Treat Invitation as join-intent | Put Membership lifecycle, seats, or StaffAssignment on Invitation |

---

## Contract surface (Access closed)

| Artifact | Module | Purpose |
|----------|--------|---------|
| `InvitationId` | `access-contract` → domain VO | Hard identity |
| `InvitationPermissionCatalog` | `access-contract` | `invitation:create\|read\|revoke` |
| `InvitationReferencePort` | `access-contract` | PENDING + tenant existence check |
| `R2dbcInvitationReferenceAdapter` | `access-infrastructure` | In-process implementation (wired by codecore-api) |

```java
public interface InvitationReferencePort {
    Mono<Boolean> existsPendingByIdAndTenant(InvitationId invitationId, TenantId tenantId);
}
```

`ACCEPTED` / `REVOKED` / `EXPIRED` → `false`.

---

## Module recipes

### Onboarding / product UX

**Owns:** invite email copy, deep links, resend UX  
**References:** create via Admin API; accept via public token endpoint  
**Never:** store raw tokens in product DBs; never SQL into `access.invitation`.

### Future Subscription / seat policy

**Owns:** commercial entitlement (plan/seat)  
**References:** may *gate* invite create later via its own port — **not** by embedding seats on Invitation  
**Never:** put `PlanId` / seat counts on Invitation without a new ADR.

### Vertical packs (Dental / Retail / Hospital)

**Owns:** presentation / copy local  
**References:** `InvitationId`; never parallel invite roots in the Core.

---

## HTTP vs internal consumption

| Consumer type | Integration |
|---------------|-------------|
| **Another backend module** | `InvitationReferencePort` in `access-contract` |
| **Frontend / mobile (admin)** | `/api/v1/access/invitations` (+ revoke) |
| **Invitee (public)** | `POST /api/v1/access/invitations/accept` |
| **Password recovery** | IAM `/api/v1/auth/forgot-password` · `/reset-password` — **not** Access |

Invitation Administration API is for **tenant admins**; accept is for **invitees**.

---

## OpenAPI

Grupo springdoc: **`access-administration`**

```text
GET /v3/api-docs/access-administration
```

Paths: `/api/v1/access/invitations` (+ `/{id}`, `/{id}/revoke`, `/accept`).

---

## Testing consumers

- Mock `InvitationReferencePort` in unit / module tests
- Do **not** load full Access infrastructure unless E2E
- Testcontainers `access` schema only in cross-BC ITs

---

## Checklist before merging a consumer of Invitation

| # | Item |
|---|------|
| 1 | Gradle depends only on `access-contract` |
| 2 | Aggregates store `InvitationId`, not Invitation entity |
| 3 | No SQL against `access.invitation` outside Access module |
| 4 | Write-time PENDING check via `InvitationReferencePort` when needed |
| 5 | Tenant filter on every admin query (ADR-003) |
| 6 | Non-PENDING blocks **new** dependent links |
| 7 | ADR-019 consulted — do not grow Invitation for seats/StaffAssignment/Membership ownership |

---

## Related documents

- [ADR-019 — Invitation Domain Model](ADR-019-INVITATION-DOMAIN-MODEL.md) — **frozen**
- [ADR-013 — Bounded Context Reference Contracts](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [ADR-009 — Production Readiness Backlog](ADR-009-PRODUCTION-READINESS-BACKLOG.md) — Password Recovery **Done**
- [PASO-23.8-ACCESS-CLOSEOUT.md](../audits/PASO-23.8-ACCESS-CLOSEOUT.md)
- [PASO-23.7-INVITATION-VERIFICATION.md](../audits/PASO-23.7-INVITATION-VERIFICATION.md)
