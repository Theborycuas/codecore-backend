# PASO 18.3 — Appointment Domain Foundation

**Appointment** is the planned commitment to provide a service to a care subject at a determined time and operational context — intentionally small, frozen by ADR-014, and ready for decades of downstream BCs without growing into a God Aggregate / God Engine.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Dominio puro (sin persistencia / HTTP / use cases)  
**Dependencias:** [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-18.2](PASO-18.2-REFERENCE-PORTS.md)

---

## One-sentence rule (aggregates importantes)

| Aggregate | Frase |
|-----------|--------|
| Tenant | La frontera de aislamiento SaaS. |
| Organization | La unidad estructural del negocio. |
| Office | La ubicación física donde opera el negocio. |
| StaffAssignment | El alcance operativo de un miembro del tenant. |
| Patient | La identidad clínica registral del sujeto de cuidado. |
| **Appointment** | **El compromiso planificado de prestar un servicio a un sujeto de cuidado en un tiempo y contexto operativo determinados.** |

Si un aggregate deja de caber en una frase clara, suele estar asumiendo demasiadas responsabilidades.

---

## Objetivo

Implementar el foundation del Aggregate Root `Appointment` **exactamente** como ADR-014 Accepted — sin rediseñar, sin infraestructura.

---

## Módulo Gradle

```text
modules/appointment-management/
  appointment-domain          ← Aggregate + VOs + exceptions + tests
  appointment-application     ← solo puertos de salida (placeholder de use cases)
  appointment-contract        ← publica AppointmentId vía api(domain); sin ReferencePort aún
  appointment-infrastructure  ← placeholder
```

Registrado en `settings.gradle.kts`.

---

## Modelo implementado

```text
Appointment
  ├── AppointmentId                         (hard identity)
  ├── TenantId                              (required, immutable)
  ├── PatientId                             (required — ref Clinical Foundation)
  ├── StaffAssignmentId                     (required — who operates)
  ├── OrganizationId                        (required — denormalized commitment context)
  ├── OfficeId?                             (optional location)
  ├── AppointmentTimeWindow                 (startsAt, endsAt; endsAt > startsAt)
  └── AppointmentStatus                     SCHEDULED | CANCELLED | COMPLETED
```

**Behaviors:** `schedule` · `reschedule` · `changePatient` · `changeStaffAssignment` · `changeOrganization` · `assignOffice` · `clearOffice` · `cancel` · `complete` · `reconstitute`

**Mutaciones** solo en estado `SCHEDULED`.  
Validación ACTIVE + coherencia StaffAssignment↔org/office → application + ReferencePorts (pasos siguientes), no el aggregate.

### Explicitamente ausente (ADR-014 §3)

Encounter · MedicalRecord · Notes · SOAP · Odontogram · Treatment · Billing · Inventory · Slot / Availability Engine · Recurrence · Waitlist · Identity · Membership · vertical product rules

---

## Application / Contract / Infrastructure

| Capa | Entregable 18.3 |
|------|-----------------|
| Application | `AppointmentRepository`, `AppointmentQueryPort` — sin use cases ni servicios |
| Contract | `AppointmentContractMarker` + `api(appointment-domain)` — **sin** `AppointmentReferencePort` |
| Infrastructure | `AppointmentInfrastructurePlaceholder` |

---

## Tests

**20** tests de dominio (todos verdes):

| Suite | Cobertura |
|-------|-----------|
| `AppointmentTest` | schedule, tenant immutable, reschedule, refs, office, cancel/complete, transiciones inválidas, reconstitución, API sin concerns clínicos/capacidad/verticales |
| `AppointmentValueObjectTest` | igualdad IDs, time window (`endsAt > startsAt`), status enum congelado |

```bash
./gradlew :modules:appointment-management:appointment-domain:test
```

---

## Fuera de alcance

Persistencia · Flyway · R2DBC · HTTP · controllers · use cases de escritura · `appointment:*` permissions · `AppointmentReferencePort`

---

## Siguiente paso

**PASO 18.4 — Appointment Persistence** — schema `scheduling`, Flyway, R2DBC.

---

## Referencias

- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-18.0](PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · [PASO-18.0.1](PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md) · [PASO-18.1](PASO-18.1-APPOINTMENT-MODEL-CONTRACT.md) · [PASO-18.2](PASO-18.2-REFERENCE-PORTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
