# PASO 19.4 — Encounter Persistence

**Encounter** queda persistido exactamente como ADR-015 lo congela: episodio de atención ocurrido, sin FK cross-BC, listo para todos los verticales del Core.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Persistencia R2DBC + Flyway  
**Dependencias:** [PASO-19.3](PASO-19.3-ENCOUNTER-DOMAIN-FOUNDATION.md) · [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Conectar el Aggregate `Encounter` a PostgreSQL siguiendo el patrón nativo CodeCore (Patient / Appointment). Sin HTTP, use cases, seguridad ni ReferencePorts propios.

---

## Decisiones

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Schema | `records` | BC Clinical Records (ADR-015) — no mezclar con `clinical` (Patient) |
| Tabla raíz | `records.encounter` | Un row = un Encounter |
| Child tables | Ninguna | Aggregate single-row (ADR-015 intentionally small) |
| `tenant_id` | UUID obligatorio, sin FK a IAM | ADR-003 |
| Refs Patient/Org/Office/Staff/Appointment | UUID lógicos, sin FK | ADR-011/013/015 — ReferencePorts en escritura futura |
| `ended_at` | NULL mientras `IN_PROGRESS` | ADR-015 — obligatorio al completar |
| Adapter | `R2dbcEncounterRepository` implementa `EncounterRepository` + `EncounterQueryPort` | Igual que Appointment |

### Por qué NO hay FK cross-BC

Misma filosofía que Appointment V20: IDs lógicos + ReferencePorts en application; schemas `clinical` / `scheduling` / `org` / `iam` no se acoplan.

---

## SQL (V22)

```text
records.encounter
  encounter_id PK
  tenant_id NOT NULL
  patient_id NOT NULL
  staff_assignment_id NOT NULL
  organization_id NOT NULL
  office_id NULL
  appointment_id NULL
  started_at NOT NULL
  ended_at NULL  (CHECK ended_at IS NULL OR ended_at >= started_at)
  status IN_PROGRESS|CANCELLED|COMPLETED
  created_at, updated_at
```

**Índices:** tenant · status · (tenant, status) · (tenant, patient) · (tenant, organization) · (tenant, staff) · (tenant, appointment) parcial.

**Ausente por diseño:** notes · SOAP · odontogram · diagnosis · prescription · billing · attachments · vertical fields.

---

## Infraestructura

```text
encounter-infrastructure
  configuration/EncounterModuleConfiguration
  persistence/entity/EncounterEntity
  persistence/mapper/EncounterMapper
  persistence/repository/SpringDataEncounterRepository
  persistence/repository/R2dbcEncounterRepository
```

`codecore-api` escanea `com.codecore.encounter` y depende de `encounter-infrastructure`.

---

## Tests (`R2dbcEncounterRepositoryIT`)

| Caso | Cubierto |
|------|----------|
| save + findById | ✓ |
| exists / existsByIdAndTenant | ✓ |
| countByTenant + findByTenant | ✓ |
| cross-tenant isolation | ✓ |
| office / appointment null / non-null | ✓ |
| findByOrganization | ✓ |
| findByPatient | ✓ |
| findByStaffAssignment | ✓ |
| findByAppointment | ✓ |
| findByStatus (+ endedAt on COMPLETED) | ✓ |
| update without duplicating row | ✓ |

**11/11** ITs verdes.

```bash
./gradlew :modules:encounter-management:encounter-infrastructure:test --tests "*R2dbcEncounterRepositoryIT"
```

---

## Fuera de alcance

HTTP · Controllers · OpenAPI · Use cases · Permissions · `EncounterReferencePort` · Notes · Eventos

---

## Siguiente paso

**PASO 19.5 — Encounter Authorization Contract** — ✅ [PASO-19.5](PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT.md). Siguiente: **19.5.1 Admin API Audit**.

---

## Referencias

- [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-19.3](PASO-19.3-ENCOUNTER-DOMAIN-FOUNDATION.md)  
- [PASO-18.4](PASO-18.4-APPOINTMENT-PERSISTENCE.md) (patrón espejo)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- Migración: `V22__create_encounter_table.sql`  
