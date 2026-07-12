# PASO 18.6 — Appointment Administration API

**Appointment** queda expuesto como ciudadano nativo del Core: misma forma de administración que Organization/Patient, con lifecycle `SCHEDULED|CANCELLED|COMPLETED` y validación multi-ReferencePort (ADR-013).

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Application + HTTP + seguridad + ITs  
**Dependencias:** [PASO-18.5.1](PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md) · [PASO-18.5](PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT.md) · [PASO-18.4](PASO-18.4-APPOINTMENT-PERSISTENCE.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Implementar la Appointment Administration API **exactamente** según el contrato aprobado en 18.5.1, reutilizando el patrón Organization / Patient. Sin rediseño, sin ADR nuevo, sin capacidades de agenda/slots.

---

## Entregables

| Área | Detalle |
|------|---------|
| Use case | `AppointmentAdministrationUseCaseImpl` — list, get, create, update, cancel, complete |
| HTTP | `AppointmentAdminController` — `/api/v1/scheduling/appointments` |
| DTOs | `CreateAppointmentRequest` · `UpdateAppointmentRequest` · `AppointmentResponse` · `PagedAppointmentResponse` |
| Query | `R2dbcAppointmentAdminQueryRepository` — paginación + filtros 18.5.1 |
| Tenant | `IamTenantContextAccessor` → JWT `tenantId` (nunca HTTP) |
| Refs | Patient · Organization · Office · StaffAssignment ReferencePorts + coherencia ADR-014 §7 |
| RBAC | `@RequiresPermission("appointment:*")` — complete usa `appointment:update` |
| Wiring | `AppointmentAdministrationConfiguration` · `AppointmentOpenApiConfiguration` · `AppointmentModuleConfiguration` |
| OpenAPI | grupo `scheduling-administration` |
| Excepciones | `AppointmentHttpExceptionHandler` — 404 refs/cross-tenant; 409 state/coherence; 400 VO |

---

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/scheduling/appointments` | `appointment:read` |
| GET | `/api/v1/scheduling/appointments/{id}` | `appointment:read` |
| POST | `/api/v1/scheduling/appointments` | `appointment:create` |
| PUT | `/api/v1/scheduling/appointments/{id}` | `appointment:update` |
| POST | `/api/v1/scheduling/appointments/{id}/cancel` | `appointment:cancel` |
| POST | `/api/v1/scheduling/appointments/{id}/complete` | `appointment:update` |

**List default:** `status=SCHEDULED` · `page=0` · `size=20` · `sort=startsAt,asc`  
**Filtros opcionales:** `organizationId`, `patientId`, `staffAssignmentId`, `officeId`, `from`, `to`  
**Response:** ids + ventana + status + timestamps — **sin `tenantId`**

---

## Decisiones (sin novedad arquitectónica)

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Patrón | Espejo Patient HTTP + lifecycle cancel/complete | Consistencia del Core |
| Path | `/api/v1/scheduling/appointments` | BC Scheduling (ADR-014) |
| Refs | ReferencePorts only | ADR-013 — sin SQL cross-BC |
| Cancel/complete | Sin revalidar ports | Historial legible (18.5.1) |
| `complete` | `appointment:update` | Contrato 18.5 |
| PUT | Full replace refs + time; `officeId=null` limpia | Contrato 18.5.1 |

---

## Tests

| Suite | Cobertura |
|-------|-----------|
| `AppointmentAdministrationUseCaseTest` | create; update; cancel/complete; patient/org/staff missing; office inválida; coherencia staff↔org/office |
| `AppointmentAdminControllerIT` | create/list/update/cancel/complete · RBAC USER 403 · cross-tenant 404 · ReferencePorts · status=CANCELLED |
| Persistencia previa | `R2dbcAppointmentRepositoryIT` (18.4) sigue verde |

Requiere Docker (Testcontainers PostgreSQL 16).

---

## Explicitamente fuera de alcance

AppointmentReferencePort · Availability · Slots · Double booking · Recurrence · Waitlist · Encounter · MedicalRecord · Billing · Notifications · Eventos · DELETE · labels enriquecidos

---

## Checklist DoD

- [x] API extremo a extremo  
- [x] Unit + WebFlux ITs verdes  
- [x] Documentación PASO-18.6  
- [x] ROADMAP actualizado  
- [x] Sin decisión arquitectónica nueva  
- [x] Appointment sigue *intentionally small* (ADR-014)  
- [x] ADR-013 respetado (sin SQL cross-BC)  
- [x] Core Platform fortalecido por consistencia multi-port  

---

## Siguiente paso

**PASO 18.7 — Appointment Verification** — ✅ [PASO-18.7](PASO-18.7-APPOINTMENT-VERIFICATION.md). Siguiente: **18.8 Closeout**.

---

## Referencias

- [PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md](PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md)  
- [PASO-17.6-PATIENT-ADMINISTRATION-API.md](PASO-17.6-PATIENT-ADMINISTRATION-API.md)  
- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
