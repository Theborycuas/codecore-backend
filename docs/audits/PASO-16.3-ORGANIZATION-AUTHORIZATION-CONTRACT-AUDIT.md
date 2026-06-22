# PASO 16.3 — Organization Authorization Contract Audit

**Fecha:** 2026-06-22  
**Estado:** ✅ Auditoría cerrada — diseño aprobado para implementación  
**Fuentes:** ADR-007, ADR-010, PASO-14.8, PASO-16.0.1

---

## Objetivo

Responder las 10 preguntas obligatorias antes de sembrar permisos Organization Management en el catálogo global IAM (ADR-007).

---

## Respuestas obligatorias

### 1 — ¿Qué permisos necesita Organization?

| Permiso | Propósito |
|---------|-----------|
| `organization:create` | Alta de unidad estructural en tenant |
| `organization:read` | Listar / obtener organizations |
| `organization:update` | Renombrar / metadata operativa |
| `organization:archive` | Baja lógica (sin delete físico — alineado aggregate 16.1) |

**No** `organization:delete` — el dominio usa `archive()`.

### 2 — ¿Qué permisos necesitará Office?

| Permiso | Propósito |
|---------|-----------|
| `office:create` | Alta de sede/consultorio bajo organization |
| `office:read` | Listar / obtener offices |
| `office:update` | Renombrar / metadata |
| `office:archive` | Baja lógica |

### 3 — ¿Qué permisos necesitará StaffAssignment?

| Permiso | Propósito |
|---------|-----------|
| `staff-assignment:create` | Vincular `membershipId` a org/office scope |
| `staff-assignment:read` | Consultar asignaciones |
| `staff-assignment:update` | Cambiar scope org/office |
| `staff-assignment:delete` | Remover asignación operativa |

StaffAssignment referencia **Membership** (IAM); no duplica Identity.

### 4 — ¿Qué permisos deben ser exclusivos de OWNER?

| Permiso | Notas |
|---------|-------|
| `tenant:update` | **Governance SaaS** — ya exclusivo OWNER (FASE 14.8) |
| `permission:read` | Catálogo global — solo OWNER |

Permisos org **no** son exclusivos de OWNER: ADMIN comparte administración estructural. `organization:archive` es ADMIN+ (no MANAGER).

### 5 — ¿Qué permisos debe tener ADMIN?

Todos los IAM de administración **excepto** `tenant:update` y `permission:read` (FASE 14.8) **más** los 12 permisos Organization Management.

Total ADMIN: **25 grants** (13 IAM + 12 org).

### 6 — ¿Qué puede hacer MANAGER?

**IAM:** `membership:read`, `user:read`, `user:update` (sin cambios 14.8).

**Organization Management:**

- `organization:read` — navegar estructura
- `office:*` — administración operativa de sedes
- `staff-assignment:*` — administración de scope operativo

**No puede:** crear/archivar/renombrar organizations (`organization:create|update|archive`).

### 7 — ¿Qué puede hacer USER?

**IAM:** `user:read`.

**Organization Management:** `organization:read`, `office:read`, `staff-assignment:read` — navegación sin mutaciones.

### 8 — ¿Qué puede hacer READ_ONLY?

**IAM:** `tenant:read`, `membership:read`, `role:read`.

**Organization Management:** los 3 permisos `*:read` de estructura.

Puede **navegar toda la jerarquía** en lectura; ninguna escritura.

### 9 — ¿Permisos reservados para fases futuras?

| Prefijo | Fase | No sembrar en 16.3 |
|---------|------|---------------------|
| `invitation:*` | 17 | ✅ Reservado |
| `module:*` / framework hooks | 18 | ✅ Reservado |
| `patient:*`, `appointment:*`, `medical-record:*` | 19 | ✅ Reservado |
| `billing:*`, `subscription:*`, `seat:*` | 20 | ✅ Reservado |

El contrato 16.3 cubre **solo** estructura organizacional — no clínica ni facturación.

### 10 — ¿Relación con ADR-007?

| Regla ADR-007 | Aplicación 16.3 |
|---------------|-----------------|
| Permisos globales en `iam.permission` | ✅ V15 inserta 12 grants `system_permission=true` |
| Roles tenant-scoped | ✅ Sin cambio — `SystemRoleTemplate` |
| RBAC membership-scoped | ✅ Sin organization-scoped roles |
| Convención `resource:action` | ✅ Extendida con hyphen en resource (`staff-assignment`) |
| Evaluación vía `@RequiresPermission` | ✅ Endpoints 16.4+ consumirán catálogo |

**Decisión documentada:** `PermissionCode` acepta `-` en resource/action para recursos compuestos (`staff-assignment`). No altera el modelo RBAC — solo validación sintáctica.

---

## Evaluación: permisos adicionales StaffAssignment

| Permiso propuesto | Veredicto | Justificación |
|-------------------|-----------|---------------|
| `staff-assignment:assign-role` | ❌ **Rechazado** | Asignación de roles RBAC = IAM (`membership:update`). Evita duplicar autorización. |
| `staff-assignment:transfer` | ❌ **Rechazado** | Transferencia de scope = `staff-assignment:update`. Permiso separado fragmentaría el catálogo sin beneficio de seguridad. |

---

## Veredicto

Contrato de **12 permisos** Organization Management aprobado. Matriz RBAC definida en [PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md](PASO-16.3-ORGANIZATION-AUTHORIZATION-CONTRACT.md).

Sin contradicción con ADR-007 tras extensión de formato `PermissionCode`.
