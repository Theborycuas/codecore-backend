# PASO 10.6 — RegisterIdentityUseCase

**Fecha auditoría:** 2026-06-02  
**Estado:** Autoauditoría previa documentada + implementación aplicada y validada (`./gradlew build` OK).

---

## Autoauditoría (antes / durante implementación)

### 1. Clases nuevas planeadas (y creadas)

| # | Clase | Paquete |
|---|-------|---------|
| 1 | `RegisterIdentityCommand` | `application.command` |
| 2 | `RegisterIdentityResult` | `application.dto` |
| 3 | `RegisterIdentityUseCase` | `application.port.in` |
| 4 | `RegisterIdentityUseCaseImpl` | `application` |
| 5 | `PasswordHasher` | `application.port.out` |
| 6 | `BCryptPasswordHasher` | `infrastructure.security` |
| 7 | `IdentityAlreadyExistsException` | `domain.exception` |
| 8 | `RegisterIdentityUseCaseTest` | `test` |

**Total producción:** 7 clases (+ 1 test). Sin capa HTTP.

---

### 2. Por qué cada una es necesaria

| Clase | Necesidad |
|-------|-----------|
| **RegisterIdentityCommand** | Contrato de entrada explícito para el puerto inbound; alinea con `AuthenticateCommand` y futuros adapters HTTP sin acoplar strings sueltos al caso de uso. |
| **RegisterIdentityResult** | Contrato de salida mínimo (id, tenant, email, status) sin exponer el aggregate ni detalles de persistencia. |
| **RegisterIdentityUseCase** | Puerto inbound hexagonal; la API o tests dependen de la interfaz, no de la implementación. |
| **RegisterIdentityUseCaseImpl** | Única orquestación del flujo: validar → unicidad → hash → construir `Identity` → `save`. No puede vivir solo como interfaz. |
| **PasswordHasher** | Puerto outbound exigido por el paso; el caso de uso no debe importar BCrypt. |
| **BCryptPasswordHasher** | Adapter mínimo (`spring-security-crypto` solo; sin `SecurityFilterChain`). |
| **IdentityAlreadyExistsException** | No existía excepción reutilizable para duplicado `tenant_id + normalized_email`; `IdentityNotFoundException` no aplica. |
| **RegisterIdentityUseCaseTest** | Requisito del paso; mocks de puertos, sin PostgreSQL. |

**Reutilización consciente (no clases nuevas):**

- `Identity`, `Credential`, `EmailAddress`, `RawPassword`, `PasswordHash`, `IdentityRepository`
- `InvalidDomainValueException` para email/password vacíos o formato inválido

---

### 3. Clases consideradas y descartadas

| Descartada | Motivo |
|------------|--------|
| `IdentityService` / `IdentityManager` / `IdentityFacade` | Capa genérica innecesaria; el caso de uso ya orquesta. |
| `IdentityFactory` | Construcción de 15 líneas en el use case; no hay variaciones de creación. |
| `IdentityDomainService` | Sin reglas de dominio nuevas que no estén en `Identity` + VOs. |
| `RegisterIdentityValidator` | Validación mínima (blank + VOs); clase extra sin valor. |
| `RegisterIdentityMapper` | Command/Result son records planos; no hay mapeo complejo. |
| Reutilizar `CredentialHashingPort` | El paso exige `PasswordHasher` con firma `String`; duplicación temporal aceptada hasta unificar con Authenticate. |
| `PasswordPolicyPort` en registro | Fuera de alcance 10.6; `RawPassword` ya exige ≥8 caracteres. |
| `RegisterIdentityUseCase` como única clase sin interfaz | Descartado: el módulo ya define puertos `port.in` para otros casos de uso. |
| DTO REST (`RegisterIdentityRequest` / `Response`) | Explícitamente fuera de alcance. |
| Eventos (`IdentityRegisteredEvent`) | No Kafka, no eventos en este paso. |
| `NotificationPort` / envío de email | Verificación de email es paso futuro. |

---

### 4. Posibles signos de sobreingeniería detectados

| Señal | Evaluación | Decisión |
|-------|------------|----------|
| Interfaz + `Impl` para un solo caso de uso | Riesgo bajo | **Mantener** — coherente con `AuthenticateUseCase` y hexagonal del módulo. |
| `PasswordHasher` vs `CredentialHashingPort` existente | Duplicación | **Aceptada** — requisito del paso; documentar unificación en Authenticate. |
| `RegisterIdentityResult` en `application.dto` | Podría confundirse con REST DTO | **Aceptada** — es DTO de aplicación (caso de uso), no HTTP; nombre alineado al paso. |
| Bean `registerIdentityUseCase` en `IamModuleConfiguration` | Wiring manual | **Necesario** — `Impl` no lleva `@Component` para mantener application libre de estereotipos opcionales; solo infra tiene `@Component` en hasher. |
| `Mono.defer` en `execute` | ¿Complejidad? | **Justificado** — validaciones síncronas deben propagarse como error reactivo en tests `StepVerifier`. |
| `IdentityAlreadyExistsException` nueva | ¿Demasiadas excepciones? | **Indispensable** — semántica distinta a `InvalidDomainValueException`. |

**Veredicto:** No hay sobreingeniería estructural. El único “deuda consciente” es la coexistencia `PasswordHasher` / `CredentialHashingPort`.

---

### 5. Confirmación explícita — NO agregado

| Elemento | ¿Agregado? |
|----------|------------|
| Controllers | **NO** |
| DTOs REST (Request/Response HTTP) | **NO** |
| JWT | **NO** |
| Eventos de dominio / integración | **NO** |
| CQRS (commands/queries separados, buses) | **NO** |
| Servicios genéricos (`IdentityService`, etc.) | **NO** |
| OAuth2 | **NO** |
| Spring Security filters / `SecurityConfig` | **NO** |
| Kafka | **NO** |
| Login / refresh tokens | **NO** |

**Sí agregado (dentro de alcance):** command/result de **aplicación**, puerto inbound, implementación del caso de uso, puerto + adapter de hashing, una excepción de dominio, tests unitarios con mocks.

---

## Implementación (post-auditoría)

### Flujo

```
RegisterIdentityCommand (tenantId, email, rawPassword)
    │
    ├─► Validar email / password no vacíos
    ├─► EmailAddress.of(email)              // normaliza (email-first)
    ├─► RawPassword.of(rawPassword)       // ≥ 8 caracteres
    ├─► existsByTenantAndEmail
    │       └─ si true → IdentityAlreadyExistsException
    ├─► PasswordHasher.hash
    ├─► Identity + Credential (PENDING_VERIFICATION)
    ├─► IdentityRepository.save
    └─► RegisterIdentityResult
```

### Estado inicial del aggregate

- **Status:** `PENDING_VERIFICATION` (coherente con `email_verified` como proyección de status en BD).
- **Credential:** embebido; `CredentialId` = `IdentityId` (1:1 documentado en 10.5).

### Archivos tocados

| Archivo | Acción |
|---------|--------|
| `RegisterIdentityCommand.java` | Creado |
| `RegisterIdentityResult.java` | Creado |
| `RegisterIdentityUseCase.java` | Creado |
| `RegisterIdentityUseCaseImpl.java` | Creado |
| `PasswordHasher.java` | Creado |
| `BCryptPasswordHasher.java` | Creado |
| `IdentityAlreadyExistsException.java` | Creado |
| `RegisterIdentityUseCaseTest.java` | Creado |
| `IamModuleConfiguration.java` | Bean `registerIdentityUseCase` |
| `build.gradle.kts` | `spring-security-crypto`, `spring-boot-starter-test` |
| `R2dbcIdentityRepositoryIT.java` | `@Import(BCryptPasswordHasher)` para contexto de test |

### Tests (`RegisterIdentityUseCaseTest`)

| Caso | Resultado esperado |
|------|-------------------|
| Registro exitoso | `PENDING_VERIFICATION`, persistencia invocada |
| Email duplicado mismo tenant | `IdentityAlreadyExistsException` |
| Mismo email otro tenant | Permitido |
| Password vacía | `InvalidDomainValueException` |
| Email vacío | `InvalidDomainValueException` |

### Verificación

```bash
./gradlew build   # BUILD SUCCESSFUL
```

---

## Riesgos / deuda documentada (no bloquean 10.6)

1. **Unificar** `PasswordHasher` y `CredentialHashingPort` cuando exista `AuthenticateUseCaseImpl`.
2. **HTTP adapter** (`RegisterIdentityController`) — PASO posterior, no 10.6.
3. **Política de contraseña** vía `PasswordPolicyPort` — opcional en registro.
4. **Envío verificación email** — fuera de IAM persistencia actual.

---

## Cierre del punto 10.6

El primer caso de uso de negocio queda acotado a la capa **application + un adapter de hashing**, reutilizando dominio y persistencia alineados en 10.5. Listo para el siguiente paso: adapter HTTP o integración E2E con R2DBC (sin expandir alcance aquí).
