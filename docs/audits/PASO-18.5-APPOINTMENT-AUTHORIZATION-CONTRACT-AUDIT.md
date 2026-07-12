# PASO 18.5 — Appointment Authorization Contract Audit

**Fecha:** 2026-07-11  
**Estado:** ✅ Auditoría cerrada — diseño aprobado e implementado  
**Tipo:** Contrato de autorización Scheduling (Core Platform)  
**Fuentes:** ADR-007 · ADR-014 · ADR-013 · PASO-17.5 · PASO-18.4

---

## Veredicto

Sembrar **exactamente cuatro** permisos transversales:

| Código | Dominio |
|--------|---------|
| `appointment:create` | Crear compromiso planificado |
| `appointment:read` | Consultar / listar |
| `appointment:update` | Reprogramar, reasignar refs, **complete** |
| `appointment:cancel` | Anular compromiso (terminal) |

**No** sembrar: `delete`, `complete` (dedicado), `restore`, `slot`, `availability`, verbos verticales.

### Decisión `complete`

`complete` → **`appointment:update`** (espejo Patient `activate` → `patient:update` / Org activate → `organization:update`).

`cancel` es permiso dedicado (como `patient:archive`) porque es transición terminal distinta y frecuente en operación diaria.

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / cancel | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform tras 18.5:** ALL **36** · OWNER 36 · ADMIN 33 · MANAGER 20 · USER 6 · READ_ONLY 8.

---

## Relación con Patient

| Pregunta | Respuesta |
|----------|-----------|
| ¿Appointment sustituye `patient:read`? | **No.** Grants independientes. |
| ¿Agendar requiere Patient? | Típicamente `patient:read` **más** `appointment:create`. |
| ¿Crear cita muta Patient? | **No.** Solo referencia `PatientId`. |

---

## Checklist Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform  
