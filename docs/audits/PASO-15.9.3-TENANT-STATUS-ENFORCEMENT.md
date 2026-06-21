# PASO 15.9.3 — Tenant Status Enforcement

**Fecha:** 2026-06-17  
**Estado:** ✅ Cerrado  
**ADR:** ADR-009 (P1)

---

## Problema

`TenantStatus` (ACTIVE / SUSPENDED / DISABLED) era persistido vía admin API pero **no enforced** en login ni runtime (PASO-15.9.1).

---

## Semántica definida

| Estado | Login | API autenticada | Reactivación vía API |
|--------|-------|-----------------|----------------------|
| **ACTIVE** | ✅ | ✅ | N/A |
| **SUSPENDED** | ❌ 403 | ❌ 403 | ✅ `PUT /iam/tenants/current` → ACTIVE (requiere `tenant:update`, típicamente OWNER) |
| **DISABLED** | ❌ 403 | ❌ 403 | ❌ — solo intervención platform/ops |

### Intención operativa

- **SUSPENDED:** suspensión temporal (billing, compliance). OWNER puede reactivar.
- **DISABLED:** offboarding permanente del tenant. No reactivación self-service.

---

## Punto único de enforcement

`TenantOperationalGuard.assertOperational(tenantId)` — verifica `tenant.status() == ACTIVE`.

### Dónde se aplica

| Capa | Enforcement |
|------|-------------|
| **Login** | `AuthenticateIdentityUseCaseImpl` — antes de emitir JWT |
| **API autenticada** | `AuthorizationContextWebFilter` — antes de resolver `AuthorizationContext` |
| **Excepción** | `PUT /api/v1/iam/tenants/current` — bypass para permitir reactivación SUSPENDED |

No se duplica en `RequiresPermissionAspect` ni en cada use case admin.

---

## Reactivación SUSPENDED

1. OWNER obtiene JWT **antes** de suspender (o conserva token válido).
2. Tras suspender: API bloqueada excepto `PUT /tenants/current` con `status: ACTIVE`.
3. Tras reactivar: login y API normales.

**Lockout:** si OWNER pierde acceso y tenant está SUSPENDED/DISABLED → intervención platform (bootstrap ops / BD).

---

## DISABLED — regla adicional

`TenantAdministrationUseCaseImpl` rechaza `status: ACTIVE` cuando tenant actual es DISABLED.

---

## Archivos clave

| Archivo | Rol |
|---------|-----|
| `TenantOperationalGuard.java` | Guard central |
| `TenantNotOperationalException.java` | Dominio |
| `AuthenticateIdentityUseCaseImpl.java` | Login |
| `AuthorizationContextWebFilter.java` | Runtime HTTP |
| `TenantAdministrationUseCaseImpl.java` | Bloqueo reactivación DISABLED |
| `IamHttpExceptionHandler.java` | 403 mapping |

---

## Tests

- `TenantOperationalGuardTest` — unit
- `TenantStatusEnforcementIT` — login bloqueado, API bloqueada, reactivación SUSPENDED
