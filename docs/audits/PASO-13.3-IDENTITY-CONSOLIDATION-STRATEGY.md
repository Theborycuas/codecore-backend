    # PASO 13.3 — Identity Consolidation Strategy

    **Fecha:** 2026-05-27  
    **Alcance:** Estrategia oficial de consolidación de identities duplicadas hacia el modelo ADR-006 (Identity Global + Membership).  
    **Restricción:** Sin implementación, sin migraciones Flyway de datos, sin modificación de datos, sin cambios de login.

    **Estado previo:** ADR-006 ✓ · 13.1 Lookup Migration ✓ · 13.2 Membership-Centric Auth Audit ✓

    ---

    ## 1. Resumen ejecutivo

    ### Problema

    El modelo legacy **Identity Tenant Scoped** permitió múltiples filas `iam_user` con el mismo `normalized_email` y distintos `identity_id`. Tras 13.1, `findByEmail()` devuelve solo la identity más antigua (`ORDER BY created_at ASC`); las demás y sus memberships quedan **inaccesibles vía login** (PASO 13.2).

    ### Objetivo de 13.3

    Definir reglas, algoritmos, riesgos, rollback y runbook para que **13.4** ejecute la consolidación de forma segura y auditable.

    ### Decisiones formales (resumen)

    | # | Decisión |
    |---|----------|
    | 1 | Identity canónica: **Opción D** — criterios combinados; primario `created_at ASC` (alineado con `findByEmail`) |
    | 2 | Password: **política híbrida** — hash de `last_login_at` más reciente; fallback hash canónico |
    | 3 | Memberships: **reassign** `identity_id` → canónica; conflictos por tenant resueltos por reglas de prioridad |
    | 4 | Identities perdedoras: **`DISABLED` + tabla de archivo**; sin borrado físico hasta 13.6 |
    | 5 | Rollback: **snapshot tablas + log por email**; restore desde backup si fallo masivo |
    | 6 | ¿Una sola migración Flyway? | **NO** — migración de datos por lotes (un email = una transacción) |

    ---

    ## 2. Análisis del modelo actual

    ### 2.1 Entidades de dominio

    | Entidad | Rol en consolidación |
    |---------|---------------------|
    | `Identity` | Aggregate global objetivo; hoy acoplado a `tenant_id` en `iam_user` |
    | `IdentityTenantMembership` | Asociación N:M; **superviviente** — se reasigna `identity_id` |
    | `TenantId` | Inmutable en membership; no se fusionan tenants |
    | `Credential` / `password_hash` | Un solo hash por email post-consolidación |
    | `IdentityStatus` | Canónica conserva estado; perdedoras → `DISABLED` |

    ### 2.2 Repositorios (impacto 13.4)

    | Repositorio | Uso post-consolidación |
    |-------------|------------------------|
    | `IdentityRepository.findByEmail` | Debe devolver única fila por email |
    | `IdentityRepository.existsByEmail` | Debe ser unívoco |
    | `MembershipRepository.findByIdentityId` | Canónica agrega todas las memberships migradas |
    | `MembershipRepository.exists` | Sin cambio de API |

    **13.4 no modifica puertos** — solo datos. Cambios de login (membership-centric) son opcionales post-13.4.

    ### 2.3 Migraciones existentes

    | Versión | Relevancia |
    |---------|------------|
    | **V2** | `iam_user`; `UNIQUE (tenant_id, normalized_email)` — **permite** duplicados cross-tenant |
    | **V7** | `identity_tenant_membership`; `UNIQUE (identity_id, tenant_id)` — impide dos memberships misma identity+tenant |
    | **V8** | Backfill membership desde `iam_user.tenant_id`; una membership por fila legacy |

    **Implicación:** En datos V8 típicos, cada identity duplicada tiene **una membership en su tenant legacy**. El merge esperado es reasignar memberships de 200→100, 300→100 sin conflicto de tenant.

    ### 2.4 ADR-006

    Regla objetivo: **un `normalized_email` → un `identity_id` → N memberships**.

    ---

    ## 3. Inventario obligatorio

    Ejecutar en **solo lectura** (Fase 1 runbook) antes de 13.4.

    ### 3.1 Caso A — Emails únicos

    ```sql
    -- A: normalized_email con exactamente una identity
    SELECT normalized_email, COUNT(*) AS identity_count
    FROM iam.iam_user
    GROUP BY normalized_email
    HAVING COUNT(*) = 1
    ORDER BY normalized_email;
    ```

    **Métrica:** `count_case_a` — no requieren consolidación.

    ---

    ### 3.2 Caso B — Emails duplicados

    ```sql
    -- B: emails con más de una identity
    SELECT
        normalized_email,
        COUNT(*)                    AS identity_count,
        array_agg(id ORDER BY created_at ASC)   AS identity_ids,
        array_agg(tenant_id ORDER BY created_at ASC) AS tenant_ids,
        MIN(created_at)             AS first_created,
        MAX(created_at)             AS last_created
    FROM iam.iam_user
    GROUP BY normalized_email
    HAVING COUNT(*) > 1
    ORDER BY identity_count DESC, normalized_email;
    ```

    **Métrica:** `count_case_b_emails` — universo de trabajo de 13.4.

    ---

    ### 3.3 Caso C — Duplicados con passwords distintas

    ```sql
    -- C: mismo email, distintos password_hash
    SELECT
        u.normalized_email,
        COUNT(DISTINCT u.id)              AS identity_count,
        COUNT(DISTINCT u.password_hash)   AS distinct_hash_count,
        array_agg(DISTINCT u.password_hash) AS hashes
    FROM iam.iam_user u
    WHERE u.normalized_email IN (
        SELECT normalized_email
        FROM iam.iam_user
        GROUP BY normalized_email
        HAVING COUNT(*) > 1
    )
    GROUP BY u.normalized_email
    HAVING COUNT(DISTINCT u.password_hash) > 1
    ORDER BY u.normalized_email;
    ```

    **Métrica:** `count_case_c` — requiere política de credenciales (sección 5).

    ---

    ### 3.4 Caso D — Duplicados con memberships múltiples

    ```sql
    -- D: total memberships asociadas a identities duplicadas del mismo email
    SELECT
        u.normalized_email,
        COUNT(DISTINCT u.id) AS identity_count,
        COUNT(m.membership_id) AS total_memberships,
        COUNT(DISTINCT m.tenant_id) AS distinct_tenant_count
    FROM iam.iam_user u
    LEFT JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
    WHERE u.normalized_email IN (
        SELECT normalized_email FROM iam.iam_user
        GROUP BY normalized_email HAVING COUNT(*) > 1
    )
    GROUP BY u.normalized_email
    ORDER BY total_memberships DESC;
    ```

    **Detalle tenants por email:**

    ```sql
    SELECT
        u.normalized_email,
        m.tenant_id,
        m.identity_id,
        m.status,
        m.created_at
    FROM iam.iam_user u
    JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
    WHERE u.normalized_email = :email
    ORDER BY m.tenant_id, m.created_at;
    ```

    ---

    ### 3.5 Caso E — Duplicados sin memberships

    ```sql
    -- E: identity duplicada sin ninguna membership
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
        SELECT 1 FROM iam.identity_tenant_membership m
        WHERE m.identity_id = u.id
    )
    ORDER BY u.normalized_email, u.created_at;
    ```

    **Acción 13.4:** Crear membership ACTIVE `(canonical, tenant_id)` si la perdedora tenía `iam_user.tenant_id` sin membership; o archivar identity huérfana.

    ---

    ### 3.6 Vista resumen inventario

    ```sql
    SELECT
        (SELECT COUNT(*) FROM (
            SELECT normalized_email FROM iam.iam_user
            GROUP BY normalized_email HAVING COUNT(*) = 1
        ) t) AS emails_unicos,
        (SELECT COUNT(*) FROM (
            SELECT normalized_email FROM iam.iam_user
            GROUP BY normalized_email HAVING COUNT(*) > 1
        ) t) AS emails_duplicados,
        (SELECT COUNT(*) FROM iam.iam_user u
        WHERE normalized_email IN (
            SELECT normalized_email FROM iam.iam_user
            GROUP BY normalized_email HAVING COUNT(*) > 1
        )) AS filas_en_grupos_duplicados;
    ```

    ---

    ## 4. Identity canónica — evaluación de alternativas

    ### Opción A — Más antigua (`created_at ASC`)

    | Pros | Contras |
    |------|---------|
    | Alineado con `findByEmail` actual (13.1) | Password puede no ser el usado en otros tenants |
    | Usuarios que ya pueden login no cambian de `identity_id` | Ignora actividad reciente en identities más nuevas |

    ### Opción B — Más reciente (`created_at DESC`)

    | Pros | Contras |
    |------|---------|
    | Preferir registro más nuevo | **Rompe** login actual de quien ya accede vía identity antigua |
    | | Cambio de `sub` en JWT para usuarios activos |

    ### Opción C — Más memberships ACTIVE

    | Pros | Contras |
    |------|---------|
    | Maximiza cobertura tenant | En legacy V8 típico todas tienen 1 membership → **empate frecuente** |
    | | No desempata sin regla adicional |

    ### Opción D — Combinación de criterios ✅ **OFICIAL**

    **Regla de selección de identity canónica** (orden de prioridad):

    ```text
    1. Mayor cantidad de memberships ACTIVE
    2. Si empate: created_at ASC (más antigua) — alineación findByEmail
    3. Si empate: mayor last_login_at (NULLS LAST)
    4. Si empate: menor id UUID (lexicográfico) — determinismo total
    ```

    **Justificación:**

    - Priorizar memberships ACTIVE maximiza tenants recuperables sin conflicto.
    - `created_at ASC` como desempate mantiene continuidad con 13.1/13.2 para el caso común (1 membership por identity).
    - UUID final garantiza reproducibilidad en dry-run y staging.

    ### Función SQL de referencia (dry-run)

    ```sql
    -- Selección canónica por normalized_email (solo lectura)
    WITH ranked AS (
        SELECT
            u.id,
            u.normalized_email,
            u.created_at,
            u.last_login_at,
            u.password_hash,
            u.status,
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
        GROUP BY u.id, u.normalized_email, u.created_at, u.last_login_at,
                u.password_hash, u.status
    )
    SELECT * FROM ranked WHERE rn = 1;   -- canónicas
    -- SELECT * FROM ranked WHERE rn > 1;  -- perdedoras
    ```

    ---

    ## 5. Credenciales — política de password hash

    ### Escenario

    ```text
    Identity 100 — HASH_A, last_login 2025-01-01
    Identity 200 — HASH_B, last_login 2026-03-15
    Identity 300 — HASH_A (igual que 100)
    ```

    ### Evaluación de opciones

    | Opción | Descripción | Veredicto |
    |--------|-------------|-----------|
    | Hash más antiguo | Siempre canónica | Simple pero ignora uso real en otros tenants |
    | Hash más reciente | Por `last_login_at` | **Mejor UX** para quien usaba password en identity no canónica |
    | Reset obligatorio | `PASSWORD_RESET_REQUIRED` si difieren | Máxima seguridad; **alto impacto** soporte |
    | Híbrida | Reglas por caso | **Recomendada** |

    ### Política oficial — **Híbrida**

    ```text
    PASO 1: Si todos los password_hash del grupo son iguales
            → conservar ese hash en la canónica (sin cambio)

    PASO 2: Si hay hashes distintos
            → conservar hash de la identity con last_login_at más reciente (NOT NULL)
            → si ninguna tiene last_login_at: hash de la identity canónica (regla §4)

    PASO 3: Si hashes distintos Y ≥2 identities con last_login_at en ventana 90 días
            → aplicar hash del paso 2
            → marcar canónica status = PASSWORD_RESET_REQUIRED
            → registrar en iam.identity_consolidation_log (13.4) para notificación

    PASO 4: Registrar en log todo grupo Caso C para auditoría pre-prod
    ```

    **Rationale:** Minimiza lockouts (paso 2 favorece password realmente usada). Paso 3 cubre ambigüedad de seguridad sin reset universal.

    **Campos adicionales en canónica post-merge:**

    - `email` / `normalized_email` — del grupo (deben coincidir)
    - `status` — el más restrictivo ganador: `LOCKED` > `DISABLED` > `PASSWORD_RESET_REQUIRED` > `PENDING_VERIFICATION` > `ACTIVE` (documentar matriz en 13.4)
    - `last_login_at` — `MAX(last_login_at)` del grupo
    - `version` — incrementar en update

    ---

    ## 6. Algoritmo de merge de memberships

    ### 6.1 Entrada / salida

    **Entrada:** `normalized_email` con `identity_count > 1`  
    **Salida:** Una identity canónica con N memberships (una por tenant distinto, salvo conflictos resueltos)

    ### 6.2 Ejemplo objetivo

    ```text
    ANTES:
    Identity 100 + Membership(A)
    Identity 200 + Membership(B)
    Identity 300 + Membership(C)

    DESPUÉS:
    Identity 100 (canónica)
    Membership(A) → identity_id = 100
    Membership(B) → identity_id = 100  [reassigned desde 200]
    Membership(C) → identity_id = 100  [reassigned desde 300]
    ```

    ### 6.3 Algoritmo (pseudocódigo)

    ```text
    FOR EACH normalized_email WHERE identity_count > 1:

    1. canonical_id ← select_canonical_identity(email)   -- §4

    2. losers ← all identity_ids for email WHERE id <> canonical_id

    3. FOR EACH membership m WHERE m.identity_id IN losers:

        3a. IF NOT EXISTS (
                SELECT 1 FROM identity_tenant_membership
                WHERE identity_id = canonical_id AND tenant_id = m.tenant_id
            ):
                UPDATE membership SET identity_id = canonical_id,
                                    updated_at = now()
                WHERE membership_id = m.membership_id

        3b. ELSE -- conflicto: canónica y perdedora tienen membership mismo tenant
                resolve_membership_conflict(canonical_id, m)   -- §7

    4. FOR EACH loser identity:
        archive_identity(loser)                            -- §8
        UPDATE iam_user SET status = 'DISABLED', updated_at = now()
        WHERE id = loser

    5. apply_credential_policy(canonical_id, group)        -- §5

    6. INSERT identity_consolidation_log (...)

    COMMIT;  -- una transacción por email
    ```

    ### 6.4 Orden de operaciones dentro de la transacción

    ```text
    1. UPDATE memberships (reassign) — antes de tocar identities
    2. Resolver conflictos
    3. UPDATE credential/status en canónica
    4. DISABLE + archive perdedoras
    5. LOG
    ```

    **Razón:** `UNIQUE (identity_id, tenant_id)` en V7 exige resolver conflictos antes de reassign si ambas ACTIVE en mismo tenant.

    ---

    ## 7. Resolución de conflictos de membership

    ### Escenario

    ```text
    Identity 100 — Membership(A) ACTIVE, created T1
    Identity 200 — Membership(A) ACTIVE, created T2
    ```

    Solo puede existir **una** fila `(canonical_id, tenant_A)` por V7.

    ### Regla oficial — ganador de membership

    | Prioridad | Criterio |
    |-----------|----------|
    | 1 | `status = ACTIVE` gana sobre `INACTIVE` |
    | 2 | Si ambas ACTIVE: membership de la **identity canónica** gana |
    | 3 | Si ambas ACTIVE y ninguna es canónica aún: `created_at ASC` (más antigua) |
    | 4 | Perdedora: `status = INACTIVE` (no DELETE físico inmediato) |

    ### Auditoría de conflicto

    Registrar en `iam.identity_consolidation_log`:

    ```text
    event_type: MEMBERSHIP_CONFLICT_RESOLVED
    normalized_email
    tenant_id
    winning_membership_id
    losing_membership_id (marcada INACTIVE)
    canonical_identity_id
    resolved_at
    resolved_by: 'PASO_13_4'
    ```

    **Retención:** mínimo 24 meses (alineado con compliance futuro).

    ---

    ## 8. Identities perdedoras — estrategia

    ### Opciones evaluadas

    | Opción | Pros | Contras |
    |--------|------|---------|
    | Soft delete (flag) | Reversible | No existe columna `deleted_at` hoy |
    | Status `DISABLED` | Usa enum existente; bloquea auth | Fila permanece en `iam_user` |
    | Tabla temporal | Historial limpio | Requiere nueva tabla |
    | Eliminación física | Schema limpio | **Irreversible**; rompe rollback |

    ### Recomendación oficial — **`DISABLED` + tabla de archivo**

    **13.4 crea (Flyway estructural, sin datos en 13.3):**

    ```sql
    -- Diseño propuesto para 13.4 (NO ejecutar en 13.3)
    CREATE TABLE iam.identity_consolidation_archive (
        archive_id           UUID PRIMARY KEY,
        source_identity_id   UUID NOT NULL,
        canonical_identity_id UUID NOT NULL,
        normalized_email     VARCHAR(320) NOT NULL,
        tenant_id            UUID NOT NULL,
        password_hash        VARCHAR(500) NOT NULL,
        status_at_archive    VARCHAR(50) NOT NULL,
        archived_at          TIMESTAMPTZ NOT NULL,
        consolidation_batch  VARCHAR(100) NOT NULL
    );

    CREATE TABLE iam.identity_consolidation_log (
        log_id               UUID PRIMARY KEY,
        event_type           VARCHAR(100) NOT NULL,
        normalized_email     VARCHAR(320) NOT NULL,
        payload              JSONB NOT NULL,
        created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
    );
    ```

    **Por identity perdedora:**

    1. `INSERT` snapshot completo en `identity_consolidation_archive`
    2. `UPDATE iam_user SET status = 'DISABLED'`
    3. **No** `DELETE` físico hasta **13.6** (tras query E = 0)

    ---

    ## 9. Rollback

    ### 9.1 Principio

    Rollback debe restaurar estado **byte-a-byte lógico** de `iam_user` + `identity_tenant_membership` para emails procesados en el lote fallido.

    ### 9.2 Componentes

    | Componente | Descripción |
    |------------|-------------|
    | **Backup completo** | `pg_dump` schema `iam` antes de 13.4 prod |
    | **Snapshot tablas** | `CREATE TABLE iam.iam_user_backup_13_4 AS SELECT * FROM iam.iam_user` (idem membership) |
    | **Log por email** | `identity_consolidation_log` con estado `COMMITTED` / `ROLLED_BACK` por email |
    | **Batch ID** | UUID por ejecución; permite rollback parcial |

    ### 9.3 Si 13.4 falla a mitad de ejecución

    ```text
    1. DETENER procesamiento (no más emails)
    2. Identificar último email COMMITTED en log
    3. Para cada email en batch actual con estado COMMITTED o PARTIAL:
        a. Restaurar filas iam_user perdedoras desde archive (status, hash, etc.)
        b. Revertir membership.identity_id desde log (valores previos)
        c. Revertir cambios credential/status en canónica
        d. Marcar log ROLLED_BACK
    4. Si corrupción masiva o rollback parcial falla:
        RESTORE desde iam_user_backup_13_4 + membership_backup_13_4
    5. Validar queries §11 = estado pre-13.4
    6. Post-mortem antes de reintentar
    ```

    ### 9.4 Script de rollback (diseño)

    ```sql
    -- Por normalized_email específico (diseño)
    -- 1. Restaurar memberships desde log payload
    -- 2. UPDATE iam_user perdedoras SET status, password_hash FROM archive
    -- 3. Revertir canónica credential desde backup row
    ```

    **Validación post-rollback:**

    - Conteo `iam_user` = pre-migración
    - Conteo `identity_tenant_membership` = pre-migración
    - Caso B emails duplicados = mismo conjunto que inventario Fase 1

    ### 9.5 Ventana de rollback

    | Entorno | Ventana |
    |---------|---------|
    | Staging | Ilimitada (re-deploy) |
    | Producción | **72 horas** con snapshots calientes; después solo restore backup frío |

    ---

    ## 10. Runbook operativo

    ### Fase 1 — Inventario (solo lectura)

    | Paso | Acción |
    |------|--------|
    | 1.1 | Ejecutar queries §3 (casos A–E) en prod |
    | 1.2 | Exportar CSV `emails_duplicados` |
    | 1.3 | Clasificar Caso C (hash conflict) — revisión manual |
    | 1.4 | Aprobar go/no-go con métricas |

    **Go criteria:** Inventario completo; owners asignados; ventana de mantenimiento acordada.

    ---

    ### Fase 2 — Dry-run

    | Paso | Acción |
    |------|--------|
    | 2.1 | Copia anonymizada prod → entorno dry-run |
    | 2.2 | Ejecutar algoritmo §6 en modo `DRY_RUN` (sin COMMIT) |
    | 2.3 | Generar reporte: canónicas elegidas, conflictos, hashes |
    | 2.4 | Comparar con expectativas negocio / soporte |
    | 2.5 | Ajustar excepciones manuales (lista blanca emails) |

    **Artefacto:** `consolidation-dry-run-{date}.json`

    ---

    ### Fase 3 — Staging

    | Paso | Acción |
    |------|--------|
    | 3.1 | Backup schema `iam` |
    | 3.2 | Ejecutar 13.4 en staging (COMMIT por email) |
    | 3.3 | Queries validación §11 — todas en verde |
    | 3.4 | Smoke login: email duplicado × cada tenant |
    | 3.5 | Ejecutar rollback de prueba en subconjunto |
    | 3.6 | Re-ejecutar 13.4 staging tras rollback test |

    ---

    ### Fase 4 — Producción

    | Paso | Acción |
    |------|--------|
    | 4.1 | Comunicación usuarios afectados (Caso B) si aplica |
    | 4.2 | `pg_dump` + snapshots backup tablas |
    | 4.3 | Feature freeze IAM durante ventana |
    | 4.4 | Ejecutar 13.4 por lotes (ej. 50 emails/transacción batch, 1 email/TX) |
    | 4.5 | Monitorear errores; pausar si >1% fallos |
    | 4.6 | Log COMMITTED por email |

    **Duración estimada:** `(emails_duplicados × ~200ms) + overhead` — planificar ventana.

    ---

    ### Fase 5 — Validación

    | Paso | Acción |
    |------|--------|
    | 5.1 | Ejecutar queries §11 (A–E) |
    | 5.2 | Login manual muestra 10% Caso B |
    | 5.3 | Verificar `identity_consolidation_log` completo |
    | 5.4 | Sign-off arquitectura + operaciones |
    | 5.5 | Habilitar monitoreo 72h post-deploy |

    ---

    ## 11. Queries de validación post-13.4

    ### A — Emails duplicados (esperado: 0)

    ```sql
    SELECT normalized_email, COUNT(*) AS cnt
    FROM iam.iam_user
    WHERE status <> 'DISABLED'   -- perdedoras DISABLED no cuentan como activas
    GROUP BY normalized_email
    HAVING COUNT(*) > 1;
    ```

    **Variante estricta ADR-006 (pre-13.6):**

    ```sql
    SELECT normalized_email, COUNT(*) AS cnt
    FROM iam.iam_user
    GROUP BY normalized_email
    HAVING COUNT(*) > 1;
    -- Esperado tras 13.4: solo perdedoras DISABLED duplican filas hasta 13.6 purge
    ```

    **Objetivo 13.5:** una fila **activa** por email; perdedoras DISABLED permitidas temporalmente.

    ---

    ### B — Memberships huérfanas (esperado: 0)

    ```sql
    SELECT m.*
    FROM iam.identity_tenant_membership m
    LEFT JOIN iam.iam_user u ON u.id = m.identity_id
    WHERE u.id IS NULL;
    ```

    ---

    ### C — Identity sin membership (esperado: 0 o justificadas)

    ```sql
    SELECT u.id, u.normalized_email, u.status
    FROM iam.iam_user u
    WHERE u.status NOT IN ('DISABLED')
    AND NOT EXISTS (
        SELECT 1 FROM iam.identity_tenant_membership m
        WHERE m.identity_id = u.id AND m.status = 'ACTIVE'
    );
    ```

    **Justificadas:** identities `PENDING_VERIFICATION` recién registradas (fuera de scope consolidación).

    ---

    ### D — Memberships inaccesibles vía findByEmail (esperado: 0)

    ```sql
    WITH canonical AS (
        SELECT DISTINCT ON (normalized_email)
            id, normalized_email
        FROM iam.iam_user
        WHERE status <> 'DISABLED'
        ORDER BY normalized_email, created_at ASC
    )
    SELECT m.*
    FROM iam.identity_tenant_membership m
    JOIN iam.iam_user u ON u.id = m.identity_id
    JOIN canonical c ON c.normalized_email = u.normalized_email
    WHERE m.identity_id <> c.id
    AND m.status = 'ACTIVE';
    ```

    ---

    ### E — Duplicados residuales activos (esperado: 0)

    ```sql
    SELECT normalized_email, COUNT(*) AS active_identities
    FROM iam.iam_user
    WHERE status <> 'DISABLED'
    GROUP BY normalized_email
    HAVING COUNT(*) > 1;
    ```

    ---

    ## 12. Matriz de riesgos

    | Riesgo | Clasificación | Mitigación |
    |--------|---------------|------------|
    | Pérdida de login (password incorrecto post-merge) | **Alto** | Política híbrida §5; `PASSWORD_RESET_REQUIRED` selectivo |
    | Pérdida de memberships | **Crítico** | TX por email; archive + rollback §9 |
    | Corrupción datos (UNIQUE violation V7) | **Alto** | Algoritmo conflictos §7; orden operaciones §6.4 |
    | Rollback fallido | **Crítico** | Snapshot completo + pg_dump; drill en staging |
    | JWT `sub` cambia para usuario activo | **Medio** | Canónica = findByEmail; minimiza cambio |
    | Consolidación parcial | **Alto** | No pausar sin batch ID; log por email |
    | Soporte inundado | **Medio** | Comunicación + lista Caso C |
    | Performance prod | **Bajo** | Lotes off-peak |
    | Falsos positivos validación | **Bajo** | Queries §11 documentadas |

    ---

    ## 13. Decisiones obligatorias

    ### 1. ¿Qué identity será la canónica?

    **Opción D — Combinación:**

    1. Más memberships ACTIVE  
    2. `created_at ASC`  
    3. `last_login_at DESC NULLS LAST`  
    4. `id ASC`

    ### 2. ¿Qué password sobrevive?

    **Política híbrida (§5):** hash unificado si iguales; si difieren, hash de `last_login_at` más reciente; fallback hash canónico; `PASSWORD_RESET_REQUIRED` si ambigüedad multi-uso reciente.

    ### 3. ¿Cómo se fusionan memberships?

    **Reassign** `identity_id` → canónica por tenant; conflictos resueltos §7; una transacción SQL por `normalized_email`.

    ### 4. ¿Qué ocurre con identities perdedoras?

    **`status = DISABLED`** + snapshot en **`iam.identity_consolidation_archive`**; sin DELETE físico hasta 13.6.

    ### 5. ¿Qué estrategia de rollback?

    **Snapshot tablas + pg_dump + log por email** con procedimiento de restore parcial o completo (§9).

    ### 6. ¿Es viable ejecutar 13.4 en una sola migración Flyway?

    ## **NO**

    | Razón | Detalle |
    |-------|---------|
    | Volumen desconocido | Inventario puede ser grande |
    | Atomicidad | Una TX global bloquea tablas IAM |
    | Rollback Flyway | Flyway no revierte datos mergeados |
    | Fallo parcial | Requiere resume por email |
    | Dry-run | Imposible en migración única |

    **Enfoque 13.4:**

    - Flyway **estructural** (tablas archive/log) — una migración
    - **Datos** vía script operativo / job (Java o `psql`) — N transacciones, 1 email cada una
    - Idempotencia: skip email si log `COMMITTED` existe

    ---

    ## 14. Diagrama de consolidación

    ```mermaid
    flowchart TB
        subgraph before [Antes — Legacy]
            I100["Identity 100\nemail=juan@...\ntenant A"]
            I200["Identity 200\nemail=juan@...\ntenant B"]
            I300["Identity 300\nemail=juan@...\ntenant C"]
            M100["Membership(A)→100"]
            M200["Membership(B)→200"]
            M300["Membership(C)→300"]
            I100 --- M100
            I200 --- M200
            I300 --- M300
        end

        subgraph after [Después — ADR-006]
            CAN["Identity 100\nCANONICAL\nACTIVE"]
            MA["Membership(A)→100"]
            MB["Membership(B)→100"]
            MC["Membership(C)→100"]
            ARC["Archive: 200, 300\nDISABLED"]
            CAN --- MA
            CAN --- MB
            CAN --- MC
        end

        before -->|"13.4 merge"| after
    ```

    ---

    ## 15. Criterios de aceptación de 13.3

    | Criterio | Estado |
    |----------|--------|
    | Estrategia completa | ✅ |
    | Algoritmo de consolidación | ✅ §6 |
    | Estrategia rollback | ✅ §9 |
    | Runbook | ✅ §10 |
    | Queries inventario | ✅ §3 |
    | Queries validación | ✅ §11 |
    | Decisiones formales para 13.4 | ✅ §13 |
    | Sin cambios de código/datos | ✅ |

    ---

    ## 16. Referencias

    | Documento | Relación |
    |-----------|----------|
    | [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md) | Modelo objetivo |
    | [PASO-13.2-MEMBERSHIP-CENTRIC-AUTHENTICATION-AUDIT.md](PASO-13.2-MEMBERSHIP-CENTRIC-AUTHENTICATION-AUDIT.md) | Problema login duplicados |
    | [PASO-13.1-IDENTITY-LOOKUP-MIGRATION.md](PASO-13.1-IDENTITY-LOOKUP-MIGRATION.md) | findByEmail |
    | `V2__create_iam_user_table.sql` | Schema legacy |
    | `V7__create_identity_tenant_membership_table.sql` | UNIQUE membership |
    | `V8__backfill_identity_tenant_membership.sql` | Backfill histórico |
