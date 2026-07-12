# PASO 18.2 — Scheduling Reference Ports

**Organization contract** completa la familia ReferencePort que Scheduling (y cualquier BC futuro) necesita — sin reabrir aggregates congelados.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Contract + adapters R2DBC (ADR-013)  
**Dependencias:** [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [PASO-18.1](PASO-18.1-APPOINTMENT-MODEL-CONTRACT.md)

---

## Objetivo

Completar la infraestructura de Reference Contracts de Organization requerida por Appointment **y** por Inventory / otros consumidores futuros.

**No** se modificó Appointment, Patient, Organization/Office/StaffAssignment aggregates, ni ADRs.

---

## Entregables

| Artefacto | Detalle |
|-----------|---------|
| `OfficeReferencePort` | `existsActiveInOrganization(officeId, organizationId, tenantId)` |
| `R2dbcOfficeReferenceAdapter` | SQL `org.office` — ACTIVE + tenant + org |
| `StaffAssignmentReferencePort` | `findScopeByIdAndTenant` → `Optional<StaffAssignmentReferenceView>` |
| `StaffAssignmentReferenceView` | Solo `staffAssignmentId` · `organizationId` · `officeId?` |
| `R2dbcStaffAssignmentReferenceAdapter` | SQL `org.staff_assignment` |
| Contract tests | `OfficeReferencePortContractTest` · `StaffAssignmentReferencePortContractTest` |
| ITs (Testcontainers) | `OfficeReferencePortIT` · `StaffAssignmentReferencePortIT` |

---

## Superficie (ADR-013)

### OfficeReferencePort

Pregunta única: ¿existe office ACTIVE en este tenant **y** pertenece a esta organization?

| Caso | Resultado |
|------|-----------|
| ACTIVE + org + tenant OK | `true` |
| ARCHIVED | `false` |
| Wrong tenant | `false` |
| Organization mismatch | `false` |
| Unknown id | `false` |

### StaffAssignmentReferencePort

Pregunta: ¿existe el assignment en el tenant? Si sí, ¿cuál es su scope mínimo?

| Caso | Resultado |
|------|-----------|
| Org-wide assignment | View con `officeId` empty |
| Office-bound assignment | View con `officeId` presente |
| Wrong tenant / unknown / deleted | `Optional.empty()` |

**No** hay status ACTIVE en StaffAssignment (ADR-011: physical delete = revoke).

### Coherencia Appointment (application — futuro 18.6)

Validación **en application**, nunca en domain Appointment, nunca SQL cross-BC:

1. `appointment.organizationId` == view.`organizationId`  
2. Si view tiene office → `appointment.officeId` == ese office  
3. Si view es org-wide → office opcional validado con `OfficeReferencePort`

---

## Gradle

```text
organization-contract          ← ports + StaffAssignmentReferenceView
        ▲
organization-infrastructure    ← R2DBC adapters (@Component)
        ▲
consumer-application           ← depende solo de *-contract
```

Sin dependencias consumer → provider domain/application/infrastructure.

---

## Tests

```bash
./gradlew :modules:organization-management:organization-contract:test \
  :modules:organization-management:organization-infrastructure:test \
  --tests "com.codecore.organization.contract.reference.*" \
  --tests "com.codecore.organization.infrastructure.adapters.*ReferencePortIT"
```

Contract + IT ReferencePorts: **verdes**.

---

## Familia Organization ReferencePorts (estado)

| Port | Contract | Adapter |
|------|----------|---------|
| `OrganizationReferencePort` | ✅ (17.2) | ✅ |
| `OfficeReferencePort` | ✅ | ✅ **18.2** |
| `StaffAssignmentReferencePort` | ✅ **18.2** | ✅ **18.2** |

---

## Fuera de alcance

Appointment domain/HTTP · mutaciones en ports · DTOs admin · Availability engine · nuevos ADR

---

## Siguiente paso

**PASO 18.3 — Appointment Domain Foundation** — ✅ [PASO-18.3](PASO-18.3-APPOINTMENT-DOMAIN-FOUNDATION.md). Siguiente: **18.4 Persistence**.

---

## Referencias

- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [PASO-17.2-REFERENCE-CONTRACTS.md](PASO-17.2-REFERENCE-CONTRACTS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
