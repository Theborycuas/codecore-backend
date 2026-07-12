# PASO 18.5.1 — Appointment Administration API Audit

**Veredicto:** la API administrativa de Appointment debe **reutilizar el patrón HTTP de Organization/Patient** (list/get/create/update + transiciones de lifecycle por `POST`), adaptado al lifecycle `SCHEDULED|CANCELLED|COMPLETED` de ADR-014. No hace falta un diseño REST distinto ni un ADR nuevo.

**Fecha:** 2026-07-11  
**Estado:** ✅ Auditoría cerrada — lista para implementación (PASO 18.6)  
**Tipo:** Solo diseño — sin código  
**Fuentes:** PASO-17.5.1 · PASO-16.3.1 · ADR-014 · ADR-013 · ADR-007 · PASO-18.5 · PASO-18.2 · PASO-18.4

---

## Quick path (18.6)

1. Base: `GET/POST /api/v1/scheduling/appointments` (+ `/{id}`, `PUT`, `POST …/cancel`, `POST …/complete`)
2. Misma paginación que Patient/Org (`page` / `size` / `sort` / `status`)
3. Default list: `status=SCHEDULED`
4. Validar refs vía ReferencePorts (Patient · Organization · Office · StaffAssignment) + coherencia ADR-014 §7
5. Tenant solo desde JWT · cross-tenant → 404
6. `complete` → `appointment:update` · `cancel` → `appointment:cancel`

---

## Respuestas obligatorias

### 1 — ¿Seguir exactamente el patrón Organization / Patient?

**Sí, en forma HTTP.** Appointment no es soft-entity `ACTIVE|ARCHIVED`, pero **sí** es un aggregate de lifecycle sin delete físico — mismo estilo de API:

| Aspecto | Decisión |
|---------|----------|
| Verbos | list · get · create (schedule) · update · cancel · complete |
| Paths | `POST …/cancel` · `POST …/complete` (no `DELETE`; no `archive`/`activate`) |
| Permisos | `appointment:*` (18.5); complete → `appointment:update` |
| Paginación | `page` / `size` / `sort` / `status` |
| Respuestas | sin `tenantId` en JSON |
| Errores | cross-tenant / missing → **404**; dominio inválido → **409**; validación → **400** |

**No inventar** calendario REST, availability endpoints, ni un “AppointmentDetail” con Encounter/notes. Consistencia del Core > originalidad.

---

### 2 — ¿Qué operaciones necesita Appointment hoy?

| Operación | Endpoint | Permiso |
|-----------|----------|---------|
| Listar | `GET /api/v1/scheduling/appointments` | `appointment:read` |
| Obtener | `GET /api/v1/scheduling/appointments/{id}` | `appointment:read` |
| Agendar | `POST /api/v1/scheduling/appointments` | `appointment:create` |
| Actualizar compromiso | `PUT /api/v1/scheduling/appointments/{id}` | `appointment:update` |
| Cancelar | `POST …/{id}/cancel` | `appointment:cancel` |
| Completar | `POST …/{id}/complete` | `appointment:update` |

**No hoy:** delete · reactivate · recurrence · waitlist · slots · availability · double-booking check · notifications · Encounter auto-create · calendar views · filtros de “agenda semanal” complejos.

**Path base:** `/api/v1/scheduling/appointments` (schema SQL `scheduling` + BC Scheduling — ADR-014).

---

### 3 — ¿Cancel / Complete = espejo Archive / Activate?

**Semántica distinta, forma HTTP idéntica.**

| | SCHEDULED | CANCELLED | COMPLETED |
|--|-----------|-----------|-----------|
| Significado | Compromiso abierto | Anulado (terminal) | Cumplido operativamente (terminal) |
| `cancel` | SCHEDULED → CANCELLED | rechazo si no SCHEDULED | rechazo |
| `complete` | SCHEDULED → COMPLETED | rechazo | rechazo |
| Reactivar | **Prohibido en v1** | crear **nuevo** Appointment | N/A |
| Delete físico | No | No | No |

Diferencia vs Patient/Org:

- No hay `activate` / `archive`.
- Estados terminales son **dos** (`CANCELLED`, `COMPLETED`), no uno.
- Mutaciones (`PUT`) solo en `SCHEDULED` (ya en dominio 18.3).

`complete` **no** crea Encounter (ADR-014).

---

### 4 — ¿GET by id devuelve CANCELLED / COMPLETED?

**Sí** — si pertenece al tenant JWT.

- Existe + mismo tenant (cualquier status) → **200**
- Inexistente u otro tenant → **404** (anti-enumeración)

Misma regla que Organization/Patient. Necesario para auditoría y UI de historial. **No** hay “reactivar” desde GET.

---

### 5 — ¿Listado filtra SCHEDULED por defecto?

**Sí.**

| `status` | Comportamiento |
|----------|----------------|
| *(omitido)* / `SCHEDULED` | Solo abiertos |
| `CANCELLED` | Solo cancelados |
| `COMPLETED` | Solo completados |
| `ALL` | Los tres |

Default operativo: recepción/agenda ve compromisos vivos. Históricos bajo filtro explícito.

---

### 6 — ¿Qué búsquedas necesita el Core hoy?

**Mínimo útil, sin motor de agenda:**

| Filtro / sort | ¿v1? | Nota |
|---------------|------|------|
| `status` | ✅ | Default SCHEDULED |
| `organizationId` | ✅ | Contexto de negocio denormalizado (ADR-014 §7) |
| `patientId` | ✅ | Sujeto de cuidado |
| `staffAssignmentId` | ✅ | Quién opera |
| `officeId` | ✅ | Ubicación opcional (incluye solo filas con ese office) |
| `from` / `to` (sobre `startsAt`) | ✅ | Rango UTC opcional — listado operativo del día/semana **sin** calendar engine |
| `q` texto libre | ❌ | No hay display name en Appointment |
| Overlap / free-slot search | ❌ | Availability — futuro BC/producto |
| Double-booking detection | ❌ | ADR-014 §9.7 — fuera de modelo |
| sort | ✅ | `startsAt`, `endsAt`, `status`, `createdAt`, `updatedAt` (default `startsAt,asc`) |

`from`/`to` es filtro de listado, **no** un endpoint de disponibilidad. Si ambos presentes: `startsAt >= from AND startsAt < to` (instantes UTC ISO-8601).

---

### 7 — ¿Aislamiento por tenant?

**Patrón IAM/Org/Patient sin cambios:**

```text
JWT → TenantContextAccessor → todos los use cases filtran por TenantId
Nunca aceptar tenantId en body/query
findByIdAndTenantId → vacío = 404
```

Sin OwnershipPolicy org-scoped. RBAC membership-scoped (ADR-007).  
`OrganizationId` en el Appointment **no** implica RBAC por organización.

---

### 8 — ¿ReferencePorts en create / update (ADR-013)?

**En application (create / update mientras SCHEDULED), no en el controller ni en el dominio cruzando repos.**

Orden recomendado de validación en use case:

| Paso | Port / regla | Fallo |
|------|--------------|-------|
| 1 | `PatientReferencePort.existsActiveByIdAndTenant` | **404** (no usable en tenant) |
| 2 | `OrganizationReferencePort.existsActiveByIdAndTenant` | **404** |
| 3 | `StaffAssignmentReferencePort.findScopeByIdAndTenant` → empty | **404** |
| 4 | Coherencia: `appointment.organizationId` == view.`organizationId` | **409** |
| 5a | Si view tiene `officeId` → appointment.office **must equal** | **409** |
| 5b | Si view es org-wide y appointment trae office → `OfficeReferencePort.existsActiveInOrganization` | **404** si false |
| 5c | Si appointment.office null y assignment office-bound → **409** (regla ADR-014 §7.2) |
| 6 | Si office presente y assignment org-wide → ya cubierto por 5b | — |

**Bridging de VOs:** Appointment domain tiene VOs locales (`com.codecore.appointment.domain.valueobject.*`). Ports usan VOs de Patient/Org. Application convierte por UUID — **nunca** depende de patient-application / organization-application / infrastructure.

Consumer Gradle: `appointment-application` → `implementation(patient-contract)` + `implementation(organization-contract)` únicamente.

Adapters ya existen (17.8 Patient · 17.2 Org · 18.2 Office/StaffAssignment). Wiring en `codecore-api` / module configuration.

Cancel / complete: **no** revalidan ReferencePorts (transición local sobre fila existente; lectura histórica debe funcionar aunque Patient/Org ya no estén ACTIVE).

---

### 9 — ¿PUT qué muta?

Solo campos del compromiso mientras `SCHEDULED`:

| Campo | Mutable en PUT |
|-------|----------------|
| `patientId` | ✅ |
| `staffAssignmentId` | ✅ |
| `organizationId` | ✅ |
| `officeId` (nullable — `null` limpia) | ✅ |
| `startsAt` / `endsAt` | ✅ |
| `status` | ❌ — solo via cancel/complete |
| `tenantId` | ❌ — nunca |

PUT = replace de refs + ventana temporal (mismo espíritu que Patient PUT de registro). Re-validar ports + coherencia en cada PUT.

`endsAt` debe ser estrictamente posterior a `startsAt` → **400** si no.

---

### 10 — ¿Hace falta un ADR nuevo?

**No.**

| Tema | ¿Irreversible nuevo? |
|------|----------------------|
| Path `/api/v1/scheduling/appointments` | Convención HTTP — ADR-014 ya la anticipa |
| Lifecycle cancel/complete | Ya ADR-014 · permisos 18.5 |
| ReferencePort validation | Ya ADR-013 · coherencia ADR-014 §7 |
| Filtros de listado | Decisión de API, no de modelo |
| DTOs / paginación | Patrón 17.5.1 / 16.3.1 |

Solo un ADR nuevo si se reabriera boundary de Appointment, delete físico, double-booking en dominio, o org-scoped RBAC — **ninguno de esos**.

---

### 11 — ¿Mejora que fortalezca el Core sin complejidad?

| Mejora | ¿Adoptar en 18.6? |
|--------|-------------------|
| Reutilizar shape `Paged*Response` / `PageQuery` | ✅ |
| Grupo OpenAPI `scheduling-administration` | ✅ |
| Validación multi-port en un solo use case | ✅ — prueba real de ADR-013 |
| Endpoint de availability / free slots | ❌ |
| DTO con Patient demographics / Org name embebidos | ❌ viola boundaries; labels = read-model futuro |
| Cascade a Encounter / MedicalRecord | ❌ |
| Notificaciones al cancelar | ❌ |

Una sola mejora concreta: documentar en OpenAPI (`x-permission`) igual que Org/Patient — cero invento.

---

## Contrato HTTP propuesto (18.6)

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/scheduling/appointments` | `appointment:read` |
| GET | `/api/v1/scheduling/appointments/{id}` | `appointment:read` |
| POST | `/api/v1/scheduling/appointments` | `appointment:create` |
| PUT | `/api/v1/scheduling/appointments/{id}` | `appointment:update` |
| POST | `/api/v1/scheduling/appointments/{id}/cancel` | `appointment:cancel` |
| POST | `/api/v1/scheduling/appointments/{id}/complete` | `appointment:update` |

**Create body (mínimo):**

```text
patientId          (required UUID)
staffAssignmentId  (required UUID)
organizationId     (required UUID)
officeId?          (optional UUID)
startsAt           (required Instant UTC)
endsAt             (required Instant UTC)
```

**Update body:** mismos campos mutables (no `tenantId`; no `status` vía PUT).

**Response:**

```text
id, patientId, staffAssignmentId, organizationId, officeId?,
startsAt, endsAt, status, createdAt, updatedAt
```

**sin `tenantId`**. Sin nombres de Patient/Org embebidos.

**List query:**

```text
page=0&size=20&sort=startsAt,asc&status=SCHEDULED
&organizationId=&patientId=&staffAssignmentId=&officeId=
&from=&to=
```

---

## Errores HTTP (normativos para 18.6)

| Situación | HTTP |
|-----------|------|
| Id inexistente / otro tenant | **404** |
| Ref Patient/Org/Office/StaffAssignment no usable | **404** |
| Coherencia StaffAssignment↔org/office rota | **409** |
| Mutación / cancel / complete sobre no-SCHEDULED | **409** |
| Body inválido (`endsAt` ≤ `startsAt`, UUID malformado) | **400** |
| Sin permiso | **403** |
| Sin JWT / tenant | **401** |

---

## Capas a implementar en 18.6 (espejo Patient)

```text
appointment-application
  admin use case (list/get/create/update/cancel/complete)
  commands / DTOs de application
  PageQuery + filtros

appointment-infrastructure
  AppointmentAdminController
  HTTP DTOs + exception handler
  Admin query repository (paginación / filtros)
  TenantContextAccessor wiring
  AppointmentAdministrationConfiguration + OpenAPI group
```

Sin `AppointmentReferencePort` en 18.6 (closeout 18.8, como PatientReferencePort en 17.8).

---

## Checklist

- [x] Patrón HTTP Organization/Patient reutilizado  
- [x] Lifecycle ADR-014 respetado (sin reactivate / delete)  
- [x] Multi-ReferencePort + coherencia §7 en application  
- [x] Sin sobreingeniería de agenda/slots  
- [x] ADR-014 / 013 / 007 / 011 / 012 respetados  
- [x] Sin ADR nuevo  
- [x] Core Platform fortalecido por **consistencia** y primera API multi-port real  

---

## Fuera de alcance 18.5.1 / 18.6

Verification E2E completa (18.7) · Scheduling Closeout / ReferencePort / guía (18.8) · Availability · Double booking · Recurrence · Waitlist · Encounter · Notifications · Eventos · Labels enriquecidos cross-BC

---

## Siguiente paso

**PASO 18.6 — Appointment Administration API** — ✅ implementado en [PASO-18.6](PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md). Siguiente: **18.7 Verification**.

---

## Referencias

- [PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md](PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md)  
- [PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md](PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md)  
- [PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT.md](PASO-18.5-APPOINTMENT-AUTHORIZATION-CONTRACT.md)  
- [PASO-18.2-REFERENCE-PORTS.md](PASO-18.2-REFERENCE-PORTS.md)  
- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
