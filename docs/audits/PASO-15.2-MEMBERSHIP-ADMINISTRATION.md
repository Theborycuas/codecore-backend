# PASO 15.2 — Membership Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP administrativa de memberships bajo `/api/v1/iam/memberships`.  
**Prerequisitos:** PASO-15.0.1 (Ownership Rules), PASO-15.1 (User Administration), PASO-15.1-15.3 Design Audit §3.

---

## 1. Resumen ejecutivo

Segunda capa administrativa IAM: gestión de `IdentityTenantMembership` por tenant. Listado paginado, alta por vinculación o registro nuevo, actualización de estado y baja lógica (`deactivate`).

| Ítem | Estado |
|------|--------|
| Endpoints `/api/v1/iam/memberships` | ✅ |
| `@RequiresPermission` (`membership:*`) | ✅ |
| Vincular identity existente (email / identityId) | ✅ |
| Crear identity + membership (email + password) | ✅ vía `IdentityRegistrationOrchestrator` |
| DELETE → `membership.deactivate()` | ✅ |
| Ownership en DELETE | ✅ `OwnershipPolicy` sobre identity objetivo |
| Tests 15.2 | ✅ 14 tests |
| Suite IAM completa | No ejecutada (por diseño) |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| Listado tenant | `MembershipAdminQueryRepository` paginado por `tenantId` del contexto |
| GET por id | `findByIdAndTenantId` → `404` si no pertenece al tenant |
| Alta caso A — vincular | `identityId` o `email` → `IdentityTenantMembership.create()` |
| Alta caso B — nueva identity | `email` + `password` → `IdentityRegistrationOrchestrator` |
| Duplicado `(identityId, tenantId)` | `MembershipAlreadyExistsException` → `409` |
| `membership:delete` | `membership.deactivate()` — no DELETE físico |
| `membership:update` | `activate()` / `deactivate()` vía `PUT` |
| Ownership en DELETE | `OwnershipPolicy.assertCanModifyUser` sobre identity del membership |
| Asignación de roles | Fuera de alcance (reservado 15.6) |
| Paginación | `page`, `size`, `sort` (`status`, `createdAt`) → `PagedMembershipResponse` |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/memberships` | `membership:read` |
| GET | `/api/v1/iam/memberships/{id}` | `membership:read` |
| POST | `/api/v1/iam/memberships` | `membership:create` |
| PUT | `/api/v1/iam/memberships/{id}` | `membership:update` |
| DELETE | `/api/v1/iam/memberships/{id}` | `membership:delete` |

---

## 4. Archivos principales

| Área | Archivos |
|------|----------|
| Dominio | `MembershipNotFoundException`, `MembershipAlreadyExistsException` |
| Repository | `MembershipRepository.findByIdAndTenantId`, `R2dbcMembershipRepository` |
| Use cases | `MembershipAdministrationUseCaseImpl`, `port.in.*AdminMembership*` |
| Commands | `CreateAdminMembershipCommand`, `UpdateAdminMembershipCommand` |
| Queries | `MembershipAdminQueryRepository`, `R2dbcMembershipAdminQueryRepository`, `AdminMembershipView` |
| HTTP | `IamMembershipAdminController`, DTOs (`MembershipResponse`, `PagedMembershipResponse`, …) |
| Rutas | `IamAdminApiPaths.MEMBERSHIPS` |
| Config | `IamAdministrationConfiguration` (beans membership) |
| Errores | `IamHttpExceptionHandler` (+404 membership, +409 duplicate) |
| Tests | `MembershipAdministrationUseCaseTest`, `IamMembershipAdminControllerIT` |

---

## 5. Tests ejecutados (solo 15.2)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.MembershipAdministrationUseCaseTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamMembershipAdminControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `MembershipAdministrationUseCaseTest` | 4 | ✅ |
| `IamMembershipAdminControllerIT` | 10 | ✅ |
| **Total** | **14** | **BUILD SUCCESSFUL** |

### Cobertura HTTP (IT)

- List / Get / Create (link + new identity) / Update / Delete con RBAC
- 401 sin JWT
- 403 sin `membership:read`
- 404 membership de otro tenant
- 409 membership duplicada

### Cobertura unitaria

- Activar / desactivar membership
- Rechazo duplicado al vincular
- Not found fuera de tenant

---

## 6. Próximo paso

**15.3 — Role Administration** (`/api/v1/iam/roles`).
