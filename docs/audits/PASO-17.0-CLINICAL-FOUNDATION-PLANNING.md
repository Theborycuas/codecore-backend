# PASO 17.0 — Clinical Foundation Planning

**Fecha:** 2026-07-11  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Reorganización de roadmap + definición de FASE 17  
**Dependencias:** FASE 16 cerrada · ADR-006 · ADR-007 · ADR-010 · ADR-011 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Objetivo

1. Alinear el ROADMAP con la arquitectura **real** (plataforma IAM + Organization Management cerrado).  
2. Decidir el **siguiente bounded context** tras Organization.  
3. Definir completamente **FASE 17 — Clinical Foundation** (planificación únicamente).  
4. Identificar el **primer Aggregate Root clínico** y las auditorías/ADR obligatorias.

**No hay código, tablas ni endpoints en este paso.**

---

## 1. Evaluación de Invitations (DDD estratégico)

| Pregunta | Respuesta | Justificación |
|----------|-----------|---------------|
| ¿Invitations valida Organization? | **No** | Invitation modela *acceso a tenant* (Identity/Membership), no estructura org/office. |
| ¿Invitations consume StaffAssignment? | **No** | StaffAssignment responde “dónde opera el staff”. Invitation responde “quién puede unirse al tenant”. |
| ¿Aporta información al dominio clínico? | **No** | No introduce Patient, Encounter ni ClinicalRecord. |
| ¿Puede implementarse después sin afectar Patient? | **Sí** | Patient se registra vía admin/API clínica; alta de usuarios staff sigue siendo Membership admin (FASE 15). Invitation mejora UX de onboarding, no el modelo clínico. |
| ¿Platform Service vs dominio inmediato? | **Platform Service (IAM-adjacent)** | Bounded context de **plataforma**, no de negocio clínico. Vive cerca de ADR-006 (Identity global + Membership). |

**Veredicto:** Invitations **no** es el siguiente paso lógico tras Organization. Se reubica en **FASE 22 — Platform Services** (junto con deuda ADR-009: password recovery, etc.), sin eliminar la funcionalidad.

```text
Invitations
  language: IAM / access provisioning
  upstream: Identity, Membership, Tenant
  downstream: (optional) notify → StaffAssignment after membership exists
  NOT a consumer that proves ADR-011
```

El primer consumidor que **prueba** Organization como BC estable es **Patient** (ADR-011 § Patient).

---

## 2. Roadmap por Bounded Context (propuesta adoptada)

| Fase | Bounded Context / foco | Por qué este orden |
|------|------------------------|--------------------|
| **10–15** | IAM (plataforma) | ✅ Cerrado |
| **16** | Organization Management | ✅ Cerrado — BC estable (ADR-010/011) |
| **17** | **Clinical Foundation** (`Patient`) | Primer consumer de Organization; sujeto del cuidado |
| **18** | **Scheduling** (`Appointment`) | Necesita `PatientId` + `StaffAssignmentId` |
| **19** | **Clinical Records** (`MedicalRecord`) | Necesita Patient; custodia `OrganizationId` |
| **20** | **Inventory** | Opera sobre `OfficeId`; independiente del flujo clínico diario |
| **21** | **Billing & Subscriptions** | Comercial; `OrganizationId` + seats Membership (ADR-006) |
| **22** | **Platform Services** | Invitations, password recovery, mejoras IAM (ADR-009) |
| **23** | **Audit & Observability** | Transversal (ADR-009 P2) |
| **24** | **Production Hardening** | JWT stale, OpenAPI auth group, hardening (ADR-009) |

### Cambios vs roadmap anterior

| Antes | Ahora | Rationale |
|-------|-------|-----------|
| 17 Invitations | **22 Platform Services** | No valida Organization; no es dominio clínico |
| 18 Business Module Framework | **Disuelto** | La política FASE 16+ *es* el framework; no es una fase de producto |
| 19 Dental / PetNova | **Después de 17–19** (product packs) | Verticales *componen* BCs clínicos; no los sustituyen |
| Patient “FASE 19” en docs | **FASE 17** | Consistente con ADR-011 (“FASE 17 onward: Patient…”) |
| Billing 20 | **21** | Tras núcleo clínico + inventory opcional |

**Nada se elimina** — solo se reordena por dependencias de dominio.

---

## 3. Primer Aggregate Root: `Patient`

### Por qué Patient

| Criterio | Decisión |
|----------|----------|
| ¿Por qué este? | Es el **sujeto del cuidado**. Appointment, MedicalRecord, Billing clínico y reportes cuelgan de `PatientId`. Sin Patient no hay dominio clínico. |
| ¿Por qué no Appointment? | Scheduling *referencia* Patient; no puede nacer antes. |
| ¿Por qué no MedicalRecord? | Documentación clínica *pertenece* a un Patient. |
| ¿Por qué no StaffAssignment? | Ya existe (FASE 16); es scope de **staff**, no identidad del paciente. |
| ¿Por qué no Identity? | Paciente ≠ usuario del sistema (ADR-006). Un paciente puede no tener login. |

### Bounded contexts

| Relación | BC | Cómo |
|----------|-----|------|
| **Consume** | IAM | Solo `TenantId` desde JWT (ADR-003). No carga Identity/Membership para “ser paciente”. |
| **Consume** | Organization Management | `OrganizationId` / `OfficeId` opcionales vía **Query Ports** (`organization-contract`) — ADR-011 |
| **No conoce** | Scheduling, Clinical Records, Billing, Inventory | Ellos referencian `PatientId` más adelante |
| **No conoce** | StaffAssignment | Visibilidad operativa del staff ≠ pertenencia del paciente |

### IDs que mantiene Patient

| ID | Obligatorio | Rol |
|----|-------------|-----|
| `PatientId` | Sí | Identidad del aggregate |
| `TenantId` | Sí | Aislamiento SaaS (**inmutable** — nunca cambia de tenant) |
| `PrimaryOrganizationId` | Opcional | Organización primaria de registro / agrupación — **no** ownership |
| `OfficeId` | **No** | Solo en Appointment / Encounter / … |

**Prohibido:** `MembershipId`, `IdentityId`, `StaffAssignmentId`, `OfficeId` como parte del registry Patient.

### Invariantes (cerradas en 17.0.1 + refinamiento)

1. Patient siempre pertenece a exactamente un `TenantId` — **nunca cambia de tenant**.  
2. Si hay `PrimaryOrganizationId`, debe existir, ser del mismo tenant y ACTIVE en altas/cambios.  
3. Patient **no** mantiene `OfficeId`.  
4. Patient es la **única** identidad clínica registral del sujeto dentro del Tenant.  
5. Lifecycle: create → update → archive (soft); sin delete físico en v1.  
6. Un aggregate Patient **no** garantiza consistencia de Appointment/MedicalRecord.

---

## 4. FASE 17 — Clinical Foundation (definición completa)

**Bounded context:** Clinical Foundation (módulo objetivo: `clinical-foundation` o `patient-management` — nombre final en 17.0.1).  
**Schema objetivo:** `clinical` (o `patient`) — decisión en auditoría.  
**HTTP objetivo:** `/api/v1/clinical/patients` (o `/api/v1/patient/patients`) — decisión en 17.5.1.

### Pasos

| Paso | Nombre | Objetivo | Aggregate | BC | Deps | Auditoría | ADR | Entregables |
|------|--------|----------|-----------|-----|------|-----------|-----|-------------|
| **17.0** | Clinical Foundation Planning | Este documento + ROADMAP | — | Planificación | FASE 16 | Este PASO | — | ROADMAP reorganizado |
| **17.0.1** | Patient Aggregate Audit | Checklist §6–§8 política; ownership; lifecycle; permisos; schema/módulo | `Patient` | Clinical Foundation | 17.0 | **Obligatoria** | Preparación ADR-012 | `PASO-17.0.1-*-AUDIT.md` |
| **17.1** | Patient Model ADR | Fijar modelo irreversible | `Patient` | Clinical Foundation | 17.0.1 | — | **ADR-012** | ADR accepted |
| **17.2** | Organization Reference Ports | Ports en `organization-contract` + adapters (sin reabrir aggregates Org) | — | Org (contract only) | ADR-011, 17.1 | No (contract evolution prevista) | — | `OrganizationReferencePort`, `OfficeReferencePort` (+ tests) |
| **17.3** | Patient Domain Foundation | Aggregate, VOs, invariantes, ports outbound | `Patient` | Clinical Foundation | 17.1 | — | — | Dominio + tests unitarios |
| **17.4** | Patient Persistence | Flyway + R2DBC + ITs | `Patient` | Clinical Foundation | 17.3 | — | — | Schema + repository ITs |
| **17.5** | Patient Authorization Contract | Catálogo `patient:*`, matriz roles, seed | — | Clinical + IAM seeds | 17.3 | Mínima (permisos) | — | Catalog + Flyway seed |
| **17.5.1** | Patient Admin API Audit | HTTP/DTO/paginación/archive | — | Clinical Foundation | 17.5 | **Obligatoria** (API shape) | — | Decisiones pre-HTTP |
| **17.6** | Patient Administration API | CRUD admin protegido | `Patient` | Clinical Foundation | 17.4, 17.5.1, 17.2 | — | — | Controllers + use cases + ITs |
| **17.7** | Patient Verification | E2E: journey, RBAC, tenant, org refs, OpenAPI | — | Clinical Foundation | 17.6 | — | — | `PatientVerificationIT` |
| **17.8** | Clinical Foundation Closeout | OpenAPI group, ROADMAP ✅, consumo guide update | — | Clinical Foundation | 17.7 | — | — | Closeout doc |

### Dependencias entre BC (FASE 17)

```text
IAM (TenantId, RBAC)
        │
        ▼
Organization Management ──organization-contract──► Clinical Foundation (Patient)
        │                    (Reference Ports)
        └── aggregates CLOSED (ADR-011)
```

Gradle (objetivo):

```text
clinical-*/domain
clinical-*/application  → organization-contract, (iam contract/permissions as today)
clinical-*/infrastructure → organization-contract adapters only via Org infra beans
```

**Prohibido:** depender de `organization-domain` / `organization-infrastructure` desde el módulo clínico (salvo wiring en app).

### Auditorías obligatorias identificadas

| Trigger (política §2–§3) | Paso |
|--------------------------|------|
| Nuevo Aggregate Root `Patient` | **17.0.1** |
| Nuevo módulo Gradle + BC | **17.0.1** |
| Nuevo ADR (modelo clínico) | **17.1 → ADR-012** |
| Shape Admin API | **17.5.1** |
| Contract ports Org (previsto ADR-011) | Documentar en 17.2; sin nueva ADR si no cambia ownership |

---

## 5. Fuera de alcance FASE 17

- Appointment / MedicalRecord / Billing / Inventory  
- Invitations / password recovery  
- Organization-scoped RBAC  
- Eventos de dominio cross-BC  
- Product packs Dental / PetNova  

---

## 6. Criterio de cierre FASE 17

1. `Patient` implementado según ADR-012.  
2. Consumo Organization **solo** por IDs + Reference Ports.  
3. Verification E2E verde.  
4. ROADMAP marca FASE 17 ✅; siguiente = **FASE 18 Scheduling**.  
5. Ningún aggregate de Organization modificado.

---

## Referencias

- [ROADMAP.md](../architecture/ROADMAP.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md](PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md)  
