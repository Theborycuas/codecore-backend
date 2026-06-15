# PASO 13.5 — Source Of Truth Verification

**Fecha:** 2026-06-15  
**Alcance:** Verificación formal de fuentes de verdad IAM tras FASE 13 (Identity Global Migration).  
**Restricción:** Sin cambios de código ni datos.

**Referencias:** [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md) · PASO 13.0–13.4 · PASO 12.3–12.9

---

## 1. Resumen ejecutivo

| Verificación | Resultado | Notas |
|--------------|-----------|-------|
| Membership = fuente oficial de pertenencia (runtime auth) | ✅ **SÍ** | Gate obligatorio en login y registro |
| TenantContext desde JWT | ✅ **SÍ** | `ReactorTenantContext` → claim `tenantId` |
| Login independiente de `iam_user.tenant_id` | ✅ **SÍ** (funcional) | `findByEmail` + membership; columna legacy persiste en persistencia |
| Registro con email global | ✅ **SÍ** | `existsByEmail` |
| ADR-006 reflejada en implementación | ⚠️ **PARCIAL** | Flujo operativo alineado; schema/dominio legacy pendiente (13.6) |

### Decisión FASE 13

La verificación confirma que **el camino crítico de autenticación** cumple el espíritu de ADR-006. La deuda **estructural** (`iam_user.tenant_id`, aggregate `Identity` tenant-scoped) queda documentada en [PASO-13.6](PASO-13.6-DEPRECATE-IAM-USER-TENANT-ID.md) y **no bloquea** FASE 14.

### Consolidación de datos (13.3 / 13.4)

**Diferida.** La BD contiene solo datos de desarrollo/pruebas; no hay usuarios productivos. Las estrategias de [PASO-13.3](PASO-13.3-IDENTITY-CONSOLIDATION-STRATEGY.md) y el inventario de [PASO-13.4](PASO-13.4-IDENTITY-CONSOLIDATION-MIGRATION-AUDIT.md) quedan archivadas para ejecución futura ante datos productivos reales.

---

## 2. Membership — fuente oficial de pertenencia

### 2.1 Evidencia en login

`AuthenticateIdentityUseCaseImpl`:

```text
findByEmail(email)
  → validate password
  → membershipRepository.findByIdentityId(identity.id())
      .filter(tenantId == command.tenantId())
      .filter(status == ACTIVE)
  → 403 si ausente/inactiva
```

| Pregunta | Respuesta |
|----------|-----------|
| ¿Login permite acceso sin membership ACTIVE? | **No** |
| ¿`iam_user.tenant_id` participa en el gate? | **No** |
| ¿Fuente de pertenencia en login? | **`identity_tenant_membership`** |

### 2.2 Evidencia en registro

`RegisterIdentityUseCaseImpl` (transaccional desde 12.7):

```text
existsByEmail(email) → 409 si existe
  → save(identity)
  → save(membership ACTIVE para command.tenantId)
```

| Pregunta | Respuesta |
|----------|-----------|
| ¿Registro crea membership? | **Sí**, atomically |
| ¿Pertenencia formal post-registro? | **Membership** |

### 2.3 Matriz de fuentes de verdad

| Concern | Fuente operativa (FASE 13) | Fuente legacy (pendiente 13.6) |
|---------|---------------------------|--------------------------------|
| ¿Puede autenticarse en tenant T? | Membership ACTIVE `(identityId, T)` | — |
| ¿Existe identidad con email E? | `existsByEmail` / `findByEmail` | — |
| ¿A qué tenant pertenece la fila `iam_user`? | **No usado en auth** | `iam_user.tenant_id` (escritura en registro) |
| Tenant en request autenticada | JWT `tenantId` → TenantContext | — |

**Veredicto:** Membership es la **fuente oficial de pertenencia** para autenticación y autorización de acceso por tenant.

---

## 3. TenantContext — tenant desde JWT

### 3.1 Cadena de propagación

```text
Login → JwtTokenProvider (claim tenantId)
     → JwtAuthenticationWebFilter
     → AuthenticatedPrincipal.tenantId
     → ReactorTenantContext.currentTenant()
```

### 3.2 Evidencia

| Componente | Comportamiento |
|------------|----------------|
| `JwtTokenProvider` | Emite `claim("tenantId", ...)` obligatorio |
| `JwtTokenValidator` | Parsea `tenantId` → `Optional<TenantId>` |
| `ReactorTenantContext` | Falla cerrado si claim ausente (`TENANT_CLAIM_ABSENT`) |
| Consulta BD en TenantContext | **No** |

**Veredicto:** ✅ TenantContext obtiene tenant **exclusivamente desde JWT** en requests autenticadas.

---

## 4. Login — independencia funcional de `iam_user.tenant_id`

### 4.1 Camino crítico actual

| Paso | Dependencia `tenant_id` columna |
|------|--------------------------------|
| Resolución identity | **No** — `findByEmail` |
| Validación password | **No** |
| Gate pertenencia | **No** — membership |
| Emisión JWT | **No** — `command.tenantId()` (header `X-Tenant-Id`) |

### 4.2 Métodos legacy

| Método | Uso en producción (use cases) |
|--------|-------------------------------|
| `findByTenantAndEmail` | **Ninguno** (solo adapter + tests IT) |
| `existsByTenantAndEmail` | **Ninguno** |

**Veredicto:** ✅ Login **no depende funcionalmente** de `iam_user.tenant_id` para lookup ni autorización de pertenencia.

**Matiz:** La columna sigue **poblada** en registro (`Identity` aggregate + `IamUserMapper`). Es deuda de persistencia, no de flujo auth.

---

## 5. Registro — email global

| Aspecto | Estado |
|---------|--------|
| Unicidad pre-insert | `existsByEmail` — global |
| Mismo email otro tenant | **409** `IdentityAlreadyExistsException` |
| Constraint DB | `UNIQUE (tenant_id, normalized_email)` — permite duplicado cross-tenant si bypass app (solo relevante con datos legacy) |

**Veredicto:** ✅ Registro utiliza **email global** a nivel aplicación (ADR-006).

---

## 6. ADR-006 — reflexión en implementación

### 6.1 Cumplimiento

| Principio ADR-006 | Implementación FASE 13 |
|-------------------|------------------------|
| Identity global por email (lookup) | ✅ `findByEmail` / `existsByEmail` |
| Membership N:M | ✅ Tabla + gate login/registro |
| JWT identity + tenant activo | ✅ `sub` + `tenantId` claim |
| TenantContext | ✅ |
| Una fila `iam_user` por email (schema) | ❌ Pendiente 13.6 |
| `Identity` sin `TenantId` (dominio) | ❌ Pendiente 13.6 |
| JWT `tenantId` desde membership validada | ⚠️ Parcial — aún desde comando HTTP |
| Consolidación identities duplicadas | ⏸ Diferida (sin prod) |

### 6.2 Modelo operativo vs. modelo estructural

```text
OPERATIVO (auth path):     ADR-006 compliant ✅
ESTRUCTURAL (schema/domain): Híbrido legacy ⚠️ → FASE futura post-14 o 15.x
DATOS (consolidación):       No requerido en dev ⏸
```

---

## 7. Queries de verificación (solo lectura)

Ejecutar en BD dev para confirmar estado local:

```sql
-- V1: Toda identity ACTIVE con login potencial tiene membership ACTIVE
SELECT u.id, u.normalized_email, u.tenant_id AS legacy_tenant
FROM iam.iam_user u
WHERE u.status NOT IN ('DISABLED')
  AND NOT EXISTS (
    SELECT 1 FROM iam.identity_tenant_membership m
    WHERE m.identity_id = u.id AND m.status = 'ACTIVE'
  );

-- V2: Drift legacy tenant vs membership (informativo)
SELECT u.id, u.normalized_email, u.tenant_id, m.tenant_id AS membership_tenant
FROM iam.iam_user u
JOIN iam.identity_tenant_membership m ON m.identity_id = u.id AND m.status = 'ACTIVE'
WHERE u.tenant_id <> m.tenant_id;

-- V3: Emails duplicados (consolidación futura)
SELECT normalized_email, COUNT(*) FROM iam.iam_user
GROUP BY normalized_email HAVING COUNT(*) > 1;
```

**Expectativa entorno dev actual:** V3 probablemente `0` filas (registro post-13.1 bloquea nuevos duplicados).

---

## 8. Gaps conocidos (no bloquean FASE 14)

| Gap | Severidad | Plan |
|-----|-----------|------|
| `iam_user.tenant_id` en schema/dominio | Media | 13.6 — diferido |
| JWT `tenantId` no derivado de membership en emisión | Baja | Mejora opcional en 14.x |
| Auth membership-centric lookup `(tenant, email)` | Baja | 14.x si multi-membership UX |
| Consolidación datos 13.3 | N/A dev | Ejecutar antes prod con usuarios reales |
| `Identity extends AggregateRoot(TenantId)` | Media | Refactor con 13.6 |

---

## 9. Criterios de aceptación

| Criterio | Estado |
|----------|--------|
| Membership verificada como fuente de pertenencia | ✅ |
| TenantContext verificado | ✅ |
| Login sin dependencia funcional de `tenant_id` | ✅ |
| Registro email global | ✅ |
| ADR-006 evaluada | ✅ (parcial estructural documentado) |
| Consolidación no bloquea cierre FASE 13 | ✅ |
| Sin cambios código/datos | ✅ |

---

## 10. Cierre

**PASO 13.5 completado.** CodeCore puede iniciar **FASE 14 — Authorization Foundation** con las fuentes de verdad operativas verificadas.
