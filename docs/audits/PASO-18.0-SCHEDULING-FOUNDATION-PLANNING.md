# PASO 18.0 — Scheduling Foundation Planning

**Scheduling** es el primer bounded context que **prueba de extremo a extremo** el Core: consume Patient, Organization, Office y StaffAssignment **sin reabrirlos**.

**Fecha:** 2026-07-11  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Definición de FASE 18 · Bounded Context Scheduling  
**Dependencias:** FASE 17 cerrada · ADR-011 · ADR-012 · ADR-013 · [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-ARCHITECTURE-REVIEW-2026-07.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. BC = **Scheduling** · primer Aggregate Root = **`Appointment`**  
2. Consumo **solo** IDs + ReferencePorts (Patient / Org / Office / StaffAssignment)  
3. Siguiente: **PASO 18.1** — ADR-014 ✅ Accepted · siguiente **18.2** Reference Ports

**Sin código. Sin tablas. Sin endpoints.**

---

## Objetivo

1. Declarar **FASE 18 — Scheduling** tras el cierre de Clinical Foundation.  
2. Identificar el **primer Aggregate Root** del BC y por qué no es otro.  
3. Fijar el plan de pasos (espejo FASE 16/17).  
4. Dejar explícito qué **no** es Scheduling (Encounter, Records, vertical packs, motor de disponibilidad completo).

---

## 1. Por qué Scheduling ahora

| Pregunta | Respuesta |
|----------|-----------|
| ¿Patient está listo? | ✅ FASE 17 cerrada · `PatientReferencePort` publicado |
| ¿Organization está listo? | ✅ FASE 16 cerrada · ports Org (+ Office declarado) |
| ¿Qué demuestra FASE 18? | Que el Core **se consume** sin reabrir IAM / Org / Patient |
| ¿Por qué no MedicalRecord? | Records documentan atención; Scheduling **planifica** atención |
| ¿Por qué no Inventory / Billing? | No validan el grafo clínico operativo Patient↔Staff↔Office |
| ¿Por qué no Invitations? | Platform Service (FASE 22) — no dominio de agenda |

```text
IAM (TenantId, RBAC)
        │
        ├── Organization Management (CLOSED) ──contract──►
        │         OrganizationId · OfficeId · StaffAssignmentId
        │
        └── Clinical Foundation (CLOSED) ──contract──►
                  PatientId
                         │
                         ▼
              Scheduling (FASE 18) — Appointment
```

**Regla de oro FASE 18:** no modificar aggregates ni schemas de IAM, Organization ni Patient.

---

## 2. Primer Aggregate Root: `Appointment`

### One-sentence rule

> **Appointment** = el compromiso planificado de atención en el tiempo.

| Interpretación | ¿Es? |
|----------------|------|
| ¿Una cita / booking? | **Sí** |
| ¿El episodio clínico real (Encounter)? | **No** — eso ocurre o se registra después |
| ¿Una ficha / historia clínica? | **No** — MedicalRecord (FASE 19) |
| ¿Un slot de calendario genérico / motor de disponibilidad? | **No en v1** — sobreingeniería; puede venir después |
| ¿Algo dental / vet específico? | **No** — Core Platform |

### ¿Por qué Appointment y no…?

| Candidato | ¿Root de Scheduling v1? | Motivo |
|-----------|-------------------------|--------|
| **Appointment** | **Sí** | Compromiso planificado con sujeto, proveedor, lugar y tiempo |
| `Schedule` / `Calendar` | No | Vista / proyección sobre Appointments |
| `Slot` / `Availability` | No (v1) | Inventario de capacidad — BC o aggregate futuro si hace falta |
| `Encounter` | No | Episodio de atención **ocurrido** — Clinical / Records, no Scheduling |
| `RecurringSeries` | No (v1) | Complejidad; diferido |
| `WaitlistEntry` | No (v1) | Diferido |

Detalle irreversible: [PASO-18.0.1](PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md).

---

## 3. Qué consume Appointment (solo contratos)

| Referencia | Obligatoria | Validación (ADR-013) |
|------------|-------------|----------------------|
| `TenantId` | Sí (inmutable) | JWT |
| `PatientId` | Sí | `PatientReferencePort.existsActiveByIdAndTenant` |
| `StaffAssignmentId` | Sí (proveedor) | `StaffAssignmentReferencePort` (**a publicar en Org contract**) |
| `OrganizationId` | Sí | `OrganizationReferencePort.existsActiveByIdAndTenant` |
| `OfficeId` | Opcional | `OfficeReferencePort.existsActiveInOrganization` (**adapter pendiente**) |

| Prohibido en Appointment |
|--------------------------|
| `MembershipId` / `IdentityId` como proveedor |
| Embed de Patient / Organization / Office / StaffAssignment |
| SQL a `clinical.*` / `org.*` / `iam.*` |
| Dependencia Gradle a `*-domain` / `*-infrastructure` de otros BC (salvo wiring app) |

Alineado con [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md) · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md).

---

## 4. FASE 18 — Scheduling (definición completa)

**Bounded context:** Scheduling  
**Módulo Gradle (propuesta):** `modules/appointment-management/` (domain · application · infrastructure · contract)  
**Schema SQL (propuesta):** `scheduling`  
**HTTP (cerrar en audit API):** `/api/v1/scheduling/appointments`

### Pasos

| Paso | Nombre | Objetivo | Auditoría / ADR | Entregable |
|------|--------|----------|-----------------|------------|
| **18.0** | Scheduling Foundation Planning | Este documento + ROADMAP | Este PASO | Plan FASE 18 |
| **18.0.1** | Appointment Aggregate Audit | Checklist política §6–§8 | **Obligatoria** | Modelo + prep. ADR-014 |
| **18.1** | Appointment Model ADR | Congelar modelo irreversible | **ADR-014** | ADR Accepted |
| **18.2** | Scheduling Reference Ports | Completar `OfficeReferencePort` adapter + `StaffAssignmentReferencePort` (+ tests) — **sin** reabrir aggregates Org | Contract evolution ADR-013 | Ports listos para escritura |
| **18.3** | Appointment Domain Foundation | Aggregate + VOs + tests | — | Dominio puro |
| **18.4** | Appointment Persistence | Flyway + R2DBC + ITs | — | Schema `scheduling` |
| **18.5** | Appointment Authorization Contract | `appointment:*` + seed | Mínima | Catalog + V20? |
| **18.5.1** | Appointment Admin API Audit | HTTP/DTO/paginación/lifecycle | **Obligatoria** | Contrato HTTP |
| **18.6** | Appointment Administration API | Use cases + controller + ITs | — | `/api/v1/scheduling/appointments` |
| **18.7** | Appointment Verification | E2E: refs, RBAC, tenant, OpenAPI | — | `AppointmentVerificationIT` |
| **18.8** | Scheduling Closeout | Guía consumo + ROADMAP ✅ | — | BC cerrado |

### Auditorías / ADR obligatorios

| Trigger (política) | Paso |
|--------------------|------|
| Nuevo Aggregate Root `Appointment` | **18.0.1** |
| Nuevo módulo + BC Scheduling | **18.0.1** |
| Nuevo ADR de modelo | **18.1 → ADR-014** |
| Ports Org incompletos para consumer | **18.2** (evolución de contract; **no** reabrir ADR-010) |
| Shape Admin API | **18.5.1** |

---

## 5. Fuera de alcance FASE 18

| Fuera | Por qué |
|-------|---------|
| Encounter / MedicalRecord / Odontogram / Notes | Otros BC / FASE 19+ |
| Motor completo de disponibilidad / slots / overbooking inteligente | Sobreingeniería v1 |
| Recurrencia / series / waitlist | Diferido |
| Telemedicina / sillas dentales / especies | Vertical packs |
| Eventos de dominio cross-BC | Solo si invariante eventual lo exige (review 17.9) |
| Modificar IAM / Org / Patient | BC cerrados |
| Organization-scoped RBAC | ADR-007 intacto |
| Product packs Dental / PetNova | Después del núcleo |

---

## 6. Criterio de cierre FASE 18

1. `Appointment` según ADR-014 (frozen).  
2. Consumo Patient/Org/Office/Staff **solo** por IDs + ReferencePorts.  
3. Verification E2E verde.  
4. ROADMAP FASE 18 ✅ · siguiente **FASE 19 Clinical Records**.  
5. Ningún aggregate de IAM / Organization / Patient modificado.

---

## 7. Relación con Architecture Review (17.9)

| Hallazgo 17.9 | Tratamiento en FASE 18 |
|---------------|------------------------|
| Office/Staff ports incompletos | **18.2** just-in-time — no bloquea 18.0/18.0.1 |
| Eventing vacío | No implementar bus “por si acaso” |
| Empezar FASE 18 sin cambios estructurales | ✅ Este plan |

---

## Referencias

- [PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md](PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md)  
- [PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md](PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md)  
- [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-ARCHITECTURE-REVIEW-2026-07.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md) · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
