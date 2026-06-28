# PASO 16.5 — Office Domain & Persistence

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** PASO-16.1 · ADR-010

---

## Entregables

| Área | Detalle |
|------|---------|
| Aggregate | `Office` — `OfficeId`, `OfficeCode`, `OfficeName`, `OfficeStatus` |
| Invariantes | `tenantId` denormalizado; `organizationId` obligatorio; `code` inmutable |
| Flyway | **V16** — tabla `org.office`, `UNIQUE (organization_id, code)` |
| Persistence | `OfficeEntity`, `OfficeMapper`, `SpringDataOfficeRepository`, `R2dbcOfficeRepository` |
| Ports | `OfficeRepository`, `OfficeQueryPort` |

## Comportamiento dominio

- Factory `Office.create` → `ACTIVE`
- `rename`, `archive`, `activate` (simétrico a Organization)
- Sin delete físico

## Tests

- `OfficeTest` — 3 escenarios dominio

## Notas

- Sin FK física a `org.organization` (decoupling ADR-010).
- Validación org pertenece al tenant en capa aplicación (16.6).
