# PASO 17.5 — Patient Authorization Contract Audit

**Fecha:** 2026-07-11  
**Estado:** ✅ Auditoría cerrada — diseño aprobado para implementación  
**Tipo:** Contrato de autorización Clinical Foundation (Core Platform)  
**Fuentes:** ADR-007 · ADR-012 · ADR-013 · PASO-16.3 · PASO-17.0.1 · PASO-17.4

---

## Veredicto

Sembrar **exactamente cuatro** permisos transversales:

| Código | Dominio |
|--------|---------|
| `patient:create` | Registrar identidad clínica |
| `patient:read` | Consultar identidad clínica |
| `patient:update` | Corregir registro (demographics, primary org, external ids, **activate**) |
| `patient:archive` | Soft-retire del registro operativo |

**No** sembrar hoy: `delete`, `merge`, `restore`, `anonymize`, `export`, `link-identity`, ni nada vertical.

Este contrato es estable a 10+ años porque Patient es *intentionally small* (ADR-012 §3) y el lenguaje describe **identidad registral**, no operaciones clínicas.

---

## 1 — Operaciones fundamentales (dominio, no CRUD)

Patient = *la identidad clínica registral del sujeto de cuidado*.

| Operación de dominio | Comportamiento | Permiso |
|----------------------|----------------|---------|
| Registrar sujeto | `create` | `patient:create` |
| Consultar identidad | lectura / listado | `patient:read` |
| Corregir registro | `updateDemographics`, assign/remove primary org, external ids | `patient:update` |
| Reactivar registro | `activate` | `patient:update` (espejo Org: activate → `organization:update`) |
| Retirar del registro operativo | `archive` | `patient:archive` |

No son operaciones de Patient: agendar, documentar, facturar, inventariar, odontograma, SOAP, consentimientos clínicos.

---

## 2 — ¿Sembrar más permisos hoy?

| Candidato | ¿Hoy? | Justificación |
|-----------|-------|---------------|
| `patient:create\|read\|update\|archive` | ✅ **Sí** | Lifecycle del aggregate congelado |
| `patient:merge` | ❌ Reservar | ADR-012: merge explícito futuro; necesita ADR propio + orquestación cross-BC |
| `patient:restore` | ❌ No | Duplica `activate`; Org usa `*:update` |
| `patient:anonymize` | ❌ Reservar | Compliance/GDPR — ADR futuro; no es v1 |
| `patient:export` | ❌ Reservar | Reporting/privacy BC; no es invariante del registry |
| `patient:link-identity` | ❌ Reservar | Portal paciente = ADR futuro (ADR-012) |
| `patient:delete` | ❌ **Nunca en v1** | Ver §3 |

Sembrar permisos “por si acaso” contaminaría el catálogo global y forzaría grants en roles sin producto. CodeCore siembra **cuando el aggregate/capability existe**.

---

## 3 — ¿`patient:delete`?

| Lente | Conclusión |
|-------|------------|
| **DDD** | Dominio solo conoce `archive` / `activate`. Delete físico contradice el aggregate. |
| **Historia clínica** | Identidad registral ancla citas, records, facturas futuras. Borrar rompe integridad referencial lógica. |
| **Auditoría / compliance** | Soft-delete + retención; hard delete exige proceso legal (anonymize), no un botón CRUD. |
| **Regulación / SaaS Enterprise** | Tenants enterprise esperan retención; hard delete es excepción gobernada. |
| **CodeCore Core** | Organization/Office usan `archive`, no `delete`. Patient debe ser idéntico. |

**Decisión:** **no** existe `patient:delete` en el contrato. Futuro `patient:anonymize` (si llega) es capability distinta, no sinónimo de delete.

---

## 4 — Matriz RBAC (system roles)

Patient es **registro operativo diario**, no raíz estructural rara (a diferencia de `organization:create`).

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| `patient:create` | ✓ | ✓ | ✓ | — | — |
| `patient:read` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `patient:update` | ✓ | ✓ | ✓ | — | — |
| `patient:archive` | ✓ | ✓ | ✓ | — | — |

| Rol | Lectura de matriz |
|-----|-------------------|
| **OWNER / ADMIN** | Gobernanza + administración completa del registry |
| **MANAGER** | Operación clínica diaria (recepción / jefatura) — ciclo completo |
| **USER** | Consulta (p. ej. staff que agenda con `appointment:*` propio) |
| **READ_ONLY** | Auditoría / navegación sin escritura |

La matriz de system roles **es suficiente** como default del Core. Tenants pueden crear roles custom (p. ej. “Recepción” con create+update sin archive) vía IAM — sin ensuciar el catálogo global.

**Conteos esperados tras 17.5:** ALL **32** · OWNER 32 · ADMIN 29 · MANAGER 16 · USER 5 · READ_ONLY 7.

---

## 5 — Appointment y evolución

| Pregunta | Respuesta |
|----------|-----------|
| ¿Appointment reutiliza `patient:read`? | **No como sustituto.** Appointment tendrá `appointment:*`. |
| ¿Staff que agenda necesita Patient? | Sí: típicamente `patient:read` **más** `appointment:create` — grants independientes. |
| ¿Crear cita implica `patient:update`? | **No.** Cita referencia `PatientId`; no muta Patient. |

Reservar `appointment:*`, `encounter:*`, `medical-record:*` para sus BCs. No sembrarlos en 17.5.

---

## 6 — Verticales futuros

| Vertical | ¿Permisos Patient bastan? | Notas |
|----------|---------------------------|-------|
| Dental | ✅ | Odontogram / planes = otros BCs + permisos propios |
| Veterinaria | ✅ | Species/guardian = extensión de perfil / otro aggregate |
| Hospital | ✅ | Encounter / ward = otros BCs |
| Laboratorio | ✅ | Orders/results = otros BCs |
| Psicología / Fisio / Oftalmología | ✅ | Documentación clínica fuera de Patient |

**Nada** de `patient:odontogram`, `patient:species`, `patient:ward`. Eso rompería el Core y ADR-012 §3.

¿Falta algo transversal? Solo el cuarteto lifecycle. Merge/anonymize/export/link llegan con sus ADRs.

---

## 7 — ¿Estable 10 años?

**Sí**, si se mantiene la disciplina:

1. Patient permanece intentionally small.  
2. Nuevas capabilities clínicas = **nuevos** recursos `resource:action`, no más verbos en `patient:`.  
3. Capacidades sensibles (merge, anonymize, portal) = ADR + permiso nuevo, no reinterpretar `update`.

Si mañana Patient necesitara `patient:schedule` o `patient:bill`, sería señal de God Aggregate — **rechazar** y crear BC.

---

## Relación con ADRs

| ADR | Aplicación 17.5 |
|-----|-----------------|
| **ADR-007** | Permisos globales `iam.permission`; roles tenant; membership-scoped; `resource:action` |
| **ADR-012** | `patient:*` sobre registry lifecycle; sin delete; merge diferido |
| **ADR-013** | Auth ≠ ReferencePort; validar Org ACTIVE sigue siendo port, no permiso |

---

## Checklist Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Hospital específicos  
- [x] Diseñado para durar muchos años  
- [x] Refuerza Core Platform, no una app de negocio  

**Modelo validado → proceder a implementación (espejo PASO 16.3).**
