# PASO 20.5 — Item Authorization Contract Audit

**Fecha:** 2026-07-12  
**Estado:** ✅ Auditoría cerrada — diseño aprobado e implementado  
**Tipo:** Contrato de autorización Inventory (Core Platform)  
**Fuentes:** ADR-007 · ADR-016 · ADR-013 · PASO-17.5 · PASO-19.5 · PASO-20.4

---

## Veredicto

Sembrar **exactamente cuatro** permisos transversales:

| Código | Dominio |
|--------|---------|
| `item:create` | Alta de identidad inventariable |
| `item:read` | Consultar / listar |
| `item:update` | Corregir registro (nombre, code, org) + **activate** |
| `item:archive` | Soft-retire |

**No** sembrar: `delete`, `activate` (dedicado), `restore`, `stock`, `adjust`, `bom`, `price`, verbos verticales (`dental-supply`, `sku-import`, …).

### Decisión `activate`

`activate` → **`item:update`** (espejo Patient `activate` → `patient:update`).

`archive` es permiso dedicado (como `patient:archive`) porque es transición de retiro distinta y frecuente en operación de catálogo.

---

## Matriz RBAC

| Permiso | OWNER | ADMIN | MANAGER | USER | READ_ONLY |
|---------|:-----:|:-----:|:-------:|:----:|:---------:|
| create / update / archive | ✓ | ✓ | ✓ | — | — |
| read | ✓ | ✓ | ✓ | ✓ | ✓ |

**Conteos platform tras 20.5:** ALL **44** · OWNER 44 · ADMIN 41 · MANAGER 28 · USER 8 · READ_ONLY 10.

---

## Relación con otros BCs

| Pregunta | Respuesta |
|----------|-----------|
| ¿Item sustituye `organization:read`? | **No.** Grants independientes. |
| ¿Crear Item con primary org requiere Org? | Típicamente `organization:read` **más** `item:create` (validación port). |
| ¿`item:*` implica stock/ajustes? | **No.** Stock tendrá permisos propios. |
| ¿Permisos clínicos en Item? | **No.** Consumo clínico futuro referencia `ItemId` outbound. |

---

## Checklist Core Platform

- [x] Sirve a cualquier producto sobre CodeCore  
- [x] Sin permisos Dental / Vet / Retail / Hospital  
- [x] Diseñado para muchos años  
- [x] Refuerza Core Platform (Item intentionally small)  
