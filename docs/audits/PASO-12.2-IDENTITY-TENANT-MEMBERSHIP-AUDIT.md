# PASO 12.2 — Identity Tenant Membership (auditoría previa)

**Fecha:** 2026-06-01

---

## 1. Estado actual

| Concepto | Estado |
|----------|--------|
| `Identity` | Aggregate con `tenantId` en fila `iam.iam_user` (modelo **1 registro por tenant+email**) |
| `Tenant` | Aggregate `iam.tenant` (12.0 / 12.1) |
| `TenantId` / `IdentityId` | VOs UUID existentes |
| FK en Flyway | **Ninguna** en migraciones V1–V6 |

Login/registro siguen usando `X-Tenant-Id` / `tenantId` en request **sin** validar membership (fuera de alcance 12.2).

---

## 2. Modelo: N:M vs 1:1

| Enfoque | Pros | Contras |
|---------|------|---------|
| **1:1** (`tenant_id` solo en `iam_user`) | Simple | Misma persona en 2 tenants = 2 filas, 2 emails/credenciales |
| **N:M** (membership + identidad global futura) | Una identity, varios tenants; alineado con producto multi-tenant | Requiere tabla puente; migración gradual desde `iam_user.tenant_id` |

**Decisión 12.2:** tabla puente **`identity_tenant_membership`** como fuente formal de pertenencia N:M. **`iam_user.tenant_id` se mantiene** (compatibilidad con registro/login actuales); convergencia en pasos posteriores (12.3+).

---

## 3. Ubicación propuesta

| Artefacto | Paquete / ruta |
|-----------|----------------|
| `IdentityTenantMembership` | `domain.model.membership` |
| `MembershipId` | `domain.valueobject` |
| `MembershipStatus` | `domain.valueobject` |
| `MembershipRepository` | `application.port.out` |
| `IamIdentityTenantMembershipEntity` | `infrastructure.persistence.entity` |
| `IamIdentityTenantMembershipMapper` | `infrastructure.persistence.mapper` |
| `SpringDataIamIdentityTenantMembershipRepository` | `infrastructure.persistence.repository` |
| `R2dbcMembershipRepository` | `infrastructure.persistence.repository` |
| Flyway | `V7__create_identity_tenant_membership_table.sql` |
| Tests | `IdentityTenantMembershipTest`, `R2dbcMembershipRepositoryIT` |

---

## 4. Persistencia

Tabla `iam.identity_tenant_membership`:

- PK `membership_id`
- `UNIQUE (identity_id, tenant_id)`
- Sin FK (estrategia actual del proyecto)
- CHECK `status IN ('ACTIVE','INACTIVE')`

---

## 5. Fuera de alcance (12.2)

Validación en login, JWT `tenantId`, Tenant Context, roles, invitations, use cases HTTP.

---

## 6. Riesgos

1. Membership no validada en login.
2. JWT sin `tenantId`.
3. Tenant Context pendiente.
4. Doble modelo (`iam_user.tenant_id` + membership) hasta refactor.
