# PASO 14.4 — Membership Roles

**Fecha:** 2026-05-27  
**Alcance:** Relación N:M Membership ↔ Role anclada al aggregate `IdentityTenantMembership`.

---

## 1. Resumen ejecutivo

### Objetivo

Asignar roles tenant-scoped a memberships (ADR-006: autorización sobre membership, no identity).

### Decisiones

| Decisión | Implementación |
|----------|----------------|
| Tabla `membership_role` | `membership_id`, `role_id`, `assigned_at` |
| Unicidad | `PRIMARY KEY` + `UNIQUE(membership_id, role_id)` |
| Sin `tenant_id` | Scope vía `identity_tenant_membership.tenant_id` + `iam.role.tenant_id` |
| Sin aggregate separado | `MembershipRoleAssignment` interno a `IdentityTenantMembership` |
| Aislamiento tenant | `assignRole(roleId, roleTenantId, now)` valida igualdad de tenant |

### Archivos principales

| Área | Archivos |
|------|----------|
| Dominio | `MembershipRoleAssignment.java`, `IdentityTenantMembership.java` |
| Puerto | `MembershipRoleRepository.java` |
| Infra | `R2dbcMembershipRoleRepository.java` |
| Flyway | `V12__create_membership_role_table.sql` |

### Tests (solo 14.4)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.domain.model.membership.IdentityTenantMembershipTest" \
  --tests "com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleRepositoryIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `IdentityTenantMembershipTest` | 5 | ✅ |
| `R2dbcMembershipRoleRepositoryIT` | 3 | ✅ |

### Resultado

**14.4 completado.**

---

## 2. Cadena de autorización (estado actual)

```text
IdentityTenantMembership
  └── MembershipRoleAssignment → Role
        └── RolePermissionAssignment → Permission
```

Listo para **14.5 Authorization Service**.

---

## 3. Próximo paso

**14.5 — Authorization Service:** evaluación `hasPermission` / `hasRole` en runtime.
