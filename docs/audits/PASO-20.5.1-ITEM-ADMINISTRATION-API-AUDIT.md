# PASO 20.5.1 — Item Administration API Audit

**Veredicto:** la API administrativa de Item debe **reutilizar el patrón Organization/Patient** (soft-entity). No hace falta un diseño distinto ni un ADR nuevo.

**Fecha:** 2026-07-12  
**Estado:** ✅ Auditoría cerrada — lista para implementación (PASO 20.6)  
**Tipo:** Solo diseño — sin código  
**Fuentes:** PASO-17.5.1 · PASO-16.3.1 · ADR-016 · ADR-013 · ADR-007 · PASO-20.5 · PASO-20.4 · PASO-20.2

---

## Quick path (20.6)

1. Base: `GET/POST /api/v1/inventory/items` (+ `/{id}`, `PUT`, `POST …/archive`, `POST …/activate`)
2. Misma paginación/status que Patient/Org (`status=ACTIVE` default)
3. Validar `primaryOrganizationId` opcional vía `OrganizationReferencePort`
4. Soft-unique `code` → conflicto **409**
5. Tenant solo desde JWT · cross-tenant → 404
6. `activate` → `item:update` · `archive` → `item:archive`

---

## Respuestas obligatorias

### 1 — ¿Seguir exactamente el patrón Organization / Patient?

**Sí.**

Item es soft-entity con `ACTIVE|ARCHIVED`, sin delete físico — igual que Patient/Organization (no Encounter/Appointment).

| Aspecto | Decisión |
|---------|----------|
| Verbos | list · get · create · update · archive · activate |
| Paths | `POST …/archive` · `POST …/activate` (no `DELETE`) |
| Permisos | `item:*` (20.5); activate → `item:update` |
| Paginación | `page` / `size` / `sort` / `status` |
| Respuestas | sin `tenantId` en JSON |
| Errores | cross-tenant / missing → **404**; conflicto dominio (code) → **409**; validación → **400** |

**No inventar** un estilo REST de WMS/POS, stock embebido, precios, BOM, ni un “ItemDetail” con cantidades. Consistencia del Core > originalidad. **Item remains intentionally small** (ADR-016 §3).

---

### 2 — ¿Qué operaciones necesita Item hoy?

| Operación | Endpoint | Permiso |
|-----------|----------|---------|
| Listar | `GET /api/v1/inventory/items` | `item:read` |
| Obtener | `GET /api/v1/inventory/items/{id}` | `item:read` |
| Registrar | `POST /api/v1/inventory/items` | `item:create` |
| Corregir registro | `PUT /api/v1/inventory/items/{id}` | `item:update` |
| Archivar | `POST …/{id}/archive` | `item:archive` |
| Reactivar | `POST …/{id}/activate` | `item:update` |

**No hoy:** delete · stock qty · adjust · transfer · price · BOM · lot · supplier · UoM catalog · Office embebido · Encounter/Patient links · import masivo SKU · merge de catálogo.

**Path base:** `/api/v1/inventory/items` (schema SQL `inventory` + BC Inventory — ADR-016).

---

### 3 — ¿Archive / Activate = misma semántica que Patient / Organization?

**Sí.**

| | ACTIVE | ARCHIVED |
|--|--------|----------|
| Significado | Catálogo operativo | Soft-retire; retención histórica |
| `archive` | ACTIVE → ARCHIVED | rechazo si ya archived |
| `activate` | ARCHIVED → ACTIVE | rechazo si ya active |
| Delete físico | No | No |

Diferencia vs Org: Item **no** tiene hijos estructurales en v1 (Stock es aggregate futuro). Archive Item **no** requiere guard de “stock ACTIVE” en 20.6 — Stock validará Item ACTIVE vía `ItemReferencePort` (20.8+).

Mutaciones de registro (`update`) solo en ACTIVE (ya en dominio 20.3).

---

### 4 — ¿GET by id devuelve ARCHIVED?

**Sí** — si pertenece al tenant JWT.

- Existe + mismo tenant (ACTIVE o ARCHIVED) → **200**
- Inexistente u otro tenant → **404** (anti-enumeración)

Misma regla que Patient/Organization. Necesario para auditoría y reactivación.

---

### 5 — ¿Listado filtra ACTIVE por defecto?

**Sí.**

| `status` | Comportamiento |
|----------|----------------|
| *(omitido)* / `ACTIVE` | Solo ACTIVE |
| `ARCHIVED` | Solo archivados |
| `ALL` | Ambos |

Default operativo: almacén/recepción ve el catálogo vivo. Archived bajo filtro explícito.

---

### 6 — ¿Qué búsquedas necesita el Core hoy?

**Mínimo útil, sin motor de catálogo e-commerce:**

| Filtro / sort | ¿v1? | Nota |
|---------------|------|------|
| `status` | ✅ | Igual Patient/Org |
| `q` (texto en `displayName`) | ✅ | Búsqueda básica de recepción/almacén |
| `code` (exact match) | ✅ | Lookup por SKU / material code |
| `primaryOrganizationId` | ✅ | Filtro opcional de agrupación custodial |
| price / stock / office filters | ❌ | Fuera de Item (ADR-016 §3) |
| full-text / fuzzy / facets | ❌ | Hipotético — no ahora |
| sort | ✅ | `displayName`, `code`, `status`, `createdAt`, `updatedAt` (default `createdAt,desc`) |

Unicidad soft de `code` ya está en DB (partial unique); la API traduce violación → **409**.

---

### 7 — ¿Aislamiento por tenant?

**Patrón IAM/Org/Patient sin cambios:**

```text
JWT → TenantContextAccessor → todos los use cases filtran por TenantId
Nunca aceptar tenantId en body/query
findByIdAndTenantId → vacío = 404
```

Sin OwnershipPolicy org-scoped. RBAC membership-scoped (ADR-007).  
`PrimaryOrganizationId` **no** implica RBAC por organización ni aislamiento de catálogo.

---

### 8 — ¿PrimaryOrganizationId + OrganizationReferencePort (ADR-013 · ADR-016)?

**En application (create / update mientras ACTIVE), no en el controller ni en el dominio cruzando repos.**

| Caso | Comportamiento |
|------|----------------|
| Campo ausente / null | OK — primary org opcional |
| Presente | `OrganizationReferencePort.existsActiveByIdAndTenant(id, tenantId)` |
| `false` | **404** (no usable en tenant — espejo Patient) |
| Mutación | **Nunca** vía OrganizationRepository / SQL a `org.*` desde Inventory |

**Bridging de VOs:** Item domain usa `PrimaryOrganizationId` local. Port usa `OrganizationId` de organization-contract. Application convierte por UUID.

Consumer Gradle: `inventory-application` → `implementation(organization-contract)` únicamente.  
Adapter ya existe (17.2 · 20.2). Wiring en `codecore-api` / module configuration.

Archive / activate: **no** revalidan ReferencePorts (transición local; lectura histórica debe funcionar aunque Organization ya no esté ACTIVE).

**Office / Patient / Encounter ports:** **prohibidos** en Item v1 (ADR-016 · PASO-20.2).

---

### 9 — ¿PUT qué muta?

Solo campos de catálogo mientras `ACTIVE`:

| Campo | Mutable en PUT |
|-------|----------------|
| `displayName` | ✅ |
| `code` (nullable — `null` limpia) | ✅ |
| `primaryOrganizationId` (nullable — `null` limpia) | ✅ |
| `status` | ❌ — solo via archive/activate |
| `tenantId` | ❌ — nunca |
| quantity / price / officeId | ❌ — no existen en el aggregate |

PUT = replace de campos de registro (mismo espíritu que Patient PUT). Re-validar OrganizationPort si org presente. Re-check soft-unique code (excluyendo el propio `itemId`) → **409** si conflicto.

---

### 10 — ¿Create body / Response shape?

**Create body (mínimo):**

| Campo | Required | Nota |
|-------|----------|------|
| `displayName` | ✅ | non-blank ≤200 |
| `code` | ❌ | soft-unique por tenant si presente |
| `primaryOrganizationId` | ❌ | UUID; ACTIVE org |

**Update body:** mismos campos mutables (no `tenantId`; no `status` vía PUT).

**Response:** `id`, `displayName`, `code?`, `primaryOrganizationId?`, `status`, `createdAt`, `updatedAt` — **sin tenantId** · **sin** qty/price/office/stock.

Create siempre entra `ACTIVE`.

---

### 11 — ¿Soft-unique `code` en API?

| Caso | HTTP |
|------|------|
| `code` null / omitido | OK (varios Items sin code) |
| `code` presente y libre en tenant | OK |
| `code` duplicado en mismo tenant | **409** |
| Mismo `code` en otro tenant | OK (aislado) |

Preferir comprobar vía `ItemQueryPort.existsByTenantIdAndCode` / `…ExcludingId` **antes** del save (mensaje claro) y/o mapear `DuplicateKeyException` → 409. Ambos aceptables; al menos uno obligatorio en 20.6.

---

### 12 — ¿Hace falta un ADR nuevo?

**No.**

| Tema | ¿Irreversible nuevo? |
|------|----------------------|
| Path `/api/v1/inventory/items` | Convención HTTP — documentar aquí |
| Soft archive / activate | Ya ADR-016 |
| ReferencePort validation | Ya ADR-013 / 20.2 |
| Soft-unique code → 409 | Consecuencia de ADR-016 + V24 |
| Filtros de listado | Decisión de API, no de modelo |
| DTOs / paginación | Patrón 17.5.1 |

Solo un ADR nuevo si se reabriera boundary de Item (qty/office en Item), delete físico, o org-scoped RBAC — **ninguno de esos**.

---

### 13 — ¿Mejora que fortalezca el Core sin complejidad?

| Mejora | ¿Adoptar en 20.6? |
|--------|-------------------|
| Reutilizar shape `Paged*Response` / `PageQuery` | ✅ |
| Grupo OpenAPI `inventory-administration` | ✅ en 20.8 (o mínimo en 20.6) |
| `x-permission` en OpenAPI | ✅ |
| Endpoint stock / adjust / price | ❌ viola ADR-016 |
| DTO “ItemDetail” con balances | ❌ |
| Cascade archive a Stock | ❌ Item no posee Stock aún |
| Import CSV SKU | ❌ pack / fase posterior |

Una sola mejora concreta: documentar en OpenAPI (`x-permission`) igual que Patient — cero invento.

---

## Contrato HTTP propuesto (20.6)

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/inventory/items` | `item:read` |
| GET | `/api/v1/inventory/items/{id}` | `item:read` |
| POST | `/api/v1/inventory/items` | `item:create` |
| PUT | `/api/v1/inventory/items/{id}` | `item:update` |
| POST | `/api/v1/inventory/items/{id}/archive` | `item:archive` |
| POST | `/api/v1/inventory/items/{id}/activate` | `item:update` |

**List query:** `page=0&size=20&sort=createdAt,desc&status=ACTIVE` (+ `q`, `code`, `primaryOrganizationId` opcionales)

**Errores:**

| Situación | Status |
|-----------|--------|
| Validación body / blank name | **400** |
| Missing / cross-tenant / org no ACTIVE | **404** |
| Domain state (archive twice, update archived) | **409** |
| Soft-unique code conflict | **409** |
| Sin permiso | **403** |
| Sin JWT | **401** |

---

## Checklist

- [x] Patrón Patient/Organization soft-entity reutilizado  
- [x] Sin sobreingeniería de stock/WMS  
- [x] ADR-016 / 013 / 007 respetados  
- [x] Sin ADR nuevo  
- [x] Soft-unique code documentado  
- [x] Core Platform fortalecido por **consistencia**, no por novedad  

---

## Fuera de alcance 20.5.1 / 20.6

Verification E2E completa (20.7) · OpenAPI closeout (20.8) · `ItemReferencePort` · Stock · precios · BOM

---

## Siguiente paso

**PASO 20.6 — Item Administration API** — implementar use cases + controller + ITs según este contrato.

---

## Referencias

- [PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md](PASO-17.5.1-PATIENT-ADMINISTRATION-API-AUDIT.md)  
- [PASO-20.5-ITEM-AUTHORIZATION-CONTRACT.md](PASO-20.5-ITEM-AUTHORIZATION-CONTRACT.md)  
- [PASO-20.4-ITEM-PERSISTENCE.md](PASO-20.4-ITEM-PERSISTENCE.md) · [PASO-20.2](PASO-20.2-INVENTORY-REFERENCE-READINESS.md)  
- [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
