# PASO 11.1 — Login HTTP (auditoría previa)

**Fecha:** 2026-06-02

---

## 1. Adapters HTTP existentes

| Elemento | Ubicación | Patrón |
|----------|-----------|--------|
| `RegisterIdentityController` | `interfaces.http` | `@RestController`, puerto `RegisterIdentityUseCase`, DTO request/response HTTP |
| `IamHttpExceptionHandler` | `interfaces.http` | `@RestControllerAdvice`, respuestas vacías (sin mensaje ni clase Java) |
| IT HTTP | `RegisterIdentityControllerIT` | `WebTestClient` + `IamHttpIntegrationTestConfiguration` + Testcontainers |

**No existe** controller de autenticación ni ruta `/api/v1/auth/login`.

---

## 2. Excepciones IAM relevantes

| Excepción | Origen login | HTTP propuesto |
|-----------|--------------|----------------|
| `InvalidCredentialsException` | Usuario inexistente / password incorrecta | **401** |
| `IdentityNotAllowedToAuthenticateException` | `status != ACTIVE` | **423** si `LOCKED`, **403** si `DISABLED`, **403** otros (PENDING, PASSWORD_RESET) |
| `InvalidDomainValueException` | Validación dominio / blank | **400** (ya mapeada) |

**Cambio mínimo:** añadir `IdentityStatus status` a `IdentityNotAllowedToAuthenticateException` para mapeo HTTP sin jerarquía de excepciones.

---

## 3. Manejo de errores actual

- Solo `IdentityAlreadyExistsException` → 409 y `InvalidDomainValueException` → 400.
- Cuerpo vacío; no expone stack ni nombres de clase.
- Validación `@Valid` → 400 vía framework (verificado en `RegisterIdentityControllerIT`).

**Extensión:** handlers para `InvalidCredentialsException`, `IdentityNotAllowedToAuthenticateException` (mismo estilo).

---

## 4. Multi-tenant y `LoginRequest`

`AuthenticateIdentityUseCase` exige `tenantId`. El requisito del paso limita el body a **email + password**.

**Decisión:** tenant vía header obligatorio `X-Tenant-Id` (UUID). Alineado con aislamiento tenant sin ampliar el DTO.

---

## 5. Seguridad (`platform-security`)

`POST /api/v1/identities` y `GET /actuator/health` son `permitAll`.  
**Añadir:** `POST /api/v1/auth/login` → `permitAll` (sin JWT previo para login).

---

## 6. Archivos a crear / modificar

| Acción | Archivo |
|--------|---------|
| Crear | `LoginRequest.java` |
| Crear | `AuthenticationController.java` |
| Crear | `AuthenticationControllerTest.java` |
| Crear | `AuthenticationControllerIT.java` |
| Modificar | `IdentityNotAllowedToAuthenticateException.java` |
| Modificar | `AuthenticateIdentityUseCaseImpl.java` |
| Modificar | `IamHttpExceptionHandler.java` |
| Modificar | `IamHttpIntegrationTestConfiguration.java` |
| Modificar | `PlatformSecurityAutoConfiguration.java` |

**Sin tocar:** `JwtTokenProvider`, `TokenProvider`, lógica JWT, dominio.

---

## 7. Riesgos

| Riesgo | Mitigación |
|--------|------------|
| Olvidar `permitAll` en login | IT + cambio en `platform-security` |
| Enumeración por distintos 403 | Mensajes vacíos; mismos cuerpos |
| Header tenant omitido | `required = true` → 400 Spring |
