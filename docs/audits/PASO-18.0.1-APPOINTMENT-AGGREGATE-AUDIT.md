# PASO 18.0.1 — Appointment Aggregate Audit (DDD Estratégico)

**Appointment** es el compromiso planificado de atención — *intentionally small*, multi-vertical, y el primer consumer real del grafo Patient + Org + Office + StaffAssignment.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado (solo arquitectura)  
**Tipo:** Auditoría obligatoria — Aggregate Root nuevo + Bounded Context Scheduling  
**Dependencias:** [PASO-18.0](PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md) · ADR-011 · ADR-012 · ADR-013 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md) · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)

---

## Objetivo

Definir el modelo correcto del Aggregate Root **`Appointment`** para que CodeCore, como Core Platform, agenda atención en dental, veterinaria, hospital, laboratorio, psicología, fisioterapia y verticales futuros — **sin** acoplar Scheduling a un producto concreto.

**Sin código. Sin tablas. Sin endpoints. Sin migraciones.**

---

## Checklist política (§8) — verdicto previo

| # | Ítem | ✓ | Nota |
|---|------|---|------|
| 1 | Aggregate Root identificado | ✅ | `Appointment` |
| 2 | Ownership definido | ✅ | BC Scheduling |
| 3 | Invariantes definidas | ✅ | § Invariantes |
| 4 | Lifecycle definido | ✅ | § Lifecycle |
| 5 | Estados definidos | ✅ | `SCHEDULED` · `CANCELLED` · `COMPLETED` |
| 6 | Permisos definidos | ✅ | Borrador `appointment:*` |
| 7 | Relaciones solo mediante IDs | ✅ | § Referencias |
| 8 | Bounded Context correcto | ✅ | Scheduling (≠ Clinical Foundation) |
| 9 | No rompe ADR vigentes | ✅ | 003/006/007/010–013 intactos; BCs cerrados |
| 10 | Escalable multi-tenant | ✅ | |
| 11 | Escalable multi-organization | ✅ | |
| 12 | Escalable millones de registros | ✅ | Aggregate delgado; sin hijos clínicos |

**Veredicto:** checklist en verde → **ADR-014 Accepted** ([PASO-18.1](PASO-18.1-APPOINTMENT-MODEL-CONTRACT.md)).

---

## Decisiones irreversibles (resumen ejecutivo)

| Decisión | Elección |
|----------|----------|
| Naturaleza | **Compromiso planificado de atención** en el tiempo (no Encounter, no Record, no Slot inventory) |
| Aggregate Root | **`Appointment`** |
| BC propietario | **Scheduling** |
| Tenant | **Siempre** — `TenantId` obligatorio e **inmutable** |
| Subject | **`PatientId` obligatorio** — validar ACTIVE vía `PatientReferencePort` |
| Provider | **`StaffAssignmentId` obligatorio** — nunca `MembershipId` / `IdentityId` |
| Lugar de negocio | **`OrganizationId` obligatorio** |
| Lugar físico | **`OfficeId` opcional** — sala / sitio; si presente → ACTIVE ∈ org |
| Tiempo | Ventana **`startsAt` + `endsAt`** (instants) — `endsAt > startsAt` |
| Unicidad dura | Solo **`AppointmentId` (UUID)** |
| Lifecycle v1 | `SCHEDULED` → `CANCELLED` \| `COMPLETED` — **sin delete físico** |
| Double-booking hard | **Fuera de v1** (política de capacidad diferida) |
| Recurrencia / waitlist / slots | **Fuera de v1** |
| Encounter / MedicalRecord | **Fuera** — otros BC; Appointment no los embebe |
| Módulo / schema (propuesta) | `appointment-management` · schema `scheduling` |
| HTTP (propuesta) | `/api/v1/scheduling/appointments` |

Borrador formal: **ADR-014 — Appointment Domain Model** — ✅ **Accepted** en [PASO-18.1](PASO-18.1-APPOINTMENT-MODEL-CONTRACT.md).

---

## 1. Naturaleza del Aggregate

### ¿Qué representa Appointment?

**Appointment es el compromiso planificado de atender a un care subject en un intervalo de tiempo, con un proveedor operativo y un contexto de organización (y opcionalmente office).**

| Interpretación | ¿Es? | Por qué |
|----------------|------|---------|
| ¿Una cita / booking? | **Sí** | Definición operativa |
| ¿Un Encounter? | **No** | Encounter = episodio **ocurrido** (tiempo real, hallazgos) |
| ¿Una historia clínica? | **No** | MedicalRecord documenta; Appointment agenda |
| ¿Disponibilidad / cupo? | **No (v1)** | Inventario de capacidad es otro problema |
| ¿Un evento de calendario genérico? | **Parcialmente** | Es un evento de agenda **clínico-operativo**, no un calendar engine |

**Regla de exclusividad:** dentro del Tenant, el **compromiso planificado de atención** vive en Scheduling como `Appointment`. Ningún vertical debe crear un “DentalAppointment” / “VetVisit” paralelo en el Core — los verticales **referencian** o **extienden** sin duplicar el root.

**Intentionally small:** Appointment no crece con odontograma, consentimientos, cargos, ni notas clínicas.

---

## 2. Aggregate Root

### ¿Por qué Appointment debe ser Aggregate Root?

- Boundary transaccional del **compromiso** (quién, a quién, dónde, cuándo, estado).  
- Ciclo de vida propio (schedule → cancel / complete).  
- `AppointmentId` estable para Encounter / Billing / notificaciones futuras.  
- Sus invariantes **no** incluyen consistencia de Patient demographics ni del MedicalRecord (política §5).

### ¿Por qué no Encounter?

Encounter responde “¿qué ocurrió en la atención?”. Nace al realizarse (o documentarse) la visita. Muchos encounters pueden existir sin appointment previo (walk-in). Appointment puede existir sin encounter (no-show / cancel).

### ¿Por qué no Slot / Availability?

Slot modela **capacidad**. Exige políticas de overbooking, recursos múltiples, buffers — tipicamente verticales. Introducirlo como root en v1 **sobreingenieriza** el Core. Si un producto lo necesita, se audita como aggregate/BC satélite **después** de Appointment estable.

### ¿Por qué no Schedule / Calendar?

Son **vistas** (queries) sobre Appointments filtrados por staff/office/día — no ownership de verdad.

### ¿Por qué no “Session” / “Booking” genérico ERP?

El nombre **Appointment** es el ubicuo en salud multi-vertical y ya está prescrito en ADR-011 / consumption guides. Un Booking ERP genérico diluiría el BC Scheduling clínico-operativo de CodeCore.

---

## 3. Ownership

| Rol | Actor / BC |
|-----|------------|
| **Propietario del modelo** | Bounded Context **Scheduling** (`appointment-management`) |
| **Quién crea** | Membership con `appointment:create` |
| **Quién modifica** (reprogramar / cambiar refs permitidas) | `appointment:update` |
| **Quién cancela** | `appointment:cancel` (o `update` + operación cancel — cerrar en 18.5) |
| **Quién completa** | `appointment:complete` **o** `appointment:update` — **cerrar en 18.5** (preferencia: permiso dedicado `cancel` + `complete` bajo lifecycle, o `update` para ambos no-create) |
| **Quién solo consulta** | `appointment:read` |
| **Quién NO es dueño** | IAM, Organization, Clinical Foundation, Billing, Records |

**Borrador permisos (18.5):**  
`appointment:read` · `appointment:create` · `appointment:update` · `appointment:cancel`  

`complete` puede mapearse a `appointment:update` (espejo activate→update) **o** a `appointment:cancel`-style verb — decisión en Authorization Contract; **no** inventar verbos verticales (`appointment:chair`, `appointment:surgery`).

Patient / Org **no** poseen Appointment. Solo son referenciados.

---

## 4. Bounded Context

| Pregunta | Respuesta |
|----------|-----------|
| ¿BC? | **Scheduling** |
| ¿Parte de Clinical Foundation? | **No** — Patient ya cerró FASE 17; Appointment es consumer |
| ¿Parte de Organization? | **No** — Org no agenda pacientes |
| ¿Módulo Gradle? | `modules/appointment-management/` (propuesta) |
| ¿Schema? | `scheduling` (propuesta) — **no** meter tablas de agenda en `clinical` ni `org` |

```text
clinical.*     → Patient (registry)
org.*          → Organization / Office / StaffAssignment
scheduling.*   → Appointment
```

---

## 5. Tenant

| Pregunta | Respuesta |
|----------|-----------|
| ¿Appointment pertenece al Tenant? | **Sí** |
| ¿Siempre? | **Sí** |
| ¿Puede cambiar de Tenant? | **Nunca** |

`TenantId` se fija en `create` y es inmutable. Cross-tenant → 404.

---

## 6. Patient (care subject)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Appointment referencia Patient? | **Sí — obligatorio** |
| ¿Cómo valida? | `PatientReferencePort.existsActiveByIdAndTenant` en **escritura** (schedule / reassign patient) |
| ¿Patient conoce Appointment? | **No** — sin colección inversa |

**Regla ADR-011 / Patient guide:** Patient archivado **bloquea nuevas** citas; citas históricas siguen legibles.

**Prohibido:** copiar demographics al aggregate como source of truth; usar `PatientRepository`.

---

## 7. StaffAssignment (provider)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Quién “atiende”? | **`StaffAssignmentId` obligatorio** |
| ¿Por qué no MembershipId? | ADR-011: Membership = acceso al tenant; Assignment = **dónde opera** |
| ¿Por qué no IdentityId? | Persona ≠ alcance operativo multi-org |

Validación escritura: `StaffAssignmentReferencePort` (a publicar en `organization-contract`, PASO 18.2):

- Existe en el tenant  
- Su `organizationId` es coherente con el `OrganizationId` del Appointment  
- Si el assignment tiene `officeId` no nulo y el Appointment declara `officeId`, deben ser compatibles (mismo office **o** documentar regla: appointment.office puede ser más específico solo si assignment es org-wide — **cerrar en ADR-014**)

**Propuesta de regla de coherencia (recomendación audit):**

1. `appointment.organizationId` **debe** igualar `staffAssignment.organizationId`.  
2. Si `staffAssignment.officeId` está presente → `appointment.officeId` debe ser **igual** o **ausente no permitido** (forzar el office del assignment).  
3. Si `staffAssignment.officeId` es null (org-wide) → `appointment.officeId` puede ser null o cualquier office ACTIVE de esa org.

StaffAssignment se **borra físicamente** al revocar (ADR-011). Nuevas citas no pueden usar assignment inexistente; citas históricas conservan el UUID (label en read model / snapshot opcional futuro).

---

## 8. Organization & Office

| Referencia | Obligatoria | Rol |
|------------|-------------|-----|
| `OrganizationId` | **Sí** | Contexto de negocio de la cita (clínica / sede / servicio) |
| `OfficeId` | **No** | Sitio / sala / box — “dónde ocurre” |

Validación:

- Org ACTIVE vía `OrganizationReferencePort`  
- Si hay Office: `OfficeReferencePort.existsActiveInOrganization(officeId, organizationId, tenantId)`

**No** usar primary organization del Patient como sustituto automático sin validar — puede ser hint de UI, no invariante del aggregate.

---

## 9. Tiempo

| Campo | Decisión v1 |
|-------|-------------|
| `startsAt` | `Instant` obligatorio |
| `endsAt` | `Instant` obligatorio |
| Zona horaria | **No** persistir TZ en aggregate v1 — instants UTC; presentación en app |
| All-day | **Fuera de v1** |
| Buffer / travel time | **Fuera de v1** |

**Invariante:** `endsAt` > `startsAt`.  
Duración máxima / mínima: opcional en application (config tenant) — **no** hardcodear vertical.

---

## 10. Lifecycle

```text
(create/schedule) → SCHEDULED
                      ├── cancel  → CANCELLED   (terminal)
                      ├── complete → COMPLETED  (terminal)
                      └── reschedule / update refs  (permanece SCHEDULED)
```

| Transición | Cuándo | Efecto |
|------------|--------|--------|
| **schedule (create)** | Alta de compromiso | Estado `SCHEDULED`; refs + ventana fijadas tras validar ports |
| **update / reschedule** | Cambio de tiempo y/o office/staff/patient (si permitido) | Solo desde `SCHEDULED`; re-validar ports en escritura |
| **cancel** | Paciente / clínica anula | `CANCELLED`; no reaparece en listados “abiertos” |
| **complete** | Atención cumplida (admin/operativo) | `COMPLETED`; puente futuro a Encounter **sin** crear Encounter aquí |
| **delete físico** | **Fuera de v1** | Retención / auditoría |
| **reactivar cancelada** | **Fuera de v1** (o `SCHEDULED` solo vía nuevo Appointment) | Evitar resurrect ambiguo |

**Por qué no ACTIVE/ARCHIVED como Org?**  
Appointment no es entidad estructural soft-archive; es **proceso operativo** con terminales semánticos (cancel vs complete). Forzar archive/activate confundiría operadores y APIs.

**No-show:** puede ser `CANCELLED` con motivo tipado **o** estado futuro `NO_SHOW`. **v1:** diferir `NO_SHOW` — usar `CANCELLED` + `cancellationReason` opcional (VO string corto) si se necesita trazabilidad mínima.

---

## 11. Double-booking y capacidad

| Pregunta | Decisión v1 |
|----------|-------------|
| ¿El aggregate prohíbe solapes del mismo StaffAssignment? | **No** (hard constraint diferida) |
| ¿Por qué? | Políticas de overbooking / salas compartidas / multi-chair son **de producto**; meterlas en el Core ahora es verticalizar |
| ¿Qué sí? | Queries de listado por staff/office/rango para que la UI detecte conflictos |
| ¿Futuro? | Política opcional / BC Availability — **nueva auditoría** si se endurece |

**Anti-sobreingeniería consciente:** no Slot engine, no resource calendar graph en FASE 18.

---

## 12. Encounter & Clinical Record

| Dirección | Decisión |
|-----------|----------|
| Appointment → Encounter | **No en v1** — sin `EncounterId` embebido |
| Encounter → Appointment | Futuro: Encounter puede guardar `AppointmentId?` opcional |
| Appointment → MedicalRecord | **Jamás** |

Completar un Appointment **no** crea Encounter automáticamente en Scheduling (evita acoplar BCs). Orquestación futura = application de otro BC o proceso diferido.

---

## 13. Identidad y unicidad

| Mecanismo | Rol |
|-----------|-----|
| `AppointmentId` (UUID) | **Única clave dura** |
| External confirmation codes | Opcional futuro — no v1 |
| “Misma persona misma hora” | No unique key — cubierto por política de capacidad diferida |

---

## 14. Referencias (solo IDs)

| Referencia | ¿Appointment la mantiene? | Validación escritura |
|------------|---------------------------|----------------------|
| `TenantId` | **Sí (obligatorio, inmutable)** | JWT |
| `PatientId` | **Sí (obligatorio)** | `PatientReferencePort` |
| `StaffAssignmentId` | **Sí (obligatorio)** | `StaffAssignmentReferencePort` |
| `OrganizationId` | **Sí (obligatorio)** | `OrganizationReferencePort` |
| `OfficeId` | **Sí (opcional)** | `OfficeReferencePort` |
| `MembershipId` / `IdentityId` | **Jamás** | — |
| `EncounterId` / `MedicalRecordId` | **Jamás (v1)** | — |
| Colección de notas / procedimientos | **Jamás** | — |

Appointment **nunca** carga aggregates externos (política §4). Validación en **application** vía ports.

---

## 15. Invariantes de dominio

1. Todo Appointment tiene exactamente un `TenantId`, asignado en create — **inmutable**.  
2. Estados válidos solo: `SCHEDULED` \| `CANCELLED` \| `COMPLETED`.  
3. `cancel` / `complete` / `update` mutaciones de compromiso solo desde `SCHEDULED` (terminales no vuelven atrás en v1).  
4. `endsAt` > `startsAt`.  
5. `PatientId`, `StaffAssignmentId`, `OrganizationId` siempre presentes.  
6. En create/update (mientras SCHEDULED): Patient ACTIVE en tenant; Organization ACTIVE; StaffAssignment existe y es coherente con org (regla §7); Office si presente ACTIVE ∈ org.  
7. Appointment **no** garantiza unicidad temporal del staff ni del office.  
8. Appointment **no** embebe ni muta Patient, Org, Office, StaffAssignment, Encounter, MedicalRecord.  
9. `AppointmentId` no se reasigna.  
10. Cross-tenant imposible.

---

## 16. Multi-organization / multi-office

```text
Tenant
 ├── Organization A
 │     └── Office A1
 └── Organization B
```

| Escenario | Soporte |
|-----------|---------|
| Cita en Org A / Office A1 | ✅ refs en Appointment |
| Mismo Patient, citas en A y B | ✅ un PatientId; citas con distinto OrganizationId |
| Staff org-wide vs office-bound | ✅ vía StaffAssignment + reglas §7 |
| Primary org del Patient ≠ org de la cita | ✅ permitido |

**Anti-patrón:** un Appointment por “fila de agenda dental” sin PatientId.

---

## 17. Escenarios multi-vertical

| Vertical | ¿Soportado sin cambiar root? | Notas |
|----------|------------------------------|-------|
| Dental | ✅ | Office = sillón/box opcional; sin odontograma |
| Veterinaria | ✅ | Patient = animal; mismo Appointment |
| Hospital | ✅ | Org = servicio; Office = consultorio |
| Laboratorio | ✅ | Cita de toma de muestra |
| Psicología / Fisio / Oftalmología | ✅ | Igual patrón ambulatorio |
| ERP / CRM genérico | ⚠️ | Si el producto no es “atención a care subject”, puede no usar este BC — no forzar |

---

## 18. Escalabilidad

| Requisito | Diseño |
|-----------|--------|
| Millones de citas | Aggregate plano; sin colecciones hijas; índices `(tenant_id, starts_at)`, `(tenant_id, staff_assignment_id, starts_at)`, `(tenant_id, patient_id, starts_at)` |
| Multi-tenant | `tenant_id` + JWT |
| Listados día / profesional | Query layer — no inflate domain |
| Evolución | Extensiones / Availability BC sin romper `AppointmentId` |

---

## 19. Extensibilidad (sin modificar Appointment)

| Futuro | Cómo usa Appointment |
|--------|----------------------|
| Encounter | `AppointmentId?` opcional |
| Notification | Lee Appointment por id / query |
| Billing | Cargo por cita completada → referencia id |
| Availability engine | Consulta / reserva encima — no invade root |
| Vertical packs | Metadatos de producto **fuera** del Core aggregate |

---

## 20. Reference Contracts — trabajo FASE 18.2

| Port | Estado hoy | Acción 18.2 |
|------|------------|-------------|
| `PatientReferencePort` | ✅ Adapter | Usar |
| `OrganizationReferencePort` | ✅ Adapter | Usar |
| `OfficeReferencePort` | Declarado, **sin** adapter | Implementar adapter R2DBC |
| `StaffAssignmentReferencePort` | **Ausente** | Declarar en `organization-contract` + adapter |

**No** modificar aggregates Org/Patient. Solo contract + infrastructure adapters (evolución ADR-013 prevista).

Método mínimo sugerido StaffAssignment:

```text
existsByIdAndTenant(StaffAssignmentId, TenantId) → boolean
// opcional view: organizationId + officeId? para coherencia sin cargar aggregate
```

Si solo boolean no basta para invariante §7, ADR-013 permite **small reference view** (ids de scope) — preferible a inyectar `StaffAssignmentRepository`.

---

## 21. Naming & packaging (propuesta → ADR-014)

| Elemento | Propuesta |
|----------|-----------|
| Bounded Context | **Scheduling** |
| Módulo Gradle | `modules/appointment-management/` |
| Schema SQL | `scheduling` |
| Tabla | `scheduling.appointment` |
| HTTP | `/api/v1/scheduling/appointments` |
| OpenAPI group | `scheduling-administration` (closeout) |

---

## 22. Riesgos y gaps conscientes

| Gap | Severidad | Mitigación |
|-----|-----------|------------|
| Sin hard double-booking | Media (producto) | Diferido; queries de conflicto en UI |
| StaffAssignment delete físico vs citas históricas | Media | Conservar UUID; labels en read model futuro |
| `NO_SHOW` / `CONFIRMED` | Baja | Diferido; CANCELLED + reason |
| Recurrencia | Media | Fuera FASE 18 |
| Encounter automático al complete | Alta si se hace mal | **Prohibido** en Scheduling v1 |
| Expandir Appointment con clínica | **Crítica** | ADR-014 frozen + intentionally small |

Ningún gap obliga a reabrir Patient/Org/IAM.

---

## 23. Comparación con patrones existentes

| Aspecto | Organization / Patient | Appointment |
|---------|------------------------|-------------|
| Naturaleza | Estructura / registry | Compromiso operativo |
| Estados | ACTIVE / ARCHIVED | SCHEDULED / CANCELLED / COMPLETED |
| Delete físico | No | No |
| Archive/activate | Sí | No (cancel/complete) |
| Consumer ports | Org←Patient | Appointment←Patient+Org+Office+Staff |

Consistencia de **plataforma** (IDs, ports, tenant, HTTP admin, permisos) — no forzar lifecycle idéntico donde el dominio difiere.

---

## Auditorías / ADR siguientes

| Paso | Acción |
|------|--------|
| **18.1** | Aceptar **ADR-014** | ✅ [PASO-18.1](PASO-18.1-APPOINTMENT-MODEL-CONTRACT.md) |
| **18.2** | Office adapter + StaffAssignmentReferencePort |
| **18.5.1** | Appointment Admin API Audit |

---

## Referencias

- [PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md](PASO-18.0-SCHEDULING-FOUNDATION-PLANNING.md)  
- [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md) — plantilla de rigor  
- [PASO-16.7-STAFF-ASSIGNMENT-AUDIT.md](PASO-16.7-STAFF-ASSIGNMENT-AUDIT.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-ARCHITECTURE-REVIEW-2026-07.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
