# PASO 17.7 — Patient Verification & Core Validation

**Veredicto:** Patient es una pieza **estable** del Core Platform. Clinical Foundation (v1 registry) está listo para que Scheduling / Appointment lo consuma **sin reabrir** ADR-012.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Verificación + consistencia + cierre de BC (sin nuevas capacidades)  
**Dependencias:** [PASO-17.6](PASO-17.6-PATIENT-ADMINISTRATION-API.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. Suite E2E: `PatientVerificationIT` (8 checks) — verde  
2. Dominio / persistencia / permisos / API auditados contra ADR-012/013  
3. Siguiente: **PASO 17.8 — Clinical Foundation Closeout**

```bash
./gradlew :modules:patient-management:patient-infrastructure:test \
  --tests "com.codecore.patient.interfaces.http.admin.PatientVerificationIT"
```

---

## Objetivo

Certificar que el primer bounded context clínico de CodeCore cumple el contrato acumulado (PASO 17.0 → 17.6) y fortalece el **Core reutilizable**, no una app vertical.

**No** se implementaron endpoints, campos, permisos ni ADRs nuevos.  
**Sí** se consolidó evidencia ejecutable + auditoría documental.

---

## Suite E2E

| # | Verificación | Resultado |
|---|--------------|-----------|
| 1 | Journey: create (± primary org) → list → filter → update → archive → activate | ✅ |
| 2 | RBAC — `READ_ONLY` no crea | 403 ✅ |
| 3 | Cross-tenant GET | 404 ✅ |
| 4 | Primary org inexistente / no ACTIVE (`OrganizationReferencePort`) | 404 ✅ |
| 5 | MANAGER archiva Patient (matriz 17.5 — distinto de `organization:archive`) | 200 ✅ |
| 6 | External identifier duplicado en tenant | 409 ✅ |
| 7 | OpenAPI grupo `clinical-administration` | ✅ |
| 8 | Sin JWT | 401 ✅ |

Patrón espejo: [PASO-16.9-ORGANIZATION-VERIFICATION.md](PASO-16.9-ORGANIZATION-VERIFICATION.md).

---

## Checklist — Aggregate “intentionally small”

| Responsabilidad | ¿En Patient? | Evidencia |
|-----------------|--------------|-----------|
| Identidad clínica registral | ✅ Sí (única) | ADR-012 · `Patient` aggregate |
| Demographics + contacts + external ids | ✅ Sí | Domain VOs |
| Lifecycle ACTIVE / ARCHIVED | ✅ Sí | Soft archive |
| Appointment / Scheduling | ❌ | Ausente en domain/API/SQL |
| Encounter | ❌ | Ausente |
| MedicalRecord / Notes / Odontogram | ❌ | Ausente |
| Billing / Inventory | ❌ | Ausente |
| Office / StaffAssignment | ❌ | Sin `OfficeId`; test `shouldNeverExposeOfficeOrOperationalConcernsInPublicApi` |
| IAM Identity / Membership | ❌ | Solo `TenantId` lógico |
| Lógica de vertical (dental, vet, …) | ❌ | Display name genérico; permisos `patient:*` neutros |

**One-sentence rule:** *Patient = la identidad clínica registral del sujeto de cuidado.*  
Sigue cabiendo en una frase → no es God Aggregate.

---

## Validación ADR-012 (congelado)

| Constraint | ¿Cumple? |
|------------|----------|
| Modelo pequeño; sin embed de concerns clínicos/operativos | ✅ |
| `TenantId` requerido e inmutable | ✅ |
| `PrimaryOrganizationId` opcional (referencia, no ownership) | ✅ |
| Prohibidos: `OfficeId`, `StaffAssignmentId`, `MembershipId`, `IdentityId` | ✅ |
| Soft archive; sin DELETE físico | ✅ |
| Email/phone no son claves de unicidad | ✅ |
| Validación primary org ACTIVE+tenant en escritura vía ReferencePort | ✅ |
| Un Patient por sujeto por tenant (no fila por org) | ✅ |
| Sin verbos de vertical en permisos | ✅ |

**Observación:** ninguna decisión de este paso exige modificar el modelo. ADR-012 permanece intacto.

---

## Validación ADR-013 (Reference Contracts)

| Constraint | ¿Cumple? |
|------------|----------|
| Consume `organization-contract` (no infra/repo) | ✅ `patient-application` |
| `OrganizationReferencePort.existsActiveByIdAndTenant` | ✅ use case |
| Nunca `OrganizationRepository` en main | ✅ |
| Nunca SQL `org.*` en patient | ✅ solo `clinical.patient*` |
| `false` del port → excepción del consumidor (`PrimaryOrganizationNotFoundException`) | ✅ → HTTP 404 |
| Sin joins cross-schema para validar org | ✅ |

---

## Validación Organization Integration (ADR-011 + Consumption Guide)

| Regla de consumo | Patient |
|------------------|---------|
| Store reference ID, no aggregate Org | ✅ `PrimaryOrganizationId` |
| Validate via port | ✅ |
| Filter by JWT tenant | ✅ `IamTenantContextAccessor` |
| No `OfficeId` on Patient | ✅ (Appointment usará Office) |
| No `StaffAssignmentId` on Patient | ✅ |
| No import organization-domain/infra en application | ✅ |

Patient es el **primer consumidor real** del patrón ReferencePort — evidencia viva de ADR-013.

---

## Validación Development Policy (FASE 16+)

| Principio | ¿Cumple? |
|-----------|----------|
| Dominio antes que DB/controller | ✅ (17.0.1 → 17.3 → 17.4 → 17.6) |
| Consistencia de Administration API con Org | ✅ mismos verbos, soft lifecycle, paginación, 404 cross-tenant |
| Sin sobreingeniería (CQRS, ES, microservicios) | ✅ |
| Auditoría antes de decisiones irreversibles | ✅ 17.0.1 / 17.5.1 |
| Framework de negocio unificado, no módulo aislado | ✅ ciudadano nativo del Core |

---

## Persistencia

`clinical.patient` + `clinical.patient_external_identifier` (V18):

| Columna / concern | ¿Presente? |
|-------------------|------------|
| Registry identity + demographics + status | ✅ |
| `tenant_id` (sin FK IAM) | ✅ |
| `primary_organization_id` (sin FK org) | ✅ |
| Appointment / MedicalRecord / Billing / Office / Staff / Identity / Membership / Inventory | ❌ |

Schema = identidad registral. Nada más.

---

## Autorización

| Permiso | Existe |
|---------|--------|
| `patient:create` | ✅ |
| `patient:read` | ✅ |
| `patient:update` | ✅ |
| `patient:archive` | ✅ |
| Otros `patient:*` / verticales | ❌ |

Activate → `patient:update` (contrato 17.5). Catalog + V19 alineados. Matrix: OWNER/ADMIN/MANAGER = 4; USER/READ_ONLY = read.

---

## API vs Organization

| Aspecto | Organization | Patient | Paridad |
|---------|--------------|---------|---------|
| Soft CRUD + archive/activate | ✅ | ✅ | ✅ |
| Cross-tenant | 404 | 404 | ✅ |
| Sin JWT | 401 | 401 | ✅ |
| Sin permiso | 403 | 403 | ✅ |
| Duplicate key | 409 | 409 (ext id) | ✅ |
| Paginación `page/size/sort/status` | ✅ | ✅ + filtros 17.5.1 | ✅ |
| `tenantId` en response | No | No | ✅ |
| MANAGER archive | Org: denegado | Patient: permitido | ⚠️ intencional (matriz distinta) |

---

## Reutilización multi-vertical

| Vertical | ¿Reutilizable sin cambiar Patient? | Por qué |
|----------|------------------------------------|---------|
| Dental | ✅ | Sujeto humano; MRN/external ids; sin odontograma en Core |
| Veterinaria | ✅ | `displayName` admite animal; sin species/breed en Core (extensión futura = otro VO/ADR, no God Aggregate) |
| Hospital | ✅ | Identidad registral + primary org; encounters fuera |
| Laboratorio | ✅ | Mismo registro; órdenes = otro BC |
| Psicología | ✅ | Sin notes clínicas en Patient |
| Fisioterapia | ✅ | Idem |
| Oftalmología | ✅ | Idem |
| Vertical futuro | ✅ | Contrato pequeño + permisos neutros + ReferencePorts |

Patient pertenece al **Core**, no a Dental ni a ningún producto.

---

## Conclusiones

1. **Clinical Foundation (Patient registry v1) está verificado** de dominio a HTTP.  
2. **ADR-012 / 013 se mantienen** — sin presión de cambio de modelo en este paso.  
3. **Scheduling puede consumir Patient** vía futuro `PatientReferencePort` (17.8+ / FASE 18) sin reabrir arquitectura de Patient.  
4. CodeCore sale **más fuerte como plataforma**, no como app clínica.

---

## Riesgos encontrados

| Riesgo | Severidad | Mitigación / nota |
|--------|-----------|-------------------|
| Confundir matriz MANAGER Patient vs Org archive | Baja | Documentado en verification #5 y PASO 17.5 |
| Ausencia aún de `PatientReferencePort` | Media (bloquea FASE 18) | Planificado; no es defecto de 17.7 — closeout 17.8 puede declarar superficie |
| Species/breed u otros atributos verticales tentadores | Media futura | Requieren ADR nuevo; no meter en Patient “porque vet” |
| OpenAPI closeout formal | Baja | Grupo ya existe; polish en 17.8 |

Ningún riesgo obliga a modificar el dominio ahora.

---

## Oportunidades de mejora (no bloqueantes)

| Mejora | Cuándo |
|--------|--------|
| `PatientReferencePort` + consumo guide “Patient” | Closeout 17.8 / inicio FASE 18 |
| Guide de consumo Clinical Foundation | 17.8 |
| Verification smoke en CI pipeline dedicado | Operativo |
| Extensiones verticales vía módulos product, no Core | Futuro SaaS |

---

## Checklist DoD

- [x] Checklist arquitectónico completo  
- [x] Validación ADR-012 / ADR-013  
- [x] Validación Development Policy  
- [x] Validación Organization Integration Guide / ADR-011  
- [x] Suite `PatientVerificationIT` verde (8/8)  
- [x] Documentación PASO-17.7  
- [x] ROADMAP → 17.7 ✅ · siguiente 17.8  
- [x] Sin cambios de modelo / ADR / endpoints nuevos  

---

## Siguiente paso

**PASO 17.8 — Clinical Foundation Closeout** — ✅ [PASO-17.8](PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md). Siguiente: **FASE 18 Scheduling**.

---

## Referencias

- [PASO-17.6-PATIENT-ADMINISTRATION-API.md](PASO-17.6-PATIENT-ADMINISTRATION-API.md)  
- [PASO-16.9-ORGANIZATION-VERIFICATION.md](PASO-16.9-ORGANIZATION-VERIFICATION.md)  
- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
