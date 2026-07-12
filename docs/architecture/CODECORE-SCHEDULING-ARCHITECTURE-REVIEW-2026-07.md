# CodeCore — Scheduling Architecture Review (FASE 18)

**Fecha:** 2026-07-11  
**Tipo:** Revisión arquitectónica post-cierre (sin código · sin modificar docs existentes · sin ADR nuevo)  
**Alcance:** FASE 18 — Scheduling (`Appointment`) ya cerrada (PASO 18.8)  
**Pregunta:** ¿Scheduling quedó como bounded context reutilizable del Core Platform, o como módulo vertical (p. ej. dental)?

**Autoridad de contraste:** [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ARCHITECTURE-REVIEW-2026-07.md) (PASO 17.9) · [ADR-014](ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Executive Summary

**Sí — Scheduling es Core Platform, no un producto vertical.**  
`Appointment` permanece *intentionally small*, consume Patient/Org/Office/StaffAssignment **solo** por IDs + ReferencePorts, publica `AppointmentReferencePort`, y no incorpora Encounter, Records, Billing, slots, availability ni reglas dentales.

No se encontró un error arquitectónico **crítico** que exija reabrir FASE 18 ni corregir código antes de FASE 19.

> **Veredicto operativo: A) FASE 19 puede comenzar sin cambios.**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| Calidad del bounded context | **9.5** | Límites claros; contaminación nula en código |
| Reutilización multi-producto | **9** | Compromiso planificado agnóstico; sujeto = `PatientId` |
| Desacoplamiento | **9** | Contracts only; SQL solo en `scheduling`; sin repos ajenos |
| Consistencia con FASE 16 / 17 | **9** | Espejo Patient/Org; divergencia HTTP menor (estado inválido) |
| Preparación para FASE 19 | **9** | Ports publicados; guía de consumo; sin bloqueo |
| **Global FASE 18** | **9.1** | Stress-test ADR-013 superado; Core fortalecido |

Comparado con revisión global 17.9 (**8.2** del Core pre-Scheduling): FASE 18 **mejora** la prueba de fuego del diseño (multi-port) y **mantiene** disciplina; no empeora el modelo.

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir FASE 18? | **No** |
| ¿Cambiar ADR-014? | **No** |
| ¿Bloquear FASE 19? | **No** |
| ¿Deuda P0 en Scheduling? | **Ninguna** |
| Opción | **A) FASE 19 puede comenzar sin cambios** |

---

## 1. Bounded Context

| Pregunta | Evidencia | ¿OK? |
|----------|-----------|------|
| ¿Límites claros? | Schema `scheduling` · módulo `appointment-management` · HTTP `/api/v1/scheduling/**` · ADR-014 §1 | ✅ |
| ¿Solo responsabilidades propias? | Compromiso planificado: tiempo, status, IDs de contexto operativo | ✅ |

### Contaminación (código `appointment-*` main)

| Concepto | ¿Presente como tipo/tabla/API? | Notas |
|----------|--------------------------------|-------|
| Encounter | **No** | Solo exclusión en javadoc / tests negativos |
| MedicalRecord | **No** | Idem |
| Billing | **No** | Idem |
| Inventory | **No** | — |
| Notifications | **No** | Solo mención como consumidor futuro del ReferencePort |
| Calendar Engine | **No** | — |
| Availability / Slot | **No** | Tests niegan métodos tipo `reserveSlot` / `setAvailability` |
| Waitlist | **No** | Tests niegan `addWaitlist` |
| Odontogram / dental verbs | **No** | Permission catalog test rechaza códigos verticales |

**Conclusión §1:** Scheduling tiene límites de BC correctos. No hay contaminación vertical ni de BCs futuros.

---

## 2. Aggregate `Appointment`

**Superficie real** (`Appointment.java`):

- Identidad: `AppointmentId`, `TenantId` (inmutable)
- Refs: `PatientId`, `StaffAssignmentId`, `OrganizationId`, `OfficeId?`
- Tiempo: `AppointmentTimeWindow` (`startsAt`/`endsAt`)
- Lifecycle: `SCHEDULED` → `CANCELLED` \| `COMPLETED`
- Mutaciones solo en `SCHEDULED`: reschedule, change refs, assign/clear office, cancel, complete

| Pregunta | Respuesta |
|----------|-----------|
| ¿Sigue *intentionally small*? | **Sí** — alineado ADR-014 §3 |
| ¿Comportamiento de otro aggregate? | **No** — no crea Encounter, no documenta, no factura, no reserva capacidad |
| ¿God Aggregate en formación? | **No** |
| ¿Invariantes ausentes *realmente* importantes? | **No para el Core v1.** Double-booking / NO_SHOW / recurrencia están **explícitamente fuera** del modelo (ADR-014 §8–§9); no son omisiones accidentales |

`OrganizationId` denormalizado es **normativo** (ADR-014 §7), no duplicación accidental: evita joins cross-BC en listados.

Coherencia StaffAssignment ↔ Org/Office vive en **application** (`validateWriteRefs`) — correcto (ADR-013 / ADR-014); el dominio no embebe esos aggregates.

---

## 3. ReferencePorts

### Consumo (Appointment → providers)

| Port | Uso | Momento |
|------|-----|---------|
| `PatientReferencePort` | `existsActiveByIdAndTenant` | create / update |
| `OrganizationReferencePort` | `existsActiveByIdAndTenant` | create / update |
| `StaffAssignmentReferencePort` | `findScopeByIdAndTenant` + coherencia | create / update |
| `OfficeReferencePort` | `existsActiveInOrganization` | create / update si office aplica |

Cancel / complete: **no** revalidan ports (estado terminal / no mutan refs) — coherente con ADR-014.

### Publicación (Appointment → consumers)

| Port | Método | Semántica |
|------|--------|-----------|
| `AppointmentReferencePort` | `existsScheduledByIdAndTenant` | Solo compromiso **SCHEDULED** |

### Reglas de aislamiento

| Regla | ¿Cumple? | Evidencia |
|-------|----------|-----------|
| Sin repos de otros BC en main | ✅ | `appointment-application` → `patient-contract` + `organization-contract` |
| Sin SQL cross-schema en appointment-infra main | ✅ | Solo `FROM scheduling.appointment` |
| Sin dependencias a `*-infrastructure` ajenos en main | ✅ | Org/Patient infra solo en **test** |
| Adapter propio solo lee `scheduling` | ✅ | `R2dbcAppointmentReferenceAdapter` |

**Conclusión §3:** Patrón ADR-013 bajo carga multi-port **probado**. FASE 18 cierra el gap de 17.9 (Office/Staff ports incompletos).

---

## 4. Authorization

Catálogo cerrado (`AppointmentPermissionCatalog`):

| Código | Uso HTTP |
|--------|----------|
| `appointment:create` | POST `/` |
| `appointment:read` | GET list/get |
| `appointment:update` | PUT + **POST `/complete`** |
| `appointment:cancel` | POST `/cancel` |

| Pregunta | Respuesta |
|----------|-----------|
| ¿Suficiente para el BC cerrado? | **Sí** |
| ¿Sobra alguno? | **No** |
| ¿Falta alguno *transversal* hoy? | **No.** `complete` → `:update` es decisión documentada (espejo Patient `activate` → `:update`). No inventar `appointment:complete` sin necesidad de matriz distinta |

Sin verbos verticales. Matriz roles (OWNER/ADMIN/MANAGER vs USER/READ_ONLY) alineada al patrón Patient.

---

## 5. API

Paridad con Organization / Patient / IAM admin:

| Aspecto | Estado |
|---------|--------|
| Prefijo versionado `/api/v1/{bc}/…` | ✅ `/api/v1/scheduling/appointments` |
| Soft actions vía POST (no DELETE) | ✅ cancel / complete |
| Paginación `content/page/size/total*` | ✅ `PagedAppointmentResponse` |
| Sin `tenantId` en body/response | ✅ |
| OpenAPI grupo dedicado | ✅ `scheduling-administration` |
| Exception handler por paquete | ✅ 401 vía seguridad; 403/404/409/400 |

### Divergencia real (no bloqueante)

| Tema | Patient | Appointment |
|------|---------|-------------|
| Estado inválido de lifecycle | `InvalidPatientStateException` → **400** | `InvalidAppointmentStateException` → **409** |

Es inconsistencia de **semántica HTTP entre BCs**, no de dominio. No rompe aislamiento ni reutilización. Clasificada como deuda **P2** (higiene de plataforma), no como defecto de Scheduling que exija reopen.

---

## 6. Persistencia (V20)

Tabla `scheduling.appointment`: columnas = identidad + refs lógicas + ventana + status + timestamps.

| Pregunta | Respuesta |
|----------|-----------|
| ¿Solo identidad propia? | **Sí** |
| ¿Duplicación innecesaria? | **No** — `organization_id` denormalizado es decisión ADR-014 |
| ¿Falta columna esencial del dominio? | **No** para el modelo congelado |
| ¿FK cross-BC? | **No** (comentarios lógicos a iam/clinical/org) |

Índices tenant-aware presentes (`tenant_id`, `tenant+status`, `tenant+patient`, `tenant+org`, `tenant+staff`).

---

## 7. Multi-tenancy

| Control | Evidencia | ¿OK? |
|---------|-----------|------|
| Tenant desde JWT / `TenantContext` | `IamTenantContextAccessor` → IAM `AuthorizationContextAccessor` | ✅ |
| HTTP no acepta `tenantId` | Create/Update DTOs sin campo; tests afirman ausencia en response | ✅ |
| Queries filtradas por tenant | `findByIdAndTenantId`, list `WHERE tenant_id = :tenantId` | ✅ |
| ReferencePorts siempre con `TenantId` | Firmas `…AndTenant` | ✅ |
| Cross-tenant | Verification 18.7: 404 | ✅ |

Aislamiento ADR-003 respetado. Sin aceptación de tenant desde el cliente como autoridad.

---

## 8. Core Platform — ¿sirve igual?

Definición operativa de Appointment: **compromiso planificado de prestar un servicio a un sujeto de cuidado en un tiempo y contexto operativo**.

| Vertical | ¿Sirve? | Justificación |
|----------|---------|---------------|
| Dental | **Sí** | Mismo compromiso; silla/odontograma = product pack, no Core |
| Veterinaria | **Sí** | Sujeto = Patient (registry); especie = pack |
| Hospital | **Sí** | Igual; Encounter/cama = otros BCs |
| Laboratorio | **Sí** | Cita de toma/proceso = compromiso planificado |
| Psicología | **Sí** | Sesión planificada = Appointment |
| Fisioterapia | **Sí** | Sesión planificada = Appointment |
| ERP de servicios | **Sí, con el mismo contrato** | Si el “cliente/sujeto del servicio” se modela como `PatientId` (identidad registral del sujeto). Appointment **no** es Order/Quote/Invoice: eso es Billing/Commerce. No sirve *como ERP comercial completo*; sí como **agenda de compromisos** del Core |

Ningún “no” duro por vertical. El único matiz: naming clínico (`Patient`) es del Core de cuidado/servicios a sujetos; no es un Appointment dental.

---

## 9. Comparación con Architecture Review 17.9

| Hallazgo 17.9 | Tras FASE 18 |
|---------------|--------------|
| Office/Staff ReferencePorts incompletos | **Resuelto** (PASO 18.2) |
| FASE 18 como stress-test ADR-013 | **Superado** (4 ports en write path) |
| Event readiness 4/10 | **Sin cambio** — correcto; Appointment no introdujo bus preventivo |
| TenantId triplicado | **Persiste** (ahora también VO en appointment-domain) — fricción conocida, no rotura |
| R2DBC starter en `*-application` | **Repetido** en appointment-application — higiene P2 |
| ¿Engordar Patient? | **No ocurrió** — Patient intacto |
| Score Core 8.2 “listo para 18” | Scheduling **eleva confianza** del patrón; no degrada BC cerrados |

| Pregunta | Respuesta |
|----------|-----------|
| ¿Mejora el diseño? | **Sí** — prueba real multi-BC + ports Org completados + consumo guide Scheduling |
| ¿Empeora? | **No** en modelo; solo hereda deuda de plataforma ya conocida |
| ¿Mantiene consistencia? | **Sí** con FASE 16/17 (espejo closeout + API soft-entity) |

---

## 10. Deuda técnica (solo real)

### P0 — Crítica (bloquea Core / FASE 19)

**Ninguna.**

### P1 — Alta (afecta integridad o escala inmediata)

**Ninguna atribuible a FASE 18.**

*(Deuda de plataforma preexistente — MembershipPort transicional, IAM monolítico, TenantId N-ario — sigue vigente pero **no** es regresión de Scheduling ni bloqueo de Clinical Records.)*

### P2 — Baja (higiene / consistencia)

| Ítem | Evidencia | Acción futura (no ahora) |
|------|-----------|---------------------------|
| `Invalid*State` → 409 (Appointment) vs 400 (Patient) | Exception handlers | Alinear política HTTP global cuando se toque platform web |
| `spring-boot-starter-data-r2dbc` en `appointment-application` | `build.gradle.kts` sin uso aparente de R2DBC en app | Quitar al tocar el módulo |
| `TenantId` por BC (iam/org/patient/appointment) | Conversión en boundaries | Consolidación opcional shared-kernel — solo si duele operativamente |
| `shared-events` vacío + ADR-002 | Sin DomainEvents en Appointment | Introducir al primer invariante eventual **real** |

Si se exige una frase: **no hay deuda P0/P1 introducida por FASE 18; la deuda listada es P2 o preexistente.**

---

## 11. Eventing (ADR-002)

| Pregunta | Respuesta con evidencia |
|----------|-------------------------|
| ¿Appointment publica Domain Events hoy? | **No** — grep sin `DomainEvent` / bus en `appointment-management` |
| ¿Hay consumidor in-process que lo exija? | **No** — verification, admin API y ports son síncronos |
| ¿Hay proyección/outbox requerida por invariante? | **No** documentada ni implementada |
| ¿Hace falta Event Bus “por si acaso”? | **No** — 17.9 ya lo difería; FASE 18 no aportó evidencia nueva que lo active |

**Conclusión §11:** No implementar eventos ahora. Revisar cuando un BC (Notifications, Analytics, Billing asíncrono) tenga un invariante eventual demostrable.

---

## 12. Preparación para FASE 19 (Clinical Records)

¿Puede Clinical Records consumir **solo** IDs + ReferencePorts?

| Recurso | ¿Disponible? | Cómo |
|---------|--------------|------|
| Patient | ✅ | `PatientId` + `PatientReferencePort` |
| Appointment | ✅ | `AppointmentId` + `AppointmentReferencePort` (`SCHEDULED`) |
| Organization | ✅ | `OrganizationId` + `OrganizationReferencePort` |
| Office | ✅ | `OfficeId` + `OfficeReferencePort` |
| StaffAssignment | ✅ | `StaffAssignmentId` + `StaffAssignmentReferencePort` |

| Bloqueo arquitectónico | ¿Existe? |
|------------------------|----------|
| Falta de contract module | **No** |
| SQL obligatorio cross-schema | **No** |
| Reabrir Appointment para Records | **No** |
| ReferencePort solo SCHEDULED insuficiente para *empezar* Records | **No** — Records ancla en `PatientId`; enlace a Appointment es opcional. Si un invariante futuro exige existencia histórica (COMPLETED), ADR-013 permite añadir `existsByIdAndTenant` **sin** reabrir el aggregate |

**Conclusión §12:** FASE 19 puede arrancar. Sin bloqueo.

---

## Riesgos residuales (disciplina, no código)

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Engordar Appointment en FASE 19+ “porque Records lo necesita” | Alta si se viola | ADR-014 freeze + consumption guide |
| Convertir `AppointmentReferencePort` en query API gorda | Media | ADR-013: boolean primero |
| Meter slots/availability dentro de Scheduling “rápido” | Alta | Fuera de modelo; nuevo audit/ADR |

Ninguno justifica opción B o C hoy.

---

## Opciones de cierre

| Opción | Criterio | ¿Aplica? |
|--------|----------|----------|
| **A) FASE 19 puede comenzar sin cambios** | Sin P0/P1; BC cerrado; ports listos | **✅ Elegida** |
| B) Corregir solo críticos antes de 19 | Existiría P0/P1 real | No — no hay |
| C) Reabrir parcialmente FASE 18 | Evidencia fuerte de modelo roto | No — ADR-014 intacto y correcto |

---

## Conclusión

FASE 18 entrega lo que el Core necesitaba probar: un BC **downstream** que consume el grafo IAM → Org → Patient **sin reabrirlos**, mantiene Appointment pequeño y vertical-agnóstico, y publica contrato de consumo para lo que sigue.

CodeCore como Core Platform queda **más fuerte** que en 17.9: la familia ReferencePort operativa está completa para Scheduling, y el camino a Clinical Records está desbloqueado.

**Siguiente paso:** iniciar FASE 19 — Clinical Records — consumiendo contratos existentes. No reabrir FASE 16 / 17 / 18.

---

## Referencias (lectura; no modificadas)

- [ADR-014-APPOINTMENT-DOMAIN-MODEL.md](ADR-014-APPOINTMENT-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [SCHEDULING-CONSUMPTION-GUIDE.md](SCHEDULING-CONSUMPTION-GUIDE.md)  
- [PASO-18.8-SCHEDULING-CLOSEOUT.md](../audits/PASO-18.8-SCHEDULING-CLOSEOUT.md)  
- [PASO-18.7-APPOINTMENT-VERIFICATION.md](../audits/PASO-18.7-APPOINTMENT-VERIFICATION.md)  
- [CODECORE-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-ARCHITECTURE-REVIEW-2026-07.md)  
- [ROADMAP.md](ROADMAP.md)  
