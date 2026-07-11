# PASO 17.1 — Clinical Foundation Contract

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado (solo arquitectura)  
**Tipo:** Cierre de contrato arquitectónico — ADR-012 Accepted  
**Dependencias:** [PASO-17.0](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md) · [PASO-17.0.1](PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)

---

## Objetivo

Cerrar oficialmente el contrato del primer bounded context clínico de CodeCore (**Clinical Foundation** / Aggregate Root **`Patient`**) **sin** implementar dominio, persistencia ni HTTP.

Al terminar 17.1, Appointment, MedicalRecord, Billing, Inventory y cualquier BC futuro pueden **consumir** Patient vía `PatientId` (+ contratos) **sin** rediseñar el modelo.

---

## Revisión arquitectónica final

### Coherencia con ADRs vigentes

| ADR | Veredicto | Evidencia |
|-----|-----------|-----------|
| **ADR-003** | ✅ | `TenantId` obligatorio e inmutable; aislamiento SaaS |
| **ADR-006** | ✅ | Patient ≠ Identity; sin `IdentityId`/`MembershipId` en v1 |
| **ADR-007** | ✅ | Mutación vía membership + `patient:*`; sin org-scoped RBAC |
| **ADR-010** | ✅ | Org/Office/StaffAssignment intactos; Patient solo referencia |
| **ADR-011** | ✅ | `PrimaryOrganizationId` opcional; `OfficeId` en BCs operativos; consumo por IDs/ports |

### Coherencia entre documentos CodeCore

| Documento | Veredicto | Nota |
|-----------|-----------|------|
| PASO-17.0 | ✅ | Alineado (fila Consume corregida: sin `OfficeId` en Patient) |
| PASO-17.0.1 | ✅ | Audit + refinamiento + principio §3 |
| ADR-012 | ✅ | Proposed → **Accepted** en este paso |
| ORGANIZATION-CONSUMPTION-GUIDE | ✅ | Recipe Patient alineado |
| ROADMAP | ✅ | Actualizado en este paso |
| DEVELOPMENT-POLICY-FASE-16-PLUS | ✅ | §§4–5–9 respetados; §3 ADR-012 refuerza anti–God Aggregate |

### Confirmaciones de diseño

| Afirmación | Estado |
|------------|--------|
| `Patient` es Aggregate Root definitivo de Clinical Foundation | ✅ |
| “Patient is intentionally small” protege el aggregate (§3) | ✅ |
| Ninguna responsabilidad clínica futura debe permanecer en Patient | ✅ |
| BCs futuros consumen Patient vía `PatientId` + `PatientReferencePort` (cuando exista) | ✅ |

**Inconsistencias bloqueantes:** ninguna.  
**Ajuste menor aplicado en 17.1:** PASO-17.0 fila “Consume Organization” ya no menciona `OfficeId` en Patient.

---

## Contrato congelado (resumen)

```text
Patient (Clinical Foundation)
  ├── PatientId
  ├── TenantId                    (required, immutable)
  ├── PrimaryOrganizationId?      (optional OrganizationId — not ownership)
  ├── registry demographics / contacts / external ids
  └── status ACTIVE | ARCHIVED

NEVER on Patient:
  OfficeId · StaffAssignmentId · MembershipId · IdentityId
  Appointment · Encounter · MedicalRecord · notes · odontogram
  TreatmentPlan · Documents · Billing · Inventory · operational context
```

**Principio permanente:** [ADR-012 §3](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) — *Patient is intentionally small.*

**Freeze:** el modelo Patient queda **congelado**. Cualquier cambio de boundary, referencias, lifecycle, exclusividad o principio §3 **requiere un nuevo ADR** (+ auditoría política §2).

---

## Consumo por bounded contexts futuros

| Consumer | Cómo usa Patient | Qué no hace |
|----------|------------------|-------------|
| Scheduling (`Appointment`) | Almacena `PatientId` | No embebe Patient; no usa Membership como sujeto |
| Clinical Records | Almacena `PatientId` | No crece Patient con notas/charts |
| Billing | Almacena `PatientId` (+ org en factura) | No lee Identity para “ser paciente” |
| Inventory | No requiere Patient (usa Office) | — |
| Vertical packs | Extienden perfil vía modelos satélite / otros aggregates | No duplican registry |

Validación runtime de existencia/ACTIVE: futuro `PatientReferencePort` en `patient-contract` (espejo ADR-013).

Validación de `PrimaryOrganizationId`: `OrganizationReferencePort` ([PASO 17.2](PASO-17.2-REFERENCE-CONTRACTS.md) · ADR-013) — ✅ implementado.

---

## Entregables 17.1

| Artefacto | Acción |
|-----------|--------|
| [ADR-012-PATIENT-DOMAIN-MODEL.md](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) | **Accepted** + Freeze rule |
| Este PASO-17.1 | Cierre de contrato |
| [ROADMAP.md](../architecture/ROADMAP.md) | 17.1 ✅; siguiente **17.2** |

**Sin código. Sin tablas. Sin endpoints.**

---

## Siguiente paso

**PASO 17.3 — Patient Domain Foundation** — Aggregate + VOs + tests bajo contrato congelado ADR-012; escritura validará `PrimaryOrganizationId` vía `OrganizationReferencePort` (ADR-013).

---

## Referencias

- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md](PASO-17.0.1-PATIENT-AGGREGATE-AUDIT.md)  
- [PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)  
- [PASO-17.2-REFERENCE-CONTRACTS.md](PASO-17.2-REFERENCE-CONTRACTS.md)  
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
