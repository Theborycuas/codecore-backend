# ADR-008 — IAM Administration API

**Status:** Accepted  
**Date:** 2026-06-15  
**Deciders:** CodeCore architecture (FASE 15)  
**Relates to:** ADR-006 (Identity + Membership), ADR-007 (Authorization), PASO-14.9.1

---

## Context

FASE 14 delivered RBAC infrastructure (`AuthorizationService`, seeds, `@RequiresPermission`) but no HTTP surface to administer users, memberships, roles, permissions, or assignments (PASO-14.9.1).

FASE 15 must expose an **administrative HTTP layer** without changing the authorization model.

---

## Decision

### 1. Base path

All IAM administration resources live under:

```text
/api/v1/iam/{resource}
```

Bootstrap endpoints (unchanged until 15.7) remain at legacy paths:

| Path | Purpose | Auth (until 15.7) |
|------|---------|---------------------|
| `POST /api/v1/tenants` | Tenant bootstrap | Public |
| `POST /api/v1/identities` | Self-registration bootstrap | Public |
| `POST /api/v1/auth/login` | Authentication | Public |
| `GET /api/v1/auth/me` | Principal introspection | JWT |

### 2. API term vs domain term

| API (HTTP) | Domain aggregate | Notes |
|------------|------------------|-------|
| **User** | `Identity` | Permissions use `user:*` (V13 catalog) |
| **Membership** | `IdentityTenantMembership` | `membership:*` |
| **Role** | `Role` | Tenant-scoped; `role:*` |
| **Permission** | `Permission` | Global catalog; `permission:*` |
| **Tenant** | `Tenant` | `tenant:*` |

### 3. Authorization on every admin operation

Every controller method under `/api/v1/iam/**` MUST declare `@RequiresPermission("resource:action")` matching `IamPermissionCatalog`.

Runtime evaluation uses FASE 14 chain: JWT → active membership in tenant → roles → permissions.

Deny by default (ADR-007).

### 4. Tenant scope

- **Tenant id** for admin operations comes from JWT `tenantId` claim (and/or `X-Tenant-Id` on login).
- Cross-tenant access is rejected at `AuthorizationContext` resolution.
- Repository queries MUST filter by resolved tenant.

### 5. Package layout (hexagonal)

```text
interfaces.http.admin.*     → controllers + DTOs (FASE 15.x)
application.port.in.*       → use cases per resource
application.*Impl         → use case implementations
domain.*                    → unchanged aggregates (FASE 10–14)
infrastructure.persistence  → existing repositories + extensions
```

No new Gradle module for FASE 15; co-location in `identity-access-management` (same as FASE 14).

### 6. System roles and seeds

- Global permissions: Flyway V13 (`system_permission = true`).
- System roles per tenant: `TenantSystemRolesProvisioner` on `CreateTenantUseCase`.
- System roles/permissions are immutable in domain; admin API must not offer delete/rename on `system_role = true` rows.

### 7. OpenAPI (15.8)

Contract generation deferred to step 15.8; controllers designed with stable DTOs from 15.1 onward.

---

## Resource map (FASE 15 target)

| Resource | Base path | Permissions |
|----------|-----------|-------------|
| Users | `/api/v1/iam/users` | `user:read`, `user:create`, `user:update`, `user:delete` |
| Memberships | `/api/v1/iam/memberships` | `membership:*` |
| Roles | `/api/v1/iam/roles` | `role:*` |
| Permissions | `/api/v1/iam/permissions` | `permission:read` |
| Role permissions | `/api/v1/iam/roles/{id}/permissions` | `permission:assign` |
| Membership roles | `/api/v1/iam/memberships/{id}/roles` | `membership:update` + role assignment rules |
| Tenants | `/api/v1/iam/tenants` | `tenant:read`, `tenant:update` |
| Foundation | `/api/v1/iam/administration` | `role:read` (15.0 probe) |

---

## Consequences

### Positive

- Clear separation bootstrap vs administration.
- Reuses FASE 14 authorization without new frameworks.
- Permissions seeded in V13 map 1:1 to admin endpoints.

### Negative

- Temporary duplication: `/api/v1/identities` (bootstrap) vs `/api/v1/iam/users` (admin) until 15.7 migration.
- All admin list endpoints require new repository methods (Flux by tenant).

### Out of scope (FASE 15)

- CQRS, event sourcing, microservices.
- Business permissions (`patient:*`, etc.).
- Organizations (FASE 16).

---

## Compliance

| Rule | How |
|------|-----|
| ADR-003 tenant isolation | JWT tenant + membership ACTIVE + tenant-scoped queries |
| ADR-006 | Users are identities; authorization via membership |
| ADR-007 | `@RequiresPermission` on all admin endpoints |
