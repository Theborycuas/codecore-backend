# PASO 10.4 — IAM Persistence Model (auditoría)

**Fecha:** 2026-06-01  
**Alcance:** Solo modelo persistente + diseño de dominio (sin Java, sin endpoints).  
**App:** `codecore-backend/apps/codecore-api`  
**Migraciones:** `src/main/resources/db/migration`

---

## Entregables

| Artefacto | Ubicación | Estado |
|-----------|-----------|--------|
| Schema IAM | `V1__create_iam_schema.sql` | Ya aplicado |
| Tabla `iam.iam_user` | `V2__create_iam_user_table.sql` | **Creado** (pendiente ejecutar Flyway en entorno) |
| Propuesta DDD/hexagonal | Este documento §4–§5 | Diseño only |

---

## 1. SQL de migración V2

Archivo: `apps/codecore-api/src/main/resources/db/migration/V2__create_iam_user_table.sql`

Incluye: tabla, PK, `UNIQUE (tenant_id, normalized_email)`, CHECK de `status` alineado con `IdentityStatus`, CHECK de emails no vacíos, CHECK `version >= 0`, comentarios de columna, 4 índices btree.

---

## 2. Columnas — propósito

| Columna | Tipo | Obligatorio | Propósito |
|---------|------|-------------|-----------|
| `id` | UUID | Sí | Identificador global del registro; mapea a `IdentityId`. |
| `tenant_id` | UUID | Sí | Aislamiento multi-tenant; clave de partición lógica en consultas. |
| `email` | VARCHAR(320) | Sí | Email “humano” (puede conservar mayúsculas del registro original). |
| `normalized_email` | VARCHAR(320) | Sí | Forma canónica para unicidad y login (`EmailAddress` en dominio ya normaliza). |
| `password_hash` | VARCHAR(500) | Sí | Hash de contraseña (Argon2id/bcrypt/SCrypt); preparación auth local. |
| `first_name` | VARCHAR(150) | No | Perfil básico; no requerido para autenticar. |
| `last_name` | VARCHAR(150) | No | Perfil básico. |
| `status` | VARCHAR(50) | Sí | Ciclo de vida (`ACTIVE`, `LOCKED`, etc.); alineado con `IdentityStatus`. |
| `email_verified` | BOOLEAN | Sí (default false) | Gate para verificación antes de auth plena. |
| `last_login_at` | TIMESTAMPTZ | No | Auditoría y UX; actualizado tras login exitoso. |
| `created_at` | TIMESTAMPTZ | Sí | Auditoría de alta. |
| `updated_at` | TIMESTAMPTZ | Sí | Auditoría de mutación; coherente con `Identity.updatedAt`. |
| `version` | BIGINT | Sí (default 0) | Optimistic locking; coherente con `AggregateRoot.version`. |

**Nota de diseño:** `email` + `normalized_email` permiten mostrar el email original y buscar/unicidad de forma estable (RFC 5321 permite 320 chars en path local+domain).

---

## 3. Índices y restricciones

| Objeto | Tipo | Rol |
|--------|------|-----|
| `pk_iam_user` | PRIMARY KEY (`id`) | Acceso por ID global (sesiones, FK futuras). |
| `uq_iam_user_tenant_normalized_email` | UNIQUE (`tenant_id`, `normalized_email`) | **Unicidad por tenant**, no global. Mismo email en otro tenant permitido. Crea índice btree implícito para login por tenant+email. |
| `ck_iam_user_status` | CHECK | Integridad referencial al enum de dominio. |
| `idx_iam_user_tenant_id` | INDEX | Listados y operaciones masivas por tenant. |
| `idx_iam_user_email` | INDEX | Búsquedas por email literal (soporte, búsqueda parcial futura). |
| `idx_iam_user_normalized_email` | INDEX | Soporte a consultas por email canónico (redundante parcialmente con UNIQUE; útil si se consulta sin `tenant_id` en jobs internos — ver §6). |
| `idx_iam_user_status` | INDEX | Filtros por estado (ej. usuarios `PENDING_VERIFICATION`). |

**Recomendación futura (no en V2):** índices compuestos `(tenant_id, status)` y `(tenant_id, email)` optimizan casi todas las queries SaaS; el UNIQUE ya cubre `(tenant_id, normalized_email)`.

---

## 4. Propuesta de estructura (sin implementar)

### 4.1 Mapeo tabla ↔ dominio existente

El módulo `modules/identity-access-management` ya define `Identity` como aggregate root. La tabla `iam.iam_user` es la **proyección persistente** de ese agregado en fase 10.4 (credencial embebida en fila; `Username` y `Credential` como entidad separada quedan para evolución).

| Persistencia (`iam_user`) | Dominio actual |
|---------------------------|----------------|
| `id` | `IdentityId` |
| `tenant_id` | `TenantId` (en `AggregateRoot`) |
| `email` / `normalized_email` | `EmailAddress` (valor normalizado = `normalized_email`) |
| `password_hash` | `Credential.passwordHash` → `PasswordHash` |
| `status` | `IdentityStatus` |
| `email_verified` | Derivable de `status != PENDING_VERIFICATION` o campo explícito futuro |
| `last_login_at` | `Identity.lastLoginAt()` |
| `version` | `AggregateRoot.version()` |

**Gaps detectados vs dominio (complemento al prompt):**

- `Username` existe en `Identity` pero **no** está en `iam_user` → V3 o módulo Profile.
- `Identity.locked` / `Identity.enabled` duplican semántica de `status` → mapper deriva `enabled`/`locked` desde `status` hasta unificar modelo.
- `Credential` tiene `passwordChangedAt`, `passwordExpiresAt`, `mustChangePassword` → candidatos V3 (`iam_credential` o columnas en `iam_user`).

### 4.2 Paquetes propuestos (hexagonal)

```
com.codecore.iam
├── domain
│   ├── model.identity
│   │   └── Identity                    ← Aggregate Root (existente)
│   ├── valueobject
│   │   ├── IdentityId, TenantId, EmailAddress, PasswordHash, IdentityStatus …
│   └── (futuro) IamUserId alias si se renombra concepto — preferible reutilizar IdentityId
├── application
│   └── port.out
│       └── IdentityRepository          ← Puerto de dominio/aplicación (existente)
└── infrastructure
    └── persistence
        ├── entity
        │   └── IamUserEntity           ← Mapeo R2DBC row ↔ tabla iam.iam_user
        ├── repository
        │   └── R2dbcIdentityRepository ← Implementa IdentityRepository
        └── mapper
            └── IamUserMapper           ← Entity ↔ Identity (+ Credential embebido)
```

### 4.3 Artefactos (propuesta, no implementados)

| Artefacto | Responsabilidad |
|-----------|-----------------|
| **Aggregate Root** | `Identity` — reglas de auth, lock, password reset (ya existe). |
| **Value Objects** | `IdentityId`, `TenantId`, `EmailAddress`, `PasswordHash`, `IdentityStatus`, `PersonName` (futuro para first/last). |
| **Domain Repository** | Puerto `IdentityRepository` (existente); queries siempre con `TenantId`. |
| **Persistence Entity** | `IamUserEntity` — anémico, anotaciones/record R2DBC, sin lógica de negocio. |
| **Mapper** | `IamUserMapper` — traduce status enum ↔ VARCHAR, normalización email, embebe `Credential` desde `password_hash`. |
| **Use Cases (futuro, post-persistencia)** | `RegisterIdentityUseCase`, `VerifyEmailUseCase`, `ActivateIdentityUseCase` (complementan auth existente: `AuthenticateUseCase`, `ChangePasswordUseCase`, …). |

---

## 5. Recomendaciones V3 y V4

### V3 — Sesiones y seguridad operativa

- `iam.iam_session` — refresh tokens, expiración, revocación (agregado `Session` ya en dominio).
- `iam.iam_login_attempt` — lockout temporal (`LoginAttemptTracker` / `FailedLoginAttempt`).
- Columnas opcionales en `iam_user`: `username`, `locked_until`, `failed_login_count` si no se modelan solo con `status`.
- Índices compuestos: `(tenant_id, status)`, `(tenant_id, email)`.
- FK: `iam_session.identity_id` → `iam_user(id)` con `ON DELETE RESTRICT`.

### V4 — Password reset y credenciales avanzadas

- `iam.iam_password_reset_request` — token hash, expiración (`PasswordResetRequest`).
- Tabla `iam.iam_credential_history` o rotación en `iam_user` — `password_changed_at`, política de expiración.
- Soft delete: `deleted_at` + partial unique index `WHERE deleted_at IS NULL` para retener historial GDPR.
- Row Level Security (RLS) en PostgreSQL si se expone conexión directa; en monolito R2DBC suele bastar filtro en repositorio.

### Buenas prácticas Flyway

- Una responsabilidad por versión (tabla vs índices vs datos semilla).
- Nunca editar V1/V2 tras aplicar en shared env; solo V3+.
- Scripts idempotentes solo si se usan fuera de Flyway (no necesario aquí).
- Convención de nombres: `V{n}__{verb}_{object}.sql`.

---

## 6. Complementos al prompt original

1. **CHECK `status`** — alineado con `IdentityStatus` Java existente.  
2. **CHECK emails no vacíos** — evita filas inválidas.  
3. **CHECK `version >= 0`** — coherente con optimistic locking.  
4. **COMMENT ON** — documentación en catálogo PostgreSQL.  
5. **Gaps documentados** — `Username`, `Credential` metadata, `locked`/`enabled` vs `status`.  
6. **Sin `DEFAULT` en UUID/timestamps** — IDs y timestamps generados en aplicación (DDD explícito).  
7. **No se crearon** endpoints, servicios, JWT ni clases Java (cumplimiento estricto del paso).

---

## 7. Verificación sugerida

```bash
# Con PostgreSQL y Flyway habilitados (profile local)
./gradlew :apps:codecore-api:bootRun
# o migración explícita si está configurada en build
```

Comprobar:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
\d iam.iam_user
```

---

## 8. Resumen ejecutivo (1 párrafo para auditor)

Se diseñó e implementó la migración **V2** que crea `iam.iam_user`, primera tabla persistente multi-tenant con unicidad `(tenant_id, normalized_email)`, campos mínimos para autenticación futura, optimistic locking y CHECKs alineados al módulo IAM existente. No se escribió código Java; se documentó el mapeo hacia el aggregate `Identity`, paquetes de persistencia hexagonal, gaps (`username`, credencial extendida) y hoja de ruta **V3** (sesiones/login attempts) y **V4** (password reset, soft delete, RLS opcional).
