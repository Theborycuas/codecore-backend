# PASO 23.8 — Access Closeout (Invitation slice)

**Veredicto:** **FASE 23 — Access (Invitation): ✅ CERRADA**

Invitation queda como BC Access estable del Core Platform: intención de Membership *intentionally small*, API admin + accept público, ReferencePort listo — **sin** Subscription, StaffAssignment, Notification God BC ni reabrir IAM foundation.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-23.7](PASO-23.7-INVITATION-VERIFICATION.md) · [ADR-019](../architecture/ADR-019-INVITATION-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Cierre formal del **slice Invitation** dentro de FASE 23 Platform Services (umbrella). Sin nuevas capacidades de negocio; superficie de consumo y documentación.

---

## Entregables

| Área | Detalle |
|------|---------|
| OpenAPI | `AccessOpenApiConfiguration` — grupo `access-administration` |
| Endpoint docs | `GET /v3/api-docs/access-administration` |
| Paths | `/api/v1/access/invitations` (+ `/{id}`, `/{id}/revoke`, `/accept`) |
| ReferencePort | `InvitationReferencePort.existsPendingByIdAndTenant` + `R2dbcInvitationReferenceAdapter` (ADR-013) |
| Guía | [ACCESS-CONSUMPTION-GUIDE.md](../architecture/ACCESS-CONSUMPTION-GUIDE.md) |
| Verificación port | `InvitationReferencePortIT` + `InvitationReferencePortContractTest` |
| ROADMAP | Access/Invitation → **✅ Cerrada** · Password Recovery **Done** · Subscription **después** |
| Architecture Review | [CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md) — veredicto **A**, ~9.2/10 |

---

## Superficie entregada (slice Invitation)

| Capa | Entregable |
|------|------------|
| Dominio | Aggregate `Invitation` + VOs (ADR-019 frozen) |
| Persistencia | Schema `access` · V30 · R2DBC |
| Auth | `invitation:create\|read\|revoke` · V31 · `ALL` 52→55 |
| HTTP | Admin + accept-by-token |
| Contract | `InvitationId` · `InvitationPermissionCatalog` · `InvitationReferencePort` |
| IAM ports usados | Membership / ActiveByEmail / SystemRole / IdentityEmail + `TenantAccessProvisionPort` |
| Tests | Domain 28 · use case 10 · persistence IT 3 · verification 8/8 · reference port IT + contract |

**Permisos:** 3 · **Migraciones Access:** V30–V31 · **ADRs:** 019 (frozen), 013 (patrón)

---

## Track A — Password Recovery (ADR-009 P1) ✅ Done

Completado en FASE 23 como track IAM (no BC nuevo):

| Área | Detalle |
|------|---------|
| Aggregate | `PasswordResetRequest` (ya existía en IAM) |
| Persistencia | `V32__create_password_reset_request_table.sql` → `iam.password_reset_request` |
| Use cases | `RequestPasswordResetUseCase` · `CompletePasswordResetUseCase` |
| HTTP | `POST /api/v1/auth/forgot-password` · `POST /api/v1/auth/reset-password` |
| Email | `LoggingSendPasswordResetEmailAdapter` |
| ADR-009 | P1 Password Recovery → **Done (FASE 23)** |

---

## Documentación de fase

| Documento | Propósito |
|-----------|-----------|
| [ADR-019](../architecture/ADR-019-INVITATION-DOMAIN-MODEL.md) | Modelo Invitation — **congelado** |
| [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) | Reference Contracts |
| [ACCESS-CONSUMPTION-GUIDE.md](../architecture/ACCESS-CONSUMPTION-GUIDE.md) | Guía consumidores |
| PASO-23.0 … PASO-23.7 | Trazabilidad implementación |

---

## Criterio de cierre (PASO 23.0 §Checklist)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Invitation según ADR-019 (*intentionally small*) | ✅ |
| 2 | IAM consumido solo por contract ports (+ provision en accept) | ✅ |
| 3 | Verification E2E verde | ✅ 23.7 (8/8) |
| 4 | ROADMAP slice Invitation ✅ · Password Recovery Done · Subscription no mezclado | ✅ |
| 5 | Ningún aggregate clínico/económico modificado (salvo seeds IAM) | ✅ |
| 6 | `InvitationReferencePort` + guía publicados | ✅ |
| 7 | Ningún Subscription / StaffAssignment / Notification BC embebido | ✅ |
| 8 | Password Recovery P1 cerrado | ✅ |

---

## Explícitamente fuera (post-Invitation)

Subscription / Plan / Seat · Org-scoped invites · custom roles · Email Verification root · Notification inbox · un-revoke / re-accept · DELETE HTTP · rich email templates · event bus preventivo

---

## Próximo

**Subscription** = BC **distinto**, **después** de este closeout — otro ADR / otro root. **No** forma parte de Access.

**Stock** (continuación Inventory) puede avanzar en paralelo de producto.

Los módulos no modifican Invitation; consumen `InvitationReferencePort` cuando necesiten validar un invite **PENDING**.

---

## Veredicto

**FASE 23 — Access (Invitation slice): ✅ CERRADA** · **Password Recovery: ✅ Done**

```text
IAM → … → Payment → Access (Invitation)
 CLOSED              CLOSED   CLOSED (Access slice)
                                  ↘ Subscription (después) · Stock paralelo
```

---

## Referencias

- [PASO-22.8-PAYMENTS-CLOSEOUT.md](PASO-22.8-PAYMENTS-CLOSEOUT.md) — patrón de cierre
- [PASO-23.7-INVITATION-VERIFICATION.md](PASO-23.7-INVITATION-VERIFICATION.md)
- [ROADMAP.md](../architecture/ROADMAP.md)
