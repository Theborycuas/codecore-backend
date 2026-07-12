# PASO 19.5.1 — Encounter Administration API Audit

**Veredicto:** la API administrativa de Encounter debe **reutilizar el patrón HTTP de Appointment** (list/get/create/update + transiciones de lifecycle por `POST`), adaptado al lifecycle `IN_PROGRESS|CANCELLED|COMPLETED` y al tiempo `startedAt`/`endedAt` de ADR-015. No hace falta un diseño REST distinto ni un ADR nuevo.

**Fecha:** 2026-07-11  
**Estado:** ✅ Auditoría cerrada — lista para implementación (PASO 19.6)  
**Tipo:** Solo diseño — sin código  
**Fuentes:** PASO-18.5.1 · PASO-17.5.1 · ADR-015 · ADR-014 · ADR-013 · ADR-007 · PASO-19.5 · PASO-19.2 · PASO-19.4

---

## Quick path (19.6)

1. Base: `GET/POST /api/v1/records/encounters` (+ `/{id}`, `PUT`, `POST …/cancel`, `POST …/complete`)
2. Misma paginación que Appointment/Patient/Org (`page` / `size` / `sort` / `status`)
3. Default list: `status=IN_PROGRESS`
4. Validar refs vía ReferencePorts (Patient · Organization · Office · StaffAssignment · Appointment?) + coherencia ADR-015 §7
5. Tenant solo desde JWT · cross-tenant → 404
6. `complete` → `encounter:update` · `cancel` → `encounter:cancel`
7. Create **siempre** abre `IN_PROGRESS` (incluso con `endedAt` opcional); retrospección = create + complete (dos pasos)

---

## Respuestas obligatorias

### 1 — ¿Seguir exactamente el patrón Appointment / Patient / Organization?

**Sí, en forma HTTP.** Encounter no es soft-entity `ACTIVE|ARCHIVED`, pero **sí** es un aggregate de lifecycle sin delete físico — mismo estilo que Appointment:

| Aspecto | Decisión |
|---------|----------|
| Verbos | list · get · create (open) · update · cancel · complete |
| Paths | `POST …/cancel` · `POST …/complete` (no `DELETE`; no `archive`/`activate`) |
| Permisos | `encounter:*` (19.5); complete → `encounter:update` |
| Paginación | `page` / `size` / `sort` / `status` |
| Respuestas | sin `tenantId` en JSON |
| Errores | cross-tenant / missing → **404**; dominio inválido → **409**; validación → **400** |

**No inventar** EHR REST, notes embebidas, SOAP, odontogram, ni un “EncounterDetail” con documentos clínicos. Consistencia del Core > originalidad. **Encounter remains intentionally small** (ADR-015 §3).

---

### 2 — ¿Qué operaciones necesita Encounter hoy?

| Operación | Endpoint | Permiso |
|-----------|----------|---------|
| Listar | `GET /api/v1/records/encounters` | `encounter:read` |
| Obtener | `GET /api/v1/records/encounters/{id}` | `encounter:read` |
| Abrir / registrar | `POST /api/v1/records/encounters` | `encounter:create` |
| Actualizar episodio | `PUT /api/v1/records/encounters/{id}` | `encounter:update` |
| Cancelar | `POST …/{id}/cancel` | `encounter:cancel` |
| Completar | `POST …/{id}/complete` | `encounter:update` |

**No hoy:** delete · reactivate · notes · SOAP · diagnosis · prescription · attachments · odontogram · billing lines · auto-create desde Appointment · auto-complete de Appointment · EpisodeOfCare · filtros de “historia clínica longitudinal” complejos · FHIR sub-states.

**Path base:** `/api/v1/records/encounters` (schema SQL `records` + BC Clinical Records — ADR-015).

---

### 3 — ¿Cancel / Complete = espejo Appointment?

**Semántica de episodio ocurrido, forma HTTP idéntica.**

| | IN_PROGRESS | CANCELLED | COMPLETED |
|--|-------------|-----------|-----------|
| Significado | Episodio abierto (vivo o en registro) | Anulado (terminal) | Cerrado operativamente (terminal) |
| `cancel` | IN_PROGRESS → CANCELLED | rechazo si no IN_PROGRESS | rechazo |
| `complete` | IN_PROGRESS → COMPLETED (`endedAt` required) | rechazo | rechazo |
| Reactivar | **Prohibido en v1** | crear **nuevo** Encounter | N/A |
| Delete físico | No | No | No |

Diferencias vs Appointment:

- Estados abiertos: `IN_PROGRESS` (no `SCHEDULED`) — no hay estado “planificado” en Encounter.
- Tiempo: `startedAt` obligatorio; `endedAt` opcional hasta `complete` (igualdad `endedAt == startedAt` permitida).
- `AppointmentId` opcional (walk-in); Appointment **no** se muta al completar Encounter (ADR-014 · ADR-015).

`complete` **no** muta Appointment. Create **no** se dispara desde Scheduling.

---

### 4 — ¿GET by id devuelve CANCELLED / COMPLETED?

**Sí** — si pertenece al tenant JWT.

- Existe + mismo tenant (cualquier status) → **200**
- Inexistente u otro tenant → **404** (anti-enumeración)

Misma regla que Appointment/Patient/Org. Necesario para auditoría e historial clínico. **No** hay “reactivar” desde GET.

---

### 5 — ¿Listado filtra IN_PROGRESS por defecto?

**Sí.**

| `status` | Comportamiento |
|----------|----------------|
| *(omitido)* / `IN_PROGRESS` | Solo abiertos |
| `CANCELLED` | Solo cancelados |
| `COMPLETED` | Solo completados |
| `ALL` | Los tres |

Default operativo: recepción/clínica ve episodios vivos. Históricos bajo filtro explícito.

---

### 6 — ¿Qué búsquedas necesita el Core hoy?

**Mínimo útil, sin motor de historia clínica:**

| Filtro / sort | ¿v1? | Nota |
|---------------|------|------|
| `status` | ✅ | Default `IN_PROGRESS` |
| `organizationId` | ✅ | Contexto denormalizado (ADR-015 §7) |
| `patientId` | ✅ | Sujeto de cuidado |
| `staffAssignmentId` | ✅ | Quién operó |
| `officeId` | ✅ | Ubicación opcional (solo filas con ese office) |
| `appointmentId` | ✅ | Origen planificado opcional |
| `from` / `to` (sobre `startedAt`) | ✅ | Rango UTC — listado del día/semana **sin** chart engine |
| `q` texto libre | ❌ | No hay display name en Encounter |
| Notes / diagnosis search | ❌ | Fuera del aggregate (ADR-015 §3) |
| sort | ✅ | `startedAt`, `endedAt`, `status`, `createdAt`, `updatedAt` (default `startedAt,desc`) |

`from`/`to`: si ambos presentes → `startedAt >= from AND startedAt < to` (instantes UTC ISO-8601).  
Default sort `startedAt,desc` (historial reciente primero) — distinto de Appointment agenda (`startsAt,asc`) a propósito.

---

### 7 — ¿Aislamiento por tenant?

**Patrón IAM/Org/Patient/Appointment sin cambios:**

```text
JWT → TenantContextAccessor → todos los use cases filtran por TenantId
Nunca aceptar tenantId en body/query
findByIdAndTenantId → vacío = 404
```

Sin OwnershipPolicy org-scoped. RBAC membership-scoped (ADR-007).  
`OrganizationId` en el Encounter **no** implica RBAC por organización.

---

### 8 — ¿ReferencePorts en create / update (ADR-013 · ADR-015 §7)?

**En application (create / update mientras IN_PROGRESS), no en el controller ni en el dominio cruzando repos.**

Orden recomendado de validación en use case:

| Paso | Port / regla | Fallo |
|------|--------------|-------|
| 1 | `PatientReferencePort.existsActiveByIdAndTenant` | **404** |
| 2 | `OrganizationReferencePort.existsActiveByIdAndTenant` | **404** |
| 3 | `StaffAssignmentReferencePort.findScopeByIdAndTenant` → empty | **404** |
| 4 | Coherencia: `encounter.organizationId` == view.`organizationId` | **409** |
| 5a | Si view tiene `officeId` → encounter.office **must equal** | **409** |
| 5b | Si view es org-wide y encounter trae office → `OfficeReferencePort.existsActiveInOrganization` | **404** si false |
| 5c | Si encounter.office null y assignment office-bound → **409** (ADR-015 §7) |
| 6 | Si `appointmentId` presente → `AppointmentReferencePort.findLinkableByIdAndTenant` | **404** si empty |
| 7 | Si appointment presente → view.`patientId` == encounter.`patientId` | **409** |

**Bridging de VOs:** Encounter domain tiene VOs locales (`com.codecore.encounter.domain.valueobject.*`). Ports usan VOs de Patient/Org/Appointment. Application convierte por UUID — **nunca** depende de `*-application` / infrastructure de otros BCs.

Consumer Gradle: `encounter-application` → `implementation(patient-contract)` + `organization-contract` + `appointment-contract` únicamente.

Adapters ya existen (Patient · Org · Office · StaffAssignment · Appointment 19.2). Wiring en `codecore-api` / module configuration.

Cancel / complete: **no** revalidan ReferencePorts (transición local; lectura histórica debe funcionar aunque Patient/Org/Appointment ya no estén ACTIVE/linkable).

Org/office/staff del Encounter **pueden diferir** del Appointment (cobertura / sala) — **no** exigir igualdad de esos IDs con Appointment (ADR-015 §7).

---

### 9 — ¿PUT qué muta?

Solo campos del episodio mientras `IN_PROGRESS`:

| Campo | Mutable en PUT |
|-------|----------------|
| `patientId` | ✅ |
| `staffAssignmentId` | ✅ |
| `organizationId` | ✅ |
| `officeId` (nullable — `null` limpia) | ✅ |
| `appointmentId` (nullable — `null` limpia) | ✅ |
| `startedAt` | ✅ |
| `endedAt` (nullable — `null` limpia mientras abierto) | ✅ |
| `status` | ❌ — solo via cancel/complete |
| `tenantId` | ❌ — nunca |

PUT = replace de refs + time bounds (mismo espíritu que Appointment PUT). Re-validar ports + coherencia en cada PUT.

Si `endedAt` presente → `endedAt >= startedAt` → **400** si no.

Cambiar `patientId` con `appointmentId` presente exige revalidar match patient↔appointment (§8 paso 7).

---

### 10 — ¿Create retrospectivo / `endedAt` en open?

**Decisión v1 (cerrada aquí):**

| Caso | Comportamiento |
|------|----------------|
| Create | **Siempre** status `IN_PROGRESS` |
| `startedAt` | Required |
| `endedAt` en create | Opcional — puede anticipar cierre sin completar |
| Retrospección (episodio pasado) | `POST` create (con `startedAt` + `endedAt`) → `POST …/complete` (usa `endedAt` existente) |
| One-shot create→COMPLETED | **No** en v1 — evita ambiguidad de status en un solo POST |

**Complete body:**

```text
endedAt?   (optional Instant UTC)
```

Resolución de `endedAt` en complete (orden):

1. Body `endedAt` si presente  
2. Else `endedAt` ya persistido en el Encounter  
3. Else `Instant.now()` (application clock)

Resultado debe satisfacer `endedAt >= startedAt` → **400** si no.

`cancel` **no** exige ni fija `endedAt`.

---

### 11 — ¿Hace falta un ADR nuevo?

**No.**

| Tema | ¿Irreversible nuevo? |
|------|----------------------|
| Path `/api/v1/records/encounters` | Convención HTTP — ADR-015 ya la anticipa |
| Lifecycle cancel/complete | Ya ADR-015 · permisos 19.5 |
| ReferencePort + Appointment linkable | Ya ADR-013 · 19.2 · coherencia ADR-015 §7 |
| Filtros de listado / retrospectiva two-step | Decisión de API, no de modelo |
| DTOs / paginación | Patrón 18.5.1 / 17.5.1 |

Solo un ADR nuevo si se reabriera boundary de Encounter (notes dentro), delete físico, auto-orquestación Appointment↔Encounter, o org-scoped RBAC — **ninguno de esos**.

---

### 12 — ¿Mejora que fortalezca el Core sin complejidad?

| Mejora | ¿Adoptar en 19.6? |
|--------|-------------------|
| Reutilizar shape `Paged*Response` / `PageQuery` | ✅ |
| Grupo OpenAPI `records-administration` | ✅ |
| Validación multi-port + Appointment linkable en un solo use case | ✅ — prueba real de ADR-013 + 19.2 |
| Endpoint de clinical notes / SOAP | ❌ viola ADR-015 §3 |
| DTO con Patient demographics / Org name / Appointment window embebidos | ❌ viola boundaries; labels = read-model futuro |
| Cascade Appointment complete / create | ❌ |
| Notificaciones al completar | ❌ |

Una sola mejora concreta: documentar en OpenAPI (`x-permission`) igual que Appointment — cero invento.

---

## Contrato HTTP propuesto (19.6)

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/records/encounters` | `encounter:read` |
| GET | `/api/v1/records/encounters/{id}` | `encounter:read` |
| POST | `/api/v1/records/encounters` | `encounter:create` |
| PUT | `/api/v1/records/encounters/{id}` | `encounter:update` |
| POST | `/api/v1/records/encounters/{id}/cancel` | `encounter:cancel` |
| POST | `/api/v1/records/encounters/{id}/complete` | `encounter:update` |

**Create body (mínimo):**

```text
patientId          (required UUID)
staffAssignmentId  (required UUID)
organizationId     (required UUID)
officeId?          (optional UUID)
appointmentId?     (optional UUID)
startedAt          (required Instant UTC)
endedAt?           (optional Instant UTC — status sigue IN_PROGRESS)
```

**Update body:** mismos campos mutables (no `tenantId`; no `status` vía PUT).

**Complete body:**

```text
endedAt?           (optional Instant UTC — ver §10)
```

**Cancel body:** vacío (sin payload).

**Response:**

```text
id, patientId, staffAssignmentId, organizationId, officeId?, appointmentId?,
startedAt, endedAt?, status, createdAt, updatedAt
```

**sin `tenantId`**. Sin nombres de Patient/Org/Appointment embebidos. Sin notes.

**List query:**

```text
page=0&size=20&sort=startedAt,desc&status=IN_PROGRESS
&organizationId=&patientId=&staffAssignmentId=&officeId=&appointmentId=
&from=&to=
```

---

## Errores HTTP (normativos para 19.6)

| Situación | HTTP |
|-----------|------|
| Id inexistente / otro tenant | **404** |
| Ref Patient/Org/Office/StaffAssignment no usable | **404** |
| Appointment no linkable (ausente / CANCELLED / otro tenant) | **404** |
| Coherencia StaffAssignment↔org/office rota | **409** |
| Appointment.patientId ≠ Encounter.patientId | **409** |
| Mutación / cancel / complete sobre no-IN_PROGRESS | **409** |
| Body inválido (`endedAt` < `startedAt`, UUID malformado) | **400** |
| Sin permiso | **403** |
| Sin JWT / tenant | **401** |

---

## Capas a implementar en 19.6 (espejo Appointment)

```text
encounter-application
  admin use case (list/get/create/update/cancel/complete)
  commands / DTOs de application
  PageQuery + filtros
  ReferencePort orchestration (Patient · Org · Office · Staff · Appointment?)

encounter-infrastructure
  EncounterAdminController
  HTTP DTOs + exception handler
  Admin query repository (paginación / filtros)
  TenantContextAccessor wiring
  EncounterAdministrationConfiguration + OpenAPI group records-administration
```

Sin `EncounterReferencePort` en 19.6 (closeout 19.8, como AppointmentReferencePort en 18.8).

---

## Checklist

- [x] Patrón HTTP Appointment/Patient/Org reutilizado  
- [x] Lifecycle ADR-015 respetado (sin reactivate / delete / one-shot COMPLETED)  
- [x] Multi-ReferencePort + coherencia §7 + Appointment linkable en application  
- [x] Sin sobreingeniería de EHR / notes / SOAP  
- [x] ADR-015 / 014 / 013 / 012 / 011 / 007 respetados  
- [x] Sin ADR nuevo  
- [x] Core Platform fortalecido por **consistencia** y Encounter **intentionally small**  

---

## Fuera de alcance 19.5.1 / 19.6

Verification E2E completa (19.7) · Clinical Records Closeout / EncounterReferencePort / guía (19.8) · Notes · SOAP · Diagnoses · Prescriptions · Attachments · Odontogram · Billing · Auto-orquestación Appointment · Notifications · Eventos · Labels enriquecidos cross-BC

---

## Siguiente paso

**PASO 19.6 — Encounter Administration API** — ✅ [PASO-19.6](PASO-19.6-ENCOUNTER-ADMINISTRATION-API.md). Siguiente: **19.7 Verification**.

---

## Referencias

- [PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md](PASO-18.5.1-APPOINTMENT-ADMINISTRATION-API-AUDIT.md)  
- [PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md](PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md)  
- [PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT.md](PASO-19.5-ENCOUNTER-AUTHORIZATION-CONTRACT.md)  
- [PASO-19.2-REFERENCE-READINESS.md](PASO-19.2-REFERENCE-READINESS.md)  
- [ADR-015](../architecture/ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
