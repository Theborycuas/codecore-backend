# PASO 15.9.2 — Bootstrap Strategy

**Fecha:** 2026-06-17  
**Estado:** ✅ Cerrado  
**ADR:** ADR-009 (P0)

---

## Problema

Tras PASO 15.7, `POST /api/v1/tenants` y `POST /api/v1/identities` requieren JWT. Una instancia vacía no podía operarse sin intervención manual en BD (PASO-15.9.1).

---

## Alternativas evaluadas

| Opción | Pros | Contras | Decisión |
|--------|------|---------|----------|
| **A. ApplicationRunner** | DDD/hexagonal; reutiliza use cases; sin HTTP expuesto | Requiere env en despliegue | **Elegida** |
| B. CLI separado | Explícito para ops | Nuevo artefacto Gradle; duplicación wiring | Descartada |
| C. Endpoint + env secret | HTTP simple | Superficie de ataque; rompe modelo admin | Descartada |
| D. Flyway seed SQL | Sin código Java | Contraseña en migración; no asigna OWNER role fácil | Descartada |

---

## Solución implementada

`ApplicationRunner` (`PlatformBootstrapRunner`) ejecuta `BootstrapPlatformUseCase` al arranque cuando:

1. `codecore.platform.bootstrap.enabled=true`
2. `tenantRepository.count() == 0`

### Flujo

```text
Application startup
  → BootstrapPlatformUseCase.executeIfNeeded()
    → CreateTenantUseCase (tenant ACTIVE + system roles)
    → IdentityRegistrationOrchestrator (identity ACTIVE + membership ACTIVE)
    → MembershipRoleRepository.assign(OWNER)
  → Log tenantId + owner email
[HTTP público]
  POST /api/v1/auth/login + X-Tenant-Id
    → JWT OWNER
```

### Configuración (`application-dev.yml`)

```yaml
codecore:
  platform:
    bootstrap:
      enabled: ${CODECORE_BOOTSTRAP_ENABLED:false}
      tenant-name: ${CODECORE_BOOTSTRAP_TENANT_NAME:CodeCore}
      owner-email: ${CODECORE_BOOTSTRAP_OWNER_EMAIL:}
      owner-password: ${CODECORE_BOOTSTRAP_OWNER_PASSWORD:}
```

### Instalación greenfield

```bash
export CODECORE_BOOTSTRAP_ENABLED=true
export CODECORE_BOOTSTRAP_OWNER_EMAIL=owner@example.com
export CODECORE_BOOTSTRAP_OWNER_PASSWORD='ValidPass1!'
./gradlew :apps:codecore-api:bootRun
```

Tras arranque: login con email/password y `X-Tenant-Id` del tenant creado.

---

## Archivos clave

| Archivo | Rol |
|---------|-----|
| `BootstrapPlatformUseCaseImpl.java` | Orquestación |
| `PlatformBootstrapRunner.java` | ApplicationRunner |
| `PlatformBootstrapProperties.java` | Config |
| `IamBootstrapConfiguration.java` | Beans |
| `BootstrapPlatformUseCaseTest.java` | Unit tests |

---

## Seguridad

- Bootstrap **deshabilitado por defecto** (`enabled=false`).
- Solo corre con BD vacía (idempotente en re-arranques).
- No expone endpoint HTTP público adicional.
- Endpoints legacy `POST /tenants` y `POST /identities` siguen requiriendo JWT.

---

## Tests

- `BootstrapPlatformUseCaseTest` — skip cuando disabled / tenants exist; flujo completo mockeado.
