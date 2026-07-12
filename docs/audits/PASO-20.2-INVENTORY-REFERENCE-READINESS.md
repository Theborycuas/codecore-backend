# PASO 20.2 — Inventory Reference Readiness

**Organization contract** ya cubre lo que Item necesita para `PrimaryOrganizationId` — sin evolucionar ports, sin reabrir ADR-010/011/013 ni aggregates congelados.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Contract check (ADR-013 · ADR-016) — **sin código nuevo**  
**Dependencias:** [ADR-016](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-20.1](PASO-20.1-ITEM-MODEL-CONTRACT.md) · [PASO-17.2](PASO-17.2-REFERENCE-CONTRACTS.md) · [PASO-18.2](PASO-18.2-REFERENCE-PORTS.md)

---

## Objetivo

Cubrir ADR-016 §8 / §11: si Item trae `PrimaryOrganizationId`, validar **existencia + ACTIVE + mismo tenant** vía `OrganizationReferencePort` — nunca SQL a `org` desde Inventory.

Confirmar que **no** hace falta evolucionar el contract (a diferencia de 19.2, donde Appointment sí necesitaba `findLinkable…`).

**No** se modificó Organization, Office, StaffAssignment, Patient, Appointment, Encounter, ni ADRs.

---

## Veredicto

| Pregunta | Respuesta |
|----------|-----------|
| ¿Qué port necesita Item v1? | Solo `OrganizationReferencePort` |
| ¿Método requerido? | `existsActiveByIdAndTenant(organizationId, tenantId)` |
| ¿Ya existe contract + adapter + tests? | **Sí** (PASO 17.2) |
| ¿Hay hueco de invariante? | **No** |
| ¿Evolución de port en 20.2? | **Ninguna** |
| ¿Office / Patient / Encounter ports? | **No** — fuera de Item v1 |

**Listo para 20.3 Domain Foundation** sin trabajo de contract.

---

## Matriz ADR-016 → port

| Invariante Item (escritura) | Port | ¿Suficiente? |
|-----------------------------|------|--------------|
| Si `PrimaryOrganizationId` presente → Organization ACTIVE en tenant | `OrganizationReferencePort.existsActiveByIdAndTenant` | ✅ |
| Sin primary org | Ningún port Org | ✅ (skip) |
| `OfficeId` en Item | — | N/A — **prohibido** en Item |
| Qty / Stock locus | — | Stock futuro → `OfficeReferencePort` |
| Clinical material “por si acaso” | — | **Rechazado** — consumo clínico referencia `ItemId` outbound |

### Semántica del port (sin cambio)

| Caso | Resultado |
|------|-----------|
| ACTIVE + tenant OK | `true` |
| ARCHIVED | `false` |
| Wrong tenant / unknown | `false` |

Coincide con Patient (`PrimaryOrganizationId`) y con Encounter/Appointment (Organization ACTIVE).

---

## Artefactos existentes (evidencia)

| Artefacto | Ubicación | Estado |
|-----------|-----------|--------|
| `OrganizationReferencePort` | `organization-contract` | ✅ |
| `R2dbcOrganizationReferenceAdapter` | `organization-infrastructure` | ✅ |
| Contract test | `OrganizationReferencePortContractTest` | ✅ |
| IT | `OrganizationReferencePortIT` | ✅ |

```text
organization-contract          ← OrganizationReferencePort (17.2)
        ▲
organization-infrastructure    ← R2dbcOrganizationReferenceAdapter
        ▲
inventory-application (20.3+)  ← dependerá solo de organization-contract
```

### Coherencia Item (application — futuro 20.6)

Validación **en application**, nunca en domain Item, nunca SQL cross-BC:

1. Si `PrimaryOrganizationId` presente → `existsActiveByIdAndTenant` == `true`  
2. Si ausente → no invocar port  
3. **Nunca** validar Office / Patient / Encounter en escritura Item  

Respuesta HTTP esperada ante `false`: **404** (espejo Patient/Encounter — no filtrar existencia cross-tenant).

---

## Ports explícitamente NO creados / NO consumidos en Item v1

| Port | Motivo |
|------|--------|
| `OfficeReferencePort` | Locus de stock → aggregate `Stock` futuro |
| `StaffAssignmentReferencePort` | Item no tiene operador |
| `PatientReferencePort` | Catálogo ≠ sujeto clínico |
| `AppointmentReferencePort` | Fuera |
| `EncounterReferencePort` | Consumo clínico futuro referencia Item, no al revés |
| `ItemReferencePort` | Se publica en **20.8** closeout — no en readiness |

Inventar cualquiera de estos “por si el material es clínico” o “para Stock ya” viola ADR-016 §3 / §11 y ADR-013 (just-in-time).

---

## Comparación con 19.2

| Aspecto | 19.2 Encounter | **20.2 Item** |
|---------|----------------|---------------|
| ¿Evolución de port? | Sí — `findLinkableByIdAndTenant` | **No** |
| Motivo | Appointment status + patientId | Org ACTIVE boolean basta |
| Código en este paso | Contract + adapter + tests | **Documentación only** |

---

## Evidencia de tests (port existente)

```bash
./gradlew :modules:organization-management:organization-contract:test \
  --tests "com.codecore.organization.contract.reference.OrganizationReferencePortContractTest" \
  :modules:organization-management:organization-infrastructure:test \
  --tests "com.codecore.organization.infrastructure.adapters.OrganizationReferencePortIT"
```

**Resultado (2026-07-12):** contract + IT **verdes** — sin cambios de código en este paso.

---

## Fuera de alcance

Item domain/HTTP · módulos Gradle Inventory · mutaciones en ports · DTOs admin · Stock · reabrir ADR-010…016 · `ItemReferencePort`

---

## Criterio de salida 20.2

- [x] Matriz ADR-016 → ports completa  
- [x] Confirmado: `existsActiveByIdAndTenant` cubre `PrimaryOrganizationId`  
- [x] Confirmado: sin evolución de contract  
- [x] Confirmado: sin ports Office/clínicos preventivos  
- [x] Sin código de producto Inventory  
- [x] ADRs / BCs cerrados intactos  

---

## Siguiente paso

**PASO 20.3 — Item Domain Foundation** ✅ — [PASO-20.3](PASO-20.3-ITEM-DOMAIN-FOUNDATION.md). Siguiente: **20.4 Persistence**.

---

## Referencias

- [ADR-016-ITEM-DOMAIN-MODEL.md](../architecture/ADR-016-ITEM-DOMAIN-MODEL.md)  
- [ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-20.1-ITEM-MODEL-CONTRACT.md](PASO-20.1-ITEM-MODEL-CONTRACT.md)  
- [PASO-17.2-REFERENCE-CONTRACTS.md](PASO-17.2-REFERENCE-CONTRACTS.md)  
- [PASO-18.2-REFERENCE-PORTS.md](PASO-18.2-REFERENCE-PORTS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
