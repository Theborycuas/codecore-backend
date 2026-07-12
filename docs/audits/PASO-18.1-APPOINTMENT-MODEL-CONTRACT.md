# PASO 18.1 — Appointment Model Contract (ADR-014)

**Appointment** queda congelado: compromiso planificado de atención, *intentionally small*, listo para que FASE 18 implemente sin reabrir el dominio.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Aceptación ADR (sin código)  
**Dependencias:** [PASO-18.0.1](PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md)

---

## Entregable

| Artefacto | Estado |
|-----------|--------|
| [ADR-014 — Appointment Domain Model](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) | **Accepted** |

Sin tablas · sin HTTP · sin persistencia · sin módulos Gradle en este paso.

---

## Contrato congelado (resumen)

| Elemento | Valor |
|----------|-------|
| Definición | Planned commitment to provide a service to a care subject at a determined time and operational context |
| Principio | **Appointment is intentionally small** (permanente) |
| Estados | `SCHEDULED` · `CANCELLED` · `COMPLETED` |
| Refs | `TenantId` · `PatientId` · `StaffAssignmentId` · `OrganizationId` · `OfficeId?` |
| Ports | Patient · Organization · Office · StaffAssignment (ADR-013) |
| Schema / módulo (objetivo) | `scheduling` · `appointment-management` |

---

## Siguiente paso

**PASO 18.2 — Scheduling Reference Ports** — adapter `OfficeReferencePort` + `StaffAssignmentReferencePort` (sin reabrir Org aggregates).

---

## Referencias

- [ADR-014-APPOINTMENT-DOMAIN-MODEL.md](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md](PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
