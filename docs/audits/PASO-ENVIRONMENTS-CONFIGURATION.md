# PASO — Configuración por ambientes (DEV / PROD / TEST)

**Fecha:** 2026-06-02  
**Estado:** Aplicado — `./gradlew build` OK.

---

## Estructura final

```
apps/codecore-api/src/main/resources/
  application.yml          # común
  application-dev.yml        # desarrollo
  application-prod.yml       # producción
  application-test.yml       # perfil test API
  db/migration/              # sin cambios

codecore-backend/
  .env                       # variables locales (gitignored)
  infrastructure/docker/docker-compose.yml  # alineado con .env
```

---

## Perfiles

| Perfil | Activación | Uso |
|--------|------------|-----|
| `dev` | `SPRING_PROFILES_ACTIVE=dev` en `.env` | Desarrollo local + Docker |
| `prod` | Variable de entorno / K8s | Sin secretos en YAML |
| `test` | CI o explícito | API test profile; IAM IT no lo usa |

**Perfil activo:** `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}` en `application.yml` (fallback `dev` si no hay variable).

**Gradle `bootRun`:** carga `codecore-backend/.env` en el proceso (Spring Boot no lee `.env` solo).

**Eliminado:** perfil `local` (sin `application-local.yml`).

---

## Compatibilidad JWT Foundation (11.0)

- `security.jwt.issuer` y `expiration` en `application.yml`.
- `security.jwt.secret` en `application-dev.yml` / `application-prod.yml` sin cambiar `JwtProperties` ni `JwtTokenProvider`.
- Tests IAM: sin cambios (`@TestPropertySource`, Testcontainers).

---

## Variables `.env`

Ver `codecore-backend/.env` — `JWT_SECRET` y `POSTGRES_PASSWORD` vacíos para carga manual.

---

## Docker Compose

- `env_file: ../../.env`
- `POSTGRES_*` desde sustitución de variables
- Puerto host `${POSTGRES_PORT:-5433}`

---

## `.gitignore`

`.env` ya estaba excluido — sin modificación.
