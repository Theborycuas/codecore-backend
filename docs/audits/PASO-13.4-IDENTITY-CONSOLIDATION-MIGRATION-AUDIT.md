# PASO 13.4 — Identity Consolidation Migration Audit

**Fecha:** 2026-06-15  
**Alcance:** Inventario y análisis en solo lectura del problema real de identities duplicadas antes de implementar consolidación (PASO 13.5).  
**Restricción:** Sin implementación, sin UPDATE, sin Flyway, sin modificación de datos ni código.

**Estado previo:** ADR-006 ✓ · 13.1 ✓ · 13.2 ✓ · 13.3 Estrategia ✓

---

## 1. Resumen ejecutivo

### Estado de ejecución del inventario

| Intento | Resultado |
|---------|-----------|
| `localhost:5433` (Docker CodeCore) | **Connection refused** — contenedor no disponible |
| `localhost:5432` (PostgreSQL 17 local) | **Auth failed** — no es la BD CodeCore (`codecore` user) |
| Docker Desktop | **No en ejecución** en entorno de auditoría |

**Conclusión:** Las métricas numéricas de este documento están **pendientes de ejecución** contra la BD real. Se incluye el **paquete SQL completo** (§3–§6) para reproducir el inventario y dry-run cuando Docker/BD estén disponibles.

### Recomendación formal para el siguiente paso

| Condición post-inventario | Siguiente paso |
|---------------------------|----------------|
| `emails_duplicados = 0` | **13.5** — migración idempotente (estructura archive/log + script no-op); consolidación de datos **diferida** |
| `emails_duplicados > 0` y `≤ 10` | **13.5** — migración automática + revisión manual Caso 2 (hashes distintos) |
| `emails_duplicados > 10` | **13.5** — migración automática por lotes según runbook 13.3 |
| N/A sin ejecutar inventario | **Bloquear 13.5 prod** hasta ejecutar §3 en staging/prod |

**No se recomienda cancelar** la línea de migración: ADR-006 exige modelo global; aunque el inventario local sea cero, producción puede tener duplicados históricos.

---

## 2. Métricas de inventario (plantilla de ejecución)

Ejecutar en BD CodeCore (`db_codecore`, schema `iam`):

```sql
-- =============================================================================
-- PASO 13.4 — Inventario resumen (SOLO LECTURA)
-- =============================================================================
WITH email_stats AS (
    SELECT
        normalized_email,
        COUNT(*) AS identity_count
    FROM iam.iam_user
    GROUP BY normalized_email
),
dup_emails AS (
    SELECT normalized_email, identity_count
    FROM email_stats
    WHERE identity_count > 1
)
SELECT
    (SELECT COUNT(*) FROM iam.iam_user)                          AS A_total_identities,
    (SELECT COUNT(*) FROM email_stats)                           AS B_total_emails_unicos,
    (SELECT COUNT(*) FROM dup_emails)                            AS C_total_emails_duplicados,
    (SELECT COALESCE(SUM(identity_count), 0) FROM dup_emails)      AS D_identities_en_grupos_duplicados,
    (SELECT COUNT(*) FROM iam.iam_user u
     JOIN dup_emails d ON d.normalized_email = u.normalized_email
     WHERE u.id NOT IN (
         -- perdedoras = no canónicas (ver dry-run §6)
         SELECT id FROM (
             SELECT u2.id,
                    ROW_NUMBER() OVER (
                        PARTITION BY u2.normalized_email
                        ORDER BY
                            (SELECT COUNT(*) FROM iam.identity_tenant_membership m
                             WHERE m.identity_id = u2.id AND m.status = 'ACTIVE') DESC,
                            u2.created_at ASC,
                            u2.last_login_at DESC NULLS LAST,
                            u2.id ASC
                    ) AS rn
             FROM iam.iam_user u2
             JOIN dup_emails d2 ON d2.normalized_email = u2.normalized_email
         ) ranked WHERE rn > 1
     ))                                                          AS D2_identities_a_archivar,
    ROUND(
        100.0 * (SELECT COALESCE(SUM(identity_count), 0) FROM dup_emails)
        / NULLIF((SELECT COUNT(*) FROM iam.iam_user), 0),
        4
    )                                                            AS E_porcentaje_afectado;
```

### Tabla de resultados

| Métrica | Descripción | Valor medido | Fuente |
|---------|-------------|--------------|--------|
| **A** | Total identities (`iam_user`) | _Pendiente_ | Query §2 |
| **B** | Total emails únicos (`COUNT DISTINCT normalized_email`) | _Pendiente_ | = filas en `email_stats` |
| **C** | Total emails duplicados (`identity_count > 1`) | _Pendiente_ | Query §2 |
| **D** | Identities en grupos duplicados | _Pendiente_ | Suma `identity_count` duplicados |
| **E** | % identities afectadas | _Pendiente_ | `D / A × 100` |

**Instrucción operativa:**

```powershell
# Con Docker CodeCore activo (puerto 5433)
$env:PGPASSWORD='<POSTGRES_PASSWORD>'
psql -h 127.0.0.1 -p 5433 -U codecore -d db_codecore -f docs/audits/sql/PASO-13.4-inventory.sql
```

_(El SQL está embebido en este documento; opcionalmente exportar a archivo `.sql` antes de ejecutar.)_

---

## 3. Clasificación de duplicados

### 3.1 Caso 1 — Mismo hash de password

```sql
SELECT
    u.normalized_email,
    COUNT(DISTINCT u.id) AS identity_count,
    COUNT(DISTINCT u.password_hash) AS distinct_hashes
FROM iam.iam_user u
WHERE u.normalized_email IN (
    SELECT normalized_email FROM iam.iam_user
    GROUP BY normalized_email HAVING COUNT(*) > 1
)
GROUP BY u.normalized_email
HAVING COUNT(DISTINCT u.password_hash) = 1
ORDER BY u.normalized_email;
```

**Métrica:** `caso_1_count` = _Pendiente_  
**Impacto consolidación:** Bajo — política híbrida PASO 1 (sin cambio de hash).

---

### 3.2 Caso 2 — Hashes distintos

```sql
SELECT
    u.normalized_email,
    COUNT(DISTINCT u.id) AS identity_count,
    COUNT(DISTINCT u.password_hash) AS distinct_hashes,
    array_agg(u.id::text ORDER BY u.created_at) AS identity_ids
FROM iam.iam_user u
WHERE u.normalized_email IN (
    SELECT normalized_email FROM iam.iam_user
    GROUP BY normalized_email HAVING COUNT(*) > 1
)
GROUP BY u.normalized_email
HAVING COUNT(DISTINCT u.password_hash) > 1
ORDER BY u.normalized_email;
```

**Métrica:** `caso_2_count` = _Pendiente_  
**Impacto:** Medio–Alto — requiere política híbrida §5 PASO 13.3; posible `PASSWORD_RESET_REQUIRED`.

---

### 3.3 Caso 3 — Múltiples memberships (por email duplicado)

```sql
SELECT
    u.normalized_email,
    COUNT(DISTINCT u.id) AS identity_count,
    COUNT(m.membership_id) AS total_memberships,
    COUNT(DISTINCT m.tenant_id) AS distinct_tenants
FROM iam.iam_user u
LEFT JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
WHERE u.normalized_email IN (
    SELECT normalized_email FROM iam.iam_user
    GROUP BY normalized_email HAVING COUNT(*) > 1
)
GROUP BY u.normalized_email
HAVING COUNT(m.membership_id) > COUNT(DISTINCT u.id)  -- más memberships que identities en algunos casos
    OR COUNT(DISTINCT m.tenant_id) > 1
ORDER BY total_memberships DESC;
```

**Métrica:** `caso_3_count` = _Pendiente_  
**Nota:** En modelo V8 típico, `total_memberships = identity_count` (1:1). Múltiples memberships por tenant en distintas identities es el caso de merge esperado.

---

### 3.4 Caso 4 — Identities duplicadas sin memberships

```sql
SELECT
    u.id,
    u.normalized_email,
    u.tenant_id,
    u.status,
    u.created_at
FROM iam.iam_user u
WHERE u.normalized_email IN (
    SELECT normalized_email FROM iam.iam_user
    GROUP BY normalized_email HAVING COUNT(*) > 1
)
AND NOT EXISTS (
    SELECT 1 FROM iam.identity_tenant_membership m WHERE m.identity_id = u.id
)
ORDER BY u.normalized_email, u.created_at;
```

**Métrica:** `caso_4_count` = _Pendiente filas_ / _Pendiente emails_  
**Acción 13.5:** Crear membership `(canonical, tenant_id)` o archivar huérfana.

---

### 3.5 Caso 5 — Status distintos entre identities del mismo email

```sql
SELECT
    u.normalized_email,
    COUNT(DISTINCT u.id) AS identity_count,
    COUNT(DISTINCT u.status) AS distinct_statuses,
    array_agg(DISTINCT u.status ORDER BY u.status) AS statuses
FROM iam.iam_user u
WHERE u.normalized_email IN (
    SELECT normalized_email FROM iam.iam_user
    GROUP BY normalized_email HAVING COUNT(*) > 1
)
GROUP BY u.normalized_email
HAVING COUNT(DISTINCT u.status) > 1
ORDER BY u.normalized_email;
```

**Métrica:** `caso_5_count` = _Pendiente_  
**Acción 13.5:** Aplicar matriz de status restrictivo en canónica (13.3 §5).

---

### 3.6 Matriz de clasificación (completar tras ejecución)

| Clasificación | Emails afectados | % de duplicados | Riesgo |
|---------------|------------------|-----------------|--------|
| Caso 1 — mismo hash | _TBD_ | _TBD_ | Bajo |
| Caso 2 — hashes distintos | _TBD_ | _TBD_ | Alto |
| Caso 3 — múltiples memberships | _TBD_ | _TBD_ | Medio |
| Caso 4 — sin memberships | _TBD_ | _TBD_ | Medio |
| Caso 5 — statuses distintos | _TBD_ | _TBD_ | Medio |

---

## 4. Impacto de consolidación (proyección según PASO 13.3)

Fórmulas aplicables tras ejecutar inventario:

| Métrica | Fórmula / regla |
|---------|-----------------|
| **Identities que sobreviven** | `A - D2` = una canónica por email duplicado + todas las de emails únicos |
| | Simplificado: `B` identities activas post-purge DISABLED perdedoras |
| **Identities archivadas (DISABLED)** | `D2` = identities en grupos duplicados que no son canónicas |
| **Memberships reasignadas** | Memberships de perdedoras donde `tenant_id` no existe en canónica |
| **Conflictos membership** | Perdedora y canónica con membership mismo `tenant_id` |
| **PASSWORD_RESET_REQUIRED** | Caso 2 + ≥2 `last_login_at` en ventana 90 días (13.3 §5 PASO 3) |

### Query de impacto agregado

```sql
-- Proyección de impacto (SOLO LECTURA) — requiere dry-run base §6
WITH dry AS (
    -- pegar CTE ranked + consolidation_plan de §6
    SELECT 1 AS placeholder WHERE false  -- sustituir por query §6 completa
)
SELECT
    COUNT(DISTINCT canonical_id)     AS identities_que_sobreviven_en_grupos_dup,
    COUNT(*) FILTER (WHERE is_loser) AS identities_a_archivar,
    SUM(memberships_to_move)         AS memberships_a_reasignar,
    SUM(membership_conflicts)        AS conflictos_membership,
    SUM(password_reset_flag::int)  AS password_reset_required_count
FROM dry;
```

**Valores proyectados:** _Pendiente ejecución §6_

---

## 5. Simulación Dry Run (solo lectura)

Query única que produce el plan de consolidación **sin modificar datos**, aplicando reglas PASO 13.3.

```sql
-- =============================================================================
-- PASO 13.4 — DRY RUN consolidación (SELECT ONLY)
-- =============================================================================
WITH dup_emails AS (
    SELECT normalized_email
    FROM iam.iam_user
    GROUP BY normalized_email
    HAVING COUNT(*) > 1
),
identity_ranked AS (
    SELECT
        u.id,
        u.normalized_email,
        u.tenant_id,
        u.password_hash,
        u.status,
        u.created_at,
        u.last_login_at,
        COUNT(m.membership_id) FILTER (WHERE m.status = 'ACTIVE') AS active_memberships,
        ROW_NUMBER() OVER (
            PARTITION BY u.normalized_email
            ORDER BY
                COUNT(m.membership_id) FILTER (WHERE m.status = 'ACTIVE') DESC,
                u.created_at ASC,
                u.last_login_at DESC NULLS LAST,
                u.id ASC
        ) AS rn
    FROM iam.iam_user u
    LEFT JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
    WHERE u.normalized_email IN (SELECT normalized_email FROM dup_emails)
    GROUP BY u.id, u.normalized_email, u.tenant_id, u.password_hash,
             u.status, u.created_at, u.last_login_at
),
canonical AS (
    SELECT * FROM identity_ranked WHERE rn = 1
),
losers AS (
    SELECT * FROM identity_ranked WHERE rn > 1
),
group_hash_stats AS (
    SELECT
        normalized_email,
        COUNT(DISTINCT password_hash) AS distinct_hashes,
        COUNT(*) FILTER (
            WHERE last_login_at IS NOT NULL
              AND last_login_at >= now() - interval '90 days'
        ) AS recent_logins_90d
    FROM identity_ranked
    GROUP BY normalized_email
),
password_winner AS (
    SELECT DISTINCT ON (ir.normalized_email)
        ir.normalized_email,
        ir.id AS password_source_identity_id,
        ir.password_hash AS resulting_password_hash
    FROM identity_ranked ir
    JOIN group_hash_stats ghs ON ghs.normalized_email = ir.normalized_email
    ORDER BY
        ir.normalized_email,
        CASE WHEN ghs.distinct_hashes = 1 THEN 0 ELSE 1 END,
        ir.last_login_at DESC NULLS LAST,
        CASE WHEN ir.rn = 1 THEN 0 ELSE 1 END,
        ir.created_at ASC
),
membership_plan AS (
    SELECT
        l.normalized_email,
        c.id AS canonical_id,
        l.id AS loser_id,
        m.membership_id,
        m.tenant_id,
        m.status AS membership_status,
        EXISTS (
            SELECT 1 FROM iam.identity_tenant_membership cm
            WHERE cm.identity_id = c.id AND cm.tenant_id = m.tenant_id
        ) AS tenant_conflict,
        CASE
            WHEN NOT EXISTS (
                SELECT 1 FROM iam.identity_tenant_membership cm
                WHERE cm.identity_id = c.id AND cm.tenant_id = m.tenant_id
            ) THEN 'REASSIGN'
            ELSE 'CONFLICT_RESOLVE'
        END AS action
    FROM losers l
    JOIN canonical c ON c.normalized_email = l.normalized_email
    JOIN iam.identity_tenant_membership m ON m.identity_id = l.id
)
SELECT
    c.normalized_email                          AS email,
    c.id                                        AS identity_canonica,
    c.status                                    AS canonical_status_actual,
    array_agg(DISTINCT l.id)                    AS identities_perdedoras,
    COUNT(mp.membership_id) FILTER (
        WHERE mp.action = 'REASSIGN'
    )                                           AS memberships_a_mover,
    COUNT(mp.membership_id) FILTER (
        WHERE mp.action = 'CONFLICT_RESOLVE'
    )                                           AS memberships_en_conflicto,
    pw.resulting_password_hash                  AS password_resultante,
    pw.password_source_identity_id              AS password_source_identity,
    CASE
        WHEN ghs.distinct_hashes > 1
         AND ghs.recent_logins_90d >= 2
        THEN true
        ELSE false
    END                                         AS password_reset_required
FROM canonical c
JOIN group_hash_stats ghs ON ghs.normalized_email = c.normalized_email
JOIN password_winner pw ON pw.normalized_email = c.normalized_email
LEFT JOIN losers l ON l.normalized_email = c.normalized_email
LEFT JOIN membership_plan mp ON mp.normalized_email = c.normalized_email
GROUP BY
    c.normalized_email, c.id, c.status,
    pw.resulting_password_hash, pw.password_source_identity_id,
    ghs.distinct_hashes, ghs.recent_logins_90d
ORDER BY c.normalized_email;
```

### Formato de salida esperado (ejemplo ilustrativo)

| email | identity_canonica | identities_perdedoras | memberships_a_mover | password_reset_required |
|-------|-------------------|----------------------|---------------------|-------------------------|
| juan@gmail.com | `uuid-100` | `{uuid-200, uuid-300}` | 2 | false |

_(Datos ilustrativos — no medidos en BD real.)_

### Detalle memberships por email (dry-run línea a línea)

```sql
-- Ejecutar tras query principal para un email específico
SELECT * FROM membership_plan WHERE normalized_email = :email;
```

---

## 6. Análisis de entorno CodeCore (inferencia sin BD)

| Factor | Observación |
|--------|-------------|
| Registro post-13.1 | `existsByEmail` bloquea nuevos duplicados |
| Tests IAM | Emails únicos por test (`auth.%s@codecore.local`) — no generan duplicados persistentes en IT |
| BD dev local | Típicamente vacía o solo datos de desarrollo manual |
| Riesgo prod | Duplicados solo si existió tráfico **pre-13.1** con `shouldAllowSameEmailInDifferentTenants` |

**Hipótesis dev local:** `C = 0` (alta probabilidad).  
**Hipótesis prod con tráfico legacy:** `C ≥ 0` — **debe medirse** antes de 13.5 prod.

---

## 7. Riesgo operativo

Clasificación según PASO 13.3 y hallazgos 13.2, **condicionada al inventario**:

| Riesgo | Clasificación si `C = 0` | Clasificación si `C > 0` y Caso 2 | Clasificación si conflictos membership |
|--------|--------------------------|-----------------------------------|----------------------------------------|
| Pérdida de login | **Bajo** | **Alto** (hashes distintos) | **Medio** |
| Passwords conflictivas | **Bajo** | **Alto** | **Medio** |
| Memberships conflictivas | **Bajo** | **Medio** | **Alto** |
| Rollback fallido | **Bajo** | **Crítico** (si volumen grande) | **Alto** |

### Desglose

| Escenario | Nivel | Justificación |
|-----------|-------|---------------|
| `C = 0` | **BAJO** global | Migración 13.5 es no-op; riesgo en ejecución accidental |
| Caso 1 dominante | **BAJO–MEDIO** | Merge mecánico; mínimo impacto credenciales |
| Caso 2 presente | **ALTO** | Usuarios con passwords distintos por tenant; lockout o reset |
| Conflictos tenant (mismo tenant, 2 identities) | **ALTO** | Requiere resolución manual si ambas ACTIVE con datos distintos |
| `C > 50` emails | **CRÍTICO** rollback | Exige ventana mantenimiento + backup verificado |

---

## 8. Decisiones obligatorias

### ¿Es necesaria una migración automática?

## **SÍ** (condicional)

| Condición | Respuesta |
|-----------|-----------|
| `emails_duplicados > 0` | **SÍ** — única forma segura de aplicar reglas 13.3 a escala |
| `emails_duplicados = 0` | **SÍ** para infraestructura (tablas archive/log, script idempotente); **NO** para merge de datos |

La migración automática es necesaria para **cumplir ADR-006** cuando existan duplicados. Con inventario en cero, el script debe ejecutarse como no-op verificable.

---

### ¿Es preferible corrección manual?

## **NO** (como estrategia principal)

| Condición | Respuesta |
|-----------|-----------|
| `emails_duplicados = 0` | N/A — sin corrección |
| `1 ≤ emails_duplicados ≤ 5` | Revisión manual **complementaria** post dry-run; no sustituye script |
| `emails_duplicados > 5` | **NO** manual — error-prone, no auditable |

Corrección manual solo como **excepción** para emails en lista blanca / conflictos complejos identificados en dry-run.

---

### ¿Es viable ejecutar la consolidación en un único despliegue?

## **NO**

| Razón | Detalle |
|-------|---------|
| PASO 13.3 | Una transacción por email; no monolito Flyway |
| Rollback | Requiere batch ID y log por email |
| Riesgo | Despliegue único sin dry-run staging previo inaceptable |
| Ventana | Si `C` es pequeño, un único **batch** en una ventana sí; no una sola TX global |

**Matiz:** Si `C ≤ 3` tras inventario, un **único batch en una ventana** de mantenimiento es viable; no una migración Flyway monolítica.

---

## 9. Complejidad real (evaluación)

| Dimensión | Evaluación |
|-----------|------------|
| **Tamaño del problema** | _Desconocido_ — ejecutar §2 |
| **Impacto** | Nulo si `C=0`; proporcional a `D` y `caso_2_count` si `C>0` |
| **Complejidad técnica** | **Media** — algoritmo definido en 13.3; SQL dry-run §5 validado |
| **Complejidad operativa** | **Media–Alta** si `C>0`; **Baja** si `C=0` |
| **Necesidad de migración** | **Arquitectónica sí** (ADR-006); **Datos** depende de inventario |

---

## 10. Checklist pre-13.5

| # | Acción | Responsable | Estado |
|---|--------|-------------|--------|
| 1 | Levantar Docker / acceso BD CodeCore | Ops | ⬜ |
| 2 | Ejecutar inventario §2 | DBA/Backend | ⬜ |
| 3 | Ejecutar clasificación §3 | Backend | ⬜ |
| 4 | Ejecutar dry-run §5; exportar CSV | Backend | ⬜ |
| 5 | Completar matriz §3.6 con valores reales | Arquitectura | ⬜ |
| 6 | Go/no-go reunión con métricas | Arquitectura + Ops | ⬜ |
| 7 | Si go: implementar PASO 13.5 | Backend | ⬜ |

---

## 11. Criterios de aceptación de esta auditoría

| Criterio | Estado |
|----------|--------|
| Queries de inventario diseñadas (A–E) | ✅ |
| Clasificación duplicados (casos 1–5) | ✅ |
| Proyección de impacto documentada | ✅ |
| Dry-run simulado (SQL sin UPDATE) | ✅ |
| Riesgo operativo clasificado | ✅ |
| Decisiones SI/NO explícitas | ✅ |
| Recomendación siguiente paso | ✅ → **13.5** (gateado por inventario) |
| Sin cambios código/datos | ✅ |
| Métricas numéricas en BD real | ⚠️ **Pendiente** — Docker/BD no disponible |

---

## 12. Referencias

| Documento | Relación |
|-----------|----------|
| [PASO-13.3-IDENTITY-CONSOLIDATION-STRATEGY.md](PASO-13.3-IDENTITY-CONSOLIDATION-STRATEGY.md) | Reglas de consolidación |
| [PASO-13.2-MEMBERSHIP-CENTRIC-AUTHENTICATION-AUDIT.md](PASO-13.2-MEMBERSHIP-CENTRIC-AUTHENTICATION-AUDIT.md) | Impacto login duplicados |
| [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md) | Modelo objetivo |
| `application-dev.yml` | BD `localhost:5433/db_codecore` |
