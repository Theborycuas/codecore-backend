# ADR-019 — Invitation Domain Model

**Status:** Accepted  
**Date:** 2026-07-12  
**Accepted:** 2026-07-12 (PASO 23.1)  
**Deciders:** CodeCore architecture (FASE 23.1)  
**Relates to:** ADR-003 · ADR-006 · ADR-007 · ADR-013 · [PASO-23.0.1-INVITATION-AGGREGATE-AUDIT.md](../audits/PASO-23.0.1-INVITATION-AGGREGATE-AUDIT.md) · [PASO-23.0-PLATFORM-SERVICES-FOUNDATION-PLANNING.md](../audits/PASO-23.0-PLATFORM-SERVICES-FOUNDATION-PLANNING.md)

---

## Context

FASE 22 closed Payments. FASE 23 Platform Services (umbrella) opens **Access** so any SaaS product on CodeCore can invite users into a Tenant — **without** reopening Identity/Membership ownership, **without** StaffAssignment/Subscription inside the invite, and **without** a Platform God BC.

---

## Decision

### 1. Bounded context

**Access** — downstream of IAM via contracts; independent of Organization structure, Subscription, Billing, clinical BCs.

Gradle: `access-management` · Schema: `access` · HTTP: `/api/v1/access/invitations`

### 2. What Invitation is

> **The intention to grant Membership in a Tenant to a recipient identified by email.**

It is the **only** Core Aggregate Root owning that tenant-invite role. Vertical packs must not invent parallel “DentalInvite” roots.

**Not:** Membership · Identity · StaffAssignment · PasswordReset · Subscription/seat · email transport · Notification inbox.

### 3. Permanence — intentionally small

Invitation may contain only: identity, tenant, invited email, system role code (≠ OWNER), invited-by membership id, token hash, expiration, status `PENDING`|`ACCEPTED`|`REVOKED`|`EXPIRED`, optional resulting membership id, and intrinsic invariants.

Everything else lives elsewhere.

### 4. Lifecycle

```text
(create) → PENDING → accept → ACCEPTED
                    → revoke → REVOKED
                    → expire → EXPIRED
```

- No DRAFT; content immutable after create.  
- Accept provisions Membership via IAM **command port** (`TenantAccessProvisionPort`) — Invitation does not own Membership.  
- No physical delete; no un-revoke / re-accept in v1.  
- Resend = new Invitation.

### 5. References

| Ref | Rule |
|-----|------|
| `TenantId` | Required, immutable |
| `invitedEmail` | Required, immutable |
| `invitedRoleCode` | System role allow-list: ADMIN, MANAGER, USER, READ_ONLY — **not OWNER** |
| `invitedByMembershipId` | Required; inviter Membership in same tenant |
| `resultingMembershipId` | Set-once on ACCEPTED |

No OrganizationId / OfficeId / PlanId on Invitation v1.

### 6. Identity at create vs accept

- **create:** Identity **not** required; reject if ACTIVE membership already exists for email+tenant; at most one PENDING per email+tenant.  
- **accept:** reuse Identity by email or create Identity+Credential (password required when creating) + Membership + assign role — all inside IAM provision port.

### 7. Permissions

`invitation:create` · `invitation:read` · `invitation:revoke`  
Accept is **public by token** (not tenant RBAC).

### 8. Freeze

Changes to boundaries (Org-scoped invite, seats, custom roles, embedding Membership, PasswordReset inside Access) require a **new ADR**.

---

## Consequences

**Positive:** Multi-user SaaS onboarding without contaminating IAM foundation or Organization.  
**Deferred:** Org-scoped invites, custom roles, Subscription seat policy on invite, rich email templates.  
**Neutral:** `InvitationReferencePort` in closeout 23.8.

---

## Acceptance

**Accepted** PASO 23.1 (2026-07-12).
