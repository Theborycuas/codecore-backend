# PASO 15.0 — IAM Administration Foundation

**Fecha:** 2026-06-15  
**Alcance:** Fundación de la capa administrativa HTTP (ADR-008) sobre infraestructura FASE 14.

---

## 1. Resumen ejecutivo

### Objetivo

Definir convenciones API administrativa y demostrar el primer endpoint **productivo** protegido con `@RequiresPermission`.

### Decisiones

| Decisión | Implementación |
|----------|----------------|
| Base path admin | `/api/v1/iam/*` (ADR-008) |
| User API ↔ Identity domain | Permisos `user:*`, aggregate `Identity` |
| Autorización obligatoria | `@RequiresPermission` en todo `/api/v1/iam/**` |
| Bootstrap legacy | `/tenants`, `/identities`, `/auth/*` públicos hasta 15.7 |
| Paquete HTTP admin | `com.codecore.iam.interfaces.http.admin` |

### Archivos principales

| Área | Archivos |
|------|----------|
| ADR | `docs/architecture/ADR-008-IAM-ADMINISTRATION-API.md` |
| Paths | `IamAdminApiPaths.java` |
| Controller | `IamAdministrationController.java` (`GET .../administration/status`) |
| Config | `IamAdministrationConfiguration.java` |
| ROADMAP | `docs/architecture/ROADMAP.md` — FASE 15 |

### Tests (solo 15.0)

```bash
./gradlew :modules:identity-access-management:test \
  --tests "com.codecore.iam.interfaces.http.admin.IamAdministrationControllerIT"
```

| Suite | Tests | Resultado |
|-------|-------|-----------|
| `IamAdministrationControllerIT` | 3 | ✅ |

### Resultado

**15.0 completado.** Primer endpoint admin productivo con RBAC HTTP.

---

## 2. Endpoint fundacional

| Método | Ruta | Permiso | Función |
|--------|------|---------|---------|
| GET | `/api/v1/iam/administration/status` | `role:read` | Probe operativo FASE 15 |

---

## 3. Brecha cerrada (vs 14.9.1)

PASO-14.9.1: ningún controller en `src/main` usaba `@RequiresPermission`.

PASO-15.0: `IamAdministrationController` en `src/main` — consumible desde `codecore-api`.

---

## 4. Próximo paso

**15.1 — User Administration:** CRUD/list bajo `/api/v1/iam/users` con `user:*`.
