# PASO 17.0.1 — Patient Aggregate Audit (DDD Estratégico)

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado + **refinamiento de cierre** (solo arquitectura)  
**Tipo:** Auditoría obligatoria — Aggregate Root nuevo + Bounded Context clínico  
**Dependencias:** [PASO-17.0](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md) · ADR-006 · ADR-007 · ADR-010 · ADR-011 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)

---

## Refinamiento de cierre (pre-17.1)

Antes de aceptar ADR-012 se consolidó el modelo con estos cambios (no es re-auditoría completa):

| Cambio | Decisión | Motivo |
|--------|----------|--------|
| Semántica org | **`PrimaryOrganizationId`** (opcional) — no “home ownership” | Evita leer “pertenece a la org” |
| `OfficeId` en Patient | **Eliminado** del aggregate | Lugar operativo → Appointment / Encounter / Inventory |
| Tenant | Invariante explícita: **nunca cambia de Tenant** | ADR-003 + registry identity |
| Unicidad registral | Patient = **única** identidad clínica registral del sujeto en el Tenant | Ningún BC futuro crea un “segundo Patient” |

**Compatibilidad ADRs:** no contradice ADR-006/007/010. Refina el *consumer recipe* de Patient en ADR-011 / Consumption Guide (OfficeId pasa a aggregates operativos) — coherente con “OfficeId = where is this thing located?”.

---

## Objetivo

Definir el modelo correcto del Aggregate Root **`Patient`** para que CodeCore, como framework SaaS, sostenga odontología, medicina, psicología, veterinaria, hospitales, clínicas, laboratorios y variantes futuras — **sin** acoplar Patient a un vertical.

**Sin código. Sin tablas. Sin endpoints. Sin migraciones.**

---

## Checklist política (§8) — verdicto previo

| # | Ítem | ✓ | Nota |
|---|------|---|------|
| 1 | Aggregate Root identificado | ✅ | `Patient` |
| 2 | Ownership definido | ✅ | BC Clinical Foundation |
| 3 | Invariantes definidas | ✅ | §16 |
| 4 | Lifecycle definido | ✅ | §11 |
| 5 | Estados definidos | ✅ | `ACTIVE` · `ARCHIVED` |
| 6 | Permisos definidos | ✅ | Borrador `patient:*` (§ Ownership) |
| 7 | Relaciones solo mediante IDs | ✅ | §15 |
| 8 | Bounded Context correcto | ✅ | Clinical Foundation |
| 9 | No rompe ADR vigentes | ✅ | Respeta 003/006/007/010/011 |
| 10 | Escalable multi-tenant | ✅ | §17 |
| 11 | Escalable multi-organization | ✅ | §13 |
| 12 | Escalable millones de registros | ✅ | §17 |

**Veredicto:** checklist en verde → proceder a **ADR-012** (17.1). Sin implementación aún.

---

## Decisiones irreversibles (resumen ejecutivo)

| Decisión | Elección |
|----------|----------|
| Naturaleza | **Identidad clínica registral** del sujeto de cuidado (no Identity IAM, no ficha clínica documental) |
| Aggregate Root | **`Patient`** — **única** identidad clínica registral del sujeto dentro del Tenant |
| BC propietario | **Clinical Foundation** |
| Tenant | **Siempre** — `TenantId` obligatorio e **inmutable** (nunca cambia de tenant) |
| Organization | **No ownership** — `PrimaryOrganizationId` opcional (registro / agrupación por defecto) |
| Office | **Fuera de Patient** — `OfficeId` solo en aggregates operativos (Appointment, Encounter, …) |
| StaffAssignment | **Jamás** en Patient |
| Unicidad dura | Solo **`PatientId` (UUID)** |
| Identificadores externos | Opcionales, soft-unique por tenant, no legislación fija |
| Lifecycle v1 | `ACTIVE` / `ARCHIVED` — sin delete físico |
| Merge | Operación explícita futura; no automática en v1 |
| Módulo / schema (propuesta) | Módulo `patient-management` · schema `clinical` |

Borrador formal: [ADR-012-PATIENT-DOMAIN-MODEL.md](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) (**Proposed**).

---

## 1. Naturaleza del Aggregate

### ¿Qué representa Patient?

**Patient es la identidad clínica registral del sujeto de cuidado dentro de un Tenant.**

| Interpretación | ¿Es? | Por qué |
|----------------|------|---------|
| ¿Una persona? | **Parcialmente** | En humanos suele serlo; en veterinaria el sujeto es un **animal** (el dueño es otra relación futura). El aggregate modela el **sujeto de cuidado**, no “ciudadano civil”. |
| ¿Una ficha clínica? | **No** | La ficha / historia es documentación (`MedicalRecord` / Clinical Record) — otro aggregate. |
| ¿Una identidad clínica? | **Sí (definición operativa)** | Entrada maestra estable a la que se cuelgan citas, encuentros, documentos y facturación clínica. |
| ¿Identity IAM? | **No** | ADR-006: Identity = autenticación global. Un paciente puede no tener login nunca. |

**Justificación framework:** “Patient” en CodeCore = **care subject master record** en el tenant. El vertical (dental, vet, lab) aporta **extensiones de perfil** (species, legal guardian, etc.) sin cambiar el root.

**Regla de exclusividad:** dentro de un Tenant, **Patient es la única identidad clínica registral** del sujeto de cuidado. Ningún bounded context futuro (Scheduling, Records, Billing, Inventory, vertical packs) debe introducir un aggregate equivalente (“Client”, “Subject”, “CareRecipient”, …) que duplique ese rol. Otros BCs **referencian** `PatientId`.

---

## 2. Aggregate Root

### ¿Por qué Patient debe ser Aggregate Root?

- Define el **boundary transaccional** de datos demográficos / registrales del sujeto.
- Tiene **ciclo de vida propio** (alta, actualización, archivo).
- Es el **ID estable** (`PatientId`) que otros BCs referencian.
- Sus invariantes **no** incluyen consistencia de citas ni notas clínicas (política §5).

### Principio de permanencia (ADR-012 §3)

> **Patient is intentionally small.**

Decisión **permanente** (no limitación de FASE 17): Patient solo identidad clínica registral + invariantes propias. Encounter, Appointment, Medical Record, notas, odontograma, Treatment Plan, documentos, billing, inventario y contexto operativo viven en **otros** aggregates. Embebidos en Patient = violación arquitectónica (**God Aggregate**). Detalle normativo: [ADR-012 §3](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md).

### ¿Por qué no Clinical Record / Medical Record?

Documentan **eventos y contenido clínico**. Nacen después del Patient. Muchos records por patient. Ownership documental ≠ registro maestro.

### ¿Por qué no Encounter?

Encounter es un **episodio de atención** (tiempo, lugar, participantes). Muchos por patient. Depende de Patient (y luego StaffAssignment / Office).

### ¿Por qué no Appointment?

Scheduling. Referencia `PatientId`; no define quién es el paciente.

---

## 3. Ownership

| Rol | Actor / BC |
|-----|------------|
| **Propietario del modelo** | Bounded Context **Clinical Foundation** (módulo `patient-management`) |
| **Quién crea** | Membership autenticado con `patient:create` (staff admin / recepción — roles IAM) |
| **Quién modifica** | `patient:update` |
| **Quién archiva** | `patient:archive` (o `patient:update` + operación archive — cerrar en 17.5) |
| **Quién solo consulta** | `patient:read` |
| **Quién NO es dueño** | IAM, Organization Management, Scheduling, Billing |

Organization **no** posee Patient. Solo puede ser referenciada.

**Permisos borrador (17.5):** `patient:read` · `patient:create` · `patient:update` · `patient:archive` (lista/paginación bajo read).

---

## 4. Identidad clínica (unicidad)

### Qué hace único a un Patient

| Mecanismo | Rol en CodeCore |
|-----------|-----------------|
| **`PatientId` (UUID)** | **Única clave de identidad dura** del aggregate |
| Identificadores externos (documento, MRN, chip, etc.) | **Opcionales**, tipados, **soft-unique por tenant** |
| Email / teléfono | Datos de contacto — **no** unicidad obligatoria |
| Combinación legal nacional | **No** hardcodear; configurable por tenant/vertical |

### Análisis

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| Solo UUID | Internacional, sin PII en clave, estable | Búsqueda humana necesita índices secundarios | **Clave primaria** |
| Email único | Familiar | Pacientes sin email; colisiones familiares; confunde con Identity | **No** como unique key |
| Teléfono único | Común en recepción | Compartido, cambia, formato país | Contacto, no unique |
| Documento nacional único | Útil en un país | SaaS multi-país; menores; vet sin DNI; privacidad | **ExternalId opcional** |
| UUID + ExternalId tipado | Flexible por vertical | Complejidad de merge | **Adoptado** |

**Regla:** no asumir legislación de un país.  
`ExternalIdentifier { type, value }` — unicidad `(tenantId, type, value)` **si** el tenant activa ese type; en v1 puede ser índice unique parcial opcional documentado en ADR-012.

---

## 5. Tenant

| Pregunta | Respuesta |
|----------|-----------|
| ¿Patient pertenece al Tenant? | **Sí** |
| ¿Siempre? | **Sí** |
| ¿Por qué? | ADR-003: aislamiento SaaS. Datos clínicos son del cliente (tenant). |
| ¿Puede cambiar de Tenant? | **Nunca** |

**Invariante formal:** `TenantId` se asigna en `create` y es **inmutable**. No existe operación `moveToTenant` / `transfer`. Un “mismo humano” en otro tenant es **otro** `PatientId` (otro registro SaaS), no una migración del aggregate.

Cross-tenant: imposible. Lectura cross-tenant → 404 (patrón Org/IAM).

---

## 6. Organization

| Pregunta | Respuesta |
|----------|-----------|
| ¿Patient **pertenece** a Organization (ownership)? | **No** |
| ¿Puede ser atendido allí? | **Sí** — vía Appointment / Encounter (otros aggregates) |

**Campo adoptado:** `PrimaryOrganizationId` (opcional) — *organización primaria de registro / agrupación por defecto* (filtros, preferencia administrativa, posible default de facturación).

| Nombre descartado | Motivo |
|-------------------|--------|
| Home Organization | Sugiere “hogar / pertenencia” ≈ ownership |
| RegistrationOrganization | Aceptable semánticamente; se unifica en **Primary** para un solo término estable en código (`PrimaryOrganizationId`) |

**No** significa “solo puede atenderse aquí”. Un mismo Patient puede atenderse en Organization Quito y Sangolquí sin duplicar el registro (§13).

Alineado con ADR-011 (OrganizationId como scope de negocio en el consumer) **sin** ownership Organizational del sujeto.

---

## 7. Office

| Pregunta | Respuesta |
|----------|-----------|
| ¿Office es ownership del Patient? | **No** |
| ¿`OfficeId` forma parte del Aggregate Patient? | **No (refinamiento de cierre)** |

**Decisión:** Patient **solo** conoce `TenantId` y, opcionalmente, `PrimaryOrganizationId`.

`OfficeId` pertenece a aggregates **operativos / de ubicación**:

- Appointment — sala / sitio de la cita  
- Encounter — lugar del episodio  
- Inventory — almacén / bin  

**Justificación:** mezclar “dónde se registró el paciente” con “dónde ocurre la atención” diluye el boundary y obliga invariantes office∈org en un aggregate que no es de ubicación. La primera atención ya captura el office en el flujo operativo.

---

## 8. StaffAssignment

| Pregunta | Respuesta |
|----------|-----------|
| ¿Patient conoce StaffAssignment? | **No** |
| ¿Quién sí? | **Appointment / Encounter** (y autoría en Clinical Record) |

**Justificación:** StaffAssignment = “dónde opera el staff” (ADR-011). Patient = sujeto de cuidado. Mezclarlos acopla demografía a roster operativo y rompe el consumption guide.

---

## 9. Appointment

| Dirección | Decisión |
|-----------|----------|
| Appointment → Patient | **Sí** — `PatientId` |
| Patient → Appointment | **No** |

Ninguna colección de citas dentro de Patient (escala + boundary).

---

## 10. Clinical Record

| Pregunta | Respuesta |
|----------|-----------|
| ¿Pertenece al Patient como parte del aggregate? | **No** |
| ¿Es otro Aggregate Root? | **Sí** (FASE 19) — referencia `PatientId` (+ `OrganizationId` custodio) |

Patient **no** embebe notas, odontogramas ni adjuntos.

---

## 11. Lifecycle

```text
(create) → ACTIVE ⇄ (update demographics / PrimaryOrganizationId)
                ↓ archive
             ARCHIVED
                ↓ activate (opcional, simétrico a Organization)
             ACTIVE
```

| Transición | Cuándo | Efecto |
|------------|--------|--------|
| **create** | Alta registral | Estado `ACTIVE`; `TenantId` fijado **para siempre** |
| **update** | Corrección demográfica, contactos, `PrimaryOrganizationId`, external ids | Solo si `ACTIVE` (default) |
| **archive** | Baja lógica, duplicado perdedor post-merge, cierre administrativo | No aparece en listados ACTIVE; histórico legible |
| **activate** | Reactivar archivo erróneo | Vuelve a ACTIVE |
| **cambiar Tenant** | **Prohibido** | — |
| **delete físico** | **Fuera de v1** | Retención clínica / legal |
| **anonymize** | **Fuera de v1** (FASE 22+/compliance) | Purga PII manteniendo `PatientId` para FKs lógicas |

**No** usar `deactivate` como tercer estado si `ARCHIVED` cubre el caso (consistencia con Organization lifecycle).

---

## 12. Merge

| Pregunta | Decisión v1 |
|----------|-------------|
| ¿Duplicados? | Posibles (alta manual, sin documento) |
| ¿Fusión automática? | **No** |
| ¿Fusión explícita? | **Sí — capacidad planificada** (`MergePatients` use case), no obligatoria en 17.3–17.6 |
| ¿Quién inicia? | Admin con permiso dedicado futuro `patient:merge` |
| ¿Cómo? | Survivor `PatientId` permanece ACTIVE; loser → ARCHIVED; referencias futuras deben apuntar al survivor (migración de FKs lógicas en application de cada BC — eventual) |

**FASE 17 núcleo:** detectar posibles duplicados por external id / búsqueda; **merge completo** puede ser 17.x posterior o post-17.8 sin cambiar el aggregate root.

---

## 13. Multi Organization

```text
Tenant
 ├── Organization Quito
 └── Organization Sangolquí
```

| Pregunta | Respuesta |
|----------|-----------|
| ¿Mismo Patient en ambas? | **Sí** — un solo `PatientId` |
| ¿Qué ocurre con la información? | Demografía única en Patient; atenciones en Appointment/Encounter llevan su `OrganizationId`/`OfficeId`; Clinical Record puede custodiar por org (ADR-011) |

`PrimaryOrganizationId` puede ser Quito mientras una cita ocurre en Sangolquí (con su propio org/office en Appointment).

**Anti-patrón prohibido:** crear un Patient por organization.

---

## 14. Escenarios

| Escenario | ¿Soportado? | Notas |
|-----------|-------------|-------|
| Clínica dental | ✅ | Patient humano; `PrimaryOrganizationId` = clínica |
| Hospital | ✅ | Multi-office en Encounter/Appointment; primary org = hospital o servicio |
| Veterinaria | ✅ | Sujeto = animal; perfil extendido (species) en VO/extension — no cambia root |
| Laboratorio | ✅ | Demografía mínima; primary org = lab |
| Psicología | ✅ | Igual que clínica ambulatoria |
| Empresa ocupacional | ✅ | Tenant = empresa/proveedor; Patient = trabajador-atendido; primary org = sede |
| Universidad | ✅ | Patient = estudiante/paciente de clínica universitaria |

**Gap consciente (no bloqueante):** perfil veterinario / tutor legal / consentimientos → extensiones o BCs futuros; **no** requieren cambiar Patient root.

---

## 15. Relaciones (solo IDs)

| Referencia | ¿Patient la mantiene? | Motivo |
|------------|----------------------|--------|
| `TenantId` | **Sí (obligatorio, inmutable)** | ADR-003 |
| `PrimaryOrganizationId` (`OrganizationId`) | **Sí (opcional)** | Registro / agrupación por defecto — **no** ownership |
| `OfficeId` | **Jamás** | Lugar operativo → Appointment / Encounter / … |
| `StaffAssignmentId` | **Jamás** | Scope de staff |
| `IdentityId` | **Jamás en v1** | Portal paciente = ADR futuro |
| `MembershipId` | **Jamás** | IAM |
| `AppointmentId` | **Jamás** | Scheduling → Patient |
| `ClinicalRecordId` | **Jamás** | Records → Patient |

Patient **nunca** carga aggregates externos (política §4).

---

## 16. Invariantes de dominio

1. Todo Patient tiene exactamente un `TenantId`, asignado en `create`.  
2. **`TenantId` nunca cambia** — no hay transferencia entre tenants.  
3. Un Patient en estado inválido no existe: solo `ACTIVE` o `ARCHIVED`.  
4. `archive` solo desde `ACTIVE`; `activate` solo desde `ARCHIVED`.  
5. Si `PrimaryOrganizationId` está presente en create/update, debe existir, pertenecer al mismo tenant y estar **ACTIVE** (escritura).  
6. Patient **no** mantiene `OfficeId`.  
7. `PatientId` es la única identidad dura; no se reasigna.  
8. Dentro del Tenant, Patient es la **única** identidad clínica registral del sujeto — no se duplica el rol en otros BCs.  
9. Patient **no** garantiza unicidad global de email/teléfono.  
10. Patient **no** incluye ni muta Appointment, Encounter, MedicalRecord ni StaffAssignment.  
11. Un Patient archivado **no** acepta nuevas asociaciones operativas desde use cases de escritura de otros BCs (ellos validan ACTIVE); lecturas históricas permitidas (ADR-011 §5).

---

## 17. Escalabilidad

| Requisito | Diseño |
|-----------|--------|
| Millones de pacientes | Aggregate pequeño; PK UUID; sin colecciones hijas de citas/records |
| Multi-organization | Un Patient / tenant; org opcional; filtros por org en query layer |
| Multi-office | Office solo referencia; atención en otros aggregates |
| Multi-especialidad | Especialidad **no** vive en Patient; vive en Encounter/Appointment/Staff |
| Evolución | Extensiones de perfil / vertical packs sin romper IDs |

Índices previstos (diseño, no migración aún): `(tenant_id)`, `(tenant_id, status)`, `(tenant_id, organization_id)`, unique parcial external ids.

---

## 18. Extensibilidad (sin modificar Patient)

| Futuro | Cómo usa Patient |
|--------|------------------|
| Appointment | `PatientId` + `StaffAssignmentId` + org/office |
| Encounter | `PatientId` + tiempo + lugar + staff |
| Medical Record | `PatientId` + `OrganizationId` custodio |
| Treatment Plan | `PatientId` |
| Documents | `PatientId` (o record id) |
| AI | Lee records/encounters por `PatientId` |
| Billing clínico | `PatientId` + `OrganizationId` en factura |

Patient permanece estable; los nuevos BCs **añaden** referencias salientes.

---

## Naming & packaging (propuesta a fijar en ADR-012)

| Elemento | Propuesta |
|----------|-----------|
| Bounded Context | **Clinical Foundation** |
| Módulo Gradle (FASE 17) | `modules/patient-management/` (domain · application · infrastructure · contract) |
| Schema SQL | `clinical` |
| Tabla | `clinical.patient` |
| HTTP (cerrar en 17.5.1) | `/api/v1/clinical/patients` |

---

## Riesgos y gaps conscientes

| Gap | Mitigación |
|-----|------------|
| Merge completo cross-BC | Diferido; documentado; no bloquea 17.3–17.6 |
| Anonymize / GDPR | FASE compliance / Platform |
| Patient portal (`IdentityId`) | ADR futuro — no contaminar v1 |
| Perfil vet / tutor | Extension VOs o BC satélite |
| Organization Reference Ports | PASO **17.2** (previsto ADR-011) |

---

## Auditorías / ADR siguientes

| Paso | Acción |
|------|--------|
| **17.1** | Aceptar **ADR-012** a partir de este audit + borrador |
| **17.2** | Organization Reference Ports |
| **17.5.1** | Patient Admin API Audit |

---

## Referencias

- [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)  
- [ADR-012-PATIENT-DOMAIN-MODEL.md](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) (Proposed)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
