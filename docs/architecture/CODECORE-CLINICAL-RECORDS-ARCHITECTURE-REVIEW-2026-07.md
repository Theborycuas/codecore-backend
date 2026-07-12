# CodeCore — Clinical Records Architecture Review (FASE 19)

**Fecha:** 2026-07-12  
**Tipo:** Revisión arquitectónica post-cierre (sin código · sin modificar docs existentes · sin ADR nuevo)  
**Alcance:** FASE 19 — Clinical Records (`Encounter`) ya cerrada (PASO 19.8)  
**Pregunta:** ¿Clinical Records quedó como bounded context reutilizable del Core Platform, o como extensión de Appointment / embrión de EHR?

**Autoridad de contraste:** [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md) · [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ARCHITECTURE-REVIEW-2026-07.md) · [ADR-015](ADR-015-ENCOUNTER-DOMAIN-MODEL.md) · [ADR-014](ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](CLINICAL-RECORDS-CONSUMPTION-GUIDE.md)

---

## Executive Summary

**Sí — Clinical Records (slice Encounter) es Core Platform, no un vertical y no un EHR.**  
`Encounter` permanece *intentionally small*, es **independiente** de Appointment (enlace opcional; sin orquestación), consume Patient/Org/Office/Staff/Appointment **solo** por IDs + ReferencePorts, publica `EncounterReferencePort`, y no incorpora Notes, SOAP, odontogram, diagnosis, prescriptions, labs, imaging, billing ni reglas dentales/hospitalarias.

No se encontró un error arquitectónico **crítico** que exija reabrir FASE 19 ni corregir código antes de FASE 20.

> **Veredicto operativo: A) FASE 20 puede comenzar sin cambios.**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| DDD | **9.5** | Aggregate Root correcto; límites y permanencia ADR-015 §3 explícitos |
| Hexagonal | **9** | Ports in/out claros; adapters R2DBC; application orquesta ReferencePorts |
| Modular Monolith | **9** | Schema `records` · módulo `encounter-management` · HTTP `/api/v1/records/**` |
| Core Platform | **9.5** | Agnóstico vertical; no EHR bag; no extensión de Appointment |
| Desacoplamiento | **9** | Contracts only; SQL solo `records.encounter`; Appointment no reabierto (solo port 19.2) |
| Reutilización | **9** | Episodio ocurrido sirve Dental/Vet/Hospital/Lab/Psic/Fisio igual |
| Consistencia | **9** | Espejo Appointment/Patient (API, soft lifecycle, closeout, guía) |
| Escalabilidad | **8.5** | Modelo listo para Notes/Billing alrededor; sin motor clínico prematuro |
| Mantenibilidad | **9** | Superficie pequeña; tests niegan métodos EHR; guide de consumo |
| Visión a largo plazo | **9.5** | Freeze anti–God Aggregate es la decisión de plataforma más valiosa de FASE 19 |
| **Global FASE 19** | **9.2** | Stress-test ADR-013 (5 ports) + independencia plan/ocurrido |

Comparado con revisión Scheduling 18 (**9.1**): FASE 19 **mantiene** la disciplina y **refuerza** la tríada Patient · Appointment · Encounter sin contaminar BCs cerrados.

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir FASE 19? | **No** |
| ¿Cambiar ADR-015? | **No** |
| ¿Bloquear FASE 20? | **No** |
| ¿Deuda P0 en Clinical Records? | **Ninguna** |
| Opción | **A) FASE 20 puede comenzar sin cambios** |

---

## Respuestas a las 14 preguntas

### 1 — ¿BC independiente o extensión de Appointment?

**Bounded context independiente.**

| Evidencia | Lectura |
|-----------|---------|
| Schema propio `records` · path `/api/v1/records/encounters` · módulo `encounter-management` | No vive bajo `scheduling` |
| Lifecycle propio `IN_PROGRESS → CANCELLED \| COMPLETED` | Distinto de `SCHEDULED → …` |
| `AppointmentId` **opcional** (walk-in) | Encounter no requiere Appointment |
| Sin auto-create / auto-complete cruzado (ADR-015 §7) | No hay orquestación |
| Consumo Appointment solo vía `findLinkable…` en write | Dependencia **hacia** Scheduling, no fusión |

Appointment es origen planificado **opcional**, no padre estructural. FASE 19 no “engorda” Scheduling: 19.2 solo amplió el **ReferencePort** (ADR-013 just-in-time), sin tocar el aggregate ni ADR-014.

---

### 2 — ¿Intentionally small o futuro EHR?

**Sigue siendo intentionally small.** No hay embrión de EHR en el aggregate, tabla, API ni permisos.

Superficie real de `Encounter`: identidad + refs ID + time bounds + status.  
Tests de dominio niegan `addSoap`, `addMedicalRecord`, `addOdontogram`, `addDiagnosis`, `addPrescription`, `addBillingLine`.  
Permisos: solo `encounter:create|read|update|cancel` — sin verbos clínicos/verticales.

El riesgo de EHR no está en el código entregado; está en la **disciplina futura** (mitigado por ADR-015 §3 + guía de consumo).

---

### 3 — ¿Responsabilidad que debería vivir en otro Aggregate?

**No** entre lo entregado.

| Candidato | ¿En Encounter hoy? | ¿Debería salir? |
|-----------|--------------------|-----------------|
| Notes / SOAP | No | N/A — correcto fuera |
| Diagnosis / Labs / Rx | No | N/A |
| Billing lines | No | N/A |
| Appointment window | No (solo `AppointmentId?`) | Correcto |
| Organization denormalizado | Sí | **No** — normativo ADR-015 §7 (igual ADR-014) |

Nada en el modelo actual “sobra” como responsabilidad ajena.

---

### 4 — ¿Algo que deba salir de Clinical Records?

**No.** El BC se entregó como **slice Encounter**; el nombre “Clinical Records” anticipa documentos futuros **alrededor** de Encounter, no dentro. Eso es coherente con ADR-015 (Notes = aggregate futuro, mismo BC o posterior).

No hay artefactos que deban mudarse a otro BC ahora.

---

### 5 — ¿ReferencePorts limpios u acoplamiento oculto?

**Limpios.**

**Consumo (Encounter → providers):**

| Port | Uso |
|------|-----|
| `PatientReferencePort` | ACTIVE en create/update |
| `OrganizationReferencePort` | ACTIVE |
| `StaffAssignmentReferencePort` | scope + coherencia §7 |
| `OfficeReferencePort` | si office aplica |
| `AppointmentReferencePort.findLinkable…` | si `appointmentId` presente + match patient |

Cancel/complete **no** revalidan ports (historial legible) — correcto.

**Publicación (Encounter → consumers):**

| Port | Métodos |
|------|---------|
| `EncounterReferencePort` | `existsInProgress…` · `findLinkable…` (+ view mínima) |

| Regla | ¿Cumple? |
|-------|----------|
| `encounter-application` → solo `*-contract` (patient/org/appointment) | ✅ |
| SQL main solo `records.encounter` | ✅ |
| Infra ajena solo en **test** | ✅ |
| Sin repos provider en main | ✅ |

Acoplamiento oculto: **no encontrado.**

---

### 6 — ¿Gradle respeta la política?

**Sí en main.**

| Módulo | Dependencias relevantes | ¿OK? |
|--------|-------------------------|------|
| `encounter-application` | domain + patient/org/appointment **contract** | ✅ |
| `encounter-infrastructure` | application + contracts + IAM (tenant/security HTTP) | ✅ patrón Appointment |
| `encounter-contract` | domain + reactor | ✅ |

`organizationInfrastructure` / `patientInfrastructure` / `appointmentInfrastructure` solo en **testImplementation** — correcto para ITs.

P2 heredado: `spring-boot-starter-data-r2dbc` en `encounter-application` (igual Appointment) — higiene, no violación de boundaries BC.

---

### 7 — ¿Contratos pequeños o God Contract?

**Suficientemente pequeños.**

| Contrato | Superficie |
|----------|------------|
| `EncounterPermissionCatalog` | 4 strings lifecycle |
| `EncounterReferencePort` | 2 métodos |
| `EncounterReferenceView` | `encounterId` · `patientId` · `status` (rechaza CANCELLED) |
| `AppointmentReferenceView` (19.2) | 3 campos linkables |

La dualidad `existsInProgress` + `findLinkable` **espeja** el estado final de Appointment post-19.2; no es un query API. No hay demographics, time window, ni org/office en la view.

---

### 8 — ¿Dificulta construir Notes / SOAP / Docs / Rx / Dx / Procedures / Labs / Imaging / Billing / Inventory / Consent / Analytics?

**No.** La evolución natural queda desbloqueada:

| Futuro | Cómo se ancla |
|--------|----------------|
| Notes / SOAP / Medical Documents | `EncounterId` + `findLinkable` / `existsInProgress` |
| Diagnosis / Procedures / Labs / Imaging | Nuevo aggregate → `EncounterId` |
| Prescriptions | Nuevo aggregate → `EncounterId` + `PatientId` |
| Billing | `EncounterId` (y/o Appointment) vía ports — no lifecycle de Encounter |
| Inventory (FASE 20) | **No necesita** Encounter; Org/Office ports bastan |
| Consent | `EncounterId?` + `PatientId` |
| Analytics | Read models / proyecciones — no engordar Encounter |

Ninguna decisión de FASE 19 fuerza a meter eso **dentro** de Encounter. El único peligro sería **violar** ADR-015 §3 después — no un bloqueo presente.

---

### 9 — ¿Fuga de vertical?

**Ninguna en código ni permisos.**

Tests de catálogo rechazan `dental` / `surgery` / `hygiene` / `soap` / `odontogram`.  
Sin campos hospitalarios (cama, ward), vet (especie), lab (panel).  
Naming `Patient` / `Encounter` / `StaffAssignment` es Core de cuidado a sujetos — no Dental.

---

### 10 — ¿Sobreingeniería?

**No de forma material.**

| Candidato | Veredicto |
|-----------|-----------|
| Dual ReferencePort | Justificado (open docs vs link completed) — espejo Appointment |
| `EncounterReferenceView` | Mínima (3 campos) — no God |
| VOs locales por BC | Patrón Core (TenantId N-ario) — fricción conocida, no invento de 19 |
| Use cases admin | Un impl / 6 ports in — espejo 18.6 |
| Event bus / CQRS / EpisodeOfCare | **Ausentes** — correcto |

No hay ports, views ni use cases “por si acaso” más allá del closeout esperado (19.8).

---

### 11 — ¿Simplificación posible SIN romper ADRs?

**Ninguna obligatoria.**

Opcional / cosmético (P2, no ahora):

- Quitar R2DBC starter de `encounter-application` si solo tipa `TransactionalOperator`
- Alinear política HTTP `Invalid*State` Patient(400) vs Appointment/Encounter(409) a nivel **platform**, no reabriendo Records

Simplificar quitando `findLinkable` del port publicado **reduciría** utilidad para Billing/Notes sobre COMPLETED — no recomendable.

---

### 12 — ¿FASE 20 limpia?

**Sí.** Inventory se ancla en Organization/Office (ADR-011/013). **No depende** de Encounter.

| ¿Corregir algo antes de FASE 20? | **No** |
|----------------------------------|--------|
| ¿Reabrir 19? | **No** |
| ¿Riesgo si Inventory intenta “stock por visita clínica” dentro de Encounter? | Disciplina — rechazar; Inventory no vive en Records |

---

### 13 — ¿Módulo reutilizable muchos años?

**Sí.** Criterio operativo: “episodio de atención ocurrido para un sujeto en un contexto operativo” cabe en una frase, sirve a todos los verticales del Core, y publica `EncounterId` estable sin forzar el contenido clínico.

La combinación ADR-015 freeze + closeout + consumption guide es el mecanismo de longevidad — igual que ADR-012/014.

---

### 14 — Notas (detalle en tabla superior)

**Nota global FASE 19: 9.2 / 10**

Si algo está bien: **casi todo lo estructural.**  
Si no cambiaría nada antes de FASE 20: **correcto — no cambiaría nada.**

---

## 1. Bounded Context

| Pregunta | Evidencia | ¿OK? |
|----------|-----------|------|
| ¿Límites claros? | Schema `records` · `encounter-management` · `/api/v1/records/**` · ADR-015 §1 | ✅ |
| ¿Solo responsabilidades propias? | Episodio ocurrido: tiempo, status, IDs de contexto | ✅ |
| ¿Independiente de Scheduling? | Appointment opcional; sin orquestación | ✅ |

### Contaminación (código `encounter-*` main)

| Concepto | ¿Presente como tipo/tabla/API? |
|----------|--------------------------------|
| Notes / SOAP / Odontogram | **No** |
| Diagnosis / Prescription / Labs / Imaging | **No** |
| Billing / Inventory | **No** |
| EpisodeOfCare | **No** |
| Calendar / Slot / Availability | **No** |
| Verbos dental/hospital/vet en permisos | **No** |
| SQL `clinical.*` / `scheduling.*` / `org.*` desde Records main | **No** |

**Conclusión §1:** BC correcto. Nombre “Clinical Records” + slice Encounter es coherente; no hay contaminación EHR.

---

## 2. Aggregate `Encounter`

**Superficie real:**

- Identidad: `EncounterId`, `TenantId` (inmutable)
- Refs: `PatientId`, `StaffAssignmentId`, `OrganizationId`, `OfficeId?`, `AppointmentId?`
- Tiempo: `EncounterTimeBounds` (`startedAt` / `endedAt?`)
- Lifecycle: `IN_PROGRESS` → `CANCELLED` \| `COMPLETED`
- Mutaciones solo en `IN_PROGRESS`

| Pregunta | Respuesta |
|----------|-----------|
| ¿Sigue *intentionally small*? | **Sí** — ADR-015 §3 |
| ¿Comportamiento de otro aggregate? | **No** |
| ¿God Aggregate en formación? | **No** |
| ¿Invariantes ausentes *realmente* importantes? | **No para Core v1.** Unicidad Appointment→Encounter 0..n es decisión explícita |

Coherencia StaffAssignment y Appointment linkable viven en **application** — correcto (ADR-013 / ADR-015).

---

## 3. ReferencePorts (detalle)

### Evolución 19.2 (Appointment)

`findLinkableByIdAndTenant` + `AppointmentReferenceView` fue **just-in-time** para invariantes Encounter, sin reabrir ADR-014. Alineado a lo que la review 18 ya anticipaba (“si hace falta existencia histórica, ampliar port”).

### Closeout 19.8 (Encounter)

Port dual publicado para consumers futuros. Tamaño aceptable; no God Contract.

---

## 4. Authorization

| Código | Uso |
|--------|-----|
| `encounter:create` | POST open |
| `encounter:read` | GET |
| `encounter:update` | PUT + **complete** |
| `encounter:cancel` | POST cancel |

`complete` → `:update` documentado (espejo Appointment). Matriz OWNER/ADMIN/MANAGER vs USER/READ_ONLY alineada. V23 seed idempotente. Sin verbos verticales.

---

## 5. API

Paridad Appointment:

| Aspecto | Estado |
|---------|--------|
| `/api/v1/records/encounters` | ✅ |
| Soft POST cancel/complete (no DELETE) | ✅ |
| Default list `IN_PROGRESS` · sort `startedAt,desc` | ✅ |
| Complete body `{ endedAt? }` | ✅ delta dominio legítimo |
| Sin `tenantId` en JSON | ✅ |
| OpenAPI `records-administration` | ✅ |
| Verification 8/8 | ✅ |

HTTP state inválido → **409** (como Appointment; distinto de Patient **400**) — inconsistencia de **plataforma**, no defecto de Records. P2.

---

## 6. Persistencia (V22)

`records.encounter` = identidad + refs lógicas + time + status + timestamps.  
Sin FK cross-BC. Índices tenant-aware. Sin columnas clínicas.

---

## 7. Multi-tenancy

JWT → `IamTenantContextAccessor` → filtros `…AndTenantId`. Cross-tenant → 404 (19.7). Ports siempre con `TenantId`. ADR-003 respetado.

---

## 8. Core Platform — ¿sirve igual?

Definición: **episodio de atención que ocurrió para un sujeto de cuidado en un contexto operativo determinado.**

| Vertical | ¿Sirve? |
|----------|---------|
| Dental / Vet / Hospital / Lab / Psic / Fisio | **Sí** — mismo episodio; contenido clínico = packs / futuros aggregates |
| ERP genérico de servicios | **Sí como episodio operativo**; no como Order/Invoice |

Ningún “no” duro por vertical.

---

## 9. Comparación con review Scheduling (FASE 18)

| Hallazgo 18 | Tras FASE 19 |
|-------------|--------------|
| Ports listos para Records | **Consumidos** (5 ports en write path) |
| Appointment port solo SCHEDULED insuficiente para enlace histórico | **Resuelto** (19.2 findLinkable) |
| Tentación engordar Appointment | **No ocurrió** |
| Event bus “por si acaso” | **No introducido** — correcto |
| Score Scheduling 9.1 | Records **9.2** — misma disciplina + independencia plan/ocurrido |

| Pregunta | Respuesta |
|----------|-----------|
| ¿Mejora el diseño del Core? | **Sí** — tríada Patient · Appointment · Encounter estable |
| ¿Empeora BCs cerrados? | **No** (solo seed IAM V23 + ampliación mínima Appointment **contract**) |
| ¿Mantiene consistencia? | **Sí** |

---

## 10. Deuda técnica (solo real)

### P0 — Crítica

**Ninguna.**

### P1 — Alta

**Ninguna atribuible a FASE 19.**

### P2 — Baja (higiene / disciplina)

| Ítem | Evidencia | Acción futura (no ahora) |
|------|-----------|---------------------------|
| `Invalid*State` → 409 (Encounter/Appointment) vs 400 (Patient) | Exception handlers | Política HTTP global en platform |
| `spring-boot-starter-data-r2dbc` en `encounter-application` | `build.gradle.kts` | Quitar al tocar el módulo |
| `TenantId` VO por BC (ahora también encounter) | Bridging UUID en boundaries | Consolidación shared-kernel solo si duele |
| Riesgo disciplinar: engordar Encounter “con una nota” | ADR-015 §3 | Rechazar; nuevo aggregate |

**No hay deuda P0/P1 introducida por FASE 19.**

---

## 11. Eventing (ADR-002)

Encounter **no** publica Domain Events. No hay consumidor que lo exija.  
**No** implementar event bus preventivo. Revisar cuando Notes/Billing/Analytics tengan invariante eventual demostrable.

---

## 12. Preparación para FASE 20 (Inventory)

| ¿Inventory necesita Encounter? | **No** |
|--------------------------------|--------|
| ¿Ports Org/Office disponibles? | **Sí** (FASE 16/18) |
| ¿Bloqueo arquitectónico en Records? | **No** |
| ¿Reabrir 19 para Inventory? | **No** |

**Conclusión:** FASE 20 puede arrancar inmediatamente.

---

## Fortalezas

1. Separación **planificado vs ocurrido** materializada y estable.  
2. Permanencia *intentionally small* escrita y testeada.  
3. Stress-test ADR-013 con **cinco** ReferencePorts.  
4. Independencia de Appointment (walk-in + sin orquestación).  
5. Closeout completo: port + guía + OpenAPI + verification.  
6. Consistencia de forma HTTP con Appointment/Patient/Org.  
7. Cero fuga vertical en permisos/schema/API.

---

## Debilidades

1. Inconsistencia HTTP de lifecycle inválido vs Patient (P2 plataforma).  
2. Naming BC “Clinical Records” puede tentear a interpretarlo como EHR — **mitigado** por ADR/guía, no por código defectuoso.  
3. Deuda de plataforma preexistente (TenantId N-ario, IAM monolítico) **heredada**, no causada por 19.

Ninguna debilidad bloquea FASE 20.

---

## Riesgos residuales (disciplina, no código)

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Meter Notes/SOAP “solo un poco” en Encounter | Alta si se viola | ADR-015 §3 · consumption guide |
| Convertir `EncounterReferencePort` en query API gorda | Media | ADR-013: boolean/view mínima |
| Orquestar Appointment↔Encounter en Scheduling o Records | Alta | ADR-014/015 freeze |
| Inventory “por visita” dentro de Encounter | Media | FASE 20 propia; no Records |

Ninguno justifica opción B o C hoy.

---

## Opciones de cierre

| Opción | Criterio | ¿Aplica? |
|--------|----------|----------|
| **A) FASE 20 puede comenzar sin cambios** | Sin P0/P1; BC cerrado; Inventory no depende de Records | **✅ Elegida** |
| B) Corregir solo críticos antes de 20 | Existiría P0/P1 real | No — no hay |
| C) Reabrir parcialmente FASE 19 | Evidencia fuerte de modelo roto | No — ADR-015 intacto y correcto |

---

## Conclusión

FASE 19 entrega lo que el Core necesitaba después de Scheduling: un BC **downstream** del grafo IAM → Org → Patient → Appointment que **no se fusiona** con Appointment, mantiene Encounter pequeño y vertical-agnóstico, y publica contrato de consumo para documentación clínica y billing futuros — **sin** convertirse en EHR.

CodeCore como Core Platform queda **más fuerte** que tras FASE 18: la tríada de cuidado (quién · planificado · ocurrido) está cerrada y lista para crecer **alrededor**.

**Si no cambiaría nada:** no cambiaría nada antes de FASE 20.

**Siguiente paso:** iniciar FASE 20 — Inventory — consumiendo contratos Organization existentes. No reabrir FASE 16 / 17 / 18 / 19.

---

## Referencias (lectura)

- [ADR-015-ENCOUNTER-DOMAIN-MODEL.md](ADR-015-ENCOUNTER-DOMAIN-MODEL.md)  
- [ADR-014-APPOINTMENT-DOMAIN-MODEL.md](ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ADR-012-PATIENT-DOMAIN-MODEL.md](ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [CLINICAL-RECORDS-CONSUMPTION-GUIDE.md](CLINICAL-RECORDS-CONSUMPTION-GUIDE.md)  
- [PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md](../audits/PASO-19.8-CLINICAL-RECORDS-CLOSEOUT.md)  
- [PASO-19.7-ENCOUNTER-VERIFICATION.md](../audits/PASO-19.7-ENCOUNTER-VERIFICATION.md)  
- [CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-SCHEDULING-ARCHITECTURE-REVIEW-2026-07.md)  
- [ROADMAP.md](ROADMAP.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
