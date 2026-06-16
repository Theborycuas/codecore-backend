# PASO 14.5 — Authorization Service

**Fecha:** 2026-05-27  
**Alcance:** Motor de evaluación RBAC en runtime (Membership → Role → Permission).

---

## 1. Resumen ejecutivo

### Objetivo

Servicio de aplicación que responde `hasPermission`, `hasAnyPermission` y `hasRole` sobre un `AuthorizationContext` membership-scoped (ADR-007).

### Decisiones

| Decisión | Implementación |
|----------|----------------|
| API reactiva | `AuthorizationService` → `Mono<Boolean>` |
| Consultas SQL | `AuthorizationQueryRepository` + `R2dbcAuthorizationQueryRepository` (EXISTS + joins) |
| Solo roles ACTIVE | Filtro `r.status = 'ACTIVE'` en SQL |
| Deny by default | Sin grant en cadena → `false` |

### Archivos principales

| Área | Archivos |
|------|----------|
| Application | `AuthorizationService.java`, `AuthorizationServiceImpl.java`, `AuthorizationContext.java` |
| Puerto out | `AuthorizationQueryRepository.java` |
| Infra | `R2dbcAuthorizationQueryRepository.java` |
| Config | `IamAuthorizationConfiguration.java` |

### Tests (solo 14.5)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.application.AuthorizationServiceTest" \
  --tests "com.codecore.iam.infrastructure.persistence.repository.R2dbcAuthorizationQueryRepositoryIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `AuthorizationServiceTest` | 4 | ✅ |
| `R2dbcAuthorizationQueryRepositoryIT` | 3 | ✅ |

### Resultado

**14.5 completado.**

---

## 2. Cadena evaluada

```text
membership_id → membership_role → role → role_permission → permission.code
```

Listo para **14.6 Authorization Context**.
