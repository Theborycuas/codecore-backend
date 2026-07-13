# PASO 24.0 — Audit Platform Foundation Planning

**Audit** es el siguiente bounded context **nuevo** del Core: registra **hechos inmutables de que una acción significativa ocurrió en un Tenant** — sin SIEM, sin métricas/tracing, sin Event Bus, sin reabrir BCs 16–23.

**Fecha:** 2026-07-13  
**Estado:** ✅ Planificación cerrada  
**Tipo:** Definición de FASE 24 · Bounded Context **Audit** · primer root **`AuditEntry`**  
**Dependencias:** FASE 16–23 cerradas (veredicto A) · ADR-009 P2 Audit Trail · ADR-013 · [CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-ACCESS-ARCHITECTURE-REVIEW-2026-07.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. BC = **Audit** · primer Aggregate Root = **`AuditEntry`** (append-only)  
2. **No** mezclar Observability (metrics/tracing) — eso queda FASE 25 / hardening  
3. Publica **`AuditAppendPort`** (command) + admin read + `AuditReferencePort` en closeout  
4. Productores reales v1: **Access Invitation** (create/accept/revoke) + **Password Recovery** (request/complete) — solo application, **sin** reabrir ADR-019  
5. Siguiente: **PASO 24.0.1** — AuditEntry Aggregate Audit  

**Sin código en este paso. Sin SIEM · Event Bus · OpenTelemetry · log shipping · audit de “todo el monolito”.**

---

## 0. ¿Cuál debe ser realmente el objetivo de FASE 24?

### Candidatos desafiados

| Candidato | ¿Ahora? | Motivo |
|-----------|---------|--------|
| **Audit Trail** (ADR-009 P2) | **Sí** | Deuda explícita; horizontal; Access Review §8 lo nombra como consumidor de InvitationId; todo SaaS necesita “quién hizo qué” |
| Observability (metrics/tracing/logs) | **No en el mismo BC** | Otra disciplina (ops); mezclar = God “Audit & Observability” |
| Subscription | No | Comercial SaaS — BC propio; no es cumplimiento transversal |
| Stock | Paralelo | Continuación Inventory; no plataforma transversal |
| JWT stale / OpenAPI harden | No | FASE 25 (ADR-009) |
| Notification inbox | No | Producto / port |
| Extraer `iam-contract` Gradle | Higiene | No es un BC de negocio; puede ir en paralelo o pre-Subscription |

### Veredicto

**Objetivo FASE 24:** entregar el **Bounded Context Audit** con slice **`AuditEntry`**: el registro append-only de hechos de acción bajo un Tenant, consumible por cualquier producto SaaS y por BCs Core vía `AuditAppendPort`.

```text
IAM / Access / Billing / …  --append-->  AuditEntry (immutable fact)
                                              ↑
                                         Admin read API
```

**Regla de oro:** no reabrir ADR-012…019 ni aggregates cerrados. Wiring de productores = orquestación application (espejo seeds IAM).

---

## 1. Bounded Context

| Candidato | ¿Adoptar? | Motivo |
|-----------|-----------|--------|
| **Audit** | **Sí** | Lenguaje de plataforma: hechos de cumplimiento / accountability |
| Observability | **No como nombre de BC** | Metrics/traces ≠ audit facts |
| Platform Logging | Débil | Confunde con logs técnicos |
| Compliance | Sesgo vertical/regulación | El Core registra hechos; packs interpretan |

**Decisión:** BC = **Audit**.  
Gradle: `audit-management` · Schema: `audit` · HTTP: `/api/v1/audit/entries`

---

## 2. Primer Aggregate Root: `AuditEntry`

### One-sentence

> **AuditEntry** = el registro inmutable de que una acción significativa ocurrió en un Tenant.

| ¿Es? | |
|------|--|
| ¿El hecho “pasó X”? | **Sí** |
| ¿Un log técnico / span / métrica? | **No** — Observability |
| ¿Un Domain Event bus? | **No** — append síncrono vía port |
| ¿La mutación de negocio en sí? | **No** — vive en su BC; Audit solo registra |

### Por qué no otros roots

| Candidato | ¿v1? | Motivo |
|-----------|------|--------|
| `AuditEntry` / `AuditRecord` | **Sí** | Unidad atómica append-only |
| `AuditTrail` (colección) | No | No es aggregate; es query |
| `AuditPolicy` / retention rules | No | Config posterior |
| `SecurityAlert` | No | SIEM / detection |

Detalle irreversible → **24.0.1**.

---

## 3. Qué consume (ADR-013)

| Ref | ¿v1? | Validación |
|-----|------|------------|
| `TenantId` | **Obligatorio** | JWT / contexto del productor |
| `actorMembershipId?` | **Opcional** | Si presente → `IamMembershipReferencePort.existsActiveByIdAndTenant` **solo en append** (actor debe ser real); acciones de sistema pueden omitirlo |
| `resourceType` + `resourceId` | **Opacos** | **No** validar existencia del recurso (el hecho puede referir algo ya VOIDED/EXPIRED) |
| Invoice/Patient/… ports | **No** | Audit no navega dominio ajeno |

| Prohibido |
|-----------|
| Repos SQL cross-BC |
| Embed de aggregates |
| Event Bus “por si acaso” |

---

## 4. Qué publica

| Artefacto | Cuándo | Para quién |
|-----------|--------|------------|
| `AuditEntryId` | domain/contract | Correlation / admin |
| **`AuditAppendPort`** | desde 24.3+ / contract | **Productores**: Access, IAM, futuros BCs |
| `AuditPermissionCatalog` | 24.5 | `audit:read` (+ `audit:append` solo si hay HTTP append admin — preferir port interno) |
| Admin API | 24.6 | `GET` list/get — **sin** update/delete |
| `AuditReferencePort` | closeout 24.8 | boolean `existsByIdAndTenant` |
| Guía | closeout | Cómo append desde un BC |

**API pública anónima:** **No.**  
**HTTP append genérico:** opcional/no preferido en v1 — productores usan port (evita API de escritura abierta).

---

## 5. Qué NO pertenece a FASE 24

| Fuera | Por qué |
|-------|---------|
| Metrics / Prometheus / tracing / OpenTelemetry | Observability — FASE 25+ |
| Log aggregation / ELK | Ops |
| SIEM / anomaly detection / alerts | Producto seguridad |
| Event Bus / outbox genérico | No requerido por invariante |
| Reescribir todos los BCs 16–22 para auditar todo | Sobreingeniería; productores **justificados** v1 |
| Retention/purge engine | Diferido |
| PII payload dumps / request bodies | Privacidad; actionCode + resource ids bastan |
| Modificar ADR-012…019 | Congelados |
| Subscription / Stock | Otras fases |

---

## 6. ¿ADR nuevo?

**Sí — ADR-020 Audit Entry Domain Model.**  
Nuevo Aggregate Root + BC + permanence (*intentionally small* / append-only) = trigger DEVELOPMENT-POLICY.

---

## 7. Productores reales v1 (anti “por si acaso”)

| Productor | Acciones | ¿Reabre ADR? |
|-----------|----------|--------------|
| **Access** `InvitationAdministrationUseCaseImpl` | `invitation.created` · `invitation.accepted` · `invitation.revoked` | **No** — solo application outbound |
| **IAM** Password Recovery use cases | `password_reset.requested` · `password_reset.completed` | **No** — application IAM |

Verification E2E: crear Invitation → ver `AuditEntry` en admin list.

---

## 8. Roadmap completo (espejo 16–23)

| Paso | Nombre |
|------|--------|
| **24.0** | Audit Foundation Planning (este documento) |
| **24.0.1** | AuditEntry Aggregate Audit |
| **24.1** | ADR-020 Accepted |
| **24.2** | Audit Reference Readiness (IAM membership port si actor) |
| **24.3** | Audit Domain Foundation |
| **24.4** | Audit Persistence (V33) |
| **24.5** | Audit Authorization (`audit:read` + V34) |
| **24.5.1** | Audit Admin API Audit |
| **24.6** | Audit Administration API + `AuditAppendPort` |
| **24.6.1** | Wire producers Access + Password Recovery |
| **24.7** | Audit Verification |
| **24.8** | Audit Closeout + guía + ReferencePort |
| **24.review** | Architecture Review independiente |

---

## 9. Cómo fortalece CodeCore (el corazón)

| Señal | Aporte |
|-------|--------|
| Accountability multi-tenant | Todo producto SaaS hereda “quién hizo qué” sin reinventar |
| Cierra ADR-009 P2 Audit Trail | Deuda de producción explícita |
| Consume hechos de Access sin engordar Invitation | Demuestra crecimiento **alrededor** de BCs cerrados |
| Contrato `AuditAppendPort` | Nuevos BCs (Subscription, Stock) auditan desde el día 1 |
| Separación Audit ≠ Observability | Evita God BC ops+compliance |

**No** es un módulo dental de historial clínico. **No** es un SIEM hospitalario.  
Es el **núcleo de hechos de acción** del Core Platform.

---

## Checklist

- [x] Objetivo = Audit Trail (≠ Observability)  
- [x] BC = Audit · Root = AuditEntry  
- [x] AppendPort + admin read · productores reales Access + PasswordReset  
- [x] ADR-020 previsto · plan 24.0 → 24.8  
- [x] Sin reabrir ADRs 012–019  

## Siguiente

**PASO 24.0.1 — AuditEntry Aggregate Audit**.
