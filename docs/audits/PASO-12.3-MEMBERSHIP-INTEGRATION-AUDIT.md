# PASO 12.3 — Membership Integration (auditoría previa)

**Fecha:** 2026-06-01

---

## 1. Flujos actuales

| Use case | Dependencia `iam_user.tenant_id` | Membership |
|----------|----------------------------------|------------|
| `RegisterIdentityUseCaseImpl` | `Identity` con `tenantId`; `existsByTenantAndEmail` | No usada |
| `AuthenticateIdentityUseCaseImpl` | `findByTenantAndEmail(tenantId, email)` | No usada |

---

## 2. Doble fuente de verdad

| Fuente | Rol hoy | Tras 12.3 |
|--------|---------|-----------|
| `iam_user.tenant_id` | Lookup registro/login por tenant+email | **Se mantiene** (legado) |
| `identity_tenant_membership` | Solo persistencia aislada | **Registro:** creación ACTIVE automática; **Login:** validación post-credenciales |

Riesgo: identity sin fila membership (datos pre-12.3 o ITs que solo guardan `iam_user`) → login **403** aunque credenciales sean válidas.

---

## 3. Transacciones reactivas

No hay `@Transactional` ni `TransactionalOperator` en el proyecto. **Estrategia 12.3:** encadenar `identityRepository.save` → `membershipRepository.save` con `flatMap` (mismo patrón secuencial que el resto de IAM). Si falla membership tras guardar identity, queda deuda documentada (compensación futura).

---

## 4. Cambios propuestos

| Artefacto | Cambio |
|-----------|--------|
| `IdentityNotMemberOfTenantException` | `domain.exception` |
| `RegisterIdentityUseCaseImpl` | + `MembershipRepository`; tras `save(identity)` → `save(membership ACTIVE)` |
| `AuthenticateIdentityUseCaseImpl` | Tras password OK → `findByIdentityId` + match tenant + `ACTIVE` |
| `IamHttpExceptionHandler` | 403 para `IdentityNotMemberOfTenantException` |
| `IamModuleConfiguration` / `IamAuthenticationConfiguration` | Inyectar `MembershipRepository` |
| Tests IT | `R2dbcMembershipRepository` en `@Import`; helpers con/sin membership |

Lookup membership: `findByIdentityId` + filtro por `tenantId` (sin nuevo método en puerto).

---

## 5. Orden validación login

1. Credenciales (`findByTenantAndEmail`, status ACTIVE, password)
2. Membership (existe par identity+tenant, `MembershipStatus.ACTIVE`)
3. Emitir JWT

Así no se revela membership si la contraseña es incorrecta.

---

## 6. Fuera de alcance

Tenant Context, JWT `tenantId`, eliminar `tenant_id` de `iam_user`, API membership.
