# PASO 19.2 — Clinical Records Reference Readiness

**Appointment contract** evoluciona lo mínimo que Encounter necesita para enlazar citas — sin reabrir ADR-014 ni el aggregate Appointment.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Contract + adapter R2DBC (ADR-013 · ADR-015)  
**Dependencias:** [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-19.1](PASO-19.1-ENCOUNTER-MODEL-CONTRACT.md)

---

## Objetivo

Cubrir ADR-015 §7 / §10: si Encounter trae `AppointmentId`, validar **existencia + status linkable + patientId** vía ReferencePort — nunca SQL a `scheduling` desde Records.

**No** se modificó el aggregate Appointment, Patient, Org, ni ADR-014.

---

## Entregables

| Artefacto | Detalle |
|-----------|---------|
| `AppointmentReferenceView` | Solo `appointmentId` · `patientId` · `status` (SCHEDULED\|COMPLETED) |
| `AppointmentReferencePort.findLinkableByIdAndTenant` | → `Optional<AppointmentReferenceView>` |
| `existsScheduledByIdAndTenant` | **Conservado** (reminders / open commitment) |
| `R2dbcAppointmentReferenceAdapter` | SQL `scheduling.appointment` — linkable IN (SCHEDULED, COMPLETED) |
| Contract tests | Port + view surface + reject CANCELLED in view ctor |
| IT | Linkable SCHEDULED / COMPLETED; empty CANCELLED / unknown |

---

## Superficie (ADR-013 / ADR-015)

### existsScheduledByIdAndTenant (sin cambio semántico)

| Caso | Resultado |
|------|-----------|
| SCHEDULED + tenant OK | `true` |
| COMPLETED / CANCELLED | `false` |
| Wrong tenant / unknown | `false` |

### findLinkableByIdAndTenant (nuevo — Encounter)

| Caso | Resultado |
|------|-----------|
| SCHEDULED | View con `patientId` + status |
| COMPLETED | View con `patientId` + status |
| CANCELLED | `Optional.empty()` |
| Wrong tenant / unknown | `Optional.empty()` |

**Coherencia Encounter (application — futuro 19.6):**

1. Si `appointmentId` presente → `findLinkable…` non-empty  
2. `view.patientId` == `encounter.patientId`  
3. Nunca revalidar org/office/staff contra la cita (ADR-015)

---

## Fuera de alcance

Encounter domain/HTTP · mutaciones en ports · DTOs admin · reabrir ADR-014 · ports “por si acaso”

---

## Siguiente paso

**PASO 19.3 — Encounter Domain Foundation** — ✅ [PASO-19.3](PASO-19.3-ENCOUNTER-DOMAIN-FOUNDATION.md). Siguiente: **19.4 Persistence**.

---

## Referencias

- [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md)  
- [PASO-18.2-REFERENCE-PORTS.md](PASO-18.2-REFERENCE-PORTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
