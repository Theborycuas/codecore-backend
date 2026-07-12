# CodeCore — Core Architecture Review

**Fecha:** 2026-07-11  
**Paso:** PASO 17.9  
**Tipo:** Revisión arquitectónica (sin código · sin ADR nuevo · sin cambio de dominio)  
**Pregunta:** ¿Está CodeCore listo para soportar 10+ bounded contexts sin replantear su arquitectura?

---

## Executive Summary

**Sí — con deuda conocida y acotada.** El Core ya opera como **plataforma modular** (IAM + Organization + Clinical Foundation), no como una app vertical. Los límites de BC, Reference Contracts (ADR-013), tenancy sin FK cross-schema y APIs admin consistentes son suficientes para iniciar **FASE 18 Scheduling** sin reabrir arquitectura.

No se encontró un error arquitectónico crítico que exija ADR nuevo o cambio de dominio **antes** de Scheduling. La recomendación principal es:

> **No realizar cambios estructurales. Comenzar FASE 18.**

Higiene (Gradle fantasma, `TenantId` triplicado, ports Org incompletos) puede resolverse **durante** FASE 18 cuando Appointment los necesite — no como precondiciones.

---

## Puntuación global del Core

| Dimensión | Nota (0–10) | Comentario |
|-----------|-------------|------------|
| Modular Monolith + Hexagonal | **9** | Capas y wiring claros; IAM monolítico Gradle es la excepción |
| DDD estratégico (BCs) | **9** | IAM / Org / Clinical bien recortados |
| DDD táctico (aggregates) | **8.5** | Patient *intentionally small*; Org jerarquía coherente |
| Multi-tenant | **9** | JWT + membership; sin FK cross-BC |
| Reference Contracts | **8.5** | Patrón probado; familia incompleta (Office/Staff/Membership) |
| Authorization escalable | **8** | Catálogos + seeds aditivos; riesgo de matriz roles a escala |
| Persistencia / ownership | **9** | Schemas `iam` / `org` / `clinical` limpios |
| Consistencia API | **8.5** | Soft-entity alineado; matrices MANAGER distintas (documentadas) |
| Event readiness | **4** | ADR-002 Accepted; `shared-events` vacío; sin DomainEvents |
| Higiene de repo / tooling | **6.5** | Modules fantasma en `settings.gradle.kts` |
| **Global** | **8.2** | **Listo para FASE 18** |

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Replantear arquitectura? | **No** |
| ¿Nuevo ADR obligatorio ahora? | **No** |
| ¿Cambiar dominio / BCs? | **No** |
| ¿Bloquear FASE 18? | **No** |
| ¿Siguiente paso? | **Iniciar FASE 18 — Scheduling (`Appointment`)** |

CodeCore ya es el **corazón reutilizable** de productos SaaS futuros (dental, vet, hospital, lab, …). FASE 18 debe **consumir** Patient/Org vía contracts — no “arreglar” el Core.

---

## 1. Arquitectura general

| Pilar | ¿Coherente? | Evidencia |
|-------|-------------|-----------|
| Modular Monolith | ✅ | Un deploy (`codecore-api`); módulos Gradle por BC |
| Hexagonal | ✅ | Ports in/out; adapters R2DBC/HTTP/IAM |
| DDD estratégico | ✅ | BC cerrados con ADRs + closeouts |
| DDD táctico | ✅ | Aggregates pequeños; IDs entre roots (policy §4–§5) |
| Multi-tenant | ✅ | ADR-003: JWT `tenantId` + membership ACTIVE; filtros por tenant |
| Event readiness | ⚠️ | ADR-002 aceptado; **cero** publicación de eventos en módulos; `shared-events` hueco |

**Conclusión:** La arquitectura runtime es coherente. Los eventos están **conscientemente diferidos** (aceptable hasta que un BC lo exija — típico Appointment/Billing). No inventar event bus “por si acaso” antes de FASE 18.

---

## 2. Bounded Contexts

| BC | Responsabilidad | ¿Límite correcto? |
|----|-----------------|-------------------|
| **IAM** | Identity, Tenant, Membership, Roles, AuthN/Z | ✅ Plataforma |
| **Organization Management** | Organization, Office, StaffAssignment | ✅ Estructura operativa |
| **Clinical Foundation** | Patient (registry) | ✅ Clínico mínimo |

| Pregunta | Respuesta |
|----------|-----------|
| ¿Responsabilidad mal ubicada? | **No** crítica. `MembershipReferencePort` vive en org-application (transicional ADR-013) — deuda conocida, no mal diseño de Patient |
| ¿Aggregate en BC equivocado? | **No** |
| ¿BC demasiado grande? | **No**. Clinical Foundation es deliberadamente un solo aggregate |
| ¿IAM demasiado grande como módulo Gradle? | **Sí como packaging** (un jar monolítico vs org/patient estratificados) — no como modelo de dominio |

---

## 3. Aggregate Roots

| Aggregate | Una responsabilidad | Solo sus invariantes | Tamaño |
|-----------|---------------------|----------------------|--------|
| **Tenant** | Frontera SaaS | ✅ | Correcto |
| **Organization** | Unidad estructural | ✅ (+ guard archive offices en app) | Correcto |
| **Office** | Ubicación bajo org | ✅ | Correcto |
| **StaffAssignment** | Alcance operativo membership↔org/office | ✅ | Correcto (delgado) |
| **Patient** | Identidad clínica registral | ✅ | *Intentionally small* — correcto |

| Pregunta | Respuesta |
|----------|-----------|
| ¿God Aggregate oculto? | **No** |
| ¿Alguno demasiado pequeño? | StaffAssignment es delgado a propósito — no fusionar con Membership |
| ¿Alguno demasiado grande? | **No**. Patient resiste tentación dental/vet |

---

## 4. Contratos e integración

### Forma oficial única

```text
Consumer BC
  → depende de {provider}-contract
  → valida con {Resource}ReferencePort
  → almacena solo IDs
  → nunca SQL / Repository del provider
```

Documentado en ADR-011 · ADR-013 · Consumption Guides. **Patient es la prueba real** del patrón.

| Artefacto | Estado |
|-----------|--------|
| ADR-011 / 012 / 013 | Aceptados; 012 congelado |
| Organization Consumption Guide | Vigente |
| Patient Consumption Guide | Vigente (17.8) |
| `OrganizationReferencePort` + adapter | ✅ |
| `PatientReferencePort` + adapter | ✅ |
| `OfficeReferencePort` | Declarado; **sin adapter** |
| `StaffAssignmentReferencePort` | Solo ADR; **sin código** |
| `MembershipReferencePort` | Transicional en org-application |

### Inconsistencias (no bloqueantes)

| Tema | Severidad | Nota |
|------|-----------|------|
| Familia ReferencePort incompleta | Media | Completar en FASE 18 al necesitar Office/Staff |
| `api(domain)` en contracts | Baja | ADR-013 §8 lo permite; acopla VOs del provider |
| Tres `TenantId` (iam/org/patient) | Media | Fricción de conversión; no rompe aislamiento |

**¿Todos los consumers siguen el mismo patrón?** Hoy solo Patient consume Org — y lo hace bien. FASE 18 será el segundo stress-test.

---

## 5. Dependencias Gradle

| Regla | ¿Cumple? |
|-------|----------|
| Consumer → solo contract (main) | ✅ Patient → `organization-contract` |
| Sin ciclos BC | ✅ |
| Sin patient → org-infra (main) | ✅ (solo test IT) |
| Sin org-domain → IAM | ✅ |

### Deuda de higiene

| Ítem | Impacto |
|------|---------|
| `settings.gradle.kts` incluye `tenant-management/*`, `user-management/*`, `authorization-management/*` **sin** `build.gradle.kts` | Confusión / ruido |
| `shared-events` vacío pero incluido | Expectativa falsa de eventing |
| `spring-boot-starter-data-r2dbc` en `*-application` sin uso | Dependencia innecesaria |

Ninguno bloquea FASE 18.

---

## 6. Reference Contracts — escalabilidad a 10+ BCs

ADR-013 escala si se respeta la disciplina:

| Futuro BC | Port esperado | ¿Patrón soporta? |
|-----------|---------------|------------------|
| Appointment | Patient + Office + StaffAssignment (+ Org) | ✅ |
| MedicalRecord | Patient + Org | ✅ |
| Billing / Invoice | Patient? + Org + Subscription | ✅ |
| Inventory | Office + Org | ✅ |
| Subscription | Membership / Tenant | ✅ (falta `iam-contract`) |
| Notification / Audit / Reporting | Preferir proyección / outbox — no engordar ReferencePorts | ✅ si ports siguen **mínimos** |

**Riesgo de escala:** convertir ReferencePorts en “API de lectura completa”. Mitigación: ADR-013 §3 — boolean primero; views solo si invariante lo exige.

---

## 7. Authorization

| Catálogo | Permisos |
|----------|----------|
| IAM (plataforma) | ~16 |
| Organization (+ office + staff) | 12 |
| Patient | 4 |
| `IamPermissionCatalog.ALL` | 32 |

**Patrón de crecimiento:** catalog en contract → Flyway seed aditivo → backfill roles → ALL crece. **Escalable** a cientos de códigos **si**:

- Se mantiene `resource:action` corto  
- No se inventan verbos verticales (`patient:odontogram`)  
- Roles sistema no acumulan “todos los permisos del mundo” sin matriz consciente  

| Observación | Severidad |
|-------------|-----------|
| MANAGER archiva Patient pero no Organization | Baja — intencional (matrices distintas); documentar en onboarding |
| Activate usa `:update` | Baja — consistente Org/Patient |

**Antes de FASE 18:** no hace falta rediseñar RBAC. Appointment añadirá `appointment:*` con el mismo patrón V15/V19.

---

## 8. Persistencia

| Schema | Ownership | FK cross-BC |
|--------|-----------|-------------|
| `iam` | IAM | Solo internas |
| `org` | Organization | `tenant_id` / `membership_id` lógicos — **sin FK** a iam |
| `clinical` | Clinical Foundation | `tenant_id` / `primary_organization_id` lógicos — **sin FK** a iam/org |

Ownership correcto. Sin duplicación estructural de Patient. Índices tenant-aware presentes.

---

## 9. API

| Aspecto | Org / Office / Staff | Patient | Paridad |
|---------|----------------------|---------|---------|
| Prefijo | `/api/v1/org/...` | `/api/v1/clinical/...` | ✅ |
| Soft archive / activate | ✅ | ✅ | ✅ |
| Paginación | page/size/sort/status | + filtros registry | ✅ |
| Sin `tenantId` en response | ✅ | ✅ | ✅ |
| 401 / 403 / 404 / 409 | ✅ | ✅ | ✅ |

Filosofía alineada. DTOs tipados por recurso (no un mega-DTO compartido) — correcto.

---

## 10. Escalabilidad a 5 años

Escenario: Appointment, Encounter, MedicalRecord, TreatmentPlan, Odontogram, Invoice, Inventory, Subscription, CRM, Notification, Workflow, Analytics…

| ¿Soporta? | Cómo |
|-----------|------|
| Sí | Un BC nuevo = domain/app/infra/contract + schema propio + ReferencePorts + seeds + admin API espejo |
| Sí | Verticales (Dental/PetNova) como **product packs** encima — no dentro de Patient |
| Condicionado | Eventos / outbox cuando aparezca consistencia eventual real |
| Condicionado | `iam-contract` cuando Membership deje de ser port transicional |
| No cambiar hoy | No introducir microservicios, CQRS global ni event sourcing “preventivo” |

### ¿Qué cambiaría HOY?

| Cambio | ¿Hacerlo hoy? |
|--------|---------------|
| Reabrir Patient / Org | **No** |
| Implementar Kafka/event bus completo | **No** |
| Completar Office/Staff ReferencePorts | **Durante FASE 18** (just-in-time) |
| Limpiar includes fantasma en settings | Opcional higiene (bajo riesgo) |

---

## 11. Framework Review

### ¿CodeCore ya se comporta como Framework SaaS?

**Sí, en lo esencial.**

| Señal de Framework | Evidencia |
|--------------------|-----------|
| BC cerrados con contrato de consumo | Closeouts 16.10 / 17.8 + guides |
| Integración estandarizada | ADR-013 ReferencePorts |
| APIs admin isomorfas | Org ↔ Patient |
| Dominio vertical-agnóstico | Patient sin dental/vet |
| Política de desarrollo | DEVELOPMENT-POLICY-FASE-16-PLUS |
| Multi-tenant de plataforma | IAM + JWT + membership |

| Residuo de “aplicación” | Evidencia |
|-------------------------|-----------|
| Eventing declarado y vacío | ADR-002 / `shared-events` |
| Packaging IAM monolítico | Un módulo vs estratificación org/patient |
| Settings con módulos fantasma | Ruido de evolución histórica |

**Juicio:** El **modelo mental y las reglas** ya son de framework. El **código de plataforma transversal** (eventos, observabilidad, invitations) aún es foundation — y está correctamente aplazado (FASE 22–24).

---

## 12. Riesgos

### Críticos

*Ninguno que bloquee FASE 18.*

### Altos

| Riesgo | Mitigación |
|--------|------------|
| Engordar Patient en FASE 18–19 “porque Appointment lo necesita” | ADR-012 frozen; consumo guide; auditoría obligatoria |
| ReferencePorts convertidos en query APIs gordas | ADR-013 §3 — boolean primero |

### Medios

| Riesgo | Cuándo abordar |
|--------|----------------|
| `MembershipReferencePort` fuera de `iam-contract` | Al abrir IAM contract (pre-Subscription / cleanup) |
| Sin Domain Events cuando Scheduling necesite proyección | Introducir al primer caso real — no antes |
| Triplicado `TenantId` | Consolidación opcional (shared-kernel / contract) post-18 si duele |
| Matriz permisos de roles sistema crece sin gobernanza | Documentar matriz por BC en cada seed |

### Bajos

| Riesgo | Nota |
|--------|------|
| Modules fantasma en settings | Limpieza cosméticas |
| R2DBC starter en application | Quitar cuando se toque el módulo |
| ADR-009 vs ROADMAP números de fase (audit 21 vs 23) | Alinear docs en FASE 22+ |

### ¿Resolver antes de FASE 18?

**Ningún riesgo medio/alto requiere fix previo.** Los altos se mitigan con **disciplina**, no con código nuevo.

---

## 13. Recomendaciones priorizadas

### Debe hacerse antes de FASE 18

| # | Acción |
|---|--------|
| 1 | **Nada estructural.** Mantener ADR-012/013 y BCs cerrados |
| 2 | Planificar Appointment **consumiendo** `PatientReferencePort` + ports Org (crear adapters faltantes **en el paso que los necesite**) |

### Puede esperar

| # | Acción |
|---|--------|
| 1 | Adapter `OfficeReferencePort` / `StaffAssignmentReferencePort` |
| 2 | Extraer `iam-contract` + mover `MembershipReferencePort` |
| 3 | Primer Domain Event / outbox (cuando haya invariante eventual) |
| 4 | Limpiar includes fantasma y `shared-events` vacío o implementar mínimo |
| 5 | Unificar `TenantId` (evaluación costo/beneficio) |
| 6 | Quitar R2DBC de capas application |

### No vale la pena hacerlo

| # | Acción |
|---|--------|
| 1 | Microservicios / split deploy por BC ahora |
| 2 | CQRS / Event Sourcing preventivo |
| 3 | Orquestador genérico / workflow engine antes de tener workflows |
| 4 | “Mega shared DTO library” para todas las admin APIs |
| 5 | Reabrir Patient para species/breed/odontogram |
| 6 | FK físicas cross-schema “para integridad” |

---

## Fortalezas

1. BC cerrados con ADRs y guías de consumo reales.  
2. Reference Contracts probados (Org ← Patient).  
3. Aggregates pequeños y vertical-agnósticos.  
4. Multi-tenant sin FK cross-BC.  
5. Admin HTTP isomorfo y predecible.  
6. Metodología (auditorías, DoD, closeouts) que escala con equipos/IA.  

---

## Debilidades

1. Event readiness documental ≠ implementada.  
2. Familia ReferencePort incompleta para Scheduling.  
3. IAM packaging monolítico vs org/patient estratificados.  
4. Higiene Gradle / módulos fantasma.  
5. VOs de referencia duplicados (`TenantId` ×3).  

---

## Oportunidades

1. FASE 18 como **segundo consumer** que consolida ADR-013.  
2. Checklist de nuevo BC (copy-paste de Patient closeout).  
3. Product packs post 17–19 sin tocar Core.  
4. `iam-contract` cuando Subscription/Billing exijan Membership limpio.  

---

## Checklist arquitectónico

- [x] Modular Monolith coherente  
- [x] Hexagonal por BC de negocio  
- [x] BCs con límites claros  
- [x] Aggregates sin God Aggregate  
- [x] Una forma oficial de integración (ReferencePorts)  
- [x] Sin dependencias Gradle consumer→provider infra (main)  
- [x] Sin ciclos  
- [x] Schemas con ownership; sin FK cross-BC  
- [x] Permisos aditivos por BC  
- [x] API admin consistente  
- [x] Multi-tenant JWT + membership  
- [x] Patient vertical-agnóstico  
- [ ] Event pipeline productivo (diferido — OK)  
- [ ] Familia ReferencePort completa (just-in-time FASE 18)  
- [x] **Listo para FASE 18 sin replanteo**  

---

## ROADMAP

**Sin cambios.** Esta revisión no detecta corrección arquitectónica obligatoria. El siguiente paso sigue siendo:

**FASE 18 — Scheduling (`Appointment`)**.

---

## Referencias

- [ROADMAP.md](ROADMAP.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-012](ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md) · [PATIENT-CONSUMPTION-GUIDE.md](PATIENT-CONSUMPTION-GUIDE.md)  
- [PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md](../audits/PASO-17.8-CLINICAL-FOUNDATION-CLOSEOUT.md) · [PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md](../audits/PASO-16.10-ORGANIZATION-MANAGEMENT-CLOSEOUT.md)  
- [ADR-009-PRODUCTION-READINESS-BACKLOG.md](ADR-009-PRODUCTION-READINESS-BACKLOG.md)  
