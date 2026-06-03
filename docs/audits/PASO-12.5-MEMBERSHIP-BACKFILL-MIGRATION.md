# PASO 12.5 — Membership Backfill Migration

**Fecha:** 2026-06-03

---

## 1. Objetivo

Poblar `iam.identity_tenant_membership` para identidades históricas en `iam.iam_user` que no tienen fila membership, restaurando login post-12.3 sin cambiar registro, login, APIs ni JWT.

---

## 2. Plan de implementación (validado contra el repo)

| Verificación | Resultado |
|--------------|-----------|
| Convención Flyway | `apps/codecore-api/src/main/resources/db/migration/V{n}__*.sql` |
| Esquema `iam_user` (V2 + V3) | Columnas usadas: `id`, `tenant_id`, `created_at`, `updated_at` |
| Esquema membership (V7) | `membership_id`, `identity_id`, `tenant_id`, `status`, timestamps |
| UUID en migraciones previas | **Ninguna** V1–V7 inserta UUID en SQL; la app usa `UUID.randomUUID()` |
| UUID en V8 | `gen_random_uuid()` (PostgreSQL 16, UUID v4 aleatorio para `membership_id`) |
| Idempotencia | `NOT EXISTS` sobre par `(identity_id, tenant_id)` — alineado con `IF NOT EXISTS` en V1 |
| Tests IAM | `processTestResources` copia migraciones; Testcontainers + Flyway JDBC |

---

## 3. SQL implementado

**Archivo:** `apps/codecore-api/src/main/resources/db/migration/V8__backfill_identity_tenant_membership.sql`

```sql
INSERT INTO iam.identity_tenant_membership (...)
SELECT
    gen_random_uuid(),
    u.id,
    u.tenant_id,
    'ACTIVE',
    u.created_at,
    u.updated_at
FROM iam.iam_user u
WHERE NOT EXISTS (
    SELECT 1 FROM iam.identity_tenant_membership m
    WHERE m.identity_id = u.id AND m.tenant_id = u.tenant_id
);
```

---

## 4. Estrategia de idempotencia

| Mecanismo | Efecto |
|-----------|--------|
| `NOT EXISTS (identity_id, tenant_id)` | No inserta si ya hay membership para el par canónico |
| `UNIQUE (identity_id, tenant_id)` (V7) | Red de seguridad ante carrera concurrente |
| Flyway versionado | Segunda ejecución de Flyway no re-aplica V8 |
| Re-ejecución manual del INSERT | Sin duplicados (validado en test) |

**Membership preexistente:** V8 **no modifica** filas existentes (p. ej. `INACTIVE` manual se conserva).

---

## 5. Validaciones (comentarios en V8)

| Query | Propósito |
|-------|-----------|
| **A** | `iam_user` sin membership canónica → esperado **0** filas post-deploy |
| **B** | Memberships huérfanas (sin `iam_user`) → esperado **0** |
| **C** | Conteos `iam_user`, membership, usuarios con membership canónica |

Solo documentadas en SQL; ejecución manual en staging/prod.

---

## 6. Tests

**Clase:** `MembershipBackfillMigrationIT`

| Caso | Escenario | Resultado esperado |
|------|-----------|-------------------|
| 1 | Identity histórica sin membership → Flyway V8 | 1 fila ACTIVE; timestamps = `iam_user` |
| 2 | Membership existente (INACTIVE) → V8 | Sigue 1 fila; status/id sin cambios |
| 3 | Re-ejecutar INSERT + Flyway `migrate()` | Sin duplicados; versión Flyway = 8 |

Infraestructura: contenedor PostgreSQL 16 dedicado; `@BeforeEach`: `clean` + migrar hasta **V7**; datos vía JDBC; luego `migrate()` a latest.

---

## 7. Riesgos

| Riesgo | Mitigación |
|--------|------------|
| Usuarios ACTIVE sin membership bloqueados (403) | V8 en deploy |
| Duplicados | `NOT EXISTS` + UNIQUE |
| Sobrescribir membership INACTIVE | V8 no hace UPDATE |
| `tenant_id` huérfano en `iam_user` | Sin FK (decisión 12.2); query B en auditoría |
| Registro parcial futuro (identity sin membership) | Fuera de alcance 12.5; deuda transaccional |

---

## 8. Cambios funcionales

**Ninguno.** No se modificaron:

- `RegisterIdentityUseCaseImpl`
- `AuthenticateIdentityUseCaseImpl`
- `MembershipRepository`
- Controllers / JWT / Tenant Context

---

## 9. Criterios de aceptación

| Criterio | Estado |
|----------|--------|
| Flyway V8 idempotente | Implementado |
| Sin duplicados | `NOT EXISTS` + test caso 2 |
| Usuarios históricos con membership | Test caso 1 |
| Sin cambios en login/registro | Sin diff en use cases |
| `./gradlew build` | Verificar en CI/local |

**Resultado local (2026-06-03):**

```
:modules:identity-access-management:test — MembershipBackfillMigrationIT
  shouldCreateMembershipForHistoricalIdentityWithoutMembership PASSED
  shouldNotDuplicateWhenMembershipAlreadyExists PASSED
  shouldBeIdempotentOnBackfillSqlReexecution PASSED
BUILD SUCCESSFUL
```

---

## 10. Referencias

- `PASO-12.4-MEMBERSHIP-BACKFILL-AUDIT.md`
- `V7__create_identity_tenant_membership_table.sql`
- `V2__create_iam_user_table.sql`
