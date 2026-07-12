# PASO 19.1 — Encounter Model Contract (ADR-015)

**Encounter** queda congelado: episodio de atención ocurrido, *intentionally small*, listo para que FASE 19 implemente sin reabrir el dominio.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Aceptación ADR (sin código)  
**Dependencias:** [PASO-19.0.1](PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md) · [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md)

---

## Entregable

| Artefacto | Estado |
|-----------|--------|
| [ADR-015 — Encounter Domain Model](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) | **Accepted** |

Sin tablas · sin HTTP · sin persistencia · sin módulos Gradle en este paso.

---

## Contrato congelado (resumen)

| Elemento | Valor |
|----------|-------|
| Definición | Care episode that occurred (or is recorded as having occurred) for a care subject at a determined operational context |
| Principio | **Encounter is intentionally small** (permanente) |
| Estados | `IN_PROGRESS` · `CANCELLED` · `COMPLETED` |
| Tiempo | `startedAt` required · `endedAt` required on complete (`≥ startedAt`) |
| Refs | `TenantId` · `PatientId` · `StaffAssignmentId` · `OrganizationId` · `OfficeId?` · `AppointmentId?` |
| Appointment link | Opcional; si presente → status SCHEDULED\|COMPLETED + mismo `patientId` |
| Ports | Patient · Organization · Office · StaffAssignment · Appointment (evolución 19.2) |
| Schema / módulo (objetivo) | `records` · `encounter-management` |

---

## Siguiente paso

**PASO 19.2 — Clinical Records Reference Readiness** — ✅ [PASO-19.2](PASO-19.2-REFERENCE-READINESS.md). Siguiente: **19.3 Domain Foundation**.

---

## Referencias

- [ADR-015-ENCOUNTER-DOMAIN-MODEL.md](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md](PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
