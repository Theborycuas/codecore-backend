# PASO 17.3 — Patient Domain Foundation

**Patient** is the clinical registry identity of the care subject — intentionally small, frozen by ADR-012, and ready for decades of downstream BCs without growing into a God Aggregate.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Dominio puro (sin persistencia / HTTP / use cases)  
**Dependencias:** [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-17.2](PASO-17.2-REFERENCE-CONTRACTS.md)

---

## One-sentence rule (aggregates importantes)

| Aggregate | Frase |
|-----------|--------|
| Tenant | La frontera de aislamiento SaaS. |
| Organization | La unidad estructural del negocio. |
| Office | La ubicación física donde opera el negocio. |
| StaffAssignment | El alcance operativo de un miembro del tenant. |
| **Patient** | **La identidad clínica registral del sujeto de cuidado.** |

Si un aggregate deja de caber en una frase clara, suele estar asumiendo demasiadas responsabilidades.

---

## Objetivo

Implementar el foundation del Aggregate Root `Patient` **exactamente** como ADR-012 Accepted — sin rediseñar, sin infraestructura.

---

## Módulo Gradle

```text
modules/patient-management/
  patient-domain          ← Aggregate + VOs + exceptions + tests
  patient-application     ← solo puertos de salida (placeholder de use cases)
  patient-contract        ← publica PatientId vía api(domain); sin ReferencePort aún
  patient-infrastructure  ← placeholder
```

Registrado en `settings.gradle.kts`.

---

## Modelo implementado

```text
Patient
  ├── PatientId                         (hard identity)
  ├── TenantId                          (required, immutable)
  ├── PatientDemographics
  │     ├── PatientDisplayName          (required — human or animal)
  │     ├── ContactEmail?               (not unique)
  │     ├── ContactPhone?               (not unique)
  │     └── DateOfBirth?
  ├── ExternalIdentifiers               (0..n typed keys; unique type)
  ├── PrimaryOrganizationId?            (UUID ref — not ownership)
  └── PatientStatus                     ACTIVE | ARCHIVED
```

**Behaviors:** `create` · `updateDemographics` · `replaceExternalIdentifiers` · `assignPrimaryOrganization` · `removePrimaryOrganization` · `archive` · `activate` · `reconstitute`

**Mutaciones de registro** solo en estado `ACTIVE`.  
Validación ACTIVE+tenant de `PrimaryOrganizationId` → application + `OrganizationReferencePort` (pasos siguientes), no el aggregate.

### Explicitamente ausente (ADR-012 §3)

Appointment · Encounter · MedicalRecord · Notes · Odontogram · TreatmentPlan · Documents · Billing · Inventory · Office · StaffAssignment · Identity · Membership

---

## Application / Contract / Infrastructure

| Capa | Entregable 17.3 |
|------|-----------------|
| Application | `PatientRepository`, `PatientQueryPort` — sin use cases ni servicios |
| Contract | `PatientContractMarker` + `api(patient-domain)` — **sin** `PatientReferencePort` |
| Infrastructure | `PatientInfrastructurePlaceholder` |

---

## Tests

**28** tests de dominio (todos verdes):

| Suite | Cobertura |
|-------|-----------|
| `PatientTest` | create, tenant immutable, demographics, external ids, assign/remove org, archive/activate, transiciones inválidas, reconstitución, API sin concerns operativos |
| `PatientValueObjectTest` | igualdad, validaciones, emails/phones/DOB, external id types, demographics opcionales, status enum |

```bash
./gradlew :modules:patient-management:patient-domain:test
```

---

## Fuera de alcance

Persistencia · Flyway · R2DBC · HTTP · controllers · use cases de escritura · `patient:*` permissions · `PatientReferencePort`

---

## Siguiente paso

**PASO 17.5 — Patient Authorization Contract** — persistencia cerrada en [PASO-17.4](PASO-17.4-PATIENT-PERSISTENCE.md).

---

## Referencias

- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-17.1](PASO-17.1-CLINICAL-FOUNDATION-CONTRACT.md) · [PASO-17.2](PASO-17.2-REFERENCE-CONTRACTS.md) · [PASO-17.4](PASO-17.4-PATIENT-PERSISTENCE.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)  
