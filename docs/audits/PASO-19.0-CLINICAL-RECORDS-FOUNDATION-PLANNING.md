# PASO 19.0 — Clinical Records Foundation Planning

**Clinical Records** es el siguiente bounded context del Core: documenta **lo que ocurrió** en la atención — sin planificar, sin registrar pacientes y sin reabrir IAM / Organization / Clinical Foundation / Scheduling.

**Fecha:** 2026-07-11  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Definición de FASE 19 · Bounded Context Clinical Records  
**Dependencias:** FASE 18 cerrada · ADR-011 · ADR-012 · ADR-013 · ADR-014 · [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md) · [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. BC = **Clinical Records** · primer Aggregate Root = **`Encounter`**  
2. Consumo **solo** IDs + ReferencePorts (Patient / Org / Office / StaffAssignment · Appointment opcional)  
3. Siguiente: **PASO 19.0.1** — Encounter Aggregate Audit ✅ · siguiente **19.1** ADR-015

**Sin código. Sin tablas. Sin endpoints. Sin DTOs. Sin SOAP / odontograma / recetas.**

---

## Objetivo

1. Declarar **FASE 19 — Clinical Records** tras el cierre de Scheduling.  
2. Nombrar el BC con rigor y elegir el **primer Aggregate Root** (uno solo).  
3. Fijar el plan de pasos (espejo FASE 17 / 18).  
4. Dejar explícito qué **no** es este BC (Billing, Inventory, vertical packs, motor clínico gordo).

---

## 1. Bounded Context — nombre

| Candidato | ¿Adoptar? | Motivo |
|-----------|-----------|--------|
| **Clinical Records** | **Sí** | Nombre estratégico correcto: documenta atención **ocurrida** (vs registry Patient, vs planning Appointment) |
| Care Episodes / Encounter Management | No como nombre de fase | Describe bien el *primer* aggregate, no el BC completo a largo plazo |
| Medical Records / EHR / Chart | No | Invita al God Aggregate “historia clínica completa” en el Core |
| Clinical Documentation | Débil | Suena a notas/SOAP, no al episodio |

**Decisión:** el BC se llama **Clinical Records**.

Es el bounded context donde el Core registra **que la atención ocurrió** y, más adelante, artefactos de documentación que cuelgan de ese episodio — **sin** convertirse en un EHR vertical.

```text
Patient (quién)     → Clinical Foundation   CLOSED
Appointment (plan)  → Scheduling            CLOSED
Encounter (ocurrió) → Clinical Records      FASE 19  ← este BC
```

**Regla de oro FASE 19:** no modificar aggregates ni schemas de IAM, Organization, Patient ni Appointment.

---

## 2. Primer Aggregate Root: `Encounter`

### Análisis de candidatos (uno solo gana)

| Candidato | ¿Root de Clinical Records v1? | Motivo |
|-----------|-------------------------------|--------|
| **`Encounter`** | **Sí** | Frontera transaccional del **episodio de atención ocurrido** (o registrado como ocurrido). Parallel perfecto de Appointment. Reutilizable en dental, vet, hospital, lab, psicología, fisioterapia, servicios. |
| `MedicalRecord` | **No (v1)** | “Historia / chart” longitudinal tiende a absorber notas, diagnósticos, adjuntos → **God Aggregate**. En el Core suele ser proyección (Patient + Encounters + docs) o aggregate **posterior**, no el primero. |
| `ClinicalEncounter` | **No** | Sinónimo ruidoso de Encounter. |
| `Visit` | **No** | Sinónimo ambiguo (ambulatorio vs hospitalario); peor reutilización internacional. |
| `EpisodeOfCare` | **No** | Episodio **longitudinal** multi-encuentro (embarazo, oncología). Demasiado grande; FHIR lo separa de Encounter. |
| `Consultation` | **No** | Sesgo de especialidad; subconjunto de Encounter. |
| `ClinicalDocument` | **No** | Artefacto documental (nota, informe). Cuelga de un episodio; no *es* el episodio. |

### Por qué no `MedicalRecord` primero (aunque el ROADMAP provisional lo nombrara)

El ROADMAP histórico decía `Clinical Records (MedicalRecord)` como hipótesis. La misión de FASE 19 (“documentar lo que **realmente ocurrió**”) y ADR-014 (Appointment ≠ Encounter; Encounter ≠ notes) apuntan al **episodio**, no al chart gordo.

| Si eligiéramos MedicalRecord primero… | Riesgo |
|----------------------------------------|--------|
| Meter SOAP / diagnósticos / adjuntos “dentro” | God Aggregate — viola política §5 / §9 |
| Dejarlo vacío (solo PatientId + OrgId) | Duplica Patient registry; poca frontera transaccional |
| Documentos sin episodio | Notas huérfanas; peor modelo para hospital / psic / fisio |

**Elegido:** **`Encounter`**.

Detalle irreversible: **PASO 19.0.1** (Aggregate Audit → prep. ADR-015).

---

## 3. One-sentence rule

> **Encounter** = el episodio de atención que **ocurrió** (o se registra como ocurrido) para un sujeto de cuidado en un contexto operativo determinado.

| Interpretación | ¿Es? |
|----------------|------|
| ¿La visita / sesión / atención real? | **Sí** |
| ¿El compromiso planificado (cita)? | **No** — eso es `Appointment` (FASE 18) |
| ¿La ficha / historia clínica longitudinal? | **No** — proyección o aggregate futuro; no engordar Encounter |
| ¿Una nota SOAP / odontograma / receta? | **No** — artefactos posteriores que referencian `EncounterId` |
| ¿Algo dental / vet específico? | **No** — Core Platform |

Patrón de frases del Core:

| Aggregate | Una frase |
|-----------|-----------|
| Organization | Unidad estructural del negocio bajo el tenant |
| Patient | Identidad clínica registral del sujeto de cuidado |
| Appointment | Compromiso **planificado** de atención en el tiempo |
| **Encounter** | Episodio de atención que **ocurrió** |

---

## 4. Por qué NO son Aggregate Root (ahora)

| Concepto | Por qué no es root del BC v1 |
|----------|------------------------------|
| Medical Note / SOAP | Contenido documental; cuelga de Encounter (o de un ClinicalDocument futuro) |
| Odontogram | Vertical dental — product pack |
| Prescription | Aggregate/BC de medicación; no el episodio |
| Diagnosis | Hecho clínico; no la frontera del episodio |
| Treatment Plan | Planificación terapéutica — otro aggregate |
| Consent | Artefacto legal/clínico aparte |
| Attachment / Clinical Image | Binarios / media — ownership distinto |
| Observation / Lab Result | Resultados; BC o aggregate de diagnósticos/labs |
| Procedure | Acto clínico facturable/documentable; no sustituye Encounter |
| MedicalRecord (chart bag) | Ver §2 — God Aggregate / proyección |

**Regla:** si responde *“qué se escribió / qué se midió / qué se facturó / qué es dental”* en lugar de *“qué episodio de atención ocurrió para este sujeto en este contexto”*, **no** pertenece dentro de Encounter.

---

## 5. Qué consume (solo IDs + ReferencePorts)

| Referencia | ¿En Encounter v1? | Validación (ADR-013) |
|------------|-------------------|----------------------|
| `TenantId` | **Obligatoria** (inmutable) | JWT / TenantContext |
| `PatientId` | **Obligatoria** | `PatientReferencePort.existsActiveByIdAndTenant` |
| `OrganizationId` | **Obligatoria** (contexto / custodia operativa del episodio) | `OrganizationReferencePort.existsActiveByIdAndTenant` |
| `StaffAssignmentId` | **Obligatoria** (quién atendió / scope operativo) | `StaffAssignmentReferencePort` |
| `OfficeId` | **Opcional** | `OfficeReferencePort.existsActiveInOrganization` |
| `AppointmentId` | **Opcional** | `AppointmentReferencePort` — walk-in sin cita; con cita, validar según invariante de 19.0.1 |

| Prohibido |
|-----------|
| `MembershipId` / `IdentityId` como “quién atendió” |
| Embed de Patient / Appointment / Organization / Office / StaffAssignment |
| SQL a `clinical.*` / `scheduling.*` / `org.*` / `iam.*` desde este BC |
| Dependencia Gradle a `*-domain` / `*-infrastructure` ajenos (salvo wiring app) |

Alineado con ADR-011 · ADR-013 · ADR-014 · guías Patient / Organization / Scheduling.

**Sin inventar** referencias a Billing, Inventory, Subscription, etc.

---

## 6. Qué NO pertenece a este BC

| Fuera | Por qué |
|-------|---------|
| IAM / Authentication | Plataforma cerrada |
| Organization / Office / StaffAssignment ownership | FASE 16 cerrada — solo IDs |
| Patient registry | FASE 17 cerrada |
| Appointment / Scheduling / Availability / Slots | FASE 18 cerrada / fuera de modelo |
| Billing / Invoices | FASE 21 |
| Inventory / Stock | FASE 20 |
| Notifications / Workflows | Platform / otros BC |
| SOAP, odontogram, prescriptions, images as Core v1 | Documentos/verticales — después, *alrededor* de Encounter |
| Product packs Dental / Vet / Hospital | Componen el Core; no viven dentro |
| Event bus “por si acaso” | Solo si un invariante eventual lo exige |
| Organization-scoped RBAC | ADR-007 intacto |

---

## 7. FASE 19 — estructura completa

**Bounded context:** Clinical Records  
**Primer Aggregate Root:** `Encounter`  
**Módulo Gradle (propuesta):** `modules/encounter-management/` (domain · application · infrastructure · contract) — nombre final en 19.0.1  
**Schema SQL (propuesta):** `records` (evitar mezclar ownership con `clinical` de Patient) — decisión en 19.0.1  
**HTTP (cerrar en audit API):** `/api/v1/records/encounters` — decisión en 19.5.1

### Pasos (espejo 17 / 18)

| Paso | Nombre | Objetivo | Auditoría / ADR | Entregable |
|------|--------|----------|-----------------|------------|
| **19.0** | Clinical Records Foundation Planning | Este documento + ROADMAP | Este PASO | Plan FASE 19 |
| **19.0.1** | Encounter Aggregate Audit | Checklist política §6–§8; lifecycle; refs; *intentionally small* | **Obligatoria** | Modelo + prep. ADR-015 |
| **19.1** | Encounter Model ADR | Congelar modelo irreversible | **ADR-015** | ADR Accepted |
| **19.2** | Clinical Records Reference Readiness | Verificar ports existentes; evolucionar **solo** si 19.0.1 exige p. ej. `AppointmentReferencePort.existsByIdAndTenant` — **sin** ports “por si acaso” | Contract evolution ADR-013 si aplica | Ports listos para escritura |
| **19.3** | Encounter Domain Foundation | Aggregate + VOs + tests | — | Dominio puro |
| **19.4** | Encounter Persistence | Flyway + R2DBC + ITs | — | Schema `records` (o decidido) |
| **19.5** | Encounter Authorization Contract | `encounter:*` + seed | Mínima | Catalog + Flyway |
| **19.5.1** | Encounter Admin API Audit | HTTP/DTO/paginación/lifecycle | **Obligatoria** | Contrato HTTP |
| **19.6** | Encounter Administration API | Use cases + controller + ITs | — | `/api/v1/records/encounters` |
| **19.7** | Encounter Verification | E2E: refs, RBAC, tenant, OpenAPI | — | `EncounterVerificationIT` |
| **19.8** | Clinical Records Closeout | Guía consumo + `EncounterReferencePort` + ROADMAP ✅ | — | BC cerrado (slice Encounter) |

### ¿Hace falta 19.2?

**Sí como paso de metodología**, no como invento de ports:

- Patient / Org / Office / StaffAssignment ports **ya existen** (FASE 17–18).  
- Appointment port publica hoy `existsScheduledByIdAndTenant`. Si Encounter debe enlazar citas **COMPLETED** (documentar después de completar), 19.0.1/19.2 puede exigir `existsByIdAndTenant` — evolución mínima ADR-013.  
- Si el audit decide “solo enlazar SCHEDULED” o “AppointmentId solo histórico sin revalidar ACTIVE”, **19.2 documenta “sin cambios de contract”** y avanza — no crea ports vacíos.

### Auditorías / ADR obligatorios

| Trigger (política) | Paso |
|--------------------|------|
| Nuevo Aggregate Root `Encounter` | **19.0.1** |
| Nuevo módulo + BC Clinical Records | **19.0.1** |
| Nuevo ADR de modelo | **19.1 → ADR-015** |
| Evolución de Appointment ReferencePort (si aplica) | **19.2** (no reabrir ADR-014) |
| Shape Admin API | **19.5.1** |

---

## 8. ReferencePorts nuevos

| Port | ¿Crear en FASE 19? | Nota |
|------|-------------------|------|
| Ports Patient / Org / Office / StaffAssignment | **No** — ya publicados | Solo consumir |
| Evolución `AppointmentReferencePort` | **Solo si 19.0.1 lo exige** | p. ej. existencia histórica; no “por si acaso” |
| `EncounterReferencePort` | **Sí — en closeout 19.8** | Espejo Patient/Appointment; para Billing / Notes / futuras docs |
| Ports para Notes / SOAP / Labs | **No** | No hay esos aggregates aún |

---

## 9. Cómo fortalece el Core Platform

| Señal de plataforma | Cómo lo aporta Encounter |
|---------------------|--------------------------|
| Separación plan vs ocurrido | Appointment planifica; Encounter registra ocurrencia — sin mezclar |
| Vertical-agnóstico | Misma visita/sesión/episodio para dental, vet, hospital, lab, psic, fisio |
| Consumo sin reabrir | Solo contracts — stress-test #2 tras Scheduling |
| Superficie estable | `EncounterId` para notas, labs, billing clínico, consentimientos **después** |
| *Intentionally small* | Resiste EHR monolítico en el Core |

**No** optimiza Dental. **No** es un hospital HIS. **No** es un PMS veterinario.  
Es el **núcleo de episodio de atención** sobre el que esos productos construyen packs.

```text
IAM → Organization → Patient → Appointment → Encounter → (docs / billing / packs)
         CLOSED        CLOSED     CLOSED        CLOSED      FASE 19
```

---

## 10. Criterio de cierre FASE 19

1. `Encounter` según ADR-015 (frozen) — *intentionally small*.  
2. Consumo Patient / Org / Office / Staff / Appointment (si aplica) **solo** por IDs + ReferencePorts.  
3. Verification E2E verde.  
4. ROADMAP FASE 19 ✅ · siguiente **FASE 20 Inventory** (o el BC que el roadmap confirme).  
5. Ningún aggregate de IAM / Organization / Patient / Appointment modificado.  
6. `EncounterReferencePort` + guía de consumo publicados en closeout.

---

## Relación con reviews previos

| Fuente | Tratamiento en FASE 19 |
|--------|------------------------|
| Scheduling Review 2026-07 — opción A | ✅ Arranque sin cambios estructurales |
| ADR-014 — Encounter ≠ Appointment | ✅ Encounter es root de Records, no de Scheduling |
| Guías — Encounter “post-18” / MedicalRecord “FASE 19” | ✅ Primer root = Encounter; MedicalRecord chart **no** es v1 |
| Eventing vacío | No implementar bus preventivo |

---

## Referencias

- [PASO-18.8-SCHEDULING-CLOSEOUT.md](PASO-18.8-SCHEDULING-CLOSEOUT.md)  
- [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md)  
- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [SCHEDULING-CONSUMPTION-GUIDE.md](../architecture/SCHEDULING-CONSUMPTION-GUIDE.md) · [PATIENT-CONSUMPTION-GUIDE.md](../architecture/PATIENT-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
