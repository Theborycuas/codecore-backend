# PASO 16.0.1 — Organizations Roadmap & Architecture Decisions

**Fecha:** 2026-06-22  
**Estado:** ✅ Diseño aprobado para implementación  
**Dependencias:** FASE 15 IAM **FOUNDATION COMPLETE** · [PASO-16.0-ORGANIZATIONS-AUDIT.md](PASO-16.0-ORGANIZATIONS-AUDIT.md)  
**Próximo paso:** **16.1 — Organizations Domain Foundation** (sin código hasta entonces)

---

## Objetivo

Definir FASE 16 completa antes de escribir código: qué es una **Organization** en CodeCore, cómo se relaciona con IAM, y el roadmap de pasos de implementación.

**Restricción:** Este documento es diseño únicamente. No introduce entidades, migraciones ni endpoints.

---

## Evidencia del modelo actual

### Tenant — frontera SaaS de aislamiento

```11:12:codecore-backend/modules/identity-access-management/src/main/java/com/codecore/iam/domain/model/tenant/Tenant.java
 * Tenant aggregate root — top-level multi-tenant boundary (not tenant-scoped under another tenant).
 */
```

```28:29:codecore-backend/apps/codecore-api/src/main/resources/db/migration/V5__create_iam_tenant_table.sql
COMMENT ON TABLE iam.tenant IS
    'Tenant aggregate root — organizational boundary for IAM multi-tenancy.';
```

El comentario SQL usa *organizational boundary* en sentido **IAM/SaaS**, no en sentido de negocio (clínica, departamento, sucursal). El dominio Java es explícito: **top-level multi-tenant boundary**.

Responsabilidades actuales: nombre, estados `ACTIVE | SUSPENDED | DISABLED`, enforcement vía `TenantOperationalGuard`, administración en `/api/v1/iam/tenants/current`.

### Identity — autenticación global

ADR-006 (aceptado):

- Una `Identity` por email normalizado en toda la plataforma.
- **No** lleva `TenantId` como parte del modelo canónico.
- JWT `subject` = `identityId`.

`Identity` **no** modela pertenencia operativa ni estructura de negocio.

### Membership — pertenencia persona ↔ tenant (IAM)

```16:18:codecore-backend/modules/identity-access-management/src/main/java/com/codecore/iam/domain/model/membership/IdentityTenantMembership.java
 * Association aggregate: links one {@link IdentityId} to one {@link TenantId} (N:M across rows).
 * Role assignments (14.4) are owned internally via {@link MembershipRoleAssignment}.
```

Campos: `identityId`, `tenantId`, `status`, `roleAssignments`. **No existe `organizationId`.**

Membership responde: *¿esta persona puede operar en este tenant?* — no *¿en qué clínica o departamento trabaja?*

### RBAC y Authorization — tenant-scoped, membership-scoped

ADR-007 (aceptado):

```text
Identity → IdentityTenantMembership → Role (tenant-scoped) → Permission (global catalog)
```

`AuthorizationContext` actual:

```12:15:codecore-backend/modules/identity-access-management/src/main/java/com/codecore/iam/application/dto/AuthorizationContext.java
public record AuthorizationContext(
        IdentityId identityId,
        TenantId tenantId,
        MembershipId membershipId
```

`IamPermissionCatalog` contiene **solo** permisos IAM (`tenant:*`, `membership:*`, `role:*`, `user:*`). Comentario explícito: *No business-module grants*.

ADR-008 declara Organizations **fuera de alcance** de FASE 15.

### Context Map — tenant ≠ organization

`codecore-specifications/architecture/core/CONTEXT-MAP.md` §26:

| Contexto | Posee |
|----------|-------|
| Tenant Management | lifecycle del tenant, activación, aislamiento |
| Organization Management | companies, branches, organization hierarchy |

Regla crítica: `tenant != organization`.

### Estado en código

**No existe** entidad `Organization`, tabla, módulo Gradle ni permiso `organization:*` en el repositorio. FASE 16 es greenfield de dominio de negocio sobre IAM cerrado.

---

## ¿Qué es una Organization en CodeCore?

### Definición

Una **Organization** es una **unidad estructural de negocio acotada por tenant** que representa una subdivisión operativa del cliente SaaS dentro de su cuenta:

- Grupo dental → clínicas (Norte, Centro, Sur)
- Hospital → departamentos o servicios (Pediatría, Cardiología)
- Empresa → sucursales regionales (Quito, Guayaquil)

### Qué es

| Atributo | Descripción |
|----------|-------------|
| Scope | Siempre bajo un `TenantId` (ADR-003) |
| Propósito | Agrupar operaciones, staff y datos de negocio |
| Relación IAM | Consume `tenantId` del JWT; **no** reemplaza Membership |
| Jerarquía | Padre de `Office` (ubicación física / consultorio) |

### Qué NO es

| Concepto | Por qué no |
|----------|------------|
| Tenant | Tenant = aislamiento SaaS y facturación; Organization = estructura interna del cliente |
| Identity | Identity = credenciales globales; sin datos de negocio |
| Membership | Membership = puerta IAM persona↔tenant; sin scope organizacional |
| Role | Role = autorización RBAC tenant-scoped; no describe estructura física |

### Modelo objetivo FASE 16

```text
Tenant                          ← IAM: aislamiento SaaS, billing, membership
 └── Organization (1..N)       ← Negocio: subdivisión estructural
      └── Office (0..N)          ← Negocio: sede / consultorio / ubicación
           └── StaffAssignment   ← Puente: membershipId + org/office scope
           └── (Patient, …)      ← FASE 19+: datos con scope tenant/org/office
```

### Separación de concerns

```text
IAM (cerrado)                         Organizations (FASE 16+)
─────────────────                     ─────────────────────────
Identity        → quién autentica     Organization  → dónde opera el negocio
Membership      → acceso al tenant     StaffAssignment → en qué org/office
Role/Permission → qué puede hacer     Office        → ubicación concreta
Tenant          → frontera SaaS       Patient scope → política de datos (FASE 19)
```

---

## Decisiones obligatorias

### 1 — Organization pertenece a: **A) Tenant**

| Opción | Veredicto | Justificación |
|--------|-----------|---------------|
| A) Tenant | ✅ **Elegida** | ADR-003 exige `tenant_id` en datos del cliente. CONTEXT-MAP: tenant ≠ organization pero organization vive bajo tenant. Escenarios multi-sede requieren N organizations por tenant. |
| B) Identity | ❌ | Identity es global (ADR-006). Una persona en varios tenants no implica una org global. |
| C) Membership | ❌ | Membership ya es el vínculo N:M Identity↔Tenant con RBAC. Mezclar estructura de negocio en IAM violaría PASO-16.0 y reabriría FASE 15. |

### 2 — ¿Puede un Tenant tener múltiples Organizations?

**Sí.** Obligatorio para Dental (3 clínicas), Médico (3 departamentos), Empresarial (3 ciudades). Cardinalidad: `1 Tenant → 1..N Organizations`.

### 3 — ¿Puede una Organization existir sin Tenant?

**No.** Sin `tenantId` no hay aislamiento ADR-003 ni contexto JWT. `organization.tenant_id` NOT NULL, inmutable tras creación.

### 4 — ¿Office depende de Organization o de Tenant?

**Office depende de Organization** (no directamente de Tenant).

| Relación | Regla |
|----------|-------|
| `Organization.tenantId` | FK lógica / columna obligatoria |
| `Office.organizationId` | FK obligatoria |
| `Office.tenantId` | Denormalizado para queries tenant-scoped (patrón ADR-003) |

Justificación: un consultorio pertenece a una clínica concreta, no al tenant abstracto. El tenant se propaga por denormalización en persistencia.

### 5 — ¿Staff pertenece a Tenant, Organization, Office o combinación?

**Combinación — modelo de dos capas:**

| Capa | Mecanismo | Obligatorio |
|------|-----------|-------------|
| Acceso plataforma | `IdentityTenantMembership` en tenant | Sí — sin membership no hay login ni RBAC |
| Scope operativo | `StaffAssignment(membershipId, organizationId?, officeId?)` | Org/office según rol |

- Administrador de tenant: membership + assignment sin org (scope tenant-wide).
- Odontólogo: membership + assignment a `Organization` + opcionalmente `Office`.
- Recepción: membership + assignment a un `Office`.

**Staff no duplica Identity ni Membership** — referencia `membershipId` (PASO-16.0).

### 6 — ¿Patient pertenece a Tenant, Organization, Office o combinación?

**Combinación — política de visibilidad (diseño FASE 16, implementación datos en FASE 19):**

| Modo | Scope | Caso Dental |
|------|-------|-------------|
| Tenant-wide | `tenantId` only | Paciente compartido entre sedes |
| Organization-scoped | `tenantId` + `organizationId` | Exclusivo de Dental Norte |
| Office-scoped | `tenantId` + `organizationId` + `officeId` | Exclusivo de Consultorio 2 |

FASE 16 define el **modelo de scoping** y FKs; entidad `Patient` queda para módulos de negocio (FASE 19).

### 7 — ¿Organization es aggregate root?

**Sí.**

| Criterio | Aplicación |
|----------|------------|
| Consistencia | Nombre, código, estado, reglas de unicidad por tenant |
| Lifecycle | `ACTIVE`, `ARCHIVED` (sin delete físico si hay offices activos) |
| Invariantes | `tenantId` inmutable; código único por `(tenant_id, code)` |
| Tamaño | Aggregate pequeño — offices en aggregate separado `Office` |

`Office` es **aggregate root separado** (referencia `organizationId`), no entidad hija dentro del aggregate Organization — evita aggregates oversized (spec 01-aggregate-design-rules).

### 8 — ¿Organizations requiere permisos propios?

**Sí — catálogo nuevo, no reutilizar IAM.**

| Permiso | Uso |
|---------|-----|
| `organization:read` | Listar / obtener |
| `organization:create` | Alta |
| `organization:update` | Renombrar, archivar |
| `organization:delete` | Baja lógica / archivar |
| `office:read` | FASE 16.5+ |
| `office:create` | FASE 16.5+ |
| `office:update` | FASE 16.5+ |
| `office:delete` | FASE 16.5+ |
| `staff-assignment:read` | FASE 16.6 |
| `staff-assignment:update` | FASE 16.6 |

Rationale:

- `tenant:*` administra el **contrato SaaS**, no clínicas internas.
- `membership:*` administra **acceso IAM**, no ubicación operativa.
- ADR-007: permisos globales en catálogo; roles tenant-scoped reciben grants vía seeds + admin.
- Seeds en migración dedicada (V14+); `OrganizationPermissionCatalog` en nuevo módulo.

RBAC sigue membership-scoped (ADR-007). FASE 16 **no** introduce organization-scoped roles — evaluación org/office es filtro de aplicación + permisos de recurso, no nuevo aggregate Role.

### 9 — ¿Dónde vive Organizations?

**Nuevo módulo Gradle + nuevo bounded context** — no dentro de `identity-access-management`.

| Opción | Veredicto |
|--------|-----------|
| Dentro de IAM | ❌ Mezcla dominio negocio con auth (riesgo PASO-16.0) |
| Nuevo módulo Gradle | ✅ `modules/organization-management` (hexagonal, como placeholders existentes) |
| Bounded context separado | ✅ Alineado con CONTEXT-MAP §26 |

Estructura propuesta:

```text
modules/organization-management/
  organization-domain/
  organization-application/
  organization-infrastructure/
  organization-contract/
```

- Schema PostgreSQL: `org` (Flyway en `codecore-api`, patrón `iam`)
- HTTP: `/api/v1/org/**` (simétrico a `/api/v1/iam/**`, ADR-008)
- Depende de: `shared-kernel`, `shared-tenancy`, `identity-access-management` (solo **contract** para `MembershipId`, `AuthorizationContext` accessor — sin mutar IAM)

**ADR-010** (Organizations Model) se creará en paso 16.1 — documenta estas decisiones formalmente. No altera ADR-006 ni ADR-007.

---

## Jerarquía y escenarios

### Escenario Dental

```text
Tenant: Grupo Dental Sonrisa
├── Organization: Dental Norte
│   ├── Office: Consultorio 1
│   └── Office: Consultorio 2
├── Organization: Dental Centro
│   └── Office: Consultorio 3
└── Organization: Dental Sur
```

Staff: `membership` en tenant + `StaffAssignment` por org/office.  
Patients: tenant-wide o `organizationId` según política.

### Escenario Médico

```text
Tenant: Hospital ABC
├── Organization: Pediatría
├── Organization: Cardiología
└── Organization: Emergencias
```

Offices opcionales (salas, pisos) bajo cada departamento.

### Escenario Empresarial

```text
Tenant: Empresa XYZ
├── Organization: Quito
├── Organization: Guayaquil
└── Organization: Cuenca
```

---

## Reglas de implementación (heredadas)

- DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC
- Toda query filtrada por `AuthorizationContext.tenantId()`
- `TenantOperationalGuard` antes de operaciones org
- Sin microservicios · CQRS · Event Sourcing · Saga
- **No modificar IAM** salvo: (a) seeds de permisos `organization:*` en `iam.permission`, (b) actualizar `SystemRoleTemplate` OWNER/ADMIN — cambio acotado, no reabre FASE 15

---

## Roadmap FASE 16 — pasos

| Paso | Nombre | Estado | Entregable principal |
|------|--------|--------|----------------------|
| **16.0** | Organizations Audit | ✅ | [PASO-16.0-ORGANIZATIONS-AUDIT.md](PASO-16.0-ORGANIZATIONS-AUDIT.md) |
| **16.0.1** | Organizations Roadmap & Decisions | ✅ | Este documento |
| **16.1** | Organizations Domain Foundation | ⏳ | ADR-010, aggregate `Organization`, value objects, ports |
| **16.2** | Organization Persistence | ⏳ | Schema `org`, Flyway V14, R2DBC repositories |
| **16.3** | Organization Permission Seeds | ⏳ | `organization:*` en catálogo + OWNER/ADMIN grants |
| **16.4** | Organization Administration API | ⏳ | CRUD `/api/v1/org/organizations` + `@RequiresPermission` |
| **16.5** | Office Domain & Persistence | ⏳ | Aggregate `Office`, tablas, repositorios |
| **16.6** | Office Administration API | ⏳ | CRUD `/api/v1/org/offices` + `office:*` |
| **16.7** | Staff Organizational Assignment | ⏳ | `StaffAssignment`, API, `staff-assignment:*` |
| **16.8** | Organization Authorization Patterns | ⏳ | Filtros tenant/org en use cases; documentación scoping Patient |
| **16.9** | Organization Verification | ⏳ | E2E `OrganizationVerificationIT` — journey completo |
| **16.10** | Organizations Closeout | ⏳ | Cierre fase, actualización ROADMAP, OpenAPI grupo `org-administration` |

### Modo de trabajo (cada paso)

1. Auditoría mínima del paso anterior  
2. Diseño (doc `PASO-16.x-*.md`)  
3. Implementación acotada  
4. Tests del paso  
5. Actualizar ROADMAP  

### 16.1 — Domain Foundation (detalle)

- ADR-010 Organizations Model  
- `OrganizationId`, `OrganizationCode`, `OrganizationName`, `OrganizationStatus`  
- Aggregate `Organization` con factory + invariantes  
- Ports: `OrganizationRepository`, `OrganizationQueryPort`  
- Sin HTTP, sin Flyway  

### 16.2 — Persistence (detalle)

```sql
-- Conceptual (implementar en 16.2)
org.organization (
  organization_id UUID PK,
  tenant_id UUID NOT NULL,
  code VARCHAR NOT NULL,
  name VARCHAR NOT NULL,
  status VARCHAR NOT NULL,
  created_at, updated_at,
  UNIQUE (tenant_id, code)
)
```

### 16.4 — API (detalle)

Patrón ADR-008:

```text
GET/POST/PUT/DELETE /api/v1/org/organizations
GET/PUT             /api/v1/org/organizations/{id}
```

Tenant desde JWT; cross-tenant → 403.

### 16.7 — Staff Assignment (detalle)

```text
org.staff_assignment (
  assignment_id UUID PK,
  tenant_id UUID NOT NULL,
  membership_id UUID NOT NULL,  -- FK lógica a iam.identity_tenant_membership
  organization_id UUID,
  office_id UUID,
  ...
)
```

Validaciones: membership.tenantId == organization.tenantId; office pertenece a organization.

### 16.9 — Verification journey

1. Bootstrap tenant + OWNER  
2. Crear 2 organizations  
3. Crear offices bajo cada org  
4. Crear membership staff + assignment  
5. Login staff → listar solo orgs del tenant  
6. Cross-tenant org access → 404/403  
7. Permisos `organization:*` enforced  

---

## Fuera de alcance FASE 16

| Ítem | Fase |
|------|------|
| Entidad Patient | 19 |
| Organization-scoped RBAC / roles por org | Post-16 (si necesario) |
| Invitations | 17 |
| `organizationId` en JWT | Opcional futuro (ADR-009 / ROADMAP deuda) |
| Extracción microservicio | Nunca en roadmap actual |

---

## Decisión final — ¿La estructura soporta los escenarios?

### **SÍ**

| Escenario | Cómo se soporta |
|-----------|-----------------|
| Clínicas multi-sede | N organizations + M offices por tenant |
| Hospitales por departamento | Organization = departamento; offices opcionales |
| Consultorios | Office bajo Organization |
| Empresas multi-sucursal | Organization = sucursal regional |
| SaaS multi-tenant | Tenant permanece frontera ADR-003; orgs aisladas por `tenant_id` |

**Justificación técnica:**

1. **Aislamiento:** `tenant_id` en toda tabla `org.*` + filtro en repositorios + JWT — compatible ADR-003.
2. **IAM estable:** Membership y RBAC sin cambios estructurales; staff usa `membershipId` como puente.
3. **Flexibilidad de datos:** Patient y módulos negocio heredan patrón tenant / org / office sin redefinir tenancy.
4. **Modularidad:** Bounded context separado permite FASE 18 Business Module Framework sin contaminar IAM.
5. **Escalabilidad del modelo:** Cardinalidades 1:N en cada nivel cubren desde consultorio único hasta grupos con decenas de sedes.

---

## Referencias

- [ROADMAP.md](../architecture/ROADMAP.md)
- [ADR-003](../../../codecore-specifications/architecture/adr/ADR-003%20—%20Multi-Tenant%20Isolation%20Strategy.md)
- [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md)
- [ADR-007](../architecture/ADR-007-AUTHORIZATION-MODEL.md)
- [ADR-008](../architecture/ADR-008-IAM-ADMINISTRATION-API.md)
- [CONTEXT-MAP.md](../../../codecore-specifications/architecture/core/CONTEXT-MAP.md) §26
- [PASO-16.0-ORGANIZATIONS-AUDIT.md](PASO-16.0-ORGANIZATIONS-AUDIT.md)
