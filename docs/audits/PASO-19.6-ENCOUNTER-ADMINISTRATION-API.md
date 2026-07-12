# PASO 19.6 — Encounter Administration API

**Encounter** queda expuesto como ciudadano nativo del Core: misma forma de administración que Appointment, con lifecycle `IN_PROGRESS|CANCELLED|COMPLETED` y validación multi-ReferencePort incluyendo Appointment linkable (ADR-013 · ADR-015).

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Application + HTTP + seguridad + ITs  
**Dependencias:** [PASO-19.5.1](PASO-19.5.1-ENCOUNTER-ADMINISTRATION-API-AUDIT.md) · [PASO-19.5](PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT.md) · [PASO-19.4](PASO-19.4-ENCOUNTER-PERSISTENCE.md) · [PASO-19.2](PASO-19.2-REFERENCE-READINESS.md) · [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Implementar la Encounter Administration API **exactamente** según el contrato aprobado en 19.5.1, reutilizando el patrón Appointment (PASO 18.6). Sin rediseño, sin ADR nuevo, sin notes/SOAP.

---

## Entregables

| Área | Detalle |
|------|---------|
| Use case | `EncounterAdministrationUseCaseImpl` — list, get, create, update, cancel, complete |
| HTTP | `EncounterAdminController` — `/api/v1/records/encounters` |
| DTOs | `CreateEncounterRequest` · `UpdateEncounterRequest` · `CompleteEncounterRequest` · `EncounterResponse` · `PagedEncounterResponse` |
| Query | `R2dbcEncounterAdminQueryRepository` — paginación + filtros 19.5.1 |
| Tenant | `IamTenantContextAccessor` → JWT `tenantId` (nunca HTTP) |
| Refs | Patient · Organization · Office · StaffAssignment · Appointment ReferencePorts + coherencia ADR-015 §7 |
| RBAC | `@RequiresPermission("encounter:*")` — complete usa `encounter:update` |
| Wiring | `EncounterAdministrationConfiguration` · `EncounterOpenApiConfiguration` · `EncounterModuleConfiguration` |
| OpenAPI | grupo `records-administration` |
| Excepciones | `EncounterHttpExceptionHandler` — 404 refs/cross-tenant; 409 state/coherence; 400 VO |

---

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/records/encounters` | `encounter:read` |
| GET | `/api/v1/records/encounters/{id}` | `encounter:read` |
| POST | `/api/v1/records/encounters` | `encounter:create` |
| PUT | `/api/v1/records/encounters/{id}` | `encounter:update` |
| POST | `/api/v1/records/encounters/{id}/cancel` | `encounter:cancel` |
| POST | `/api/v1/records/encounters/{id}/complete` | `encounter:update` |

**List default:** `status=IN_PROGRESS` · `page=0` · `size=20` · `sort=startedAt,desc`  
**Filtros opcionales:** `organizationId`, `patientId`, `staffAssignmentId`, `officeId`, `appointmentId`, `from`, `to`  
**Response:** ids + time bounds + status + timestamps — **sin `tenantId`**

**Create:** siempre abre `IN_PROGRESS`; `endedAt` opcional vía `assignEndedAt` tras open.  
**Complete body:** `{ endedAt? }` — resolución: body → persistido → `Instant.now()`.  
**Cancel:** sin body.

---

## Decisiones (sin novedad arquitectónica)

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Patrón | Espejo Appointment HTTP + lifecycle cancel/complete | Consistencia del Core |
| Path | `/api/v1/records/encounters` | BC Clinical Records (ADR-015) |
| Refs | ReferencePorts only (+ Appointment linkable) | ADR-013 — sin SQL cross-BC |
| Cancel/complete | Sin revalidar ports | Historial legible (19.5.1) |
| `complete` | `encounter:update` | Contrato 19.5 |
| PUT | Full replace refs + time; `officeId`/`appointmentId`/`endedAt` null limpian | Contrato 19.5.1 |

---

## Tests

| Suite | Cobertura |
|-------|-----------|
| `EncounterAdministrationUseCaseTest` | create; update; cancel/complete; endedAt resolution; appointment linkable/mismatch/missing; patient/org/staff missing; office inválida; coherencia staff↔org/office |
| `EncounterAdminControllerIT` | create/list/update/cancel/complete · walk-in + appointmentId · RBAC USER 403 · cross-tenant 404 · ReferencePorts · status=CANCELLED |
| Persistencia previa | `R2dbcEncounterRepositoryIT` (19.4) sigue verde |

Requiere Docker (Testcontainers PostgreSQL 16).

---

## Explicitamente fuera de alcance

EncounterReferencePort · Notes · SOAP · Diagnoses · Prescriptions · Attachments · Odontogram · Billing · Auto-orquestación Appointment · Notifications · Eventos · DELETE · labels enriquecidos

---

## Checklist DoD

- [x] API extremo a extremo  
- [x] Unit + WebFlux ITs verdes  
- [x] Documentación PASO-19.6  
- [x] ROADMAP actualizado  
- [x] Sin decisión arquitectónica nueva  
- [x] Encounter sigue *intentionally small* (ADR-015)  
- [x] ADR-013 respetado (sin SQL cross-BC)  
- [x] Core Platform fortalecido por consistencia multi-port  

---

## Siguiente paso

**PASO 19.7 — Encounter Verification** — E2E VerificationIT.

---

## Referencias

- [PASO-19.5.1-ENCOUNTER-ADMINISTRATION-API-AUDIT.md](PASO-19.5.1-ENCOUNTER-ADMINISTRATION-API-AUDIT.md)  
- [PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md](PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md)  
- [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
