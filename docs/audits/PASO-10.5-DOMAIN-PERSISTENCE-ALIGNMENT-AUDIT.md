# Auditoría de alineamiento Dominio ↔ Persistencia (IAM)

**Fecha:** 2026-06-01  
**Alcance:** `Identity`, VOs, `IdentityRepository`, `IamUserEntity`, `IamUserMapper`, `V2__create_iam_user_table.sql`  
**Restricción:** Solo análisis — sin cambios de código.

---

## Resumen ejecutivo

El aggregate `Identity` y la tabla `iam.iam_user` **no representan hoy el mismo concepto de forma bidireccional**. La persistencia es un subconjunto embebido (credencial + estado) con columnas anticipadas sin dominio, mientras el dominio incluye conceptos no persistidos (`Username`, `locked`, `enabled`, metadatos de `Credential`) o persistidos de forma derivada/inconsistente.

**Veredicto:** Apto para demostrar hexagonal + R2DBC (PASO 10.5), **no apto** para casos de uso productivos sin correcciones mínimas planificadas.

**Principio rector recomendado:** Una sola fuente de verdad por dimensión (identidad, credencial, ciclo de vida, verificación de email).

---

## 1. Diagnóstico completo

### 1.1 Modelo conceptual esperado (IAM en SaaS multi-tenant)

| Concepto | Responsabilidad |
|----------|-----------------|
| **Identity** | Sujeto de autenticación dentro de un tenant (quién puede autenticarse y con qué estado). |
| **Credential** | Secreto verificable (hash), rotación, expiración. |
| **Email** | Identificador de login principal (normalizado por tenant). |
| **Lifecycle** | Si puede autenticarse, está bloqueado, pendiente de verificación, etc. |

La tabla `iam_user` materializa un **row-oriented** de ese concepto. El dominio lo modela como aggregate + entidad `Credential` interna.

### 1.2 Desalineamientos estructurales

1. **Triple estado de ciclo de vida** en dominio (`status` + `locked` + `enabled`) vs **una columna** `status` en BD.
2. **Username** en dominio y puerto, **ausente** en BD (derivación + SQL artificioso).
3. **email_verified** en BD, **ausente** en dominio (inferencia unidireccional al guardar).
4. **first_name / last_name** en BD, **ausentes** en dominio (siempre `null` al guardar).
5. **Credential** rico en dominio, **solo** `password_hash` en BD.
6. **email** vs **normalized_email**: dos columnas en BD; dominio solo tiene `EmailAddress` (siempre normalizado) — no hay “email de visualización” real.
7. **Config global** `spring.r2dbc.properties.schema: iam` — acopla toda la app al schema IAM (riesgo multi-módulo).

---

## 2. Matriz Dominio ↔ Persistencia

| Concepto / Campo | Dominio (`Identity` / VOs) | `iam.iam_user` | Mapper | Estado |
|------------------|----------------------------|----------------|--------|--------|
| ID | `IdentityId` | `id` | ↔ | **Alineado** |
| Tenant | `TenantId` (AggregateRoot) | `tenant_id` | ↔ | **Alineado** |
| Email canónico | `EmailAddress` (normalizado) | `normalized_email` | `toEntity`: ambos email y normalized = mismo valor | **Parcial** — `email` columna redundante con dominio actual |
| Email display | — | `email` | `toDomain` lee `email`; dominio no distingue | **Faltante en dominio** / **Redundante en BD** |
| Password hash | `Credential.passwordHash` | `password_hash` | ↔ embebido | **Parcial** — solo hash |
| Credential ID | `CredentialId` | — | Forzado = `IdentityId` | **Derivado** (aceptable si 1:1 explícito) |
| passwordChangedAt | `Credential` | — | Siempre `null` al leer | **Faltante** |
| passwordExpiresAt | `Credential` | — | Siempre `null` | **Faltante** |
| mustChangePassword | `Credential` | — | Inferido solo de `PASSWORD_RESET_REQUIRED` | **Parcial** |
| Credential version | `Credential.version` | — | Siempre `0` al leer | **Faltante** |
| Username | `Username` | — | Derivado de email al leer; no se persiste | **Faltante** / **Riesgo alto** |
| Status | `IdentityStatus` | `status` | ↔ | **Alineado** (enum = CHECK) |
| enabled | `boolean enabled` | — | Derivado: `status != DISABLED` | **Redundante en dominio** |
| locked | `boolean locked` | — | Derivado: `status == LOCKED` | **Redundante en dominio** |
| email_verified | — | `email_verified` | Escritura: `status != PENDING_VERIFICATION`; lectura: **ignorada** | **Desalineado** |
| last_login_at | `lastLoginAt` | `last_login_at` | ↔ | **Alineado** |
| created_at | `createdAt` | `created_at` | ↔ | **Alineado** |
| updated_at | `updatedAt` | `updated_at` | ↔ | **Alineado** |
| version (aggregate) | `AggregateRoot.version` | `version` | ↔ optimistic lock | **Alineado** |
| first_name | — | `first_name` | Siempre `null` al guardar | **Huérfano en BD** |
| last_name | — | `last_name` | Siempre `null` al guardar | **Huérfano en BD** |

### Puertos / consultas

| Método `IdentityRepository` | Soporte real en BD | Estado |
|-----------------------------|-------------------|--------|
| `findByTenantAndEmail` | `UNIQUE (tenant_id, normalized_email)` + índice | **Alineado** |
| `findByTenantAndUsername` | SQL derivado sobre email (no username real) | **Frágil** |
| `existsByTenantAndUsername` | Idem | **Frágil** |
| `AuthenticateCommand` (futuro) | Usa **email**, no username | Coherente con email-first |

---

## 3. Análisis por tema

### 3.1 Username

**Situación actual**

- Dominio: `Username` obligatorio en `Identity`; puerto expone `findByTenantAndUsername` / `existsByTenantAndUsername`.
- BD: sin columna; unicidad solo por `(tenant_id, normalized_email)`.
- Mapper: `deriveUsername(normalizedEmail, id)` — regla no reversible 100%.
- SQL: `regexp_replace(split_part(normalized_email,'@',1), ...)` — debe coincidir con derivación Java.
- Autenticación planeada (`AuthenticateCommand`): **email + password**, no username.

**Opciones**

| Opción | Descripción | Pros | Contras |
|--------|-------------|------|---------|
| A. Eliminar del dominio (por ahora) | Quitar `Username` y métodos del puerto | Mínimo concepto; coherente con login por email | Refactor dominio + puerto; rompe API interna si algo ya usa username |
| B. Persistir explícitamente (V3) | `username VARCHAR` + `UNIQUE(tenant_id, username)` | Login alternativo; identidad humana; multi-tenant correcto | Migración; reglas de normalización; posible colisión con email |
| C. Mantener derivado | Estado actual | Sin migración | **No enterprise**; round-trip frágil; imposible username ≠ local-part |

**Recomendación:** **Opción B en V3** si el producto admitirá login por username o alias distinto del email. **Opción A** si el producto es **email-only** hasta fase 2 (menor cambio ahora).

Dado `AuthenticateCommand` ya es email-based: **corto plazo → A (eliminar username del aggregate y puerto)**; **medio plazo → B** si negocio lo exige.

**Riesgo si no se corrige:** Alto — búsquedas por username incorrectas, identidades “fantasma”, soporte imposible de explicar.

**Multi-tenant:** Username debe ser único **por tenant**, nunca global (igual que email).

---

### 3.2 first_name / last_name

**Situación:** Columnas en V2; mapper siempre `null`; no existen en `Identity`.

**Análisis DDD**

- **IAM** = autenticación/autorización identidad técnica.
- **Perfil humano** (nombre, avatar, preferencias) → bounded context **user-management** (ya previsto en `settings.gradle.kts`).

**Opciones**

| Opción | Recomendación |
|--------|----------------|
| En `Identity` | No — infla aggregate con concerns de perfil |
| En `iam_user` | No ideal — mezcla IAM con perfil |
| En user-management | **Sí** — `user_profile` o similar referenciando `identity_id` |
| Eliminar columnas de `iam_user` | Limpio si aún sin datos productivos |

**Recomendación:** **No agregar a `Identity`**. Tratar columnas como **deuda de diseño V2**. Plan: **V3 — drop `first_name`, `last_name`** (o dejar NULL hasta user-management con link explícito si se necesita denormalización leyendo).

**Riesgo:** Bajo hoy (siempre null); Medio si otro equipo escribe SQL directo esperando nombres en IAM.

---

### 3.3 email_verified

**Situación**

- BD: `email_verified BOOLEAN NOT NULL DEFAULT FALSE`
- Dominio: gate vía `IdentityStatus.PENDING_VERIFICATION` en `validateAuthenticationEligibility()`
- Mapper **escribe** `emailVerified = (status != PENDING_VERIFICATION)` pero **no lee** al reconstruir

**Duplicidad con `status`**

| status | email_verified (mapper write) | ¿Coherente? |
|--------|------------------------------|-------------|
| PENDING_VERIFICATION | false | Sí |
| ACTIVE | true | Sí |
| LOCKED | true | **Discutible** — bloqueado pero “verificado” |
| DISABLED | true | Discutible |

**Opciones**

| Opción | Evaluación |
|--------|------------|
| Inferir solo desde `status` | Eliminar columna; un solo campo | **Recomendado mínimo** |
| `boolean emailVerified` en dominio | Mapper bidireccional; `verifyEmail()` actualiza ambos | Enterprise claro |
| VO `EmailVerification(verified, verifiedAt)` | Más expresivo para auditoría | Mejor largo plazo, más cambios |

**Recomendación:** Corto plazo — **fuente única = `IdentityStatus`** para gates de auth; **eliminar `email_verified` en V3** O mapearlo bidireccional si negocio necesita “verificado pero deshabilitado”. Si se mantiene columna: **agregar `emailVerified` al dominio** (no VO obligatorio) y reglas en `verifyEmail()` / `markEmailVerified()`.

**Riesgo actual:** **Alto** — lectura ignora columna; datos en BD pueden contradecir comportamiento en memoria.

---

### 3.4 enabled / locked / status

**Dominio actual**

```text
validateAuthenticationEligibility():
  - !enabled
  - locked || status.isLocked()
  - status == PENDING_VERIFICATION
  - status == PASSWORD_RESET_REQUIRED
  - !status.mayAuthenticate()
```

**Persistencia:** solo `status` (CHECK alineado con enum).

**Mapper lectura**

- `enabled = (status != DISABLED)`
- `locked = (status == LOCKED)`

**Problemas**

1. **Tres fuentes** en memoria; **una** en BD → estados imposibles en dominio (constructor) no sobreviven round-trip.
2. `lockAccount()` pone `locked=true` y `status=LOCKED` — OK si siempre vía métodos.
3. `PASSWORD_RESET_REQUIRED` + `mustChangePassword` en credential — parcialmente alineado.
4. Tras `unlockAccount()`: `locked=false`, `status=ACTIVE` — pierde historial de “era locked” (aceptable).

**Recomendación (fuente única):** **`IdentityStatus` como única verdad de ciclo de vida**. Eliminar `enabled` y `locked` del aggregate; expresar todo vía enum y métodos de dominio. Persistir solo `status`.

**Riesgo:** Medio — bugs sutiles si código construye `Identity` con flags inconsistentes.

---

### 3.5 Mapper — pérdida de información

| Dirección | Pérdida / transformación |
|-----------|-------------------------|
| Entity → Domain | `email_verified`, `first_name`, `last_name` ignorados; username inventado; credential metadata perdida |
| Domain → Entity | `username` no guardado; email display no diferenciado; flags enabled/locked no guardados |

**Conclusión:** El mapper **no es isomórfico**; es un **proyector parcial** con reglas implícitas (deuda técnica documentada).

---

### 3.6 Repositorio (`R2dbcIdentityRepository`)

| Aspecto | Evaluación |
|---------|------------|
| Implementa puerto `IdentityRepository` | **Correcto** (hexagonal) |
| `@Repository` en adapter | **Aceptable** en Spring; alternativa: `@Bean` en `IamModuleConfiguration` sin estereotipo |
| `save` con `find` previo para insert/update | **Correcto** con `Persistable` |
| Tenant en todas las queries | **Correcto** |
| `SpringDataIamUserRepository` en mismo package + `@EnableR2dbcRepositories` | **Correcto** |

**Cambios necesarios:** **Ninguno obligatorio** para hexagonal. Opcional (bajo): registrar adapter explícitamente en configuración y quitar `@Repository` para dejar infra “puramente cableada” — estética, no funcional.

**Riesgo:** Bajo.

---

### 3.7 Configuración R2DBC (`IamModuleConfiguration`)

**Actual**

- `@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")`
- `spring.r2dbc.properties.schema: iam` en `application.yml` (global)
- `@Query` en Spring Data usa `iam.iam_user` calificado; entidad usa `@Table("iam_user")` + schema por propiedad

**Cuando existan otros módulos**

| Módulo | Schema probable |
|--------|-----------------|
| tenant-management | `tenant` |
| authorization-management | `authz` |
| audit | `audit` |
| billing | `billing` |

**Problema:** `schema: iam` global hace que **todas** las entidades R2DBC por defecto busquen tablas en `iam` → **fallo transversal**.

**Recomendación**

| Horizonte | Acción |
|-----------|--------|
| Ahora (solo IAM) | Mantener en `IamModuleConfiguration` — **válido temporalmente** |
| Primer segundo módulo persistente | Quitar `schema` global; usar `@Table(schema = "…", name = "…")` por entidad **o** multi-`ConnectionFactory` (overkill) |
| platform-r2dbc | Solo utilidades compartidas (converters, listeners, base config), **no** `@EnableR2dbcRepositories` centralizado que mezcle módulos |

**Riesgo:** **Alto** a medio plazo si no se corrige antes del segundo bounded context.

---

## 4. Problemas encontrados (consolidado)

| # | Problema | Riesgo |
|---|----------|--------|
| P1 | Username no persistido; derivación + SQL frágil | **Alto** |
| P2 | `email_verified` no leído en mapper; duplicidad con `status` | **Alto** |
| P3 | `enabled` / `locked` redundantes vs `status` | **Medio** |
| P4 | `first_name` / `last_name` huérfanos en BD | **Bajo** (hoy) |
| P5 | `Credential` parcialmente persistido | **Medio** (políticas password) |
| P6 | `email` vs `normalized_email` sin semántica en dominio | **Medio** |
| P7 | `schema: iam` global en `application.yml` | **Alto** (multi-módulo) |
| P8 | `@Query` con `iam.iam_user` vs entidad con schema por property | **Bajo** |
| P9 | Puertos `findByTenantAndUsername` sin caso de uso email-first | **Medio** |

---

## 5. Recomendación global

**Objetivo:** Mismo concepto en dominio y fila `iam_user` = **Identity técnica autenticable por tenant**, sin perfil ni datos derivados frágiles.

**Decisiones mínimas enterprise**

1. **Auth identifier principal:** email normalizado (`EmailAddress` ↔ `normalized_email`; decidir si `email` columna aporta o se unifica).
2. **Ciclo de vida:** solo `IdentityStatus` en dominio y BD.
3. **Verificación email:** o solo `PENDING_VERIFICATION` en status, o `emailVerified` explícito en dominio **con mapper bidireccional** — no ambos sin reglas.
4. **Username:** eliminar del dominio **o** persistir en V3 — no mantener derivado.
5. **Perfil:** fuera de IAM (user-management).
6. **Credential:** aceptar embebido en `iam_user` hasta V4 **o** documentar campos diferidos (`password_changed_at` en V3).

**No hacer:** microservicios, nuevos módulos, split de aggregate masivo, Event Sourcing.

---

## 6. Cambios mínimos sugeridos (sin sobreingeniería)

| Cambio | Tipo | Esfuerzo |
|--------|------|----------|
| Eliminar `locked` y `enabled` de `Identity`; usar solo `status` | Dominio | S |
| Alinear `email_verified` ↔ dominio **o** drop columna V3 | BD + dominio/mapper | S/M |
| Decidir username: quitar de dominio **o** V3 `username` + UNIQUE tenant | BD + dominio + mapper | M |
| Mapper bidireccional estricto para campos que permanezcan | Infra | S |
| V3 drop `first_name`, `last_name` (si sin datos) | Flyway | S |
| Quitar `schema: iam` global antes de 2º módulo; schema por `@Table` | Config | S |
| V3 opcional: `password_changed_at`, `must_change_password` | Flyway | S |
| Deprecar `findByTenantAndUsername` si email-only | Puerto | S |

---

## 7. Plan de corrección (orden recomendado)

### Fase 0 — Decisiones de producto (bloqueante)

1. ¿Login solo email o también username?
2. ¿Perfil en user-management separado?
3. ¿`email_verified` como columna independiente o solo status?

### Fase 1 — Dominio (fuente de verdad)

1. Simplificar ciclo de vida → solo `IdentityStatus`.
2. Resolver username (eliminar **o** preparar campo persistido).
3. Añadir `emailVerified` **o** documentar que `PENDING_VERIFICATION` es la única señal.

### Fase 2 — Migración Flyway V3 (mínima)

1. Si email-only: `DROP COLUMN first_name, last_name`; opcional `DROP email_verified` o mantener con contrato claro.
2. Si username: `ADD username`, `UNIQUE (tenant_id, username)`.
3. Opcional: `password_changed_at TIMESTAMPTZ`.

### Fase 3 — Mapper + repositorio

1. Mapper isomórfico para campos acordados.
2. Eliminar `deriveUsername` y queries `regexp_replace` si username persistido o eliminado.
3. Ajustar `IdentityRepository` a métodos reales.

### Fase 4 — Configuración

1. Antes de tenant-management: quitar `spring.r2dbc.properties.schema: iam` global.
2. `@Table(schema = "iam", name = "iam_user")` en entidad; unificar `@Query`.

### Fase 5 — Validación

1. Extender `R2dbcIdentityRepositoryIT` con round-trip de estados y email_verified.
2. Test de unicidad username (si aplica).

---

## 8. Conclusión

La implementación PASO 10.5 cumple **arquitectura hexagonal y persistencia reactiva**, pero el **modelo de dominio es más rico que el modelo relacional**, y el **mapper introduce reglas silenciosas** que impiden garantizar “el mismo concepto” en ambos lados.

La corrección enterprise con **menor superficie** es: **status único**, **email como identificador**, **eliminar derivaciones (username, enabled, locked)**, **resolver email_verified**, **sacar perfil de IAM**, y **preparar config multi-schema** antes de nuevos módulos.

---

*Documento para auditoría continua (ChatGPT / revisión interna). Sin cambios de código aplicados.*
