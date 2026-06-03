# Auditoría — Configuración por ambientes (pre-cambio)

**Fecha:** 2026-06-02  
**Fuente:** `apps/codecore-api/src/main/resources/application.yml` (único archivo YAML de aplicación)

---

## 1. Propiedades detectadas

| Propiedad | Valor actual | Sensible |
|-----------|--------------|----------|
| `spring.application.name` | `codecore-api` | No |
| `spring.profiles.active` | `local` (perfil inexistente en archivos) | No |
| `spring.r2dbc.url` | `r2dbc:postgresql://localhost:5433/db_codecore` | Parcial |
| `spring.r2dbc.username` | `codecore` | Parcial |
| `spring.r2dbc.password` | hardcoded | **Sí** |
| `spring.flyway.url` | `jdbc:postgresql://localhost:5433/db_codecore` | Parcial |
| `spring.flyway.user` | `codecore` | Parcial |
| `spring.flyway.password` | hardcoded | **Sí** |
| `spring.flyway.enabled` | `true` | No |
| `spring.flyway.baseline-on-migrate` | `true` | No |
| `spring.flyway.locations` | `classpath:db/migration` | No |
| `server.port` | `8080` | No |
| `security.jwt.secret` | `${JWT_SECRET:default...}` | **Sí** |
| `security.jwt.issuer` | `codecore-api` | No |
| `security.jwt.expiration` | `900s` | No |
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | No |
| `management.endpoint.health.show-details` | `always` | No |

**No configurado en API (pero en classpath):** Redis (`platform-redis`), Kafka — sin propiedades en YAML.

**Tests IAM:** `modules/identity-access-management/src/test/resources/application.yml` (solo JWT) + `@TestPropertySource` + `@DynamicPropertySource` (Testcontainers) — **no dependen** del YAML de `codecore-api`.

---

## 2. Mapa de reubicación

| Propiedad | Destino |
|-----------|---------|
| `spring.application.name`, flyway común, management común, JWT issuer/expiration común | `application.yml` |
| R2DBC, Flyway JDBC, JWT secret (con default dev opcional) | `application-dev.yml` |
| R2DBC, Flyway, JWT sin defaults sensibles | `application-prod.yml` |
| JWT fijo de test (sin DB) | `application-test.yml` |
| `spring.profiles.active` | **Eliminar** de YAML → `.env` `SPRING_PROFILES_ACTIVE=dev` |
| Credenciales PostgreSQL / JWT | `.env` + variables de entorno |

---

## 3. Compatibilidad PASO 11.0 (JWT Foundation)

- Prefijo `security.jwt.*` y `JwtProperties` **sin cambios**.
- `application-dev.yml` mantiene `secret: ${JWT_SECRET:...}` (mismo default local que antes).
- `application-prod.yml` exige `JWT_SECRET` sin default.
- Tests IAM no cargan perfiles de `codecore-api` → **sin impacto** en `./gradlew build`.

---

## 4. Docker Compose (`infrastructure/docker/docker-compose.yml`)

| Variable compose | Alineación `.env` |
|------------------|-------------------|
| `POSTGRES_DB` | `${POSTGRES_DB}` |
| `POSTGRES_USER` | `${POSTGRES_USER}` |
| `POSTGRES_PASSWORD` | `${POSTGRES_PASSWORD}` |
| `env_file` | `../../.env` |

---

## 5. `.gitignore`

Ya incluye `.env` (línea 27) — sin cambio requerido.
