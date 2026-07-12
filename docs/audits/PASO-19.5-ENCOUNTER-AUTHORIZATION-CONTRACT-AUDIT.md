# PASO 19.5 — Encounter Authorization Contract Audit

**Fecha:** 2026-07-11  
**Estado:** ✅ Auditoría cerrada — diseño aprobado e implementado  
**Tipo:** Contrato de autorización Clinical Records (Core Platform)  
**Fuentes:** ADR-007 · ADR-015 · ADR-013 · PASO-18.5 · PASO-19.4

---

## Veredicto

Sembrar **exactamente cuatro** permisos transversales:

| Código | Dominio |
|--------|---------|
| `encounter:create` | Abrir / registrar episodio ocurrido |
| `encounter:read` | Consultar / listar |
| `encounter:update` | Ajustar refs / tiempo mientras abierto; **complete** |
| `encounter:cancel` | Anular episodio (terminal) |

**No** sembrar: `delete`, `complete` (dedicado), `restore`, `note`, `soap`, `odontogram`, verbos verticales (`surgery`, `hygiene`, …).

### Decisión `complete`

`complete` → **`encounter:update`** (espejo Appointment `complete` → `appointment:update`).

`cancel` es permiso dedicado (como `appointment:cancel` / `patient:archive`) porque es transición terminal distinta y frecuente en operación diaria.

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / cancel | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform tras 19.5:** ALL **40** · OWNER 40 · ADMIN 37 · MANAGER 24 · USER 7 · READ_ONLY 9.

---

## Relación con Patient / Appointment

| Pregunta | Respuesta |
|----------|-----------|
| ¿Encounter sustituye `patient:read`? | **No.** Grants independientes. |
| ¿Abrir Encounter requiere Patient? | Típicamente `patient:read` **más** `encounter:create`. |
| ¿Abrir Encounter muta Appointment? | **No.** Solo referencia opcional `AppointmentId`. |
| ¿`appointment:update` implica `encounter:*`? | **No.** Scheduling y Records son grants independientes. |

---

## Checklist Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform (Encounter intentionally small)  
