# PASO 15.1 — User Administration

**Fecha:** 2026-06-17  
**Alcance:** API HTTP administrativa de usuarios (Identity) bajo `/api/v1/iam/users`.  
**Prerequisitos:** PASO-15.0.1 (Ownership Rules), PASO-15.1-15.3 Design Audit.

---

## 1. Resumen ejecutivo

Primera capa administrativa real de usuarios sobre RBAC FASE 14. Listado membership-first, mutaciones con ownership (15.0.1), registro compartido con bootstrap vía `IdentityRegistrationOrchestrator`.

| Ítem | Estado |
|------|--------|
| Ownership Rules (15.0.1) | ✅ `PASO-15.0.1-OWNERSHIP-RULES.md` |
| Endpoints `/api/v1/iam/users` | ✅ |
| `@RequiresPermission` | ✅ |
| Tests 15.1 | ✅ 24 tests |
| Suite IAM completa | No ejecutada (por diseño) |

---

## 2. Decisiones tomadas

| Decisión | Implementación |
|----------|----------------|
| User API ↔ Identity | `UserResponse` DTO; dominio `Identity` |
| Listado tenant | JOIN `identity_tenant_membership` → `iam_user` (no `tenant_id` legacy) |
| `user:delete` | `Identity.disable()` — no DELETE físico |
| Registro admin | `IdentityRegistrationOrchestrator` compartido con `RegisterIdentityUseCase` |
| Admin create status | Default `ACTIVE`; bootstrap sigue `PENDING_VERIFICATION` |
| Ownership mutaciones | `OwnershipPolicy` en PUT/DELETE (PASO 15.0.1) |
| Paginación | `page`, `size`, `sort` → `PageQuery` / `PagedUserResponse` |
| Optimistic lock save | `R2dbcIdentityRepository.save` sincroniza `@Version` desde DB |

---

## 3. Endpoints y permisos

| Método | Ruta | Permiso |
|--------|------|---------|
| GET | `/api/v1/iam/users` | `user:read` |
| GET | `/api/v1/iam/users/{id}` | `user:read` |
| POST | `/api/v1/iam/users` | `user:create` |
| PUT | `/api/v1/iam/users/{id}` | `user:update` |
| DELETE | `/api/v1/iam/users/{id}` | `user:delete` |

---

## 4. Archivos principales

| Área | Archivos |
|------|----------|
| Ownership | `OwnershipPolicy.java`, `RolePrivilegeLevel.java`, `PASO-15.0.1-OWNERSHIP-RULES.md` |
| Registro compartido | `IdentityRegistrationOrchestrator.java`, `RegisterIdentityUseCaseImpl.java` (refactor) |
| Use cases | `UserAdministrationUseCaseImpl.java`, `port.in.*AdminUser*` |
| Queries | `IdentityAdminQueryRepository`, `R2dbcIdentityAdminQueryRepository`, `PageQueryParser` |
| HTTP | `IamUserAdminController.java`, `dto/UserResponse`, `PagedUserResponse`, … |
| Dominio | `Identity.changeEmail()`, `OwnershipDeniedException` |
| Config | `IamAdministrationConfiguration.java`, `IamModuleConfiguration.java` |
| Errores | `IamHttpExceptionHandler` (+404, ownership 403, IllegalState 400) |
| Tests | `IamUserAdminControllerIT`, `UserAdministrationUseCaseTest`, `OwnershipPolicyTest`, `IdentityChangeEmailTest` |

---

## 5. Tests ejecutados (solo 15.1)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.admin.*" \
  --tests "com.codecore.iam.domain.model.identity.IdentityChangeEmailTest" \
  --tests "com.codecore.iam.application.RegisterIdentityUseCaseTest" \
  --tests "com.codecore.iam.interfaces.http.admin.IamUserAdminControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `OwnershipPolicyTest` | 4 | ✅ |
| `UserAdministrationUseCaseTest` | 3 | ✅ |
| `IdentityChangeEmailTest` | 3 | ✅ |
| `RegisterIdentityUseCaseTest` | 5 | ✅ |
| `IamUserAdminControllerIT` | 9 | ✅ |
| **Total** | **24** | **BUILD SUCCESSFUL** |

### Cobertura HTTP (IT)

- List / Get / Create / Update / Delete con RBAC
- 401 sin JWT
- 403 sin `user:read`
- 403 ownership (ADMIN → OWNER)
- 404 usuario de otro tenant

---

## 6. Próximo paso

**15.2 — Membership Administration** (`/api/v1/iam/memberships`).
