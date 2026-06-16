# PASO 14.6 — Authorization Context

**Fecha:** 2026-05-27  
**Alcance:** Contexto de autorización request-scoped: JWT → Tenant → Membership ACTIVE.

---

## 1. Resumen ejecutivo

### Objetivo

Exponer `AuthorizationContext` (`identityId`, `tenantId`, `membershipId`) en use cases vía Reactor Context.

### Decisiones

| Decisión | Implementación |
|----------|----------------|
| DTO | `AuthorizationContext` (application/dto) |
| Reactor holder | `AuthorizationReactorContext` (mismo patrón que `AuthenticationContext`) |
| Resolución | `ReactorAuthorizationContextAccessor` + puerto `AuthorizationContextAccessor` |
| Membership ACTIVE | `MembershipRepository.findActiveByIdentityIdAndTenantId` |
| HTTP filter | `AuthorizationContextWebFilter` @Order(-98), post-JWT |
| Sin membership ACTIVE | 403 (deny by default) |

### Archivos principales

| Área | Archivos |
|------|----------|
| DTO | `AuthorizationContext.java` |
| Application | `ReactorAuthorizationContextAccessor.java`, `AuthorizationContextAccessor.java` |
| HTTP | `AuthorizationReactorContext.java`, `AuthorizationContextWebFilter.java` |
| Membership | `MembershipRepository` + Spring Data query por status ACTIVE |

### Tests (solo 14.6)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.ReactorAuthorizationContextAccessorTest"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `ReactorAuthorizationContextAccessorTest` | 5 | ✅ |

### Resultado

**14.6 completado.**

---

## 2. Flujo request

```text
JWT (AuthenticatedPrincipal)
  → TenantId del claim
  → Membership ACTIVE en tenant
  → AuthorizationContext en Reactor Context
```

Listo para **14.7 HTTP Authorization**.
