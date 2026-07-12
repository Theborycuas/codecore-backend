# PASO 19.0.1 — Encounter Aggregate Audit (DDD Estratégico)

**Encounter** es el episodio de atención que **ocurrió** — *intentionally small*, multi-vertical, y el primer Aggregate Root del BC **Clinical Records**.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado (solo arquitectura)  
**Tipo:** Auditoría obligatoria — Aggregate Root nuevo + Bounded Context Clinical Records  
**Dependencias:** [PASO-19.0](PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) · ADR-011 · ADR-012 · ADR-013 · ADR-014 · [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Objetivo

Definir el modelo correcto del Aggregate Root **`Encounter`** para que CodeCore, como Core Platform, registre episodios de atención en dental, veterinaria, hospital, laboratorio, psicología, fisioterapia y verticales futuros — **sin** acoplar Clinical Records a un producto concreto ni convertir Encounter en un EHR.

**Sin código. Sin tablas. Sin endpoints. Sin migraciones. Sin SOAP / odontograma / recetas.**

---

## Checklist política (§8) — verdicto previo

| # | Ítem | ✓ | Nota |
|---|------|---|------|
| 1 | Aggregate Root identificado | ✅ | `Encounter` |
| 2 | Ownership definido | ✅ | BC Clinical Records |
| 3 | Invariantes definidas | ✅ | § Invariantes |
| 4 | Lifecycle definido | ✅ | § Lifecycle |
| 5 | Estados definidos | ✅ | `IN_PROGRESS` · `COMPLETED` · `CANCELLED` |
| 6 | Permisos definidos | ✅ | Borrador `encounter:*` |
| 7 | Relaciones solo mediante IDs | ✅ | § Referencias |
| 8 | Bounded Context correcto | ✅ | Clinical Records (≠ Foundation, ≠ Scheduling) |
| 9 | No rompe ADR vigentes | ✅ | 003/006/007/010–014 intactos; BCs cerrados |
| 10 | Escalable multi-tenant | ✅ | |
| 11 | Escalable multi-organization | ✅ | |
| 12 | Escalable millones de registros | ✅ | Aggregate delgado; sin hijos clínicos |

**Veredicto:** checklist en verde → **ADR-015 Accepted** ([PASO-19.1](PASO-19.1-ENCOUNTER-MODEL-CONTRACT.md)).

---

## Decisiones irreversibles (resumen ejecutivo)

| Decisión | Elección |
|----------|----------|
| Naturaleza | **Episodio de atención ocurrido** (o registrado como ocurrido) — no Appointment, no chart, no nota |
| Aggregate Root | **`Encounter`** |
| BC propietario | **Clinical Records** |
| Tenant | **Siempre** — `TenantId` obligatorio e **inmutable** |
| Subject | **`PatientId` obligatorio** — validar ACTIVE vía `PatientReferencePort` |
| Provider | **`StaffAssignmentId` obligatorio** — nunca `MembershipId` / `IdentityId` |
| Lugar de negocio | **`OrganizationId` obligatorio** (denormalizado; coherencia con StaffAssignment) |
| Lugar físico | **`OfficeId` opcional** — si presente → ACTIVE ∈ org |
| Origen planificado | **`AppointmentId` opcional** — walk-in sin cita |
| Tiempo | **`startedAt` obligatorio**; **`endedAt` opcional** mientras `IN_PROGRESS`; obligatorio al `complete` |
| Unicidad dura | Solo **`EncounterId` (UUID)** |
| Lifecycle v1 | `IN_PROGRESS` → `CANCELLED` \| `COMPLETED` — **sin delete físico** |
| Notas / SOAP / diagnósticos / imágenes | **Fuera** — aggregates futuros referencian `EncounterId` |
| MedicalRecord chart bag | **Fuera de v1** — no es este root |
| Auto-crear Encounter al completar Appointment | **Prohibido** |
| Auto-completar Appointment al completar Encounter | **Prohibido** |
| Módulo / schema (propuesta) | `encounter-management` · schema `records` |
| HTTP (propuesta) | `/api/v1/records/encounters` |

Borrador formal: **ADR-015 — Encounter Domain Model** — ✅ **Accepted** en [PASO-19.1](PASO-19.1-ENCOUNTER-MODEL-CONTRACT.md).

---

## 1. Naturaleza del Aggregate

### ¿Qué representa Encounter?

**Encounter es el episodio de atención que ocurrió (o se registra como ocurrido) para un care subject, con un proveedor operativo y un contexto de organización (y opcionalmente office y appointment de origen).**

| Interpretación | ¿Es? | Por qué |
|----------------|------|---------|
| ¿Una visita / sesión / atención real? | **Sí** | Definición operativa |
| ¿Una cita / booking? | **No** | Eso es `Appointment` (planificado) |
| ¿Una historia clínica longitudinal? | **No** | Chart = proyección o aggregate futuro |
| ¿Una nota SOAP / documento? | **No** | Artefacto documental — cuelga de Encounter |
| ¿Un EpisodeOfCare (embarazo, oncología)? | **No** | Multi-encuentro longitudinal — otro aggregate si hace falta |
| ¿Algo dental / vet específico? | **No** | Core Platform |

**Regla de exclusividad:** dentro del Tenant, el **episodio de atención ocurrido** vive en Clinical Records como `Encounter`. Ningún vertical debe crear un “DentalVisit” / “VetConsult” paralelo en el Core que duplique ese rol — los verticales **referencian** `EncounterId`.

**Intentionally small:** Encounter no crece con odontograma, SOAP, consentimientos, cargos, labs ni adjuntos.

---

## 2. Aggregate Root

### ¿Por qué Encounter debe ser Aggregate Root?

- Boundary transaccional del **episodio** (quién, a quién, dónde, cuándo ocurrió, estado).  
- Ciclo de vida propio (open → cancel / complete).  
- `EncounterId` estable para Notes / Labs / Billing / Consent futuros.  
- Sus invariantes **no** incluyen consistencia de demografía Patient ni del contenido documental (política §5).

### Principio de permanencia (para ADR-015)

> **Encounter is intentionally small.**

Decisión **permanente** (no limitación de FASE 19): Encounter solo representa el episodio ocurrido + invariantes propias. Notas, SOAP, diagnósticos, tratamientos, imágenes, odontograma, prescriptions y billing viven en **otros** aggregates. Embebidos en Encounter = violación arquitectónica (**God Aggregate**).

### ¿Por qué no MedicalRecord?

MedicalRecord-as-chart absorbe documentación longitudinal → God Aggregate. Sin episodio, las notas quedan huérfanas. Encounter es la frontera correcta; el “chart” es Patient + Encounters + docs (query) o un aggregate posterior delgado.

### ¿Por qué no Appointment?

Appointment responde “¿qué está **comprometido**?”. Encounter responde “¿qué **ocurrió**?”. Walk-in: Encounter sin Appointment. No-show: Appointment sin Encounter.

### ¿Por qué no ClinicalDocument / Note?

Documento ≠ episodio. Muchos documentos por Encounter. Ownership documental distinto.

### ¿Por qué no EpisodeOfCare / Visit / Consultation?

Ya descartados en [PASO-19.0](PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md) §2 — ambigüedad, sesgo vertical o alcance longitudinal prematuro.

---

## 3. Ownership

| Rol | Actor / BC |
|-----|------------|
| **Propietario del modelo** | Bounded Context **Clinical Records** (`encounter-management`) |
| **Quién crea (open)** | Membership con `encounter:create` |
| **Quién modifica** (tiempo / refs mientras abierto) | `encounter:update` |
| **Quién cancela** | `encounter:cancel` |
| **Quién completa** | `encounter:update` (espejo Appointment `complete` → `:update`) — cerrar en 19.5 |
| **Quién solo consulta** | `encounter:read` |
| **Quién NO es dueño** | IAM, Organization, Clinical Foundation, Scheduling, Billing |

**Borrador permisos (19.5):**  
`encounter:read` · `encounter:create` · `encounter:update` · `encounter:cancel`  

`complete` → `encounter:update`. **No** verbos verticales (`encounter:surgery`, `encounter:hygiene`).

Patient / Appointment / Org **no** poseen Encounter. Solo son referenciados.

---

## 4. Bounded Context

| Pregunta | Respuesta |
|----------|-----------|
| ¿BC? | **Clinical Records** |
| ¿Parte de Clinical Foundation? | **No** — Patient cerró FASE 17 |
| ¿Parte de Scheduling? | **No** — Appointment cerró FASE 18 |
| ¿Módulo Gradle? | `modules/encounter-management/` (propuesta) |
| ¿Schema? | `records` (propuesta) — **no** meter episodios en `clinical` (Patient) ni `scheduling` |

```text
clinical.*     → Patient (registry)           FOUNDATION CLOSED
scheduling.*   → Appointment (planned)        SCHEDULING CLOSED
records.*      → Encounter (occurred)         CLINICAL RECORDS
org.*          → Organization / Office / StaffAssignment
```

Clinical Records puede crecer después con `ClinicalDocument` / Note **en el mismo BC** (mismo patrón Org→Office), siempre referenciando `EncounterId` — sin engordar Encounter.

---

## 5. Tenant

| Pregunta | Respuesta |
|----------|-----------|
| ¿Encounter pertenece al Tenant? | **Sí** |
| ¿Siempre? | **Sí** |
| ¿Puede cambiar de Tenant? | **Nunca** |

`TenantId` se fija en `create` y es inmutable. Cross-tenant → 404.

---

## 6. Patient (care subject)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Encounter referencia Patient? | **Sí — obligatorio** |
| ¿Cómo valida? | `PatientReferencePort.existsActiveByIdAndTenant` en **escritura** (open / reassign patient) |
| ¿Patient conoce Encounter? | **No** — sin colección inversa |

**Regla:** Patient archivado **bloquea nuevos** encounters; encounters históricos siguen legibles.

**Prohibido:** copiar demographics; usar `PatientRepository`.

---

## 7. StaffAssignment (quién atendió)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Quién “atendió”? | **`StaffAssignmentId` obligatorio** |
| ¿Por qué no MembershipId / IdentityId? | ADR-011 — scope operativo, no acceso al tenant |

Validación escritura: `StaffAssignmentReferencePort.findScopeByIdAndTenant` + coherencia (misma regla que Appointment ADR-014 §7):

1. `encounter.organizationId` **debe** igualar `staffAssignment.organizationId`.  
2. Si assignment tiene `officeId` → `encounter.officeId` **debe** igualar ese office.  
3. Si assignment es org-wide (`officeId` null) → `encounter.officeId` puede ser null o cualquier office ACTIVE de esa org.

StaffAssignment delete físico: encounters históricos conservan el UUID (labels en read model futuro).

---

## 8. Organization & Office

| Referencia | Obligatoria | Rol |
|------------|-------------|-----|
| `OrganizationId` | **Sí** | Contexto / custodia operativa del episodio |
| `OfficeId` | **No** | Sitio / sala / box — “dónde ocurrió” |

Validación: Org ACTIVE; Office si presente ACTIVE ∈ org.

**Denormalización de `OrganizationId`:** normativa (espejo Appointment) — listados por org sin join cross-BC; coherencia write-time con StaffAssignment.

**No** sustituir por `Patient.primaryOrganizationId` automáticamente.

---

## 9. Appointment (origen opcional)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Encounter requiere Appointment? | **No** — walk-in / atención espontánea |
| ¿Puede referenciar Appointment? | **Sí — opcional** `AppointmentId` |
| ¿Completar Appointment crea Encounter? | **No** (ADR-014) |
| ¿Completar Encounter completa Appointment? | **No** — BCs desacoplados |

### Validación write-time (si `AppointmentId` presente)

| Regla | Decisión v1 |
|-------|-------------|
| Existe en tenant | **Sí** — obligatorio |
| ¿Solo `SCHEDULED`? | **No** — documentar tras `COMPLETED` es caso real |
| ¿Permitir `CANCELLED`? | **No** — bloquear **nuevos** enlaces a citas canceladas |
| Port hoy | `existsScheduledByIdAndTenant` **insuficiente** (excluye COMPLETED) |

**Acción PASO 19.2 (just-in-time, no “por si acaso”):** evolucionar `AppointmentReferencePort` con superficie mínima que permita:

- existencia en tenant, **y**  
- status ∈ {`SCHEDULED`, `COMPLETED`}  

Opciones ADR-013 (elegir una en 19.1/19.2):

1. `existsLinkableForEncounter(AppointmentId, TenantId) → boolean`, o  
2. Small view `{ status }` + boolean existence.

**No** reabrir ADR-014. **No** SQL a `scheduling` desde Records.

Appointment e Encounter pueden divergir en org/office/staff respecto a la cita original (reagenda / cobertura) — coherencia Encounter↔Appointment **no** es invariante dura de igualdad de todos los IDs; solo tenant + patient típico. **Recomendación:** en create, si hay `AppointmentId`, validar que `appointment.patientId` == `encounter.patientId` (requiere view mínima o confiar en UI — **cerrar en ADR-015**: preferir view/`resolve` mínimo con `patientId` + `status` si la invariante patient-match es normativa).

**Recomendación audit para ADR-015:**

| Campo del port view (máximo) | ¿Incluir? |
|------------------------------|-----------|
| `status` | **Sí** — linkable rule |
| `patientId` | **Sí** — debe coincidir con Encounter.patientId |
| demografía / ventana de cita | **No** |

Si 19.1 decide que patient-match es solo application soft-check, el port puede quedarse en boolean linkable — documentar trade-off.

---

## 10. Tiempo

| Campo | Decisión v1 |
|-------|-------------|
| `startedAt` | `Instant` **obligatorio** (UTC) |
| `endedAt` | `Instant` **opcional** mientras `IN_PROGRESS`; **obligatorio** al pasar a `COMPLETED` |
| Zona horaria | No persistir TZ en aggregate v1 |
| All-day / overnight policies | Fuera de v1 (application/product) |

**Invariantes de tiempo:**

1. Si `endedAt` está presente → `endedAt` ≥ `startedAt` (igualdad permitida para episodios instantáneos / registro mínimo).  
2. `complete` exige `endedAt` (si null en comando, application puede fijar `now`).  
3. `cancel` **no** exige `endedAt`.

Retrospective documentation (alta de episodio pasado con ambos timestamps) → create directo en `IN_PROGRESS` luego `complete`, **o** create ya con `endedAt` + transición a `COMPLETED` en el mismo use case — detalle de API en 19.5.1; dominio permite ambos timestamps en create.

---

## 11. Lifecycle

```text
(create/open) → IN_PROGRESS
                   ├── cancel   → CANCELLED   (terminal)
                   ├── complete → COMPLETED   (terminal; endedAt required)
                   └── update refs / tiempo     (permanece IN_PROGRESS)
```

| Transición | Cuándo | Efecto |
|------------|--------|--------|
| **open (create)** | Inicio o registro del episodio | Estado `IN_PROGRESS`; refs + `startedAt` tras ports |
| **update** | Ajuste de tiempo / office / staff / patient / appointment link | Solo desde `IN_PROGRESS`; re-validar ports |
| **cancel** | Episodio abortado / abierto por error | `CANCELLED` |
| **complete** | Atención cerrada operativamente | `COMPLETED`; `endedAt` fijado |
| **delete físico** | **Fuera de v1** | Retención |
| **reactivar** | **Fuera de v1** | Nuevo Encounter si hace falta |

**Por qué no ACTIVE/ARCHIVED?**  
Encounter es proceso operativo con terminales semánticos (cancel vs complete), no entidad estructural soft-archive.

**Por qué no PLANNED?**  
Planificar es Appointment. Un Encounter “planned” duplicaría Scheduling.

**Por qué no ARRIVED / ON_LEAVE / FHIR completo?**  
Sobreingeniería Core v1. Product packs pueden mapear subestados encima.

---

## 12. Relación con Appointment & documentación futura

| Dirección | Decisión |
|-----------|----------|
| Appointment → Encounter | **No** embebido; no auto-create |
| Encounter → Appointment | `AppointmentId?` opcional |
| Encounter → Note / SOAP / Lab | Futuro: ellos guardan `EncounterId` |
| Encounter → MedicalRecord bag | **Jamás** embebido |

---

## 13. Identidad y unicidad

| Mecanismo | Rol |
|-----------|-----|
| `EncounterId` (UUID) | **Única clave dura** |
| “Un Encounter por Appointment” | **No** unique key en v1 — un appointment podría mapear 0..n encounters en casos edge; producto puede imponer 0..1 |
| External visit numbers | Opcional futuro — no v1 |

---

## 14. Referencias (solo IDs)

| Referencia | ¿Encounter la mantiene? | Validación escritura |
|------------|-------------------------|----------------------|
| `TenantId` | **Sí (obligatorio, inmutable)** | JWT |
| `PatientId` | **Sí (obligatorio)** | `PatientReferencePort` |
| `StaffAssignmentId` | **Sí (obligatorio)** | `StaffAssignmentReferencePort` |
| `OrganizationId` | **Sí (obligatorio)** | `OrganizationReferencePort` + coherencia assignment |
| `OfficeId` | **Sí (opcional)** | `OfficeReferencePort` |
| `AppointmentId` | **Sí (opcional)** | Port Scheduling evolucionado (19.2) |
| `MembershipId` / `IdentityId` | **Jamás** | — |
| `MedicalRecordId` / NoteId / … | **Jamás (v1)** | — |
| Colección de notas / procedimientos | **Jamás** | — |

Encounter **nunca** carga aggregates externos. Validación en **application** vía ports.

---

## 15. Invariantes de dominio

1. Todo Encounter tiene exactamente un `TenantId`, asignado en create — **inmutable**.  
2. Estados válidos solo: `IN_PROGRESS` \| `CANCELLED` \| `COMPLETED`.  
3. `cancel` / `complete` / mutaciones solo desde `IN_PROGRESS` (terminales no vuelven atrás en v1).  
4. `startedAt` siempre presente; si `endedAt` presente → `endedAt` ≥ `startedAt`.  
5. `complete` requiere `endedAt`.  
6. `PatientId`, `StaffAssignmentId`, `OrganizationId` siempre presentes.  
7. En create/update (mientras `IN_PROGRESS`): Patient ACTIVE; Organization ACTIVE; StaffAssignment coherente (§7); Office si presente ACTIVE ∈ org; Appointment si presente linkable (§9).  
8. Encounter **no** embebe ni muta Patient, Appointment, Org, Office, StaffAssignment, Notes, Billing.  
9. `EncounterId` no se reasigna.  
10. Cross-tenant imposible.  
11. Completar Encounter **no** muta Appointment; crear Encounter **no** nace desde Scheduling automáticamente.

---

## 16. Multi-organization / multi-office

| Escenario | Soporte |
|-----------|---------|
| Episodio en Org A / Office A1 | ✅ |
| Mismo Patient, encounters en A y B | ✅ |
| Walk-in sin Appointment | ✅ |
| Encounter ligado a Appointment COMPLETED | ✅ (tras 19.2 port) |
| Primary org Patient ≠ org del Encounter | ✅ permitido |

---

## 17. Escenarios multi-vertical

| Vertical | ¿Soportado sin cambiar root? | Notas |
|----------|------------------------------|-------|
| Dental | ✅ | Office = sillón; sin odontograma |
| Veterinaria | ✅ | Patient = animal |
| Hospital | ✅ | Org = servicio; Office = consultorio/cama lógica futura fuera |
| Laboratorio | ✅ | Episodio de toma / atención |
| Psicología / Fisio | ✅ | Sesión = Encounter |
| ERP de servicios | ✅ | Si el sujeto es `PatientId`; no es Order/Invoice |

---

## 18. Escalabilidad

| Requisito | Diseño |
|-----------|--------|
| Millones de episodios | Aggregate plano; sin colecciones hijas; índices `(tenant_id, started_at)`, `(tenant_id, patient_id)`, `(tenant_id, staff_assignment_id)`, `(tenant_id, organization_id)`, `(tenant_id, appointment_id)` parcial |
| Multi-tenant | `tenant_id` + JWT |
| Listados día / profesional | Query layer — no inflate domain |
| Evolución | Notes / Labs / EpisodeOfCare sin romper `EncounterId` |

---

## 19. Extensibilidad (sin modificar Encounter)

| Futuro | Cómo usa Encounter |
|--------|--------------------|
| Clinical Note / SOAP | `EncounterId` obligatorio o fuerte |
| Lab Result / Observation | `EncounterId?` o PatientId según BC |
| Consent / Attachment | Referencia Encounter o Patient |
| Billing | Cargo por episodio completado → `EncounterId` |
| Vertical packs | Metadatos **fuera** del Core aggregate |
| EpisodeOfCare | Referencia muchos `EncounterId` — nuevo aggregate |

---

## 20. Reference Contracts — trabajo FASE 19.2

| Port | Estado hoy | Acción 19.2 |
|------|------------|-------------|
| `PatientReferencePort` | ✅ | Usar |
| `OrganizationReferencePort` | ✅ | Usar |
| `OfficeReferencePort` | ✅ | Usar |
| `StaffAssignmentReferencePort` | ✅ | Usar |
| `AppointmentReferencePort` | ✅ solo `existsScheduled…` | **Evolucionar** para link SCHEDULED\|COMPLETED (+ patientId si ADR-015 lo exige) |
| `EncounterReferencePort` | Ausente | **Closeout 19.8** (no 19.2) |

**No** modificar aggregates Appointment/Patient/Org. Solo contract + adapter Scheduling si aplica.

---

## 21. Naming & packaging (propuesta → ADR-015)

| Elemento | Propuesta |
|----------|-----------|
| Bounded Context | **Clinical Records** |
| Módulo Gradle | `modules/encounter-management/` |
| Schema SQL | `records` |
| Tabla | `records.encounter` |
| HTTP | `/api/v1/records/encounters` |
| OpenAPI group | `records-administration` (closeout) |
| Permisos | `encounter:*` |

---

## 22. Riesgos y gaps conscientes

| Gap | Severidad | Mitigación |
|-----|-----------|------------|
| Appointment port insuficiente para COMPLETED | Media | **19.2** just-in-time |
| 0..n Encounters por Appointment | Baja | Sin unique; producto impone 0..1 si quiere |
| Sin subestados FHIR (arrived, …) | Baja | Diferido / packs |
| StaffAssignment delete vs históricos | Media | Conservar UUID; labels futuros |
| Expandir Encounter con clínica | **Crítica** | ADR-015 frozen + intentionally small |
| Confundir Clinical Records con EHR completo | Alta | BC empieza en Encounter; docs después |

Ningún gap obliga a reabrir IAM / Org / Patient / Appointment / ADR-010…014.

---

## 23. Comparación con patrones existentes

| Aspecto | Patient | Appointment | Encounter |
|---------|---------|-------------|-----------|
| Naturaleza | Registry | Compromiso planificado | Episodio ocurrido |
| Estados | ACTIVE / ARCHIVED | SCHEDULED / CANCELLED / COMPLETED | IN_PROGRESS / CANCELLED / COMPLETED |
| Delete físico | No | No | No |
| Soft archive | Sí | No (cancel/complete) | No (cancel/complete) |
| Tiempo | No ventana operativa | startsAt/endsAt required | startedAt required; endedAt on complete |
| Consumer ports | Org | Patient+Org+Office+Staff | Patient+Org+Office+Staff+Appointment? |

Consistencia de **plataforma** (IDs, ports, tenant, HTTP admin, permisos) — lifecycle refleja dominio, no copia ciega.

---

## Auditorías / ADR siguientes

| Paso | Acción |
|------|--------|
| **19.1** | Aceptar **ADR-015 — Encounter Domain Model** (congelar) |
| **19.2** | Evolución mínima `AppointmentReferencePort` si 19.1 confirma link COMPLETED |
| **19.5.1** | Encounter Admin API Audit |

---

## Referencias

- [PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md](PASO-19.0-CLINICAL-RECORDS-FOUNDATION-PLANNING.md)  
- [PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md](PASO-18.0.1-APPOINTMENT-AGGREGATE-AUDIT.md) — plantilla de rigor  
- [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md)  
- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md)  
- [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
