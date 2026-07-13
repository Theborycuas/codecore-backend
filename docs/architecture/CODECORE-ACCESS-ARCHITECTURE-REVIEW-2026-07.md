# CodeCore — Access Architecture Review (FASE 23 Invitation)

**Fecha:** 2026-07-12  
**Tipo:** Revisión arquitectónica post-cierre (sin modificar ADRs · sin reabrir slice · sin código de corrección)  
**Alcance:** FASE 23 — Access (`Invitation` slice) ya cerrada (PASO 23.8) + Password Recovery track Done  
**Pregunta:** ¿Access quedó como BC reutilizable del Core Platform (join-intent), o como embrión de Subscription / Notification / IAM rewrite?

**Autoridad de contraste:** [CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md) · [ADR-019](ADR-019-INVITATION-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-006](ADR-006-IDENTITY-GLOBAL-MEMBERSHIP.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md) · PASO 23.0 → 23.8

---

## Executive Summary

**Sí — Access (slice Invitation) es Core Platform, no Subscription y no un God BC “Platform”.**  
`Invitation` permanece *intentionally small*: intención de Membership por email, lifecycle `PENDING` → `ACCEPTED`|`REVOKED`|`EXPIRED`, consume IAM solo vía contract ports + `TenantAccessProvisionPort` en accept, publica `InvitationReferencePort.existsPendingByIdAndTenant`, y **no** incorpora seats, StaffAssignment, Notification inbox ni ownership de Membership.

Password Recovery (ADR-009 P1) se cerró como **track IAM** (V32 + HTTP auth) — correcto y separado.

> **Veredicto operativo: A) La siguiente fase / Subscription planning puede comenzar sin cambios en Invitation.**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| DDD | **9.5** | Aggregate Root correcto; freeze ADR-019 anti–Platform God BC |
| Hexagonal | **9** | Ports in/out claros; provision port en accept; ReferencePort publicado |
| Modular Monolith | **9** | Schema `access` · `access-management` · HTTP `/api/v1/access/invitations` |
| Core Platform | **9.5** | Multi-user SaaS onboarding sin contaminar IAM foundation |
| Desacoplamiento | **9.5** | Sin FK/SQL hacia `iam.*`; Membership owned by IAM |
| Reutilización | **9** | Cualquier vertical invita con system roles; sin “DentalInvite” |
| Consistencia | **9** | Misma disciplina closeout 16–22; accept público por token |
| Escalabilidad | **9** | Subscription crece **alrededor**; riesgo = meter seats en Invitation |
| Mantenibilidad | **9** | Superficie mínima; verification 8/8; guía de consumo |
| Visión a largo plazo | **9** | Acceso multi-usuario sin mezclar comercial SaaS |
| **Global FASE 23 Access** | **9.2** | Misma banda que Payments/Billing; stress-test IAM contracts |

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir Invitation slice? | **No** |
| ¿Modificar ADR-019? | **No** |
| ¿Puede planificarse Subscription? | **Sí — BC distinto, después** |
| ¿Deuda P0 en Access? | **Ninguna** |
| Opción | **A) Siguiente sin cambios en Invitation** |

---

## 1. Bounded Context

**Access está correctamente delimitado** como hogar del join-intent (`Invitation`) — **no** como “todo el onboarding / seats / email de la plataforma”.

| Evidencia | Lectura |
|-----------|---------|
| Schema `access` · módulo `access-management` · HTTP `/api/v1/access/invitations` | Límites claros |
| Independiente de Billing / Payments / clinical BCs / Subscription | Política + ADR-019 |
| Consume IAM solo vía contracts | Downstream correcto |
| Password Recovery en IAM (Track A) | Evita God BC Platform |

**Riesgo real (disciplina):** meter Plan/Seat, Org-scoped invites o Notification orchestration dentro de Access. Mitigación: ADR-019 § freeze + guía — Subscription = **nuevo** BC.

---

## 2. Aggregate `Invitation`

**Sigue siendo intentionally small.**

- Identidad: `InvitationId`, `TenantId` (inmutable)
- Contenido: email, roleCode ≠ OWNER, invitedByMembershipId, tokenHash, expiresAt
- Lifecycle: create → `PENDING` → accept / revoke / expire
- Sin DRAFT; sin content update; sin un-revoke / re-accept; sin DELETE físico

| Pregunta | Respuesta |
|----------|-----------|
| ¿Intentionally small? | **Sí** — alineado ADR-019 |
| ¿God Aggregate? | **No** — Membership/StaffAssignment/Subscription fuera |
| ¿Accept escribe Membership? | Solo vía `TenantAccessProvisionPort` — correcto |

---

## 3. Integración (IAM ports)

| Port | Uso | ¿OK? |
|------|-----|------|
| `IamMembershipReferencePort` | Create (inviter ACTIVE) | ✅ |
| `IamActiveMembershipByEmailPort` | Create (no duplicate ACTIVE) | ✅ |
| `IamSystemRoleReferencePort` | Create (system role) | ✅ |
| `TenantAccessProvisionPort` | Accept | ✅ |
| `InvitationReferencePort` | Closeout publish | ✅ |

Sin FK a `iam.*`. Sin repos IAM en application Access (solo contracts).

---

## 4. Reutilización multi-vertical

Definición: **intención de otorgar Membership en un Tenant a un email.**

| Vertical | ¿Sirve sin modificar Invitation? |
|----------|----------------------------------|
| Dental / Vet / Hospital / Retail / B2B | **Sí** — system roles genéricos |

Packs **no** crean roots paralelos — ADR-019 §2.

---

## 5. Consumo futuro

| Necesidad | ¿Cubierta sin reabrir Invitation? |
|-----------|-----------------------------------|
| ¿Invite PENDING? | ✅ `existsPendingByIdAndTenant` |
| Seat / plan gate | Subscription BC futuro — **no** campos en Invitation |
| Password recovery | ✅ IAM track Done (fuera de Access) |
| Stock / clinical | **No consumen** Invitation |

---

## 6. Hallazgos / deuda

### Contaminación (`access-*` main)

Subscription · StaffAssignment · Notification inbox · Membership ownership · email transport real → **Ausentes** (logging stub OK).

### P0 / P1

**Ninguna** atribuible al modelo Invitation entregado.

### P2 (higiene)

| Ítem | Acción futura (no ahora) |
|------|--------------------------|
| HTTP error bodies / thin-contract `api(domain)` | Política platform-wide |
| Email adapter real (SES/SMTP) | Sustituir logging stubs |
| Tentación seats-on-invite | Disciplina post-closeout |

**¿Resolver algo antes de Subscription planning?** **No.**

---

## 7. Roadmap — ¿qué sigue?

| Candidato | ¿Ahora? | Motivo |
|-----------|---------|--------|
| **Subscription** (BC propio) | **Después** — planning dedicado | Comercial SaaS ≠ Access |
| **Stock** (Inventory) | Paralelo válido | No reabre Access |
| Audit / Observability (FASE 24) | ADR-009 P2 | Transversal |
| Reabrir Invitation con seats | **No** | Anti-patrón explícito |

**Sí — congelar slice Invitation** y avanzar a **Subscription planning** (otro ADR) y/o Stock **sin reabrir** ADR-012…019.

---

## Aspectos destacados

1. **Invitation ≠ Membership** — IAM owns provision; Access owns join-intent.  
2. **Umbrella Platform Services no se volvió God BC** — Password Recovery track separado.  
3. **Accept público por token** — sin contaminar RBAC con `invitation:accept`.  
4. **Closeout completo** — ReferencePort + guía alineada ADR-013.  
5. **Verification viva** — V30–V31 + OpenAPI `access-administration` + 8/8.

---

## Riesgos residuales (disciplina)

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Meter Subscription/seats en Invitation | Alta si se viola | ADR-019 freeze + consumption guide |
| Reabrir IAM para “simplificar” accept | Media | Mantener `TenantAccessProvisionPort` |
| Notification God BC | Media | Outbound email port only |

---

## Referencias

- [PASO-23.8-ACCESS-CLOSEOUT.md](../audits/PASO-23.8-ACCESS-CLOSEOUT.md)
- [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md)
- [ADR-019-INVITATION-DOMAIN-MODEL.md](ADR-019-INVITATION-DOMAIN-MODEL.md)
- [ROADMAP.md](ROADMAP.md)
