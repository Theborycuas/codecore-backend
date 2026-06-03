# PASO 12.4 — Membership Backfill & Consistency Audit

**Fecha:** 2026-06-03  
**Alcance:** Auditoría y plan de migración. **Sin cambios de código, Flyway ni endpoints.**

**Estado previo verificado:** PASO 12.3 — registro crea membership ACTIVE; login valida membership ACTIVE; `BUILD SUCCESSFUL`.

---

## 1. Resumen ejecutivo

Tras 12.3 coexisten **dos fuentes de verdad** para la pertenencia a tenant:

| Fuente | Rol actual |
|--------|------------|
| `iam.iam_user.tenant_id` | Lookup de registro/login (`tenantId + email`) |
| `iam.identity_tenant_membership` | Vínculo formal N:M; **obligatorio en login** desde 12.3 |

Identidades creadas **antes de 12.3** o por rutas que solo persisten `iam_user` quedan **bloqueadas en login** (403) aunque la contraseña sea correcta. El PASO 12.4 define el diagnóstico, los riesgos y la estrategia de backfill para el siguiente paso de implementación.

**Decisión recomendada:** ejecutar backfill idempotente en Flyway (V8+) **antes** de desplegar 12.3 en producción, o inmediatamente después si 12.3 ya está desplegado sin backfill.

---

## 2. Auditoría del modelo actual

### 2.1 Artefactos analizados

| Artefacto | Responsabilidad relevante |
|-----------|---------------------------|
| `Identity` | Aggregate con `tenantId` heredado de `AggregateRoot`; PK global `IdentityId` |
| `IdentityTenantMembership` | Aggregate de asociación identity ↔ tenant; estados ACTIVE/INACTIVE |
| `IdentityRepository` | CRUD + `findByTenantAndEmail` / `existsByTenantAndEmail` sobre `iam_user` |
| `MembershipRepository` | CRUD + `findByIdentityId` / `findByTenantId` / `exists` |
| `RegisterIdentityUseCaseImpl` | `save(identity)` → `save(membership ACTIVE)` encadenado con `flatMap` |
| `AuthenticateIdentityUseCaseImpl` | Lookup legado → credenciales → `requireActiveMembership` → JWT |

### 2.2 Preguntas de consistencia

#### 1. ¿Puede existir Identity sin Membership?

**Sí**, en estos escenarios:

| Escenario | Probabilidad | Evidencia |
|-----------|--------------|-----------|
| Datos históricos (pre-12.3) | Alta en prod si hubo tráfico | V7 añade tabla vacía para filas existentes |
| Fallo tras `save(identity)` y antes de `save(membership)` | Baja pero posible | Sin `@Transactional` / `TransactionalOperator` (documentado en 12.3) |
| Tests o scripts que insertan solo `iam_user` | Media en dev/CI | `AuthenticateIdentityUseCaseIT.shouldRejectLoginWhenMembershipMissing` simula `persistIdentityOnly` |
| Registro vía use case desde 12.3 | No en flujo feliz | `RegisterIdentityUseCaseImpl` siempre encadena membership |

El dominio **no** modela membership como parte obligatoria del aggregate `Identity`; la invariante es solo **aplicación** en registro, no persistencia ni FK.

#### 2. ¿Puede existir Membership sin Identity?

**Sí**, a nivel de base de datos:

- V7 **no define FK** `identity_id → iam_user.id` (coherente con estrategia V1–V6 del proyecto).
- Nada impide insertar una fila membership con `identity_id` inexistente.
- No hay validación en `R2dbcMembershipRepository.save`.

Riesgo: **membership huérfana** (datos basura o errores manuales).

#### 3. ¿Puede existir más de una Membership para el mismo tenant?

**No** para el par `(identity_id, tenant_id)`:

- `UNIQUE (identity_id, tenant_id)` en V7.

**Matiz importante (modelo híbrido):**

- Un mismo **email** en dos tenants distintos implica **dos filas** `iam_user` con **dos `identity_id` distintos** (`RegisterIdentityUseCaseIT.shouldAllowSameEmailInDifferentTenants`).
- Cada una puede tener su membership ACTIVE en su tenant.
- El N:M “real” (una identity global, varios tenants) **aún no está materializado** en `iam_user`; hoy es 1 fila = 1 tenant + 1 identity_id.

#### 4. ¿Puede existir inconsistencia entre `iam_user.tenant_id` y `membership.tenant_id`?

**Sí**, porque no hay restricción DB ni transacción atómica que las acople:

| Situación | Efecto en login |
|-----------|-----------------|
| `iam_user` tenant **A**, membership solo tenant **B** | Login tenant **A**: encuentra identity → password OK → filtro membership en **A** falla → **403** |
| `iam_user` tenant **A**, membership tenant **A** + membership tenant **B** | Login tenant **A**: OK si membership A ACTIVE. Login tenant **B**: `findByTenantAndEmail(B)` **no encuentra fila** → **401** (credenciales inválidas) |
| Sin membership, `iam_user.tenant_id` correcto | Login tenant correcto → **403** tras password OK |
| Edición manual / script parcial | Comportamiento impredecible según tenant del request |

El login **nunca compara** explícitamente `iam_user.tenant_id` con `membership.tenant_id`; la coherencia se infiere porque el lookup inicial ya acota por tenant del comando.

#### 5. ¿Qué restricciones actuales evitan inconsistencias?

| Capa | Restricción | Alcance |
|------|-------------|---------|
| DB | `uq_iam_user_tenant_normalized_email` | Un email normalizado por tenant en `iam_user` |
| DB | `uq_identity_tenant_membership_identity_tenant` | Un membership por par identity+tenant |
| DB | CHECK de status en ambas tablas | Valores válidos de enum |
| Aplicación | `RegisterIdentityUseCaseImpl` usa el mismo `tenantId` para identity y membership | Solo en registro exitoso completo |
| Aplicación | `requireActiveMembership` filtra por `tenantId` del comando | Login |
| Tests | IT de registro verifica fila membership ACTIVE | Regresión 12.3 |

#### 6. ¿Qué restricciones faltan?

| Restricción | Prioridad | Motivo |
|-------------|-----------|--------|
| FK `identity_tenant_membership.identity_id → iam_user.id` | Media | Evita memberships huérfanas |
| FK `identity_tenant_membership.tenant_id → iam.tenant.tenant_id` | Media | Evita tenant inexistente |
| Transacción atómica identity + membership en registro | Alta | Evita identity sin membership |
| Trigger / job de reconciliación periódica | Baja (post-backfill) | Detecta drift |
| Invariante de dominio “Identity registrada implica membership” | Media | Refuerzo en capa de aplicación |
| Índice dedicado `(tenant_id)` en membership | Media | Consultas `findByTenantId` |
| Eliminar dependencia de `iam_user.tenant_id` en lookup | Baja (futuro) | Convergencia N:M real |

---

## 3. Auditoría de base de datos

### 3.1 Tabla `iam.identity_tenant_membership` (V7)

```sql
-- Estado actual (V7)
PK:   membership_id
UQ:   (identity_id, tenant_id)
CHECK: status IN ('ACTIVE', 'INACTIVE')
FK:   ninguna
```

### 3.2 Tabla `iam.iam_user` (V2, relevante)

```sql
PK:   id  (= identity_id de dominio)
UQ:   (tenant_id, normalized_email)
IDX:  tenant_id, email, normalized_email, status
FK:   ninguna hacia iam.tenant
```

### 3.3 Claves foráneas — recomendación (no implementar en 12.4)

| FK propuesta | ON DELETE | Justificación |
|--------------|-----------|---------------|
| `identity_id → iam_user(id)` | `CASCADE` o `RESTRICT` | CASCADE limpia memberships al borrar identity; RESTRICT evita borrados accidentales |
| `tenant_id → iam.tenant(tenant_id)` | `RESTRICT` | Membership solo para tenants existentes |

**Nota:** introducir FK en producción requiere **pre-validación** (filas huérfanas = 0) como parte del backfill.

### 3.4 Índices — evaluación

| Índice | ¿Existe? | ¿Necesario? | Recomendación |
|--------|----------|-------------|---------------|
| PK `(membership_id)` | Sí | Sí | Mantener |
| UNIQUE `(identity_id, tenant_id)` | Sí | Sí | Cubre login (`findByIdentityId` + filtro tenant); en PostgreSQL sirve para búsquedas por prefijo `identity_id` |
| `(identity_id)` solo | Implícito vía UNIQUE compuesto | Opcional | No añadir índice redundante |
| `(tenant_id)` | **No** | **Sí** para `findByTenantId`, auditorías por tenant, backfill inverso | **Recomendar** `idx_identity_tenant_membership_tenant_id` en paso posterior |
| `(tenant_id, status)` | No | Baja | Solo si hay listados admin filtrados por estado |

---

## 4. Auditoría de login

### 4.1 Flujo actual

```
Request (tenantId + email + password)
    │
    ▼
identityRepository.findByTenantAndEmail(tenantId, email)   ← usa iam_user.tenant_id
    │ vacío → InvalidCredentialsException → HTTP 401
    ▼
identity.status == ACTIVE?                                 ← no PENDING, LOCKED, etc.
    │ no → IdentityNotAllowedToAuthenticateException → HTTP 403/423
    ▼
passwordHasher.matches(...)
    │ no → InvalidCredentialsException → HTTP 401
    ▼
membershipRepository.findByIdentityId(identity.id())
    .filter(m → m.tenantId == tenantId)
    .next()
    │ vacío o INACTIVE → IdentityNotMemberOfTenantException → HTTP 403
    ▼
tokenProvider.generateAccessToken(...) → HTTP 200 + JWT
```

**Orden de validación:** credenciales **antes** que membership (no se filtra información de membership con password incorrecta). Cubierto por tests unitarios.

### 4.2 Casos borde

#### Caso A — Identity existe, Membership no existe

| Campo | Valor |
|-------|-------|
| **Resultado esperado** | `IdentityNotMemberOfTenantException` → HTTP **403** |
| **Implementación** | `switchIfEmpty` en `requireActiveMembership` |
| **Tests** | `AuthenticateIdentityUseCaseTest.shouldRejectWhenNoMembershipForTenant`, `AuthenticateIdentityUseCaseIT.shouldRejectLoginWhenMembershipMissing` |
| **Impacto prod pre-backfill** | Usuarios históricos bloqueados tras despliegue 12.3 |

#### Caso B — Identity existe, Membership INACTIVE

| Campo | Valor |
|-------|-------|
| **Resultado esperado** | `IdentityNotMemberOfTenantException` → HTTP **403** |
| **Implementación** | Rechazo si `membership.status() != ACTIVE` |
| **Tests** | `shouldRejectWhenMembershipInactive` (unit + IT) |

#### Caso C — `iam_user.tenant_id` legado ≠ `membership.tenant_id`

| Subcaso | Resultado |
|---------|-----------|
| Login con tenant del **iam_user** (A), membership solo en **B** | Password OK → **403** (sin membership para A) |
| Login con tenant **B**, identity solo en **A** | **401** (identity no encontrada por lookup legado) |
| Membership en A y B, iam_user solo en A | Login A OK; login B **401** (modelo híbrido; N:M no operativo en lookup) |

**Conclusión:** la inconsistencia **no produce login en tenant incorrecto**; produce **denegación** (401 o 403). Riesgo principal: **usuarios legítimos bloqueados**, no escalada cross-tenant.

#### Caso D — Identity con múltiples memberships

| Campo | Valor |
|-------|-------|
| **Estado teórico** | Varios `(identity_id, tenant_j)` con distintos `tenant_j` |
| **Estado práctico hoy** | Una fila `iam_user` por `identity_id` con un solo `tenant_id`; login a otro tenant requiere otra fila (otro id) |
| **Código** | `findByIdentityId` → `filter(tenantId)` → `.next()` (primer match) |
| **Resultado esperado** | Si UNIQUE se respeta: **como máximo una** membership por tenant; login exitoso solo si existe membership ACTIVE para el tenant del request **y** lookup legado encuentra fila |
| **Riesgo** | Bajo hoy; **medio** cuando se elimine `tenant_id` de lookup y se use solo membership |

---

## 5. Estrategia de backfill

### 5.1 Objetivo

Poblar `identity_tenant_membership` para todas las filas de `iam_user` que no tengan ya el par `(identity_id, tenant_id)`.

### 5.2 Regla propuesta

Por cada fila en `iam_user`:

```
SI NOT EXISTS membership WHERE identity_id = iam_user.id
                         AND tenant_id   = iam_user.tenant_id
ENTONCES INSERT membership:
  membership_id = gen_random_uuid()   -- o UUID v7 desde app en migración Java
  identity_id   = iam_user.id
  tenant_id     = iam_user.tenant_id
  status        = ACTIVE
  created_at    = iam_user.created_at   -- recomendado (ver §5.4)
  updated_at    = iam_user.updated_at   -- recomendado
```

**SQL idempotente (borrador para PASO 12.5):**

```sql
INSERT INTO iam.identity_tenant_membership (
    membership_id, identity_id, tenant_id, status, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    u.tenant_id,
    'ACTIVE',
    u.created_at,
    u.updated_at
FROM iam.iam_user u
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.identity_tenant_membership m
    WHERE m.identity_id = u.id
      AND m.tenant_id   = u.tenant_id
);
```

### 5.3 Ventajas

| Ventaja | Detalle |
|---------|---------|
| Idempotente | `NOT EXISTS` / `ON CONFLICT DO NOTHING` permite re-ejecución |
| Alineación con login 12.3 | Restaura acceso a usuarios ACTIVE con credenciales válidas |
| Bajo impacto en registro | Nuevos registros ya crean membership; backfill no duplica gracias a UNIQUE |
| Sin cambio de API | Solo datos |
| Auditable | Conteo pre/post: `COUNT(*) FROM iam_user` vs memberships creadas |

### 5.4 Riesgos y casos especiales

| Caso | Riesgo | Mitigación propuesta |
|------|--------|----------------------|
| **`created_at = now()`** vs histórico | Pierde trazabilidad | Preferir `iam_user.created_at` / `updated_at` |
| Identity **DISABLED/LOCKED** | Membership ACTIVE no habilita login (status identity) | Aceptable; membership refleja pertenencia, no elegibilidad |
| Identity **PENDING_VERIFICATION** | Tras backfill sigue sin login (identity no ACTIVE) | Coherente con reglas actuales |
| **`tenant_id` en `iam_user` apunta a tenant borrado** | FK futura fallaría; membership inválida | Pre-flight: `LEFT JOIN iam.tenant`; reportar huérfanos |
| **Membership huérfana preexistente** (identity inexistente) | No la corrige el backfill | Query de limpieza separada antes de FK |
| **Race con registro concurrente** | Doble insert mismo par | UNIQUE + `ON CONFLICT DO NOTHING` |
| **Fallo parcial en migración** | Algunas filas insertadas | Migración en una transacción Flyway; script de verificación post-deploy |
| Usuario desactivado en membership manualmente (futuro) | Backfill podría re-activar si mal diseñado | Backfill solo `NOT EXISTS`, no UPDATE |

### 5.5 Consultas de auditoría pre/post (solo lectura)

```sql
-- A) Identities sin membership (debe → 0 tras backfill)
SELECT u.id, u.tenant_id, u.email, u.status
FROM iam.iam_user u
LEFT JOIN iam.identity_tenant_membership m
  ON m.identity_id = u.id AND m.tenant_id = u.tenant_id
WHERE m.membership_id IS NULL;

-- B) Memberships huérfanas (identity inexistente)
SELECT m.*
FROM iam.identity_tenant_membership m
LEFT JOIN iam.iam_user u ON u.id = m.identity_id
WHERE u.id IS NULL;

-- C) Mismatch tenant (misma identity, distinto tenant entre tablas para el par canónico)
SELECT u.id, u.tenant_id AS user_tenant, m.tenant_id AS membership_tenant
FROM iam.iam_user u
JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
WHERE m.tenant_id <> u.tenant_id;
-- Nota: puede ser válido en N:M futuro; hoy indica inconsistencia del modelo híbrido

-- D) Conteo resumen
SELECT
  (SELECT COUNT(*) FROM iam.iam_user) AS users,
  (SELECT COUNT(*) FROM iam.identity_tenant_membership) AS memberships,
  (SELECT COUNT(*) FROM iam.iam_user u
   WHERE EXISTS (
     SELECT 1 FROM iam.identity_tenant_membership m
     WHERE m.identity_id = u.id AND m.tenant_id = u.tenant_id
   )) AS users_with_canonical_membership;
```

---

## 6. Riesgos de producción

### 6.1 Riesgos funcionales

| Riesgo | Severidad | Descripción | Mitigación |
|--------|-----------|-------------|------------|
| Usuarios bloqueados (403) | **Alta** | Pre-12.3 sin membership | Backfill antes/después inmediato de deploy 12.3 |
| 403 inesperado post-backfill | Media | Membership INACTIVE manual | Runbook; no incluir INACTIVE en backfill |
| Tenant incorrecto | Baja | Lookup legado acota por tenant del request | No observado escalada; monitor queries tipo C |
| Membership duplicada | Baja | UNIQUE en DB | Idempotencia en script |
| Registro parcial | Media | Identity sin membership | Transacción atómica (deuda 12.5+) |
| PENDING_VERIFICATION confundido con 403 membership | Media | Soporte interpreta mal | Documentar códigos: 403 identity status vs 403 membership |

### 6.2 Riesgos técnicos

| Riesgo | Severidad | Descripción | Mitigación |
|--------|-----------|-------------|------------|
| Race conditions registro vs backfill | Baja | UNIQUE protege | `ON CONFLICT DO NOTHING` |
| Duplicados lógicos (mismo email, distintos ids) | Baja | Modelo actual intencional | No unificar en backfill |
| Datos huérfanos | Media | membership sin user o user sin tenant | Queries §5.5 + FK fase posterior |
| Fallos parciales Flyway | Media | Mitad de filas | Transacción única; rollback automático |
| Lock en tablas grandes | Baja (volumen actual) | INSERT…SELECT bloquea | Ventana de mantenimiento si crece volumen |
| R2DBC sin transacción multi-repo | Media | Registro no atómico | `TransactionalOperator` en 12.5+ |

---

## 7. Deuda técnica priorizada

### ALTA

| # | Deuda | Origen |
|---|-------|--------|
| 1 | **Ausencia de backfill** en producción | Identidades históricas sin membership |
| 2 | **Doble fuente de verdad** (`iam_user.tenant_id` vs membership) | 12.2 / 12.3 |
| 3 | **Registro sin transacción atómica** identity + membership | `RegisterIdentityUseCaseImpl` |
| 4 | **403 masivo** si deploy 12.3 sin migración de datos | Impacto operativo |

### MEDIA

| # | Deuda | Origen |
|---|-------|--------|
| 5 | Sin FK membership → user / tenant | V7 |
| 6 | Sin índice `(tenant_id)` en membership | `findByTenantId` |
| 7 | JWT sin claim `tenantId` | 11.x / 12.x |
| 8 | Sin Tenant Context en runtime | Roadmap |
| 9 | Modelo híbrido: N:M en membership pero lookup 1:1 en `iam_user` | Convergencia pendiente |
| 10 | Sin API de gestión membership (activate/deactivate operacional) | Fuera de alcance |

### BAJA

| # | Deuda | Origen |
|---|-------|--------|
| 11 | Membership huérfana sin job de limpieza | Integridad referencial |
| 12 | `created_at` de backfill vs histórico | Estética auditoría |
| 13 | Deprecación futura de `iam_user.tenant_id` | Refactor grande |
| 14 | Invitations / roles por tenant | Producto |

---

## 8. Decisiones

| ID | Decisión | Rationale |
|----|----------|-----------|
| D1 | Backfill **obligatorio** antes de considerar 12.3 estable en prod | Evita 403 en usuarios existentes |
| D2 | Regla de backfill: **ACTIVE**, par `(id, tenant_id)` de `iam_user` | Alineado con login 12.3 |
| D3 | Usar **`iam_user.created_at`** (no `now()`) salvo acuerdo explícito | Trazabilidad |
| D4 | Script **idempotente** (`NOT EXISTS` o `ON CONFLICT DO NOTHING`) | Re-deploy seguro |
| D5 | **No** introducir FK en el mismo paso que backfill | Validar huérfanos primero |
| D6 | **No** eliminar `tenant_id` de `iam_user` en 12.5 | Fuera de alcance acordado |
| D7 | Mantener orden login: credenciales → membership → JWT | Seguridad (12.3) |

---

## 9. Plan de ejecución — PASO 12.5 (siguiente paso, no implementar ahora)

| Fase | Acción | Entregable |
|------|--------|------------|
| **12.5.0** | Ejecutar queries §5.5 en staging/prod (solo lectura) | Informe conteos A–D |
| **12.5.1** | Flyway `V8__backfill_identity_tenant_membership.sql` idempotente | Migración |
| **12.5.2** | Verificación post-migración (A → 0 filas) | Checklist deploy |
| **12.5.3** | Smoke test login usuarios muestreados | QA |
| **12.5.4** | (Opcional) `V9` índice `tenant_id` + FK tras limpieza huérfanos | Integridad |
| **12.5.5** | `TransactionalOperator` en registro | Deuda ALTA #3 |
| **12.6+** | JWT `tenantId`, Tenant Context, deprecar lookup legado | Roadmap |

### Criterios de aceptación 12.5

- [ ] Query A retorna **0** filas.
- [ ] `./gradlew build` sigue en verde.
- [ ] Login IT + smoke manual usuario backfilled → **200** (identity ACTIVE).
- [ ] Registro nuevo sigue creando membership (sin regresión).
- [ ] Re-ejecución de V8 es no-op (idempotente).

---

## 10. Fuera de alcance (confirmado)

- Tenant Context
- JWT claim `tenantId`
- Eliminación de `iam_user.tenant_id`
- Membership API / invitations / roles
- Implementación Flyway V8
- Cambios de código

---

## 11. Referencias

| Documento | Relación |
|-----------|----------|
| `PASO-12.2-IDENTITY-TENANT-MEMBERSHIP-AUDIT.md` | Modelo N:M, sin FK |
| `PASO-12.3-MEMBERSHIP-INTEGRATION.md` | Flujos registro/login |
| `PASO-12.3-MEMBERSHIP-INTEGRATION-AUDIT.md` | Riesgo identity sin membership |
| `V2__create_iam_user_table.sql` | `iam_user` |
| `V7__create_identity_tenant_membership_table.sql` | Membership |

---

## 12. Conclusión

El modelo 12.3 es **funcionalmente correcto** para identidades nuevas, pero **incompleto en datos históricos** y **frágil en consistencia** por ausencia de FK, transacción atómica y dependencia dual del lookup legado. El backfill propuesto es **seguro, idempotente y alineado con el login actual**. El siguiente paso natural (**12.5**) es implementar la migración Flyway y las verificaciones, sin abrir aún Tenant Context ni JWT tenant.
