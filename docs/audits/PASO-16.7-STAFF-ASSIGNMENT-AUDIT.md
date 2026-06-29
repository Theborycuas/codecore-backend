# PASO 16.7 — Staff Assignment Audit

**Fecha:** 2026-06-28  
**Estado:** ✅ Auditoría cerrada — listo para implementación  
**Tipo:** Decisión irreversible + cross-BC (Organization Management ↔ IAM)  
**Dependencias:** ADR-010 · PASO-16.3 · PASO-16.3.1 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Checklist previo a implementar

| # | Ítem | ✓ |
|---|------|---|
| 1 | Aggregate Root identificado | ✅ `StaffAssignment` |
| 2 | Ownership definido | ✅ Organization Management BC |
| 3 | Invariantes definidas | ✅ § Invariantes |
| 4 | Lifecycle definido | ✅ § Lifecycle |
| 5 | Estados definidos | ✅ Sin status — existencia = asignación activa |
| 6 | Permisos definidos | ✅ V15 — `staff-assignment:*` (4) |
| 7 | Relaciones solo mediante IDs | ✅ `MembershipId`, `OrganizationId`, `OfficeId` |
| 8 | Bounded Context correcto | ✅ `organization-management` — no tocar IAM aggregates |
| 9 | No rompe ADR vigentes | ✅ ADR-003/006/007/010 |
| 10 | Escalable multi-tenant | ✅ `tenant_id` + JWT filter |
| 11 | Escalable multi-organization | ✅ Scope org u office |
| 12 | Escalable millones de registros | ✅ Índices + paginación SQL |

---

## Objetivo

Modelar el **puente operativo** entre IAM (`Membership`) y la estructura de negocio (`Organization` / `Office`) sin duplicar Identity ni Membership.

```text
Membership (IAM)     → acceso al tenant + RBAC
StaffAssignment (Org) → en qué org/office opera el staff
```

---

## 1 — Aggregate Root

| Pregunta | Respuesta |
|----------|-----------|
| ¿Quién es el root? | **`StaffAssignment`** — aggregate root propio |
| ¿Por qué? | Ciclo de vida y unicidad de scope (`membershipId` + org/office) son reglas de negocio org, no de IAM |
| ¿Por qué no Membership? | Membership pertenece a IAM; modificarlo acoplaría BC y violaría ADR-006 |
| ¿Por qué no Organization? | Organization no debe cargar colecciones de assignments ni garantizar invariantes de membership |

---

## 2 — Ownership

| Entidad | Owner BC | Puede mutar | Solo consulta |
|---------|----------|-------------|---------------|
| `StaffAssignment` | Organization Management | Org module (use cases) | IAM, Clinical (futuro) |
| `Membership` | IAM | IAM | Organization (solo `MembershipId`) |
| `Organization` / `Office` | Organization Management | Org module | StaffAssignment (solo IDs) |

---

## 3 — Modelo de scope

| Nivel | `organizationId` | `officeId` | Significado |
|-------|------------------|------------|-------------|
| **Organización** | required | `null` | Staff opera en toda la org |
| **Office** | required | required | Staff opera en un consultorio concreto |

**Reglas:**

- `organizationId` **siempre obligatorio** — no existe assignment “solo tenant”.
- Si `officeId` presente → office debe pertenecer a `organizationId` (validación application layer).
- `tenantId` denormalizado (ADR-003) — copiado del JWT en create, inmutable.

---

## 4 — Invariantes

| # | Invariante | Capa |
|---|-----------|------|
| I1 | `membershipId` + scope único por tenant | DB partial unique indexes + use case |
| I2 | Membership debe existir y estar **ACTIVE** en el tenant | Application (`MembershipReferencePort`) |
| I3 | Organization debe existir, pertenecer al tenant, estar **ACTIVE** | Application |
| I4 | Office (si presente) debe existir, pertenecer a org+tenant, estar **ACTIVE** | Application |
| I5 | No crear assignment sobre org/office **ARCHIVED** | Application (409) |
| I6 | `membershipId` **inmutable** tras create | Domain + API |
| I7 | Archive org/office **no muta** assignments existentes | PASO-16.3.1 — histórico preservado |
| I8 | Cross-tenant → 404 | Repository filter |

**Consistencia DDD (§5 política):** StaffAssignment no garantiza consistencia transaccional sobre Membership ni Organization — valida en use case y persiste solo su aggregate.

---

## 5 — Referencias por ID

| Campo | Tipo dominio | FK física |
|-------|--------------|-----------|
| `tenantId` | `TenantId` | No |
| `membershipId` | `MembershipId` | No — referencia lógica IAM |
| `organizationId` | `OrganizationId` | No |
| `officeId` | `OfficeId` (optional) | No |

Navegación a email/identity → **Query Port** o IAM API — nunca cargar aggregate IAM en dominio org.

---

## 6 — Lifecycle

| Operación | HTTP | Permiso | Dominio |
|-----------|------|---------|---------|
| Crear | `POST /staff-assignments` | `staff-assignment:create` | `StaffAssignment.create` |
| Leer | `GET` list / by id | `staff-assignment:read` | Query |
| Cambiar scope | `PUT /staff-assignments/{id}` | `staff-assignment:update` | `changeScope(org, office)` |
| Eliminar | `DELETE /staff-assignments/{id}` | `staff-assignment:delete` | Delete físico |

**Sin archive/activate** — diferente de Organization/Office. Permiso sembrado es `delete`, no `archive`. La asignación es un vínculo operativo; eliminar = revocar scope.

**No** reasignar `membershipId` en update — crear nueva asignación o delete + create.

---

## 7 — Persistencia (V17)

```text
org.staff_assignment (
  assignment_id   UUID PK,
  tenant_id       UUID NOT NULL,
  membership_id   UUID NOT NULL,
  organization_id UUID NOT NULL,
  office_id       UUID NULL,
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL
)
```

Índices únicos parciales:

- `(tenant_id, membership_id, organization_id) WHERE office_id IS NULL`
- `(tenant_id, membership_id, office_id) WHERE office_id IS NOT NULL`

---

## 8 — API (patrón Organization/Office)

Base: `/api/v1/org/staff-assignments`

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/staff-assignments` | `staff-assignment:read` |
| GET | `/staff-assignments/{id}` | `staff-assignment:read` |
| POST | `/staff-assignments` | `staff-assignment:create` |
| PUT | `/staff-assignments/{id}` | `staff-assignment:update` |
| DELETE | `/staff-assignments/{id}` | `staff-assignment:delete` |

**List filters:** `membershipId`, `organizationId`, `officeId` (opcionales, combinables) + paginación.

---

## 9 — Escalabilidad

| Dimensión | Soporte |
|-----------|---------|
| Multi-tenant | `tenant_id` en todas las queries |
| Multi-org / multi-office | Scope explícito + índices |
| Miles de assignments/tenant | Paginación SQL, índices en FK lógicas |
| Evolución | Lectura operativa vs histórica — ver ADR-011 §5 (16.8) |

---

## 10 — Contradicciones / alineación

| Ítem | Resolución |
|------|------------|
| ADR-010 lista `staff-assignment:update` only | **Supersedido** por V15 — también create/read/delete |
| Org/Office usan archive | StaffAssignment usa **delete** — justificado (vínculo vs entidad estructural) |
| PASO-16.3.1 bloqueo nuevas assignments en org archived | ✅ Implementar en create/update |

---

## Veredicto

**Sin bloqueantes.** Implementar dominio → use cases → V17 → HTTP siguiendo patrón 16.4–16.6.

**Próximo paso:** código 16.7 + `PASO-16.7-STAFF-ORGANIZATIONAL-ASSIGNMENT.md` al cerrar.

---

## Referencias

- [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)
- [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md](PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md)
- [PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md](PASO-16.3.1-ORGANIZATION-ADMINISTRATION-AUDIT.md)
