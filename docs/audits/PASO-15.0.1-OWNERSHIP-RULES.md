# PASO 15.0.1 — Ownership Rules Audit

**Fecha:** 2026-06-17  
**Alcance:** Reglas de propiedad y jerarquía para operaciones administrativas que mutan usuarios (y base para 15.2–15.3).  
**Restricción:** Solo decisión arquitectónica; implementación en 15.1+.

**Fuentes:** ADR-006, ADR-007, ADR-008, `PASO-15.1-15.3-ADMINISTRATION-DESIGN-AUDIT.md`, `SystemRoleTemplate`, `IamPermissionCatalog`.

---

## 1. Principios

| Principio | Aplicación |
|-----------|------------|
| **Deny by default** | Sin permiso RBAC o sin nivel de ownership suficiente → rechazo |
| **RBAC primero** | `@RequiresPermission` es la primera barrera (FASE 14) |
| **Ownership segundo** | Aun con `user:update`, un MANAGER no puede mutar un ADMIN |
| **Tenant isolation** | Ownership se evalúa solo entre memberships del **mismo tenant** (JWT `tenantId`) |
| **Jerarquía explícita** | Basada en roles system sembrados: OWNER > ADMIN > MANAGER > USER > READ_ONLY |

La ownership **no sustituye** RBAC; la **refina** para evitar escalada de privilegios dentro del tenant.

---

## 2. Nivel de privilegio por rol system

| Rol | Nivel (mayor = más privilegio) |
|-----|-------------------------------|
| OWNER | 5 |
| ADMIN | 4 |
| MANAGER | 3 |
| USER | 2 |
| READ_ONLY | 1 |

**Usuario sin roles asignados** en el tenant: se trata como nivel **USER (2)** para ownership (mínimo operativo).

**Usuario con varios roles:** se usa el **máximo** nivel entre sus roles en ese tenant.

**Roles custom (15.3+):** nivel **USER (2)** hasta que exista política específica (fuera de 15.0.1).

---

## 3. Matriz de modificación de usuarios

«Modificar» = `POST` (crear no aplica a targets existentes), `PUT`, `DELETE` sobre un usuario **ya perteneciente al tenant** vía membership.

Lectura (`GET`) se rige solo por `user:read` (sin ownership adicional en 15.1).

| Actor \ Puede modificar target | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|--------------------------------|-------|-------|---------|------|-----------|
| **OWNER** | Sí | Sí | Sí | Sí | Sí |
| **ADMIN** | **No** | **No** | Sí | Sí | Sí |
| **MANAGER** | No | No | **No** | Sí | Sí |
| **USER** | No | No | No | No | No |
| **READ_ONLY** | No | No | No | No | No |

### Respuestas obligatorias por actor

#### OWNER
Puede modificar: **OWNER, ADMIN, MANAGER, USER, READ_ONLY** — control total del tenant.

#### ADMIN
Puede modificar: **MANAGER, USER, READ_ONLY**.  
No puede modificar: **OWNER, ADMIN** (ni pares ni superiores).

#### MANAGER
Puede modificar: **USER, READ_ONLY**.  
No puede modificar: **OWNER, ADMIN, MANAGER**.

#### USER
No puede modificar **otros usuarios** (sin `user:update` / `user:delete` en catálogo).

#### READ_ONLY
No puede modificar **ningún recurso** administrativo (sin permisos de escritura IAM).

---

## 4. Justificación

### SaaS multi-tenant
- El **OWNER** es el custodio del contrato; solo él (o quien tenga su nivel) puede tocar cuentas de máximo privilegio.
- **ADMIN** opera el día a día pero **no** puede degradar o deshabilitar al OWNER ni a otros ADMIN (evita toma de cuenta del tenant).
- **MANAGER** gestiona operadores (USER / READ_ONLY) sin tocar la capa administrativa.

### RBAC (ADR-007)
- Permisos ya limitan quién llega al endpoint (`user:update` → OWNER, ADMIN, MANAGER).
- Ownership evita que un MANAGER con `user:update` deshabilite un ADMIN.

### Deny by default
- Si el actor no puede clasificarse o el target tiene nivel superior o igual (según reglas): **403 Forbidden** (`OwnershipDeniedException`).

### Tenant isolation
- Niveles se calculan con `membership_role` + `role` **del tenant del JWT**, nunca de otro tenant.

---

## 5. Regla algorítmica (implementación 15.1)

```text
actorLevel    = max privilege of caller's roles in tenant
targetLevel   = max privilege of target's roles in tenant (default USER if none)

OWNER actor     → allow
READ_ONLY/USER actor → deny
ADMIN actor     → allow iff targetLevel <= MANAGER (3)
MANAGER actor   → allow iff targetLevel <= USER (2)
```

HTTP: **403** si falla ownership (tras pasar `@RequiresPermission`).

---

## 6. Alcance por fase

| Fase | Ownership |
|------|-----------|
| **15.1** | `PUT` / `DELETE` `/api/v1/iam/users/{id}` |
| **15.2** | Membership mutations (misma jerarquía sobre membership del target) |
| **15.3** | Role mutations (system roles inmutables; custom según `role:*` + ownership) |

---

## 7. Estado

| Ítem | Estado |
|------|--------|
| Matriz OWNER / ADMIN / MANAGER / USER / READ_ONLY | ✅ Definida |
| Justificación multi-tenant + RBAC | ✅ |
| Listo para implementar en 15.1 | ✅ |

**Siguiente paso:** PASO 15.1 — User Administration con `OwnershipPolicy` en mutaciones.
