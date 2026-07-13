# PASO 23.5.1 — Invitation Admin / Public API Audit

**Obligatoria** (espejo Payment/Invoice Admin API Audit) — decisiones HTTP del slice Invitation (documentadas post-implementación alineadas al código 23.6; el contrato queda congelado aquí).

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-23.5](PASO-23.5-INVITATION-AUTHORIZATION-CONTRACT.md) · [ADR-019](../architecture/ADR-019-INVITATION-DOMAIN-MODEL.md) · [PASO-23.0.1](PASO-23.0.1-INVITATION-AGGREGATE-AUDIT.md)

---

## Decisiones HTTP

| Decisión | Valor |
|----------|-------|
| Base path admin | `/api/v1/access/invitations` |
| Endpoints admin | `GET /` (list) · `GET /{id}` · `POST /` (create) · `POST /{id}/revoke` |
| Accept público | `POST /api/v1/access/invitations/accept` — **permitAll** (secreto = raw token) |
| Sin | `PUT` (contenido inmutable) · `DELETE` físico · `POST /{id}/resend` (nueva Invitation) |
| Default list `status` | `PENDING` (cola de trabajo vigente) |
| Filtros list | `status=PENDING\|ACCEPTED\|REVOKED\|EXPIRED\|ALL` |
| `tenantId` en body | **Nunca** — JWT / TenantContext en admin; accept no usa tenant del cliente como autoridad |
| Create body | `email` · `roleCode` (≠ OWNER) — sin id/status/token |
| Create response | Invitation + **raw token una sola vez** (`InvitationCreatedResponse`) |
| Accept body | `token` · `password?` (obligatorio si Identity nueva) |
| Exceptions → HTTP | NotFound→404 · InvalidState/Domain→400 · Authz→403 · provision conflict→409 |
| OpenAPI group | `access-administration` |

## Password Recovery (track IAM — paths relacionados)

| Path | Auth |
|------|------|
| `POST /api/v1/auth/forgot-password` | permitAll · anti-enumeración → **204** |
| `POST /api/v1/auth/reset-password` | permitAll |

No forman parte del BC Access; documentados aquí solo por cercanía de FASE 23.

## Por qué default `status=PENDING`

Los administradores buscan invites **abiertas** (aún no aceptadas/revocadas/expiradas) — espejo de cola de trabajo Invoice `DRAFT` / Payment `RECORDED`.

## Por qué accept no usa `invitation:accept` RBAC

El aceptante aún no es Membership del tenant. La capacidad es **posesión del raw token**, no un permiso de rol (ADR-019 §7).

## Siguiente

**PASO 23.6 — Invitation Administration API** (implementación alineada a este contrato).
