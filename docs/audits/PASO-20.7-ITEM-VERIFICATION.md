# PASO 20.7 — Item Verification & Core Validation

**Veredicto:** Item es una pieza **estable** del Core Platform. Inventory (catalog identity v1) está listo para closeout y consumo futuro **sin reabrir** ADR-016.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Verificación + consistencia + cierre de slice Item (sin nuevas capacidades)  
**Dependencias:** [PASO-20.6](PASO-20.6-ITEM-ADMINISTRATION-API.md) · [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. Suite E2E: `ItemVerificationIT` (8 checks) — verde  
2. Dominio / persistencia / permisos / API auditados contra ADR-016/013  
3. Siguiente: **PASO 20.8 — Inventory Closeout (Item)**

```bash
./gradlew :modules:inventory-management:inventory-infrastructure:test \
  --tests "com.codecore.inventory.interfaces.http.admin.ItemVerificationIT"
```

---

## Objetivo

Certificar que el primer aggregate de Inventory de CodeCore cumple el contrato acumulado (PASO 20.0 → 20.6) y fortalece el **Core reutilizable**, no un WMS/POS vertical ni un Stock God Aggregate.

**No** se implementaron endpoints, campos, permisos ni ADRs nuevos.  
**Sí** se consolidó evidencia ejecutable + auditoría documental.

---

## Suite E2E

| # | Verificación | Resultado |
|---|--------------|-----------|
| 1 | Journey: create → list → filter org/code/q → update → archive → activate (+ status filters) | ✅ |
| 2 | RBAC — `READ_ONLY` no crea | 403 ✅ |
| 3 | Cross-tenant GET | 404 ✅ |
| 4 | PrimaryOrganization inexistente (`OrganizationReferencePort`) | 404 ✅ |
| 5 | MANAGER archiva Item (matriz 20.5) | 200 ✅ |
| 6 | Código duplicado (mismo tenant) | 409 ✅ |
| 7 | OpenAPI grupo `inventory-administration` | ✅ |
| 8 | Sin JWT | 401 ✅ |

Patrón espejo: [PASO-19.7-ENCOUNTER-VERIFICATION.md](PASO-19.7-ENCOUNTER-VERIFICATION.md) · [PASO-17.7-PATIENT-VERIFICATION.md](PASO-17.7-PATIENT-VERIFICATION.md).

Config: `ItemAdministrationVerificationTestConfiguration` (stack Item + OpenAPI + IAM OpenAPI).

---

## Checklist — Aggregate “intentionally small”

| Responsabilidad | ¿En Item? | Evidencia |
|-----------------|-----------|-----------|
| Identidad inventariable (catálogo stockable) | ✅ Sí (única) | ADR-016 · `Item` aggregate |
| `displayName` + `code?` + lifecycle ACTIVE/ARCHIVED | ✅ Sí | Domain VOs |
| `PrimaryOrganizationId?` (custodia, no stock) | ✅ Sí | Solo UUID lógico |
| Stock / qty / location / lot | ❌ | Ausente |
| Price / UoM / BOM / Product catalog | ❌ | Ausente |
| Encounter / Patient / Appointment | ❌ | Ausente |
| OfficeId / Warehouse | ❌ | Ausente |
| Lógica de vertical (dental SKU, POS) | ❌ | Permisos `item:*` neutros |

**One-sentence rule:** *Item = la identidad inventariable de algo que puede stockearse, moverse o consumirse bajo un Tenant.*  
Sigue cabiendo en una frase → no es God Aggregate de inventario.

---

## Validación ADR-016 (congelado)

| Constraint | ¿Cumple? |
|------------|----------|
| Modelo pequeño; sin qty/precio/BOM/clínico | ✅ |
| `TenantId` requerido e inmutable | ✅ |
| Estados `ACTIVE` / `ARCHIVED` (soft-entity) | ✅ |
| Sin DELETE físico | ✅ |
| `PrimaryOrganizationId` opcional (custodial) | ✅ |
| `code` soft-unique por tenant (nullable) | ✅ |
| Activate → `item:update` (no permiso dedicado) | ✅ |
| Sin verbos de vertical en permisos | ✅ |
| HTTP `/api/v1/inventory/items` · schema `inventory` | ✅ |

**Observación:** ninguna decisión de este paso exige modificar el modelo. ADR-016 permanece intacto.

---

## Validación ADR-013 (Reference Contracts)

| Constraint | ¿Cumple? |
|------------|----------|
| Consume `organization-contract` | ✅ `inventory-application` |
| `OrganizationReferencePort` para primary org | ✅ use case |
| Nunca repos Org en main / nunca SQL `org.*` | ✅ solo `inventory.item` |
| Port `false` / empty → excepción consumidor → HTTP 404 | ✅ |
| Archive/activate sin revalidar port | ✅ (patrón Patient-like) |

Item es el **primer consumidor de Inventory** que usa Organization bajo carga HTTP E2E.

---

## Validación Development Policy (FASE 16+)

| Principio | ¿Cumple? |
|-----------|----------|
| Dominio antes que DB/controller | ✅ (20.0.1 → 20.3 → 20.4 → 20.6) |
| Consistencia de Administration API con Patient/Org | ✅ mismos patrones HTTP/tenant/RBAC |
| Sin sobreingeniería (WMS, Stock en Item, ES) | ✅ |
| Auditoría antes de HTTP | ✅ 20.0.1 / 20.5.1 |
| Framework de negocio unificado | ✅ ciudadano nativo del Core |

---

## Persistencia

`inventory.item` (V24):

| Columna / dimensión | ¿Presente? |
|---------------------|------------|
| Identity + displayName + code? + status + primary org? | ✅ |
| `tenant_id` (sin FK IAM) | ✅ |
| Partial unique `(tenant_id, code) WHERE code IS NOT NULL` | ✅ |
| Qty / price / UoM / lot / office | ❌ |

Schema = inventoriable catalog identity. Nada más.

---

## Autorización

| Permiso | Existe |
|---------|--------|
| `item:create` | ✅ |
| `item:read` | ✅ |
| `item:update` | ✅ |
| `item:archive` | ✅ |
| `item:activate` (dedicado) / verticales | ❌ |

Activate → `item:update` (contrato 20.5). Catalog + V25 alineados. Matrix: OWNER/ADMIN/MANAGER = 4; USER/READ_ONLY = read.

---

## API vs Patient (soft-entity)

| Aspecto | Patient | Item | Paridad |
|---------|---------|------|---------|
| Lifecycle POST | archive/activate | archive/activate | ✅ forma |
| Cross-tenant | 404 | 404 | ✅ |
| Sin JWT | 401 | 401 | ✅ |
| Sin permiso | 403 | 403 | ✅ |
| Conflict | 409 | 409 duplicate code | ✅ |
| Paginación `page/size` + filtros | ✅ | ✅ `q`/`code`/`primaryOrganizationId`/`status` | ✅ |
| `tenantId` en response | No | No | ✅ |
| ReferencePort | Org (primary) | Org (primary) | ✅ |
| List default status | ACTIVE | ACTIVE | ✅ |

---

## Reutilización multi-vertical

| Vertical | ¿Reutilizable sin cambiar Item? | Por qué |
|----------|----------------------------------|---------|
| Dental | ✅ | Catálogo genérico; consumibles fuera de qty |
| Veterinaria | ✅ | Mismo inventoriable identity |
| Hospital | ✅ | Stock/lots = aggregates posteriores |
| Laboratorio | ✅ | Reactivos = Item + Stock futuro |
| Retail / POS | ✅ | Price/SKU commerce fuera del Core Item |
| Vertical futuro | ✅ | Contrato pequeño + permisos neutros + ReferencePorts |

Item pertenece al **Core**, no a Dental ni a ningún WMS de producto.

---

## Conclusiones

1. **Inventory (Item v1) está verificado** de dominio a HTTP.  
2. **ADR-016 / 013 se mantienen** — sin presión de cambio de modelo.  
3. **Closeout 20.8** puede publicar `ItemReferencePort` + guía de consumo.  
4. CodeCore sale **más fuerte como plataforma**, no como inventario vertical.

---

## Riesgos encontrados

| Riesgo | Severidad | Mitigación / nota |
|--------|-----------|-------------------|
| Tentación de meter qty/precio “solo un poco” | Media futura | ADR-016 §3 — rechazar; Stock/Pricing aggregates |
| Ausencia aún de `ItemReferencePort` | Baja | Planificado en closeout 20.8 |
| Labels Org enriquecidos en response | Baja | Read-model futuro; no inflar DTO |

Ningún riesgo obliga a modificar el dominio ahora.

---

## Checklist DoD

- [x] Checklist arquitectónico completo  
- [x] Validación ADR-016 / ADR-013  
- [x] Validación Development Policy  
- [x] Suite `ItemVerificationIT` verde (8/8)  
- [x] Documentación PASO-20.7  
- [x] ROADMAP → 20.7 ✅ · siguiente 20.8  
- [x] Sin cambios de modelo / ADR / endpoints nuevos  

---

## Siguiente paso

**PASO 20.8 — Inventory Closeout (Item)** — `ItemReferencePort` + guía de consumo. Cierra FASE 20 (Item slice).

---

## Referencias

- [PASO-20.6-ITEM-ADMINISTRATION-API.md](PASO-20.6-ITEM-ADMINISTRATION-API.md)  
- [PASO-19.7-ENCOUNTER-VERIFICATION.md](PASO-19.7-ENCOUNTER-VERIFICATION.md)  
- [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
