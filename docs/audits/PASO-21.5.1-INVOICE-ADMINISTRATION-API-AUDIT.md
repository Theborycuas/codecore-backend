# PASO 21.5.1 — Invoice Administration API Audit

**Veredicto:** la API administrativa de Invoice **no** reutiliza el patrón soft-entity (Patient/Organization/Item — `ACTIVE|ARCHIVED`) ni el patrón occurred-episode (Encounter). Es un **lifecycle de tres estados con transiciones explícitas** (`DRAFT → ISSUED → VOIDED`), más cercano a Encounter/Appointment en forma (verbos de transición vía `POST`) pero con su propia matriz de permisos por transición. No hace falta un diseño distinto al ya congelado en ADR-017 ni un ADR nuevo.

**Fecha:** 2026-07-12  
**Estado:** ✅ Auditoría cerrada — implementado en [PASO-21.6](PASO-21.6-INVOICE-ADMINISTRATION-API.md)  
**Tipo:** Solo diseño — sin código (diseño) · implementación en 21.6  
**Fuentes:** PASO-20.5.1 · PASO-19.5.1 · ADR-017 · ADR-013 · ADR-007 · PASO-21.5 · PASO-21.4 · PASO-21.3

---

## Quick path (21.6)

1. Base: `GET/POST /api/v1/billing/invoices` (+ `/{id}`, `PUT`)
2. Transiciones dedicadas: `POST …/{id}/issue`, `POST …/{id}/void` (no `PATCH status`)
3. Validar `issuerOrganizationId` + bill-to (Patient XOR Organization) vía `OrganizationReferencePort` / `PatientReferencePort`; líneas vía `ItemReferencePort` / `EncounterReferencePort`
4. Soft-unique `invoiceNumber` → conflicto **409**; mismatch Encounter↔Patient bill-to → conflicto **409**
5. Tenant solo desde JWT · cross-tenant → 404
6. `issue` → `invoice:issue` · `void` → `invoice:void` · `update` solo si `DRAFT`

---

## Respuestas obligatorias

### 1 — ¿Seguir el patrón soft-entity (Patient/Org/Item) o el patrón lifecycle (Encounter/Appointment)?

**Ninguno exactamente — lifecycle propio de 3 estados, forma HTTP tipo Encounter.**

| Aspecto | Decisión |
|---------|----------|
| Verbos | list · get · create · update · **issue** · **void** |
| Paths | `POST …/issue` · `POST …/void` (no `archive`/`activate`, no `DELETE`) |
| Permisos | `invoice:*` (21.5); `issue`→`invoice:issue`, `void`→`invoice:void` |
| Paginación | `page` / `size` / `sort` / `status` |
| Respuestas | sin `tenantId` en JSON |
| Errores | cross-tenant / missing / referencia inválida → **404**; conflicto dominio → **409**; validación/estado inválido → **400** |

**No inventar** `PAID`, `PATCH /status`, delete físico, ni un endpoint de "recalculate total" — el total es siempre derivado (ADR-017 §7).

---

### 2 — ¿Qué operaciones necesita Invoice hoy?

| Operación | Endpoint | Permiso |
|-----------|----------|---------|
| Listar | `GET /api/v1/billing/invoices` | `invoice:read` |
| Obtener | `GET /api/v1/billing/invoices/{id}` | `invoice:read` |
| Crear | `POST /api/v1/billing/invoices` | `invoice:create` |
| Actualizar contenido | `PUT /api/v1/billing/invoices/{id}` | `invoice:update` |
| Emitir | `POST /api/v1/billing/invoices/{id}/issue` | `invoice:issue` |
| Anular | `POST /api/v1/billing/invoices/{id}/void` | `invoice:void` |

**No hoy:** pago · recordatorios de cobro · notas de crédito · impuestos · asientos contables · exportación fiscal · delete · un-void.

**Path base:** `/api/v1/billing/invoices` (schema SQL `billing` + BC Billing — ADR-017).

---

### 3 — ¿`update` solo en DRAFT, igual que Encounter en progreso?

**Sí.** `PUT` es full-replace de contenido (issuer, bill-to, invoiceNumber, líneas) y el dominio (`Invoice.updateContent`) rechaza la mutación fuera de `DRAFT` con `InvalidInvoiceStateException` → **400**. `issue` e `void` son las únicas transiciones de estado; no hay un-void ni retorno a DRAFT desde ISSUED.

---

### 4 — ¿GET by id devuelve ISSUED/VOIDED?

**Sí** — si pertenece al tenant JWT, en cualquier estado (auditoría/histórico de facturación).

- Existe + mismo tenant (cualquier status) → **200**
- Inexistente u otro tenant → **404** (anti-enumeración)

---

### 5 — ¿Listado filtra por status y cuál es el default?

**Default `DRAFT`** (cola de trabajo editable — facturas aún no emitidas son las que requieren acción).

| `status` | Comportamiento |
|----------|----------------|
| *(omitido)* | `DRAFT` |
| `DRAFT` \| `ISSUED` \| `VOIDED` | Solo ese estado |
| `ALL` | Los tres estados |

Filtros adicionales opcionales: `q` (texto libre — invoiceNumber/descripción), `issuerOrganizationId`, `billToPatientId`, `billToOrganizationId`.

---

### 6 — ¿Aislamiento por tenant?

**Patrón IAM/Org/Patient/Item sin cambios:**

```text
JWT → TenantContextAccessor → todos los use cases filtran por TenantId
Nunca aceptar tenantId en body/query
findByIdAndTenantId → vacío = 404
```

Sin OwnershipPolicy org-scoped. RBAC membership-scoped (ADR-007).

---

### 7 — ¿Validación multi-port (issuer, bill-to, líneas) dónde vive?

**En application (`InvoiceAdministrationUseCaseImpl`), no en el controller ni en el dominio cruzando repos** — igual que Encounter (19.6) con `EncounterReferencePort`.

| Referencia | Port | Regla |
|------------|------|-------|
| `issuerOrganizationId` | `OrganizationReferencePort.existsActiveByIdAndTenant` | `false` → 404 |
| Bill-to Organization | `OrganizationReferencePort.existsActiveByIdAndTenant` | `false` → 404; debe ≠ issuer (dominio) |
| Bill-to Patient | `PatientReferencePort.existsActiveByIdAndTenant` (o equivalente) | `false` → 404 |
| Línea `ItemId` | `ItemReferencePort` | `false` → 404 |
| Línea `EncounterId` | `EncounterReferencePort.findLinkableByIdAndTenant` | vacío → 404; si bill-to es Patient, `view.patientId` debe coincidir → **409** `InvoicePatientMismatchException` si no coincide |

Bridging de VOs: Invoice domain usa VOs locales (`OrganizationId`, `BillToPatientId`, `ItemId`, `EncounterId`); application convierte por UUID hacia los contratos de cada BC. Consumer Gradle: `billing-application` → `implementation(organizationContract, patientContract, encounterContract, inventoryContract)`.

`issue` / `void`: **no** revalidan ReferencePorts (transición local sobre contenido ya validado en `create`/`update`).

---

### 8 — ¿PUT qué muta?

Full-replace de contenido, solo mientras `DRAFT`:

| Campo | Mutable en PUT |
|-------|----------------|
| `issuerOrganizationId` | ✅ (re-validado) |
| `billToPatientId` / `billToOrganizationId` | ✅ (xor re-validado) |
| `invoiceNumber` | ✅ nullable — re-check soft-unique excluyendo el propio `invoiceId` → **409** si conflicto |
| `lines` (reemplazo completo) | ✅ (re-validadas) |
| `status` | ❌ — solo vía `issue`/`void` |
| `tenantId` | ❌ — nunca |

---

### 9 — ¿Create body / Response shape?

**Create body:**

| Campo | Required | Nota |
|-------|----------|------|
| `issuerOrganizationId` | ✅ | UUID; org ACTIVE |
| `billToPatientId` **xor** `billToOrganizationId` | ✅ (exactamente uno) | UUID |
| `invoiceNumber` | ❌ | soft-unique por tenant si presente |
| `currency` | ✅ | derivado también de líneas; validado ISO 4217 |
| `lines[]` | ✅ ≥1 | `description`, `amountMinor`, `itemId?`, `encounterId?` |

**Update body:** mismos campos (full-replace, no parcial).

**Response:** `id`, `issuerOrganizationId`, `billToPatientId?`, `billToOrganizationId?`, `invoiceNumber?`, `currency`, `totalAmountMinor` (derivado), `status`, `lines[]`, `createdAt`, `updatedAt` — **sin `tenantId`**.

Create siempre entra `DRAFT`.

---

### 10 — ¿Soft-unique `invoiceNumber` en API?

| Caso | HTTP |
|------|------|
| `invoiceNumber` null / omitido | OK (varias invoices sin número) |
| Presente y libre en tenant | OK |
| Duplicado en mismo tenant | **409** |
| Mismo número en otro tenant | OK (aislado) |

Vía `InvoiceQueryPort.existsByTenantIdAndInvoiceNumber(ExcludingId)` antes del save y/o mapeo de `DuplicateKeyException` → 409 (defensa en profundidad, ambos presentes en 21.6).

---

### 11 — ¿Hace falta un ADR nuevo?

**No.**

| Tema | ¿Irreversible nuevo? |
|------|----------------------|
| Path `/api/v1/billing/invoices` | Convención HTTP — documentar aquí |
| Lifecycle DRAFT/ISSUED/VOIDED | Ya ADR-017 |
| ReferencePort validation multi-BC | Ya ADR-013 / 19.6 / 20.6 |
| Soft-unique invoiceNumber → 409 | Consecuencia de ADR-017 + V26 |
| `InvoicePatientMismatchException` → 409 | Consecuencia de ADR-017 §10 (coherencia Encounter↔bill-to) |
| Filtros de listado / default `status=DRAFT` | Decisión de API, no de modelo |

Solo un ADR nuevo si se reabriera el boundary de Invoice (pagos, impuestos, contabilización) — **fuera de alcance FASE 21**.

---

### 12 — ¿Mejora que fortalezca el Core sin complejidad?

| Mejora | ¿Adoptar en 21.6? |
|--------|-------------------|
| Reutilizar shape `Paged*Response` / `PageQuery` | ✅ |
| Grupo OpenAPI `billing-administration` | ✅ |
| `x-permission` en OpenAPI (via `@RequiresPermission`) | ✅ |
| Endpoint de pago / nota de crédito | ❌ viola ADR-017 |
| `totalAmountMinor` persistido (en vez de derivado) | ❌ innecesario — siempre calculable desde líneas |
| Cascade void → algo externo | ❌ Invoice no conoce Payments/Accounting |

---

## Contrato HTTP propuesto (21.6)

| Método | Path | Permiso |
|--------|------|---------|
| GET | `/api/v1/billing/invoices` | `invoice:read` |
| GET | `/api/v1/billing/invoices/{id}` | `invoice:read` |
| POST | `/api/v1/billing/invoices` | `invoice:create` |
| PUT | `/api/v1/billing/invoices/{id}` | `invoice:update` |
| POST | `/api/v1/billing/invoices/{id}/issue` | `invoice:issue` |
| POST | `/api/v1/billing/invoices/{id}/void` | `invoice:void` |

**List query:** `page=0&size=20&sort=createdAt,desc&status=DRAFT` (+ `q`, `issuerOrganizationId`, `billToPatientId`, `billToOrganizationId` opcionales)

**Errores:**

| Situación | Status |
|-----------|--------|
| Validación body / VO inválido | **400** |
| Transición de estado inválida (`update`/`issue` fuera de DRAFT) | **400** |
| Missing / cross-tenant / referencia (org, patient, item, encounter) inválida | **404** |
| `invoiceNumber` duplicado | **409** |
| Encounter↔bill-to Patient mismatch | **409** |
| Sin permiso | **403** |
| Sin JWT | **401** |

---

## Checklist

- [x] Lifecycle de 3 estados propio, sin forzar patrón soft-entity ni Encounter 1:1
- [x] Sin sobreingeniería de pagos/impuestos/contabilidad
- [x] ADR-017 / 013 / 007 respetados
- [x] Sin ADR nuevo
- [x] Soft-unique invoiceNumber + mismatch Encounter↔Patient documentados
- [x] Core Platform fortalecido por consistencia, no por novedad

---

## Fuera de alcance 21.5.1 / 21.6

`InvoiceReferencePort` (21.8+, solo si algún consumidor lo necesita) · Payments · Accounting/Ledger · Tax

---

## Siguiente paso

**PASO 21.6 — Invoice Administration API** — ✅ [PASO-21.6](PASO-21.6-INVOICE-ADMINISTRATION-API.md). Siguiente: **21.7 Verification**.

---

## Referencias

- [PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md](PASO-20.5.1-ITEM-ADMINISTRATION-API-AUDIT.md)
- [PASO-21.5-INVOICE-AUTHORIZATION-CONTRACT.md](PASO-21.5-INVOICE-AUTHORIZATION-CONTRACT.md)
- [PASO-21.4-INVOICE-PERSISTENCE.md](PASO-21.4-INVOICE-PERSISTENCE.md) · [PASO-21.3](PASO-21.3-INVOICE-DOMAIN-FOUNDATION.md)
- [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)
- [ROADMAP.md](../architecture/ROADMAP.md)
