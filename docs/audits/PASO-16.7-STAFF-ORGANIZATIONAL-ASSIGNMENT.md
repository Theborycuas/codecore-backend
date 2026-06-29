# PASO 16.7 — Staff Organizational Assignment

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** PASO-16.7-STAFF-ASSIGNMENT-AUDIT · PASO-16.4 · PASO-16.5 · PASO-16.6

---

## Entregables

| Área | Detalle |
|------|---------|
| Dominio | `StaffAssignment` aggregate — scope org u office, `changeScope`, sin archive |
| Persistencia | Flyway **V17** — `org.staff_assignment` + índices únicos parciales |
| HTTP | `StaffAssignmentAdminController` — `/api/v1/org/staff-assignments` |
| Use cases | `StaffAssignmentAdministrationUseCaseImpl` — CRUD + delete físico |
| Cross-BC | `R2dbcMembershipReferenceAdapter` — valida membership ACTIVE en tenant |
| RBAC | `@RequiresPermission("staff-assignment:*")` (V15) |

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/staff-assignments` | `staff-assignment:read` |
| GET | `/staff-assignments/{id}` | `staff-assignment:read` |
| POST | `/staff-assignments` | `staff-assignment:create` |
| PUT | `/staff-assignments/{id}` | `staff-assignment:update` |
| DELETE | `/staff-assignments/{id}` | `staff-assignment:delete` |

**Filtros listado:** `membershipId`, `organizationId`, `officeId` (opcionales) + paginación.

## Reglas aplicación

- Create: membership ACTIVE en tenant; org ACTIVE; office ACTIVE si presente y pertenece a org.
- Scope único por `(tenant, membership, org)` u `(tenant, membership, office)`.
- Update: solo `organizationId` / `officeId` — `membershipId` inmutable.
- Delete físico — no archive (vínculo operativo vs entidad estructural).
- Cross-tenant → 404.

## Tests

- `StaffAssignmentTest` — dominio (4)
- `R2dbcStaffAssignmentRepositoryIT` — persistencia + unicidad + delete
- `StaffAssignmentAdminControllerIT` — create/list/delete, duplicate 409

## Notas

- Sin FK física a `iam.identity_tenant_membership` — referencia lógica por ID.
- Archive org/office no muta assignments existentes (histórico preservado).
- **Próximo:** 16.9 — verificación E2E (`OrganizationVerificationIT`).
