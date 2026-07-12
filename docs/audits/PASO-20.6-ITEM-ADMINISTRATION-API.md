# PASO 20.6 — Item Administration API

**Item** queda expuesto como ciudadano nativo del Core: misma forma de administración soft-entity que Patient / Organization, sin lógica de stock, precios ni WMS.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Application + HTTP + seguridad + ITs  
**Dependencias:** [PASO-20.5.1](PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md) · [PASO-20.5](PASO-20.5-ITEM-AUTHORIZATION-CONTRACT.md) · [PASO-20.4](PASO-20.4-ITEM-PERSISTENCE.md) · [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Implementar la Item Administration API **exactamente** según el contrato aprobado en 20.5.1, reutilizando el patrón Patient / Organization soft-entity. Sin rediseño, sin ADR nuevo, sin capacidades de inventario posteriores.

---

## Entregables

| Área | Detalle |
|------|---------|
| Use case | `ItemAdministrationUseCaseImpl` — list, get, create, update, archive, activate |
| HTTP | `ItemAdminController` — `/api/v1/inventory/items` |
| DTOs | `CreateItemRequest` · `UpdateItemRequest` · `ItemResponse` · `PagedItemResponse` |
| Query | `R2dbcItemAdminQueryRepository` — paginación + filtros 20.5.1 (`q`, `code`, `primaryOrganizationId`) |
| Tenant | `IamTenantContextAccessor` → JWT `tenantId` (nunca HTTP) |
| Primary org | `OrganizationReferencePort.existsActiveByIdAndTenant` únicamente |
| RBAC | `@RequiresPermission("item:*")` — activate usa `item:update` |
| Wiring | `InventoryAdministrationConfiguration` · `InventoryOpenApiConfiguration` · `InventoryModuleConfiguration` |
| OpenAPI | grupo `inventory-administration` |
| Excepciones | `ItemHttpExceptionHandler` — 404 cross-tenant / primary org; 409 duplicate key; 400 invalid state/domain |

---

## Rutas

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/inventory/items` | `item:read` |
| GET | `/api/v1/inventory/items/{id}` | `item:read` |
| POST | `/api/v1/inventory/items` | `item:create` |
| PUT | `/api/v1/inventory/items/{id}` | `item:update` |
| POST | `/api/v1/inventory/items/{id}/archive` | `item:archive` |
| POST | `/api/v1/inventory/items/{id}/activate` | `item:update` |

**List default:** `status=ACTIVE` · `page=0` · `size=20` · `sort=createdAt,desc`  
**Filtros opcionales:** `q` (displayName ILIKE), `code` (exact), `primaryOrganizationId`  
**Sort:** `displayName` · `code` · `status` · `createdAt` · `updatedAt`  
**Response:** displayName + code? + primaryOrganizationId? + status + timestamps — **sin `tenantId`**

---

## Decisiones (sin novedad arquitectónica)

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Patrón | Espejo Patient soft-entity | Consistencia del Core |
| Path | `/api/v1/inventory/items` | BC Inventory (ADR-016) |
| Primary org | ReferencePort only | ADR-013 — sin tablas `org.*` |
| Archive | Soft, sin guard de stock | Stock aún no existe |
| PUT | Full replace; `code`/`primaryOrganizationId` null limpia | Contrato 20.5.1 |
| Soft-unique code | `DuplicateKeyException` → 409 | Partial unique V24 |
| Permisos | Catálogo 20.5 (sin nuevos) | ADR-007 seeds V25 |

---

## Tests

| Suite | Cobertura |
|-------|-----------|
| `ItemAdministrationUseCaseTest` | create sin/con primary org; rechazo org inactiva |
| `ItemAdminControllerIT` | create/list · archive/activate · RBAC USER 403 · cross-tenant 404 · primary org ReferencePort · update + list ARCHIVED + duplicate code 409 |
| Persistencia previa | `R2dbcItemRepositoryIT` (20.4) sigue verde |

Requiere Docker (Testcontainers PostgreSQL 16).

**Resultado 20.6:** unit 3/3 · IT 6/6 — **BUILD SUCCESSFUL**.

---

## Explicitamente fuera de alcance

ItemReferencePort · Stock · qty · adjust · transfer · price · BOM · lot · Office · Encounter/Patient · Merge · Export · DELETE · fuzzy search · eventos · nuevos permisos · ADR nuevo

---

## Checklist DoD

- [x] API extremo a extremo  
- [x] Unit + WebFlux ITs verdes  
- [x] Documentación PASO-20.6  
- [x] ROADMAP actualizado  
- [x] Sin decisión arquitectónica nueva  
- [x] Item sigue *intentionally small* (ADR-016)  
- [x] Core Platform fortalecido por consistencia, no por novedad vertical  

---

## Siguiente paso

**PASO 20.7 — Item Verification** — VerificationIT E2E + Core validation.

---

## Referencias

- [PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md](PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md)  
- [PASO-17.6-PATIENT-ADMINISTRATION-API.md](PASO-17.6-PATIENT-ADMINISTRATION-API.md)  
- [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
