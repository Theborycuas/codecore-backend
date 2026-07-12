# PASO 18.4 — Appointment Persistence

**Appointment** queda persistido exactamente como ADR-014 lo congela: compromiso planificado de cuidado, sin FK cross-BC, listo para todos los verticales del Core.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Persistencia R2DBC + Flyway  
**Dependencias:** [PASO-18.3](PASO-18.3-APPOINTMENT-DOMAIN-FOUNDATION.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Conectar el Aggregate `Appointment` a PostgreSQL siguiendo el patrón nativo CodeCore (Organization / Office / Patient). Sin HTTP, use cases, seguridad ni ReferencePorts propios.

---

## Decisiones

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Schema | `scheduling` | BC Scheduling (ADR-014) |
| Tabla raíz | `scheduling.appointment` | Un row = un Appointment |
| Child tables | Ninguna | Aggregate single-row (ADR-014 intentionally small) |
| `tenant_id` | UUID obligatorio, sin FK a IAM | ADR-003 · mismo patrón Org/Patient |
| Refs Patient/Org/Office/StaffAssignment | UUID lógicos, sin FK | ADR-011/013/014 — ReferencePorts en escritura futura |
| Adapter | `R2dbcAppointmentRepository` implementa `AppointmentRepository` + `AppointmentQueryPort` | Igual que Patient / Organization |

### Por qué NO hay FK cross-BC

| Referencia | FK física | Motivo |
|------------|-----------|--------|
| `tenant_id` → IAM | ❌ | Desacopla lifecycle de schemas; aislamiento por ID (ADR-003 · ADR-014). Tenant permanece denormalizado en la fila para queries locales y aislamiento SaaS sin joins cross-BC. |
| `patient_id` → Clinical | ❌ | Patient está cerrado. Scheduling no posee ni acopla el schema `clinical`. ACTIVE+tenant se valida vía `PatientReferencePort` (ADR-013 / ADR-014 §10). |
| `organization_id` → Org | ❌ | Organization está cerrado. `OrganizationId` denormalizado es parte del compromiso (ADR-014 §7); ACTIVE vía `OrganizationReferencePort`, no FK. |
| `staff_assignment_id` → Org | ❌ | StaffAssignment es referencia lógica de *quién opera*. Coherencia org/office se valida con `StaffAssignmentReferencePort` en application (ADR-014 §7), nunca con FK física. |
| `office_id` → Org | ❌ | Office opcional; misma filosofía Reference Contract (ADR-013). |
| FK internas del BC | — | No existen tablas hijas; no hace falta ninguna FK intra-schema. |

Citas normativas: **ADR-013** (Reference Contracts — IDs + ports, never foreign repos/SQL) · **ADR-014** §§7–10 (refs IDs only, denormalized `OrganizationId`, ports).

---

## SQL (V20)

```text
scheduling.appointment
  appointment_id PK
  tenant_id NOT NULL
  patient_id NOT NULL
  staff_assignment_id NOT NULL
  organization_id NOT NULL
  office_id NULL
  starts_at, ends_at  (CHECK ends_at > starts_at)
  status SCHEDULED|CANCELLED|COMPLETED
  created_at, updated_at
```

**Índices:** tenant · status · (tenant, status) · (tenant, patient) · (tenant, organization) · (tenant, staff_assignment).

**Ausente por diseño:** notes · SOAP · odontogram · encounter · treatment · billing · slot · availability · calendar · recurrence · waitlist · vertical fields.

---

## Infraestructura

```text
appointment-infrastructure
  configuration/AppointmentModuleConfiguration
  persistence/entity/AppointmentEntity
  persistence/mapper/AppointmentMapper
  persistence/repository/SpringDataAppointmentRepository
  persistence/repository/R2dbcAppointmentRepository
```

`codecore-api` escanea `com.codecore.appointment` y depende de `appointment-infrastructure`.

### Queries mínimas (puertos)

| Método | Uso |
|--------|-----|
| `findById` / `findByIdAndTenantId` | Carga |
| `existsById` / `existsByIdAndTenantId` | Existencia + aislamiento |
| `findByTenantId` / `countByTenantId` | Listado tenant |
| `findByTenantIdAndStatus` | Filtro lifecycle |
| `findByTenantIdAndOrganizationId` | Contexto negocio |
| `findByTenantIdAndPatientId` | Sujeto de cuidado |
| `findByTenantIdAndStaffAssignmentId` | Quién opera |

Sin calendario, disponibilidad ni filtros avanzados.

---

## Tests (`R2dbcAppointmentRepositoryIT`)

| Caso | Cubierto |
|------|----------|
| save + findById | ✓ |
| exists / existsByIdAndTenant | ✓ |
| countByTenant + findByTenant | ✓ |
| cross-tenant isolation | ✓ |
| office_id null / non-null | ✓ |
| findByOrganization | ✓ |
| findByPatient | ✓ |
| findByStaffAssignment | ✓ |
| findByStatus | ✓ |
| update without duplicating row | ✓ |

**10/10** ITs verdes.

```bash
./gradlew :modules:appointment-management:appointment-infrastructure:test
```

Requiere Docker Desktop (Testcontainers), igual que Patient / Organization ITs.

---

## Validación arquitectónica

| Check | Resultado |
|-------|-----------|
| ADR-014 | ✅ solo columnas del contrato congelado |
| ADR-013 | ✅ sin repos/FK a Patient/Org; ReferencePorts siguen siendo el camino de validación |
| ADR-003 | ✅ tenant obligatorio en fila e índices |
| Sin FK cross-BC | ✅ |
| Sin acoplamiento Patient | ✅ |
| Sin acoplamiento Organization | ✅ |
| Intentionally small | ✅ |
| Reutilizable por Dental/Vet/Hospital/Lab/… | ✅ — planned commitment only |

---

## Fuera de alcance

HTTP · Controllers · OpenAPI · Use cases · Permissions · `AppointmentReferencePort` · Availability · Double booking · Recurrence · Waitlist · Notifications · Eventos

---

## Siguiente paso

**PASO 18.5 — Appointment Authorization Contract** — seeds `appointment:*`.

---

## Referencias

- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-18.3](PASO-18.3-APPOINTMENT-DOMAIN-FOUNDATION.md)  
- [PASO-17.4](PASO-17.4-PATIENT-PERSISTENCE.md) (patrón espejo)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- Migración: `V20__create_appointment_table.sql`  
