# PASO 13.6 — Deprecate `iam_user.tenant_id`

**Fecha:** 2026-06-15  
**Alcance:** Análisis de viabilidad para eliminar `iam.iam_user.tenant_id` y deuda asociada.  
**Restricción:** Sin implementación en este paso.

**Contexto FASE 13:** La consolidación de datos (13.3/13.4) queda **diferida** — BD solo dev/test, sin usuarios productivos. Este análisis evalúa eliminación **estructural** de la columna.

---

## 1. Decisión obligatoria

## ¿Puede eliminarse `iam_user.tenant_id` hoy?

# **NO**

La columna sigue siendo **load-bearing** en persistencia, dominio y schema. El camino de autenticación ya no la usa para lookup (13.1), pero **no puede dropearse** sin migración Flyway, refactor de dominio y actualización de repositorios.

**No bloquea FASE 14.** La eliminación se programa como deuda técnica **post-Authorization Foundation** (FASE 15.x o hito dedicado).

---

## 2. Inventario de usos restantes

### 2.1 Schema y migraciones

| Artefacto | Dependencia | Riesgo |
|-----------|-------------|--------|
| `V2__create_iam_user_table.sql` | `tenant_id NOT NULL` | **Alto** |
| `V2` | `UNIQUE (tenant_id, normalized_email)` | **Alto** |
| `V2` | `idx_iam_user_tenant_id` | Medio |
| `V8__backfill_*.sql` | Lee `u.tenant_id` para membership | Histórico — no re-ejecutable |

**Migraciones necesarias (futuro):**

```sql
-- Diseño futuro (NO ejecutar en FASE 13)
ALTER TABLE iam.iam_user DROP CONSTRAINT uq_iam_user_tenant_normalized_email;
ALTER TABLE iam.iam_user ADD CONSTRAINT uq_iam_user_normalized_email UNIQUE (normalized_email);
ALTER TABLE iam.iam_user DROP COLUMN tenant_id;
DROP INDEX IF EXISTS iam.idx_iam_user_tenant_id;
```

---

### 2.2 Persistencia

| Archivo | Uso de `tenant_id` |
|---------|-------------------|
| `IamUserEntity.java` | Campo `@Column("tenant_id")` |
| `IamUserMapper.java` | `toEntity` / `toDomain` mapeo bidireccional |
| `SpringDataIamUserRepository.java` | `findByTenantIdAnd*`, `existsByTenantIdAnd*` |
| `R2dbcIdentityRepository.save` | Pre-read `findByTenantIdAndId(identity.tenantId(), ...)` |
| `R2dbcIdentityRepository.findById` | Parámetro tenant obligatorio |
| `R2dbcIdentityRepository.delete` | Parámetro tenant obligatorio |
| `R2dbcIdentityRepository` | Métodos legacy `findByTenantAndEmail` (compatibilidad) |

---

### 2.3 Dominio

| Archivo | Uso |
|---------|-----|
| `Identity.java` | Constructor recibe `TenantId`; `extends AggregateRoot` |
| `AggregateRoot.java` | `tenantId()` en todos los aggregates IAM |
| `Session.java`, `LoginAttemptTracker.java`, `PasswordResetRequest.java` | Heredan `TenantId` — diseño futuro tenant-scoped **operacional**, no de pertenencia |
| `IdentityAlreadyExistsException` | Documenta unicidad `tenant_id + email` |

---

### 2.4 Application layer

| Archivo | Uso |
|---------|-----|
| `RegisterIdentityUseCaseImpl` | Crea `Identity(..., tenantId, ...)`; membership usa `saved.tenantId()` |
| `RegisterIdentityCommand` | `tenantId` obligatorio (membership target, no identity scope) |
| `RegisterIdentityRequest` | Body `tenantId` |
| `IdentityRepository` port | `findById(tenant, id)`, `delete(tenant, id)`, métodos legacy |

**Nota:** `AuthenticateIdentityUseCaseImpl` **no** usa columna para lookup.

---

### 2.5 HTTP

| Archivo | Uso |
|---------|-----|
| `AuthenticationController` | `X-Tenant-Id` → comando (membership gate, no columna) |
| `RegisterIdentityController` | `tenantId` en body |

---

### 2.6 Tests

| Grupo | Archivos afectados |
|-------|-------------------|
| Repository IT | `R2dbcIdentityRepositoryIT` — tenant-scoped CRUD |
| Use case IT | Helpers `persistIdentityOnly(tenantId, ...)` |
| Register / Auth IT | Assertions SQL `tenant_id` |

Estimado: **~15+ clases** de test requieren actualización con el drop.

---

## 3. Qué impide eliminarlo hoy (lista exacta)

| # | Bloqueador | Tipo |
|---|------------|------|
| 1 | Columna `NOT NULL` sin sustituto en `iam_user` | Schema |
| 2 | Constraint `uq_iam_user_tenant_normalized_email` — no garantiza unicidad global | Schema |
| 3 | `Identity` aggregate requiere `TenantId` en constructor | Dominio |
| 4 | `IamUserMapper` / `IamUserEntity` mapean columna en cada save | Persistencia |
| 5 | `R2dbcIdentityRepository.save` usa `findByTenantIdAndId` | Persistencia |
| 6 | `IdentityRepository.findById(tenant, id)` — API port tenant-scoped | Application |
| 7 | Registro persiste `tenant_id` en INSERT | Use case |
| 8 | Flyway V2 — migración drop no preparada | Migración |
| 9 | Specs `repositories.md` — documentan API tenant-scoped | Documentación |
| 10 | Suite tests IAM acoplada a `tenant_id` | Tests |

**No bloqueador para FASE 14:** Los ítems 1–10 no impiden implementar roles/permissions sobre membership.

---

## 4. Prerrequisitos para eliminación futura

| Orden | Prerrequisito | Fase sugerida |
|-------|---------------|---------------|
| 1 | Consolidación datos si `emails_duplicados > 0` en prod | Pre-drop (13.3 ejecutable) |
| 2 | `UNIQUE (normalized_email)` global | Flyway |
| 3 | Refactor `Identity` — sin `TenantId` en aggregate | Dominio |
| 4 | `IdentityRepository` — `findById(id)` sin tenant | Port |
| 5 | Registro — `tenantId` solo en membership, no en identity row | Use case |
| 6 | Eliminar métodos legacy `findByTenantAndEmail` | Port + adapter |
| 7 | Actualizar tests + specs | QA |
| 8 | Drop columna + índice | Flyway |
| 9 | Re-audit queries PASO 13.5 §7 = 0 drift | Verificación |

---

## 5. Relación con consolidación (13.3 / 13.4)

| Escenario | ¿Requiere consolidación antes de drop? |
|-----------|--------------------------------------|
| Dev/test sin duplicados | **No** — drop estructural posible tras refactor |
| Prod con duplicados legacy | **Sí** — ejecutar 13.3/13.4 antes de `UNIQUE (normalized_email)` |
| Entorno actual CodeCore | Consolidación **diferida** — sin usuarios productivos |

---

## 6. Recomendación de scheduling

```text
FASE 14 — Authorization Foundation     ← AHORA (no requiere drop tenant_id)
FASE 15.x — Schema cleanup IAM         ← Drop tenant_id + domain refactor
FUTURE-PROD — Data consolidation       ← Solo si inventario prod > 0 duplicados
```

---

## 7. Criterios de aceptación

| Criterio | Estado |
|----------|--------|
| Usos restantes inventariados | ✅ |
| Decisión SI/NO explícita | ✅ **NO** — no eliminable hoy |
| Bloqueadores documentados | ✅ |
| No bloquea FASE 14 | ✅ |
| Sin cambios código/datos | ✅ |

---

## 8. Referencias

| Documento | Relación |
|-----------|----------|
| [PASO-13.0-TENANT-AWARE-OPERATIONS-AUDIT.md](PASO-13.0-TENANT-AWARE-OPERATIONS-AUDIT.md) | Inventario original ~120 refs |
| [PASO-13.5-SOURCE-OF-TRUTH-VERIFICATION.md](PASO-13.5-SOURCE-OF-TRUTH-VERIFICATION.md) | Auth path ya desacoplado |
| [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md) | Objetivo final |
