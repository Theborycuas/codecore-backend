# PASO 17.5.1 — Patient Administration API Audit

**Veredicto:** la API administrativa de Patient debe **reutilizar el patrón Organization/Office** (soft-entity). No hace falta un diseño distinto ni un ADR nuevo.

**Fecha:** 2026-07-11  
**Estado:** ✅ Auditoría cerrada — lista para implementación (PASO 17.6)  
**Tipo:** Solo diseño — sin código  
**Fuentes:** PASO-16.3.1 · ADR-012 · ADR-013 · ADR-007 · PASO-17.5

---

## Quick path (17.6)

1. Base: `GET/POST /api/v1/clinical/patients` (+ `/{id}`, `PUT`, `POST …/archive`, `POST …/activate`)
2. Misma paginación/status que Organization (`status=ACTIVE` default)
3. Validar `primaryOrganizationId` opcional vía `OrganizationReferencePort`
4. Tenant solo desde JWT · cross-tenant → 404

---

## Respuestas obligatorias

### 1 — ¿Seguir exactamente el patrón Organization/Office?

**Sí.**

Patient es soft-entity con `ACTIVE|ARCHIVED`, sin delete físico — igual que Organization/Office (no StaffAssignment).

| Aspecto | Decisión |
|---------|----------|
| Verbos | list · get · create · update · archive · activate |
| Paths | `POST …/archive` · `POST …/activate` (no `DELETE`) |
| Permisos | `patient:*` (17.5); activate → `patient:update` |
| Paginación | `page` / `size` / `sort` / `status` |
| Respuestas | sin `tenantId` en JSON |
| Errores | cross-tenant / missing → **404**; conflicto dominio → **409**; validación → **400** |

**No inventar** un estilo REST distinto. Consistencia del Core > originalidad.

---

### 2 — ¿Qué operaciones necesita Patient hoy?

| Operación | Endpoint | Permiso |
|-----------|----------|---------|
| Listar | `GET /api/v1/clinical/patients` | `patient:read` |
| Obtener | `GET /api/v1/clinical/patients/{id}` | `patient:read` |
| Registrar | `POST /api/v1/clinical/patients` | `patient:create` |
| Corregir registro | `PUT /api/v1/clinical/patients/{id}` | `patient:update` |
| Archivar | `POST …/{id}/archive` | `patient:archive` |
| Reactivar | `POST …/{id}/activate` | `patient:update` |

**No hoy:** merge · anonymize · export · link-identity · delete · appointments embebidos.

**Path base:** `/api/v1/clinical/patients` (schema SQL `clinical` + BC Clinical Foundation — cierra la duda de PASO-17.0).

---

### 3 — ¿Archive / Activate = misma semántica que Organization?

**Sí.**

| | ACTIVE | ARCHIVED |
|--|--------|----------|
| Significado | Registro operativo | Soft-retire; retención histórica |
| `archive` | ACTIVE → ARCHIVED | rechazo si ya archived |
| `activate` | ARCHIVED → ACTIVE | rechazo si ya active |
| Delete físico | No | No |

Diferencia vs Org: Patient **no** tiene hijos estructurales (offices). Archive Patient **no** requiere guard de “hijos ACTIVE”. Downstream (Appointment) validará Patient ACTIVE vía ReferencePort futuro — fuera de 17.6.

Mutaciones de registro (`update`) solo en ACTIVE (ya en dominio 17.3).

---

### 4 — ¿GET by id devuelve ARCHIVED?

**Sí** — si pertenece al tenant JWT.

- Existe + mismo tenant (ACTIVE o ARCHIVED) → **200**
- Inexistente u otro tenant → **404** (anti-enumeración)

Misma regla que Organization (PASO-16.3.1 §4). Necesario para auditoría y reactivación.

---

### 5 — ¿Listado filtra ACTIVE por defecto?

**Sí.**

| `status` | Comportamiento |
|----------|----------------|
| *(omitido)* / `ACTIVE` | Solo ACTIVE |
| `ARCHIVED` | Solo archivados |
| `ALL` | Ambos |

Default operativo: recepción ve el registro vivo. Archived bajo filtro explícito.

---

### 6 — ¿Qué búsquedas necesita el Core hoy?

**Mínimo útil, sin sobreingeniería:**

| Filtro / sort | ¿v1? | Nota |
|---------------|------|------|
| `status` | ✅ | Igual Org |
| `q` (texto en `display_name`) | ✅ | Búsqueda básica de recepción |
| `primaryOrganizationId` | ✅ | Filtro opcional de agrupación |
| `externalIdentifierType` + `externalIdentifierValue` | ✅ | Lookup por MRN/documento/chip |
| email / phone como unique search | ❌ | Contacto, no clave (ADR-012) |
| full-text / fuzzy / phonetic | ❌ | Hipotético — no ahora |
| sort | ✅ | `displayName`, `status`, `createdAt`, `updatedAt` (default `createdAt,desc`) |

Unicidad soft de external ids ya está en DB; la API no inventa merge.

---

### 7 — ¿Aislamiento por tenant?

**Patrón IAM/Org sin cambios:**

```text
JWT → TenantContextAccessor → todos los use cases filtran por TenantId
Nunca aceptar tenantId en body/query
findByIdAndTenantId → vacío = 404
```

Sin OwnershipPolicy org-scoped. RBAC membership-scoped (ADR-007).

---

### 8 — ¿PrimaryOrganizationId + OrganizationReferencePort (ADR-013)?

**En application (create / update), no en el controller ni en el dominio cruzando repos.**

| Caso | Comportamiento |
|------|----------------|
| Campo ausente / null | OK — primary org opcional |
| Presente | `OrganizationReferencePort.existsActiveByIdAndTenant(id, tenantId)` |
| `false` | error de dominio/aplicación → **404** o **409** (preferir **404** si “no usable en tenant”, alineado MembershipReferencePort en StaffAssignment) |
| Mutación | **Nunca** vía OrganizationRepository / SQL a `org.*` desde Patient |

Consumer: `patient-application` → `implementation(organization-contract)` únicamente.  
Adapter ya existe en `organization-infrastructure` (17.2). Wiring en `codecore-api`.

No se expone aggregate Organization. No se rompe ADR-013.

---

### 9 — ¿Hace falta un ADR nuevo?

**No.**

| Tema | ¿Irreversible nuevo? |
|------|----------------------|
| Path `/api/v1/clinical/patients` | Convención HTTP — documentar aquí |
| Soft archive / activate | Ya ADR-012 |
| ReferencePort validation | Ya ADR-013 |
| Filtros de listado | Decisión de API, no de modelo de dominio |
| DTOs / paginación | Patrón 16.3.1 |

Solo un ADR nuevo si se reabriera boundary de Patient, delete físico, o org-scoped RBAC — **ninguno de esos**.

---

### 10 — ¿Mejora que fortalezca el Core sin complejidad?

| Mejora | ¿Adoptar en 17.6? |
|--------|-------------------|
| Reutilizar shape `Paged*Response` / `PageQuery` | ✅ |
| Grupo OpenAPI `clinical-administration` | ✅ en 17.8 (o mínimo en 17.6) |
| `replaceExternalIdentifiers` en el mismo PUT | ✅ un solo update de registro |
| Endpoint de merge / search avanzada | ❌ |
| DTO “PatientDetail” con citas/records | ❌ viola ADR-012 |
| Cascade archive a otros BCs | ❌ Patient no posee esos aggregates |

Una sola mejora concreta: documentar en OpenAPI (`x-permission`) igual que Org — cero invento.

---

## Contrato HTTP propuesto (17.6)

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/clinical/patients` | `patient:read` |
| GET | `/api/v1/clinical/patients/{id}` | `patient:read` |
| POST | `/api/v1/clinical/patients` | `patient:create` |
| PUT | `/api/v1/clinical/patients/{id}` | `patient:update` |
| POST | `/api/v1/clinical/patients/{id}/archive` | `patient:archive` |
| POST | `/api/v1/clinical/patients/{id}/activate` | `patient:update` |

**Create body (mínimo):** `displayName` (+ opcionales: email, phone, dateOfBirth, primaryOrganizationId, externalIdentifiers[])  
**Update body:** mismos campos mutables de registro (no `tenantId`; no status vía PUT — status solo archive/activate)  
**Response:** `id`, demographics, `primaryOrganizationId?`, `externalIdentifiers`, `status`, `createdAt`, `updatedAt` — **sin tenantId**

**List query:** `page=0&size=20&sort=createdAt,desc&status=ACTIVE` (+ `q`, `primaryOrganizationId`, external id pair opcionales)

---

## Checklist

- [x] Patrón Organization/Office reutilizado  
- [x] Sin sobreingeniería de búsqueda  
- [x] ADR-012 / 013 / 007 respetados  
- [x] Sin ADR nuevo  
- [x] Core Platform fortalecido por **consistencia**, no por novedad  

---

## Fuera de alcance 17.5.1 / 17.6

Verification E2E completa (17.7) · OpenAPI closeout (17.8) · PatientReferencePort · merge · portal Identity

---

## Siguiente paso

**PASO 17.6 — Patient Administration API** — ✅ implementado en [PASO-17.6](PASO-17.6-PATIENT-ADMINISTRATION-API.md). Siguiente: **17.7 Verification**.

---

## Referencias

- [PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md](PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md)  
- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
