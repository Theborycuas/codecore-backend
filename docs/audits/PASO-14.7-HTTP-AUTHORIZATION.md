# PASO 14.7 — HTTP Authorization

**Fecha:** 2026-05-27  
**Alcance:** Proteger endpoints con `@RequiresPermission("resource:action")` en WebFlux.

---

## 1. Resumen ejecutivo

### Objetivo

Equivalente WebFlux a method-level permission checks; denegación → HTTP 403.

### Decisiones

| Decisión | Implementación |
|----------|----------------|
| Anotación | `@RequiresPermission` en métodos (o clase) controller |
| Enforcement | `RequiresPermissionAspect` (AOP reactivo sobre `Mono` return) |
| WebFilter descartado | `handlerMapping.getHandler` + `chain.filter` causaba doble commit de respuesta |
| Excepción dominio | `AuthorizationDeniedException` → 403 en `IamHttpExceptionHandler` |
| AOP | `spring-boot-starter-aop` + `@EnableAspectJAutoProxy` |

### Archivos principales

| Área | Archivos |
|------|----------|
| HTTP | `RequiresPermission.java`, `RequiresPermissionAspect.java` |
| Excepción | `AuthorizationDeniedException.java` |
| Handler | `IamHttpExceptionHandler.java` (+ handler 403) |
| Test probe | `AuthorizationProbeController` (test sources) |

### Tests (solo 14.7)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.interfaces.http.AuthorizationHttpIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `AuthorizationHttpIT` | 2 | ✅ |

### Resultado

**14.7 completado.**

---

## 2. Uso

```java
@GetMapping("/patients")
@RequiresPermission("patient:read")
public Mono<PatientListResponse> list() { ... }
```

Listo para **14.8 Seeds** y **14.9 Verification**.
