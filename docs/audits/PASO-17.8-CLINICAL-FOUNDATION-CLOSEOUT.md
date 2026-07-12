# PASO 17.8 — Clinical Foundation Closeout

**Veredicto:** **FASE 17 — Clinical Foundation: ✅ CERRADA**

Patient queda como BC estable del Core Platform: registry pequeño, API admin, ReferencePort listo para Scheduling.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Dependencias:** [PASO-17.7](PASO-17.7-PATIENT-VERIFICATION.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Cierre formal de **FASE 17 — Clinical Foundation**. Sin nuevas capacidades de negocio; superficie de consumo y documentación para FASE 18+.

---

## Entregables

| Área | Detalle |
|------|---------|
| OpenAPI | `PatientOpenApiConfiguration` — grupo `clinical-administration` |
| Endpoint docs | `GET /v3/api-docs/clinical-administration` |
| Paths | `/api/v1/clinical/patients` (+ `/{id}`, archive, activate) |
| ReferencePort | `PatientReferencePort` + `R2dbcPatientReferenceAdapter` (ADR-013) |
| Guía | [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md) |
| Verificación port | `PatientReferencePortIT` + contract test |
| ROADMAP | FASE 17 → **✅ Cerrada** · siguiente **FASE 18 Scheduling** |

---

## Superficie entregada (FASE 17)

| Capa | Entregable |
|------|------------|
| Dominio | Aggregate `Patient` + VOs (ADR-012 frozen) |
| Persistencia | Schema `clinical` · V18 · R2DBC |
| Auth | `patient:create\|read\|update\|archive` · V19 |
| HTTP | Soft admin API espejo Organization |
| Contract | `PatientId` · `PatientPermissionCatalog` · `PatientReferencePort` |
| Tests | Domain · use case · persistence IT · admin IT · verification 8/8 · reference port IT |

**Permisos:** 4 · **Migraciones:** V18–V19 · **ADRs:** 012 (frozen), 013 (Accepted)

---

## Documentación de fase

| Documento | Propósito |
|-----------|-----------|
| [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) | Modelo Patient — **congelado** |
| [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) | Reference Contracts |
| [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md) | Guía consumidores |
| [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md) | Patient como consumer de Org |
| PASO-17.0 … PASO-17.7 | Trazabilidad implementación |

---

## Criterio de cierre FASE 17 (PASO 17.0 §6)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Patient según ADR-012 | ✅ |
| 2 | Org solo por IDs + Reference Ports | ✅ |
| 3 | Verification E2E verde | ✅ 17.7 |
| 4 | ROADMAP FASE 17 ✅ · siguiente Scheduling | ✅ |
| 5 | Ningún aggregate Org modificado | ✅ |

---

## Explicitamente fuera (post-17)

Appointment · Encounter · MedicalRecord · Billing · Merge · Invitations · vertical packs · eventos cross-BC · org-scoped RBAC

---

## Próximo

**FASE 18 — Scheduling (`Appointment`)** — consume `PatientId` + `PatientReferencePort` + `StaffAssignmentId` / Org ports **sin reabrir FASE 17**.

Los módulos de negocio no modifican Patient; crecen **alrededor** vía ReferencePorts.

---

## Veredicto

**FASE 17 — Clinical Foundation: ✅ CERRADA**

Identidad clínica registral entregada, verificada E2E, documentada y publicada para consumo a largo plazo. CodeCore queda fortalecido como Core Platform multi-vertical.

---

## Referencias

- [PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md](PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md) — patrón de cierre  
- [PASO-17.7-PATIENT-VERIFICATION.md](PASO-17.7-PATIENT-VERIFICATION.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
