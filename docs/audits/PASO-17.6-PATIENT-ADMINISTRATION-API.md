# PASO 17.6 — Patient Administration API

**Patient** queda expuesto como ciudadano nativo del Core: misma forma de administración soft-entity que Organization, sin lógica de vertical clínico.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Application + HTTP + seguridad + ITs  
**Dependencias:** [PASO-17.5.1](PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md) · [PASO-17.5](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md) · [PASO-17.4](PASO-17.4-PATIENT-PERSISTENCE.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Implementar la Patient Administration API **exactamente** según el contrato aprobado en 17.5.1, reutilizando el patrón Organization / Office / StaffAssignment. Sin rediseño, sin ADR nuevo, sin capacidades clínicas posteriores.

---

## Entregables

| Área | Detalle |
|------|---------|
| Use case | `PatientAdministrationUseCaseImpl` — list, get, create, update, archive, activate |
| HTTP | `PatientAdminController` — `/api/v1/clinical/patients` |
| DTOs | `CreatePatientRequest` · `UpdatePatientRequest` · `PatientResponse` · `PagedPatientResponse` |
| Query | `R2dbcPatientAdminQueryRepository` — paginación + filtros 17.5.1 |
| Tenant | `IamTenantContextAccessor` → JWT `tenantId` (nunca HTTP) |
| Primary org | `OrganizationReferencePort.existsActiveByIdAndTenant` únicamente |
| RBAC | `@RequiresPermission("patient:*")` — activate usa `patient:update` |
| Wiring | `PatientAdministrationConfiguration` · `PatientOpenApiConfiguration` · `PatientModuleConfiguration` |
| OpenAPI | grupo `clinical-administration` |
| Excepciones | `PatientHttpExceptionHandler` — 404 cross-tenant / primary org; 409 duplicate key |

---

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/clinical/patients` | `patient:read` |
| GET | `/api/v1/clinical/patients/{id}` | `patient:read` |
| POST | `/api/v1/clinical/patients` | `patient:create` |
| PUT | `/api/v1/clinical/patients/{id}` | `patient:update` |
| POST | `/api/v1/clinical/patients/{id}/archive` | `patient:archive` |
| POST | `/api/v1/clinical/patients/{id}/activate` | `patient:update` |

**List default:** `status=ACTIVE` · `page=0` · `size=20` · `sort=createdAt,desc`  
**Filtros opcionales:** `q`, `primaryOrganizationId`, `externalIdentifierType` + `externalIdentifierValue`  
**Response:** demographics + primary org + external ids + status + timestamps — **sin `tenantId`**

---

## Decisiones (sin novedad arquitectónica)

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Patrón | Espejo Organization soft-entity | Consistencia del Core |
| Path | `/api/v1/clinical/patients` | BC Clinical Foundation |
| Primary org | ReferencePort only | ADR-013 — sin tablas `org.*` |
| Archive | Soft, sin guard de hijos | Patient no tiene offices |
| PUT | Full replace; `primaryOrganizationId=null` limpia | Contrato 17.5.1 |
| Permisos | Catálogo 17.5 (sin nuevos) | ADR-007 seeds V19 |

---

## Tests

| Suite | Cobertura |
|-------|-----------|
| `PatientAdministrationUseCaseTest` | create sin/con primary org; rechazo org inactiva |
| `PatientAdminControllerIT` | create/list · archive/activate · RBAC USER 403 · cross-tenant 404 · primary org ReferencePort · update + list ARCHIVED |
| Persistencia previa | `R2dbcPatientRepositoryIT` (17.4) sigue verde |

Requiere Docker (Testcontainers PostgreSQL 16).

---

## Explicitamente fuera de alcance

PatientReferencePort · Appointment · Encounter · MedicalRecord · Billing · Merge · Export · DELETE · search avanzada · fuzzy · eventos · nuevos permisos · ADR nuevo

---

## Checklist DoD

- [x] API extremo a extremo  
- [x] Unit + WebFlux ITs verdes  
- [x] Documentación PASO-17.6  
- [x] ROADMAP actualizado  
- [x] Sin decisión arquitectónica nueva  
- [x] Patient sigue *intentionally small* (ADR-012)  
- [x] Core Platform fortalecido por consistencia, no por novedad vertical  

---

## Siguiente paso

**PASO 17.7 — Patient Verification** — ✅ [PASO-17.7](PASO-17.7-PATIENT-VERIFICATION.md). Siguiente: **17.8 Closeout**.

---

## Referencias

- [PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md](PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md)  
- [PASO-16.4-ORGANIZATION-ADMINISTRATION-API.md](PASO-16.4-ORGANIZATION-ADMINISTRATION-API.md)  
- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
