# PASO 10.8 — Diagnóstico Security

## Contexto

En runtime, `apps/codecore-api` depende de `platform-security`, que a su vez trae `spring-boot-starter-security`. El endpoint `POST /api/v1/identities` (adapter HTTP del paso 10.8) respondía **403 Forbidden** con cuerpo **Access Denied**. En logs aparecía **Using generated security password**, señal de que Spring Security estaba activo con la configuración por defecto.

En este paso **no** existen login, JWT, roles ni permisos. Solo se requiere acceso público a registro de identidad y a health de Actuator.

## 1. Por qué la ruta respondía 403

| Factor | Detalle |
|--------|---------|
| Starter en classpath | `codecore-api` → `platform-security` → `spring-boot-starter-security` |
| Sin `SecurityWebFilterChain` propio | El módulo `platform-security` no tenía código Java; solo `build.gradle.kts` |
| Auto-configuración por defecto | `SecurityAutoConfiguration` / `ReactiveSecurityAutoConfiguration` registran una cadena que exige autenticación en **todas** las peticiones |
| Petición sin credenciales | `AuthorizationManager` deniega el acceso → **403 Access Denied** (no hay sesión ni Basic enviado) |
| IT IAM sin 403 | `RegisterIdentityControllerIT` usa `IamHttpIntegrationTestConfiguration` **sin** `platform-security`; los tests no reproducían el runtime de la API |

El mensaje **Using generated security password** proviene de `ReactiveUserDetailsServiceAutoConfiguration` / usuario por defecto en dev cuando no hay `UserDetailsService` personalizado. Confirma que el filtro de seguridad está activo; no implica que ese usuario se use para el POST (la petición sigue sin autenticarse y cae en `deny`/`authenticated`).

## 2. Qué configuración bloqueaba la ruta

No había un bean explícito en el proyecto. El bloqueo lo produce la **cadena por defecto** de Spring Boot 3 + WebFlux:

- `SecurityWebFilterChain` auto-configurada con `anyExchange().authenticated()` (equivalente efectivo).
- Filtros de autorización delante del `DispatcherHandler` / controladores.
- `RegisterIdentityController` en `com.codecore.iam` está correctamente registrado vía `scanBasePackages`; el 403 ocurre **antes** del use case.

`CodeCoreApiApplication` escanea solo `com.codecore.api` y `com.codecore.iam`; la corrección vive en `platform-security` mediante **auto-configuración** (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), no mediante component scan.

## 3. Corrección aplicada (mínima)

**Módulo:** `platform/platform-security`

**Clase:** `com.codecore.platform.security.PlatformSecurityAutoConfiguration`

- `@AutoConfiguration` + `@EnableWebFluxSecurity`
- Bean `SecurityWebFilterChain` que:
  - **permitAll:** `POST /api/v1/identities`
  - **permitAll:** `GET /actuator/health`
  - **authenticated:** resto de exchanges (sin JWT/login aún; otras rutas siguen protegidas)
- CSRF, HTTP Basic y form login deshabilitados para evitar 403 adicionales en POST público sin sesión.

**No implementado (fuera de alcance 10.8):** JWT, login, refresh, roles, permisos, `CredentialHashingPort` unificado.

## 4. Rutas públicas vs protegidas

| Método | Ruta | Acceso PASO 10.8 |
|--------|------|------------------|
| POST | `/api/v1/identities` | Público |
| GET | `/actuator/health` | Público |
| * | Resto | Requiere autenticación (401/403 según filtro) |

## 5. Validación

### Build

```bash
./gradlew build
```

Resultado: **BUILD SUCCESSFUL**.

### Manual (API con PostgreSQL local según `application.yml`)

Tras reiniciar `codecore-api` con el JAR que incluye `PlatformSecurityAutoConfiguration`:

| Prueba | Resultado |
|--------|-----------|
| `GET /actuator/health` | **200** — `{"status":"UP",...}` |
| `POST /api/v1/identities` (JSON válido) | **201 Created** — ya no **403** |
| `POST` con cuerpo mal formado | **400** (validación HTTP), no 403 |

Ejemplo (usar `curl.exe` en Windows o fichero JSON para evitar que PowerShell altere las comillas):

```bash
curl.exe -i -X POST http://localhost:8080/api/v1/identities \
  -H "Content-Type: application/json" \
  --data-binary "@register-body.json"
```

```bash
curl.exe -i http://localhost:8080/actuator/health
```

El log **Using generated security password** puede seguir apareciendo (usuario dev por defecto); no bloquea las rutas `permitAll`.

## 6. Próximos pasos (post 10.8)

- Sustituir `authenticated()` por política explícita cuando existan JWT y rutas protegidas.
- Tests de slice/API que incluyan `platform-security` para alinear IT con runtime.
- Eliminar usuario generado en dev (`spring.security.user`) o documentar solo para pruebas manuales de rutas protegidas.
