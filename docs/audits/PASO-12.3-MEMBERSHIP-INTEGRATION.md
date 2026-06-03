# PASO 12.3 — Membership Integration

**Fecha:** 2026-06-01

---

## 1. Objetivo

Integrar `IdentityTenantMembership` en **registro** y **login**, manteniendo `iam_user.tenant_id` como modelo legado.

---

## 2. Flujos actualizados

### Registro (`RegisterIdentityUseCase`)

1. Validar email/password y unicidad tenant+email.
2. Crear y persistir `Identity`.
3. Crear `IdentityTenantMembership.create(identityId, tenantId, now)` → **ACTIVE**.
4. Persistir membership.
5. Devolver `RegisterIdentityResult`.

### Login (`AuthenticateIdentityUseCase`)

1. Buscar identity por `tenantId` + email (legado).
2. Validar status ACTIVE y password.
3. **Nuevo:** `findByIdentityId` → filtrar `tenantId` → exigir `MembershipStatus.ACTIVE`.
4. Emitir JWT.

Si falla membership → `IdentityNotMemberOfTenantException` → HTTP **403** (sin body).

---

## 3. Estrategia de transición

| Fuente | Rol |
|--------|-----|
| `iam_user.tenant_id` | Lookup registro/login (sin cambios en 12.3) |
| `identity_tenant_membership` | Vínculo formal; creado en registro; validado en login |

Datos antiguos sin fila membership: login falla con 403 aunque credenciales sean correctas → migración/backfill futuro.

Persistencia identity + membership: encadenamiento reactivo (`flatMap`), sin transacción global (deuda si falla el segundo paso).

---

## 4. Artefactos

| Artefacto | Cambio |
|-----------|--------|
| `IdentityNotMemberOfTenantException` | Dominio |
| `RegisterIdentityUseCaseImpl` | + `MembershipRepository` |
| `AuthenticateIdentityUseCaseImpl` | Validación post-password |
| `IamHttpExceptionHandler` | 403 |
| Tests | Unit + IT + HTTP 403 sin membership |

---

## 5. Riesgos y deuda

1. **Doble fuente de verdad** (`tenant_id` vs membership).
2. Membership **no** es aún la única fuente de tenancy.
3. JWT **sin** `tenantId`.
4. **Tenant Context** pendiente.
5. Sin transacción atómica identity+membership.

---

## 6. Pasos futuros habilitados

- Backfill membership para identities existentes.
- Validar tenant existe (`TenantRepository`) en registro.
- Claim `tenantId` en JWT + Tenant Context.
- Membership API / invitations / roles.

---

## 7. Verificación

`./gradlew build` → **BUILD SUCCESSFUL**
