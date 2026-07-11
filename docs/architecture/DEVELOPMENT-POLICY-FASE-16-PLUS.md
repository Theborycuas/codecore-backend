# Política de Desarrollo Arquitectónico — CodeCore (FASE 16+)

**Estado:** Vigente — *Constitution Document* del proyecto  
**Fecha:** 2026-06-28  
**Alcance:** Todo el proyecto a partir de FASE 16  
**Relacionado:** [ROADMAP.md](ROADMAP.md) · ADR-010 · ADR-008

---

## Contexto

**FASE 10–15** centró la fundación técnica:

- DDD táctico · Hexagonal · Modular Monolith
- IAM · Authorization · RBAC · HTTP Administration
- Persistencia · Flyway · Seguridad · Multi-tenant

Esa etapa se considera **estable**.

**A partir de FASE 16** el foco pasa a construir el **modelo de negocio** de CodeCore como framework SaaS sólido, coherente, extensible y mantenible — no solo módulos aislados, sino un **framework de negocio unificado**.

---

## 0. Filosofía

| Antes (FASE 10–15) | Ahora (FASE 16+) |
|--------------------|------------------|
| Construir módulos | Construir un **framework de negocio** donde todos los módulos parezcan diseñados por la misma persona |

Eso implica coherencia en:

- Estilo y convenciones de código
- Nombres de aggregates, VOs, permisos y rutas HTTP
- Lifecycle (`create` · `rename` · `archive` · `activate` — no delete físico salvo excepción ADR)
- Forma de las Administration APIs (`/api/v1/{bc}/...`, paginación, filtros `status`)
- OpenAPI y documentación de pasos
- Estructura de carpetas Gradle (domain → application → infrastructure → contract)
- Patrones de testing (domain unit · use case · WebFlux IT)
- Filosofía DDD compartida

**Objetivo de largo plazo:** dentro de dos o tres años, con varios desarrolladores o asistentes de IA, CodeCore sigue siendo arquitectónicamente coherente. Esa consistencia es uno de los activos más valiosos del framework.

---

## 1. Cambia el foco

| Prioridad | Qué |
|-----------|-----|
| **Primero** | Modelo de dominio correcto |
| **Después** | Casos de uso, persistencia, HTTP |

**Reglas:**

- El dominio prevalece sobre la implementación.
- **Nunca** diseñar desde la base de datos.
- **Nunca** diseñar desde el controller.
- **Siempre** diseñar desde el modelo de negocio.

---

## 2. Decisiones irreversibles

Hay decisiones que, una vez implementadas, cuestan **muchísimo** cambiar. Requieren **más reflexión** que elegir un endpoint o un DTO.

| Área | Ejemplos de decisión irreversible |
|------|-----------------------------------|
| **Aggregate Root** | Qué entidad es root y cuál no |
| **Ownership** | Quién es dueño de la verdad del dato |
| **Bounded Context** | Límites del módulo y responsabilidades |
| **Relaciones entre módulos** | Referencias, acoplamientos, contratos |
| **Eventos de dominio** | Introducción de eventos cross-aggregate |
| **Lifecycle** | Archive vs delete, estados terminales |
| **Jerarquía operativa** | Tenant → Organization → Office → … |

**Regla:** las decisiones consideradas **irreversibles** deben validarse mediante **auditoría de arquitectura** antes de implementarse.

### Triggers de auditoría obligatoria (decisiones irreversibles)

- Creación de un **Aggregate Root** nuevo
- División o fusión de un **bounded context**
- Introducción de un **nuevo módulo Gradle**
- Cambio del **ownership** de una entidad
- **Modificación** de un ADR existente
- Introducción de **eventos de dominio**
- Cambios de **jerarquía** Tenant → Organization → Office (u otra estructura operativa)

**Objetivo:** evitar romper el dominio dentro de seis meses por decisiones apresuradas.

---

## 3. Auditorías estratégicas

**No** hay auditoría obligatoria antes de cada paso rutinario.

**Sí** hay auditoría obligatoria cuando aplica §2 (decisiones irreversibles) o cuando aparece un aggregate que conecta bounded contexts o introduce invariantes importantes.

### Triggers adicionales (ejemplos)

| Aggregate / área | Motivo |
|------------------|--------|
| **StaffAssignment** (16.7) | Alcance organizacional sobre Membership (IAM) |
| **Patient** | Primer aggregate clínico |
| **Appointment** | Conecta scheduling + clinical + org |
| **MedicalRecord** | Invariantes clínicas y trazabilidad |
| **Billing** | Finanzas e invariantes propias |
| **Subscription** | Modelo comercial SaaS |
| **Inventory** | Stock y operaciones |
| Cualquier aggregate **cross-BC** | Riesgo de acoplamiento o invariantes mal ubicadas |

### Formato

`docs/audits/PASO-x.y-{NOMBRE}-AUDIT.md`

**Objetivo:** validar el modelo de dominio antes de implementar.

---

## 4. Referencias por ID (regla estricta)

**Ningún Aggregate Root** podrá mantener referencias a otros aggregates mediante **objetos completos**.

**Únicamente** podrá mantener **IDs** (value objects de referencia: `TenantId`, `OrganizationId`, `MembershipId`, etc.).

La navegación entre aggregates se resuelve **siempre** mediante:

- **Application Services** (orquestación en un use case), o
- **Query Ports** (lectura desacoplada)

**Prohibido en el dominio:** cargar otro aggregate root dentro del aggregate actual para mutar su estado o garantizar reglas ajenas.

Esta regla será crítica cuando aparezcan Patient, Appointment, MedicalRecord, Billing, etc.

**Guía de consumo:** [ORGANIZATION-CONSUMPTION-GUIDE.md](ORGANIZATION-CONSUMPTION-GUIDE.md) · [ADR-011](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)

---

## 5. Consistencia entre aggregates (DDD puro)

> **Cada Aggregate Root es responsable únicamente de sus propias invariantes.**

> **Nunca** garantizará consistencia transaccional sobre otro Aggregate Root.

- Una transacción = un aggregate (salvo orquestación eventual explícita documentada en ADR).
- Reglas que cruzan aggregates → validación en **application layer**, **sagas** o **eventual consistency** — nunca FK + cascade que simule un mega-aggregate.
- Ejemplo ya aplicado: archivar Organization no muta Office; se valida en use case (`countActiveByOrganizationId` → 409).

---

## 6. Preguntas obligatorias (nuevo Aggregate Root)

Responder explícitamente **en la auditoría** (§2–§3) antes de implementar:

### Aggregate Root

- ¿Quién es realmente el Aggregate Root? ¿Por qué? ¿Por qué no otra entidad?

### Ownership

- ¿Quién es dueño de la información?
- ¿Qué aggregate mantiene la consistencia?
- ¿Qué aggregate puede modificar los datos? ¿Cuáles solo consultan?

### Invariantes

- ¿Qué reglas de negocio protege?
- ¿Qué estados inválidos impide?
- ¿Qué operaciones deben ser atómicas?
- ¿Qué nunca debe romperse?

### Referencias

- ¿Qué relaciones mantiene **solo mediante IDs**? (§4)
- ¿Qué aggregates **nunca** debe cargar directamente?
- ¿Qué relaciones se resuelven vía queries o servicios de aplicación?

### Lifecycle

- ¿Cómo nace? ¿Cómo cambia? ¿Cómo finaliza?
- ¿Archive? ¿Disable? ¿Delete?
- ¿Qué significado tiene cada transición?

### Escalabilidad

¿El diseño soporta sin romper el modelo:

- Multi-tenant · Multi-organization · Multi-office
- Miles / millones de registros
- Evolución futura del dominio

---

## 7. Bounded contexts limpios

- **No** acoplar módulos innecesariamente.
- Referencias **solo por ID** (§4).
- Evitar dependencias directas entre bounded contexts en Gradle (contract module para permisos/contratos compartidos).
- **No** introducir FK físicas entre módulos salvo decisión arquitectónica explícita (ADR).

Independencia entre, entre otros:

IAM · Organization Management · Clinical · Billing · Scheduling · Inventory

---

## 8. Checklist previo a implementar

**Antes de escribir código**, responder esta checklist. Publicar respuestas en auditoría o en el documento del paso.

| # | Ítem | ✓ |
|---|------|---|
| 1 | Aggregate Root identificado | □ |
| 2 | Ownership definido | □ |
| 3 | Invariantes definidas | □ |
| 4 | Lifecycle definido | □ |
| 5 | Estados definidos | □ |
| 6 | Permisos definidos | □ |
| 7 | Relaciones **solo** mediante IDs | □ |
| 8 | Bounded Context correcto | □ |
| 9 | No rompe ningún ADR vigente | □ |
| 10 | Escalable multi-tenant | □ |
| 11 | Escalable multi-organization | □ |
| 12 | Escalable para millones de registros | □ |

**Regla:** si **alguna** respuesta es **No** → **detener** la implementación. Resolver diseño o auditoría primero.

---

## 9. No reinventar

A partir de FASE 16 aparecerán muchos módulos con forma similar:

Organization · Office · Patient · Provider · Employee · Product · Warehouse · …

Todos compartirán el mismo **patrón operativo**:

| Capa | Patrón consolidado (referencia) |
|------|----------------------------------|
| Lifecycle | `create` · `rename` · `archive` · `activate` — sin delete físico |
| HTTP | Administration API bajo `/api/v1/{bc}/...` |
| Listados | Paginación `page`/`size`/`sort` + filtro `status=ACTIVE\|ARCHIVED\|ALL` |
| Auth | `@RequiresPermission` · tenant desde JWT |
| Persistencia | Schema propio · R2DBC · sin FK cross-BC |
| Docs | `PASO-x.y-*.md` + ROADMAP |

**Reglas:**

1. Antes de introducir un **patrón nuevo**, comprobar si el proyecto ya tiene uno equivalente.
2. Si existe patrón consolidado (**IAM** FASE 15 o **Organization** FASE 16) → **reutilizarlo**.
3. Desviación solo con **justificación arquitectónica documentada** (auditoría o ADR).

Referencias de implementación:

- [ADR-008 — IAM Administration API](ADR-008-IAM-ADMINISTRATION-API.md)
- [ADR-010 — Organizations Model](ADR-010-ORGANIZATIONS-MODEL.md)
- [PASO-16.3.1 — Organization Admin API Audit](../audits/PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md)

---

## 10. Secuencia de trabajo (orden fijo)

| # | Fase | Cuándo |
|---|------|--------|
| 1 | **Checklist** (§8) | Siempre |
| 2 | **Auditoría** | Cuando aplique §2–§3 |
| 3 | **ADR** | Si cambia arquitectura o ADR existente |
| 4 | **Dominio** | Aggregate, VOs, invariantes |
| 5 | **Casos de uso** | Application layer |
| 6 | **Persistencia** | Flyway, R2DBC |
| 7 | **HTTP** | Controllers, DTOs |
| 8 | **OpenAPI** | Documentación API |
| 9 | **Tests** | Dominio, use cases, IT |
| 10 | **Documentación** | `PASO-x.y-*.md` |
| 11 | **ROADMAP** | Estado y enlaces |

**Nunca invertir este orden.**

---

## 11. Documentación viva

Cada paso actualiza:

- [ROADMAP.md](ROADMAP.md)
- ADR correspondiente (si aplica)
- Documento del paso (`docs/audits/PASO-*.md`)
- Auditoría (cuando exista)

Toda decisión importante — especialmente las **irreversibles** (§2) — queda documentada.

---

## 12. Rol del agente / equipo

Comportarse como **arquitecto de software**, no solo como generador de código.

| Situación | Acción |
|-----------|--------|
| Decisión irreversible (§2) | Auditoría **antes** de código |
| Checklist incompleta (§8) | **Detener** implementación |
| Patrón ya existe (§9) | **Reutilizar** IAM / Organization |
| Dominio claro, paso rutinario | Implementar sin auditoría extra |
| Riesgo para el futuro del framework | Proponer auditoría o ADR |

**Objetivo:** CodeCore como framework SaaS coherente durante muchos años — no solo cerrar fases.

---

## Referencias

- [ROADMAP.md](ROADMAP.md) — FASE 16 cerrada; FASE 17 Clinical Foundation en curso  
- [PASO-17.0 — Clinical Foundation Planning](../audits/PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)  
- [ADR-010 — Organizations Model](ADR-010-ORGANIZATIONS-MODEL.md)  
- [ADR-011 — Organization Integration Patterns](ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ADR-008 — IAM Administration API](ADR-008-IAM-ADMINISTRATION-API.md)  
- [PASO-16.3.1 — Organization Admin API Audit](../audits/PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md)
