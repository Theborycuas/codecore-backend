# ADR-009 — Production Readiness Backlog

**Status:** Accepted  
**Date:** 2026-06-17  
**Deciders:** CodeCore architecture (post FASE 15)  
**Relates to:** PASO-15.9.1, ADR-006, ADR-007, ADR-008, ROADMAP FASE 16+

---

## Context

PASO-15.9.1 audited IAM after FASE 15 closure. Verdict: **READY WITH MAJOR DEBT**. FASE 16 (Organizations) is **not blocked**, but several production gaps must be tracked explicitly so they are not lost during Organizations work.

This ADR registers deferred production debt with priority tiers. It does **not** redesign IAM, RBAC, Identity, or Membership.

---

## Decision

Adopt a formal backlog with three priority tiers. Items closed in PASO 15.9.2–15.9.4 are marked **Done** below.

---

## P0 — Greenfield / platform bootstrap

| Item | Status | Owner step | Notes |
|------|--------|------------|-------|
| **Bootstrap Strategy** — first tenant + OWNER without manual DB | **Done (15.9.2)** | PASO-15.9.2 | `ApplicationRunner` + env-gated `codecore.platform.bootstrap.*` |

---

## P1 — Production UX / tenant lifecycle

| Item | Status | Owner step | Notes |
|------|--------|------------|-------|
| **Tenant Status Enforcement** — ACTIVE / SUSPENDED / DISABLED semantics at runtime | **Done (15.9.3)** | PASO-15.9.3 | Login + AuthorizationContext; reactivation path for SUSPENDED |
| **Identity Disable Semantics** — tenant offboarding vs global disable | **Done (15.9.4)** | PASO-15.9.4 | `DELETE /users` = membership deactivate; global via `PUT` status |
| **Password Recovery** | Deferred | FASE 21+ or pre-pilot | Ports exist; no HTTP/DB implementation |

---

## P2 — Compliance, DX, hardening (deferred)

| Item | Status | Target phase | Notes |
|------|--------|--------------|-------|
| **Audit Trail** — admin mutations, IAM lifecycle events | Deferred | FASE 21 | No structured audit log today |
| **JWT stale token mitigation** — revalidate identity status on each request | Deferred | FASE 22 | Tokens valid until expiry after disable |
| **OpenAPI improvements** — `@ApiResponse`, examples, auth group | Deferred | FASE 22 | Admin group operational; auth/bootstrap undocumented |
| **Logging / Observability** — structured logs, metrics, tracing | Deferred | FASE 21 | Transversal |

---

## Non-goals (this ADR)

- Redesign RBAC (ADR-007)
- Change Identity Global + Membership model (ADR-006)
- New IAM admin endpoints beyond 15.9.2–15.9.4 scope
- Block FASE 16 on P2 items

---

## Consequences

- FASE 16 may start when 15.9.2–15.9.4 are closed and IAM is marked **FOUNDATION COMPLETE**.
- P1 Password Recovery and all P2 items remain tracked here until explicitly scheduled.
- New production gaps discovered during FASE 16+ should be appended to this ADR or a successor backlog ADR — not silently ignored.

---

## References

- [PASO-15.9.1-IAM-PRODUCTION-READINESS-AUDIT.md](../audits/PASO-15.9.1-IAM-PRODUCTION-READINESS-AUDIT.md)
- [PASO-15.9.2-BOOTSTRAP-STRATEGY.md](../audits/PASO-15.9.2-BOOTSTRAP-STRATEGY.md)
- [PASO-15.9.3-TENANT-STATUS-ENFORCEMENT.md](../audits/PASO-15.9.3-TENANT-STATUS-ENFORCEMENT.md)
- [PASO-15.9.4-IDENTITY-DISABLE-SEMANTICS.md](../audits/PASO-15.9.4-IDENTITY-DISABLE-SEMANTICS.md)
