# PASO 19.3 — Encounter Domain Foundation

**Encounter** is the care episode that occurred (or is recorded as having occurred) for a care subject at a determined operational context — intentionally small, frozen by ADR-015, and ready for decades of downstream documentation BCs without growing into an EHR God Aggregate.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Dominio puro (sin persistencia / HTTP / use cases)  
**Dependencias:** [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-19.2](PASO-19.2-REFERENCE-READINESS.md)

---

## One-sentence rule (aggregates importantes)

| Aggregate | Frase |
|-----------|--------|
| Patient | La identidad clínica registral del sujeto de cuidado. |
| Appointment | El compromiso planificado de prestar un servicio a un sujeto de cuidado en un tiempo y contexto operativo determinados. |
| **Encounter** | **El episodio de atención que ocurrió para un sujeto de cuidado en un contexto operativo determinado.** |

Si un aggregate deja de caber en una frase clara, suele estar asumiendo demasiadas responsabilidades.

---

## Objetivo

Implementar el foundation del Aggregate Root `Encounter` **exactamente** como ADR-015 Accepted — sin rediseñar, sin infraestructura real.

---

## Módulo Gradle

```text
modules/encounter-management/
  encounter-domain          ← Aggregate + VOs + exceptions + tests
  encounter-application     ← solo puertos de salida (placeholder de use cases)
  encounter-contract        ← publica EncounterId vía api(domain); sin ReferencePort aún
  encounter-infrastructure  ← placeholder
```

Registrado en `settings.gradle.kts`. **No** cableado aún en `codecore-api` (persistencia/HTTP posteriores).

---

## Modelo implementado

```text
Encounter
  ├── EncounterId                           (hard identity)
  ├── TenantId                              (required, immutable)
  ├── PatientId                             (required — ref Clinical Foundation)
  ├── StaffAssignmentId                     (required — who operated)
  ├── OrganizationId                        (required — denormalized episode context)
  ├── OfficeId?                             (optional location)
  ├── AppointmentId?                        (optional planned origin)
  ├── EncounterTimeBounds                   (startedAt; endedAt? ≥ startedAt)
  └── EncounterStatus                       IN_PROGRESS | CANCELLED | COMPLETED
```

**Behaviors:** `open` · `changeStartedAt` · `assignEndedAt` · `clearEndedAt` · `changePatient` · `changeStaffAssignment` · `changeOrganization` · `assignOffice` · `clearOffice` · `linkAppointment` · `clearAppointment` · `cancel` · `complete(endedAt)` · `reconstitute`

**Mutaciones** solo en estado `IN_PROGRESS`.  
`complete` exige `endedAt` (≥ `startedAt`; igualdad permitida).  
Validación ACTIVE + coherencia StaffAssignment + Appointment linkable → application + ReferencePorts (pasos siguientes), no el aggregate.

### Explicitamente ausente (ADR-015 §3)

Note · SOAP · MedicalRecord bag · Odontogram · Diagnosis · Prescription · Treatment · Observation · Lab · Attachment · Image · Billing · Inventory · Identity · Membership · auto-orquestación Appointment

---

## Application / Contract / Infrastructure

| Capa | Entregable 19.3 |
|------|-----------------|
| Application | `EncounterRepository`, `EncounterQueryPort` — sin use cases ni servicios |
| Contract | `EncounterContractMarker` + `api(encounter-domain)` — **sin** `EncounterReferencePort` |
| Infrastructure | `EncounterInfrastructurePlaceholder` |

---

## Tests

**22** tests de dominio (todos verdes):

| Suite | Cobertura |
|-------|-----------|
| `EncounterTest` | open, tenant immutable, time bounds, refs, appointment link, cancel/complete, transiciones inválidas, reconstitución, API sin concerns clínicos/verticales |
| `EncounterValueObjectTest` | igualdad IDs, time bounds (`endedAt >= startedAt`), status enum congelado |

```bash
./gradlew :modules:encounter-management:encounter-domain:test
```

---

## Fuera de alcance

Persistencia · Flyway · R2DBC · HTTP · controllers · use cases de escritura · `encounter:*` permissions · `EncounterReferencePort` · wiring `codecore-api`

---

## Siguiente paso

**PASO 19.4 — Encounter Persistence** — ✅ [PASO-19.4](PASO-19.4-ENCOUNTER-PERSISTENCE.md). Siguiente: **19.5 Authorization Contract**.

---

## Referencias

- [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-19.0](PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) · [PASO-19.0.1](PASO-19.0.1-ENCOUNTER-AGGREGATE-AUDIT.md) · [PASO-19.1](PASO-19.1-ENCOUNTER-MODEL-CONTRACT.md) · [PASO-19.2](PASO-19.2-REFERENCE-READINESS.md)  
- [PASO-18.3-APPOINTMENT-DOMAIN-FOUNDATION.md](PASO-18.3-APPOINTMENT-DOMAIN-FOUNDATION.md) — plantilla  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
