# PASO 17.2 — Bounded Context Reference Contracts

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Patrón arquitectónico reutilizable + primera implementación  
**Dependencias:** [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [PASO-17.1](PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md)

---

## Objetivo

Establecer el **patrón oficial de integración entre bounded contexts** de CodeCore (framework SaaS, décadas / decenas de BCs), no solo habilitar `Patient → Organization`.

Entregables:

1. **[ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)** — Accepted  
2. Familia de contratos de referencia (`OrganizationReferencePort`, `OfficeReferencePort` declarado)  
3. Adapter R2DBC + tests de contrato / IT  
4. Actualización ROADMAP + guías de consumo

---

## Por qué hace falta ADR-013

ADR-011 ya describe *qué* ports existirán para Organization. No define la **familia genérica** que usarán Patient, Appointment, Billing, Inventory, Scheduling, etc. durante toda la vida del producto.

Sin ADR-013, cada equipo inventaría:

- repositorios cross-BC  
- DTOs gordos  
- “REST interno” entre módulos  
- métodos `validate` que lanzan excepciones del provider  

Eso colapsa el modular monolith. **ADR-013 es la convención irreversible** de CodeCore para lecturas de referencia entre BCs.

| Documento | Rol |
|-----------|-----|
| ADR-011 | Integración *con Organization* (IDs, recipes, Gradle) |
| ADR-012 | Modelo Patient congelado |
| **ADR-013** | Patrón *Reference Contracts* para **cualquier** BC |

---

## Respuestas explícitas (diseño)

### ¿Qué responsabilidad tiene un ReferencePort?

Responder **preguntas de referencia** necesarias para proteger invariantes del **aggregate consumidor** en escritura:

- ¿Existe este ID en este tenant?  
- ¿Está en estado linkable (típicamente ACTIVE)?  
- (Raro) ¿Cuál es la vista mínima inmutable para un guard multi-campo?

Es la cara **anti-corrupción / query** del BC proveedor hacia otros BCs.

### ¿Qué NO debe hacer nunca?

| Prohibido | Motivo |
|-----------|--------|
| Exponer aggregates / entities | Fuga de interno; invita mutación |
| Exponer persistencia / SQL | Rompe hexagonal |
| Mutaciones (`save`, `archive`, …) | Escritura solo vía application API del provider |
| Sustituir APIs HTTP de administración | HTTP = humanos/admin; ports = integración in-process |
| Convertirse en “god query API” | Solo operaciones justificadas por invariantes |
| Inyectarse como repositorio del provider | Repos viven dentro del BC proveedor |
| Dependencias Gradle circulares | Consumer → `*-contract` únicamente |

### ¿Debe exponer aggregates completos?

**Nunca.**

### ¿Debe devolver DTOs?

**No** DTOs de aplicación/admin. Solo:

- `Mono<Boolean>` (default), o  
- **reference views** inmutables mínimas (IDs + status + label opcional) cuando boolean no basta.

### ¿Debe devolver únicamente la información mínima necesaria?

**Sí.** Eso es el criterio de diseño de cada método.

### ¿Qué operaciones son válidas?

| Operación | ¿Válida? | Uso |
|-----------|----------|-----|
| `exists…` | ✅ | Presente en tenant (cualquier status) — solo si un invariante lo exige |
| `existsActive…` | ✅ **default** | Linkable en escritura |
| `resolve…` / `find…Reference` | ✅ raro | Vista mínima |
| `validate…` que lanza del provider | ❌ | Consumer mapea `false` → su error de dominio |
| Mutaciones | ❌ | |

### ¿Debe permitir mutaciones?

**Nunca.**

### ¿Cómo evitamos que termine como API remota interna?

- Interfaces Java + adapters in-process (R2DBC).  
- **No** controllers, Feign, ni comandos de mensajería en el contrato.  
- Si un día hay deployables separados, el **mismo** contrato puede ganar adapter remoto — sin rediseñar call sites. HTTP no es el diseño por defecto.

### ¿Debe vivir en `organization-contract`?

**Sí** para recursos de Organization. Regla general: `{providing-bc}-contract` · adapter en `{providing-bc}-infrastructure`.

### ¿Cómo se repite el patrón?

| Port | Contract | Primer consumidor |
|------|----------|-------------------|
| `OrganizationReferencePort` | `organization-contract` | Patient `PrimaryOrganizationId` (**este paso**) |
| `OfficeReferencePort` | `organization-contract` | Appointment / Inventory (interface declarada; adapter cuando haya consumidor) |
| `StaffAssignmentReferencePort` | `organization-contract` | Appointment |
| `PatientReferencePort` | `patient-contract` | Appointment / MedicalRecord / Billing |
| `MembershipReferencePort` | futuro `iam-contract` | Org StaffAssignment (hoy: port transitorio en org-application) |
| `SubscriptionReferencePort` / `BillingReferencePort` | futuros contracts comerciales | Seats / facturas |

Plantilla: **mínimo de métodos**, tenant-scoped, read-only, invariante documentado.

---

## Implementación 17.2

### Contrato

```text
organization-contract
  └── …/reference/OrganizationReferencePort.java
        Mono<Boolean> existsActiveByIdAndTenant(OrganizationId, TenantId)
  └── …/reference/OfficeReferencePort.java
        Mono<Boolean> existsActiveInOrganization(OfficeId, OrganizationId, TenantId)
        // familia; sin adapter en 17.2 (Patient v1 no usa Office)
```

### Adapter

```text
organization-infrastructure
  └── …/adapters/R2dbcOrganizationReferenceAdapter
        COUNT(*) > 0 FROM org.organization WHERE id+tenant+ACTIVE
```

### Tests

| Test | Qué prueba |
|------|------------|
| `OrganizationReferencePortContractTest` | Superficie mínima del contrato (sin Docker) |
| `OrganizationReferencePortIT` | ACTIVE → true; wrong tenant / unknown → false; ARCHIVED → false (requiere Docker/Testcontainers) |

Gradle: `organization-contract` → `api(reactor-core)`; `organization-infrastructure` → `implementation(organization-contract)`.

---

## Coherencia con ADRs / políticas

| Artefacto | Veredicto |
|-----------|-----------|
| ADR-003 | ✅ checks siempre con `TenantId` |
| ADR-006 | ✅ Org no lee Identity; MembershipReference sigue transitorio |
| ADR-007 | ✅ ports no otorgan permisos |
| ADR-010 | ✅ aggregates Org intactos |
| ADR-011 | ✅ ports previstos; ADR-013 generaliza la familia |
| ADR-012 | ✅ habilita validación `PrimaryOrganizationId` sin tocar Patient aún |
| DEVELOPMENT-POLICY | ✅ contracts · sin FK/repos cross-BC |
| ORGANIZATION-CONSUMPTION-GUIDE | ✅ decision tree actualizado a ADR-013 |

---

## Fuera de alcance 17.2

- Dominio / persistencia / HTTP de Patient (→ **17.3+**)  
- Adapter de `OfficeReferencePort` / `StaffAssignmentReferencePort`  
- Migrar `MembershipReferencePort` a `iam-contract`  

---

## Siguiente paso

**PASO 17.3 — Patient Domain Foundation** — Aggregate + VOs + tests bajo ADR-012 frozen; use case de escritura usará `OrganizationReferencePort` cuando se implemente aplicación.

---

## Referencias

- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
