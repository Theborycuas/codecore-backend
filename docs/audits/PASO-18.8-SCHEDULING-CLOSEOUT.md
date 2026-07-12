# PASO 18.8 — Scheduling Closeout

**Veredicto:** **FASE 18 — Scheduling: ✅ CERRADA**

Appointment queda como BC estable del Core Platform: compromiso planificado *intentionally small*, API admin, ReferencePort listo para Encounter / Records / Billing.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-18.7](PASO-18.7-APPOINTMENT-VERIFICATION.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Cierre formal de **FASE 18 — Scheduling**. Sin nuevas capacidades de negocio; superficie de consumo y documentación para FASE 19+.

---

## Entregables

| Área | Detalle |
|------|---------|
| OpenAPI | `AppointmentOpenApiConfiguration` — grupo `scheduling-administration` |
| Endpoint docs | `GET /v3/api-docs/scheduling-administration` |
| Paths | `/api/v1/scheduling/appointments` (+ cancel, complete) |
| ReferencePort | `AppointmentReferencePort` + `R2dbcAppointmentReferenceAdapter` (ADR-013) |
| Guía | [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md) |
| Verificación port | `AppointmentReferencePortIT` + contract test |
| ROADMAP | FASE 18 → **✅ Cerrada** · siguiente **FASE 19 Clinical Records** |

---

## Superficie entregada (FASE 18)

| Capa | Entregable |
|------|------------|
| Dominio | Aggregate `Appointment` + VOs (ADR-014 frozen) |
| Persistencia | Schema `scheduling` · V20 · R2DBC |
| Auth | `appointment:create\|read\|update\|cancel` · V21 |
| HTTP | Soft admin API espejo Patient |
| Contract | `AppointmentId` · `AppointmentPermissionCatalog` · `AppointmentReferencePort` |
| Reference ports in | Patient · Org · Office · StaffAssignment (18.2 + app validation) |
| Tests | Domain · use case · persistence IT · admin IT · verification 8/8 · reference port IT |

**Permisos:** 4 · **Migraciones:** V20–V21 · **ADRs:** 014 (frozen), 013 (patrón)

---

## Documentación de fase

| Documento | Propósito |
|-----------|-----------|
| [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) | Modelo Appointment — **congelado** |
| [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) | Reference Contracts |
| [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md) | Guía consumidores |
| [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md) | Appointment como consumer de Patient |
| [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md) | Org / Office / StaffAssignment |
| PASO-18.0 … PASO-18.7 | Trazabilidad implementación |

---

## Criterio de cierre FASE 18 (PASO 18.0 §6)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Appointment según ADR-014 | ✅ |
| 2 | Patient/Org/Office/Staff solo por IDs + Reference Ports | ✅ |
| 3 | Verification E2E verde | ✅ 18.7 |
| 4 | ROADMAP FASE 18 ✅ · siguiente Clinical Records | ✅ |
| 5 | Ningún aggregate IAM / Org / Patient modificado | ✅ |

---

## Explicitamente fuera (post-18)

Encounter · MedicalRecord · Availability / Slots · Double-booking · Recurrence · Waitlist · Billing · Notifications · vertical packs · eventos cross-BC · org-scoped RBAC · DELETE HTTP

---

## Próximo

**FASE 19 — Clinical Records** — crece **alrededor** de Patient (+ opcionalmente `AppointmentId`) vía ReferencePorts **sin reabrir FASE 16/17/18**.

Los módulos de negocio no modifican Appointment; consumen `AppointmentReferencePort` cuando necesiten validar un compromiso `SCHEDULED`.

---

## Veredicto

**FASE 18 — Scheduling: ✅ CERRADA**

Compromiso planificado entregado, verificado E2E, documentado y publicado para consumo a largo plazo. CodeCore queda fortalecido como Core Platform multi-vertical: IAM → Org → Patient → **Appointment**.

---

## Referencias

- [PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md](PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md) — patrón de cierre  
- [PASO-18.7-APPOINTMENT-VERIFICATION.md](PASO-18.7-APPOINTMENT-VERIFICATION.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
