# PASO 13.1 — Identity Lookup Migration Audit

**Fecha:** 2026-05-27  
**Alcance:** Auditoría previa obligatoria antes de implementar lookup global por email (ADR-006).  
**Restricción:** Este documento describe el estado **pre-implementación** analizado; los cambios se documentan en [PASO-13.1-IDENTITY-LOOKUP-MIGRATION.md](PASO-13.1-IDENTITY-LOOKUP-MIGRATION.md).

---

## 1. Resumen

La resolución de identidad en login y registro dependía exclusivamente de métodos tenant-scoped en `IdentityRepository`, respaldados por `UNIQUE (tenant_id, normalized_email)` en `iam_user`. ADR-006 exige lookup global por email con membership como gate de pertenencia.

---

## 2. Artefactos analizados

| Artefacto | Rol |
|-----------|-----|
| `IdentityRepository.java` | Puerto outbound — API tenant-scoped |
| `R2dbcIdentityRepository.java` | Adapter hexagonal R2DBC |
| `SpringDataIamUserRepository.java` | Spring Data queries sobre `iam_user` |
| `IamUserEntity.java` | Mapeo columna `tenant_id` |
| `IamUserMapper.java` | `toDomain` / `toEntity` con `tenantId` |
| `AuthenticateIdentityUseCaseImpl.java` | Login — `findByTenantAndEmail` |
| `RegisterIdentityUseCaseImpl.java` | Registro — `existsByTenantAndEmail` |
| Tests IAM | Unit + IT con supuestos tenant-scoped |

---

## 3. Pregunta 1 — ¿Qué métodos dependen de `tenantId` para localizar una Identity?

### Puerto `IdentityRepository`

| Método | Usa `tenantId` para lookup | Consumidor (pre-13.1) |
|--------|---------------------------|------------------------|
| `findByTenantAndEmail(TenantId, EmailAddress)` | **Sí** | `AuthenticateIdentityUseCaseImpl` |
| `existsByTenantAndEmail(TenantId, EmailAddress)` | **Sí** | `RegisterIdentityUseCaseImpl` |
| `findById(TenantId, IdentityId)` | **Sí** | Tests, futuros use cases |
| `save(Identity)` | Indirecto — pre-read `findByTenantIdAndId` | Registro, IT helpers |
| `delete(TenantId, IdentityId)` | **Sí** | Tests |

### `SpringDataIamUserRepository`

| Método | SQL implícito |
|--------|---------------|
| `findByTenantIdAndNormalizedEmail` | `WHERE tenant_id = ? AND normalized_email = ?` |
| `existsByTenantIdAndNormalizedEmail` | `EXISTS ... tenant_id + normalized_email` |
| `findByTenantIdAndId` | `WHERE tenant_id = ? AND id = ?` |

### Use cases (camino crítico)

```
Login:  findByTenantAndEmail(command.tenantId(), email)
Register: existsByTenantAndEmail(command.tenantId(), email)
```

**Conclusión:** Dos métodos del puerto y dos del Spring Data repository eran el cuello de botella. `findById` y `save` también acotaban por tenant pero no participaban en el flujo auth principal.

---

## 4. Pregunta 2 — ¿Qué consultas SQL deberán modificarse?

### Nuevas consultas requeridas (sin Flyway en 13.1)

| Operación | Consulta nueva | Índice existente |
|-----------|----------------|------------------|
| `findByEmail` | `SELECT ... FROM iam.iam_user WHERE normalized_email = ? ORDER BY created_at ASC LIMIT 1` | `idx_iam_user_normalized_email` |
| `existsByEmail` | `SELECT EXISTS(... WHERE normalized_email = ?)` | `idx_iam_user_normalized_email` |

### Consultas legacy (mantenidas temporalmente)

| Operación | Consulta | Estado 13.1 |
|-----------|----------|-------------|
| `findByTenantAndEmail` | `tenant_id + normalized_email` | **Mantenida** — compatibilidad |
| `existsByTenantAndEmail` | `tenant_id + normalized_email` | **Mantenida** — compatibilidad |

### Sin cambios en 13.1

- `iam_user.tenant_id` — columna intacta
- `uq_iam_user_tenant_normalized_email` — constraint intacto
- Flyway V2–V8 — sin migraciones nuevas

**Nota:** La unicidad global efectiva en registro se aplica a nivel **aplicación** (`existsByEmail`) antes del INSERT. La constraint DB sigue permitiendo mismo email en distintos tenants hasta PASO 13.3/13.5.

---

## 5. Pregunta 3 — ¿Qué tests asumen mismo email + distintos tenants = distintas identities?

| Test | Archivo | Comportamiento pre-13.1 | Cambio 13.1 |
|------|---------|-------------------------|-------------|
| `shouldAllowSameEmailInDifferentTenant` | `RegisterIdentityUseCaseTest` | Permitía registro en tenant B | **Reemplazado** por `shouldRejectDuplicateEmailInDifferentTenant` |
| `shouldAllowSameEmailInDifferentTenants` | `RegisterIdentityUseCaseIT` | Dos registros exitosos | **Reemplazado** por `shouldRejectDuplicateEmailInDifferentTenant` |
| `shouldEnforceUniqueNormalizedEmailPerTenant` | `R2dbcIdentityRepositoryIT` | DB constraint por tenant | **Sin cambio** — sigue válido para mismo tenant |
| `shouldPersistFindAndDeleteIdentityByTenantAndEmail` | `R2dbcIdentityRepositoryIT` | Legacy tenant-scoped API | **Sin cambio** — método legacy mantenido |

**Tests que no asumían multi-tenant email:** `AuthenticateIdentityUseCaseTest`, `AuthenticateIdentityUseCaseIT`, `AuthenticationControllerIT` — emails únicos por test; siguen válidos con `findByEmail`.

---

## 6. Pregunta 4 — ¿Qué restricciones de base de datos impiden unicidad global?

| Restricción / índice | Efecto | Impide unicidad global |
|----------------------|--------|------------------------|
| `uq_iam_user_tenant_normalized_email` UNIQUE `(tenant_id, normalized_email)` | Permite mismo `normalized_email` en distintos `tenant_id` | **Sí** — a nivel DB |
| `pk_iam_user` PRIMARY KEY `(id)` | Una fila por `identity_id` | No |
| `idx_iam_user_normalized_email` | Índice no único en email | No impide; no garantiza unicidad |
| `idx_iam_user_tenant_id` | Filtro por tenant | No |

**Conclusión:**

- **DB:** La constraint compuesta **impide** `UNIQUE (normalized_email)` global hasta PASO 13.5 (nueva migración Flyway).
- **Aplicación (13.1):** `existsByEmail` rechaza registro duplicado con **409** antes de INSERT.
- **Datos legacy:** Filas existentes con mismo email y distintos `identity_id` no se consolidan en 13.1; `findByEmail` usa `ORDER BY created_at ASC LIMIT 1` como resolución determinista transitoria hasta PASO 13.3.

---

## 7. Riesgos identificados en auditoría

| Riesgo | Mitigación en 13.1 |
|--------|-------------------|
| Duplicados legacy en `findByEmail` | `findFirstByNormalizedEmailOrderByCreatedAtAsc` |
| Login falla si identity canónica no tiene membership en tenant solicitado | Membership gate sin cambios — 403 |
| Registro pasa app check pero falla en DB si race condition | Constraint `(tenant_id, email)` sigue activa por tenant |
| Drift `iam_user.tenant_id` vs membership | Fuera de alcance — PASO 13.4 |

---

## 8. Criterios de aceptación de auditoría

| Criterio | Estado |
|----------|--------|
| Métodos dependientes de `tenantId` identificados | ✅ |
| Consultas SQL a modificar identificadas | ✅ |
| Tests con supuesto multi-identity por email identificados | ✅ |
| Restricciones DB documentadas | ✅ |
