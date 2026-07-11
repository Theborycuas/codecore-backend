# PASO 17.4 — Patient Persistence

**Patient** queda persistido exactamente como ADR-012 lo congela: identidad clínica registral, sin FK cross-BC, listo para todos los verticales del Core.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Persistencia R2DBC + Flyway  
**Dependencias:** [PASO-17.3](PASO-17.3-PATIENT-DOMAIN-FOUNDATION.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)

---

## Objetivo

Conectar el Aggregate `Patient` a PostgreSQL siguiendo el patrón nativo CodeCore (Organization / Office / StaffAssignment). Sin HTTP, use cases, seguridad ni ReferencePorts.

---

## Decisiones

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Schema | `clinical` | BC Clinical Foundation (ADR-012) |
| Tabla raíz | `clinical.patient` | Un row = un Patient |
| External IDs | Tabla hija `clinical.patient_external_identifier` | Unicidad soft por tenant; FK **solo** intra-BC |
| `tenant_id` | UUID obligatorio, sin FK a IAM | ADR-003 · mismo patrón Org |
| `primary_organization_id` | UUID nullable, sin FK a `org` | ADR-011/013 — referencia lógica |
| Adapter | `R2dbcPatientRepository` implementa `PatientRepository` + `PatientQueryPort` | Igual que Organization |
| Child rows | `DatabaseClient` replace-all | Mismo estilo que `MembershipRole` |

### Por qué NO hay FK cross-BC

| Referencia | FK física | Motivo |
|------------|-----------|--------|
| `tenant_id` → IAM | ❌ | Desacopla lifecycle de schemas; aislamiento por ID |
| `primary_organization_id` → Org | ❌ | Org cerrado; validación ACTIVE vía `OrganizationReferencePort` (ADR-013) en escritura futura |
| `patient_id` → external ids | ✅ | Misma BC (`clinical`) — ownership del aggregate |

---

## SQL (V18)

```text
clinical.patient
  patient_id PK
  tenant_id NOT NULL
  primary_organization_id NULL
  display_name, contact_email?, contact_phone?, date_of_birth?
  status ACTIVE|ARCHIVED
  created_at, updated_at

clinical.patient_external_identifier
  PK (patient_id, identifier_type)
  FK → clinical.patient ON DELETE CASCADE
  UNIQUE (tenant_id, identifier_type, identifier_value)
```

**Índices:** tenant · status · (tenant, status) · (tenant, primary_organization_id) · ext patient/tenant.

**Ausente por diseño:** office_id · appointment · encounter · medical_record · billing · identity · membership · staff_assignment.

---

## Infraestructura

```text
patient-infrastructure
  configuration/PatientModuleConfiguration
  persistence/entity/PatientEntity
  persistence/mapper/PatientMapper
  persistence/repository/SpringDataPatientRepository
  persistence/repository/R2dbcPatientRepository
```

`codecore-api` escanea `com.codecore.patient` y depende de `patient-infrastructure`.

---

## Tests (R2dbcPatientRepositoryIT)

| Caso | Cubierto |
|------|----------|
| save + findById | ✓ |
| exists / existsByIdAndTenant | ✓ |
| countByTenant + findByTenant | ✓ |
| cross-tenant isolation | ✓ |
| PrimaryOrganizationId null/non-null + query | ✓ |
| ExternalIdentifiers persist + replace | ✓ |
| Duplicate ext-id same tenant → DuplicateKey | ✓ |
| Same ext-id different tenants OK | ✓ |
| Update without duplicating row | ✓ |
| findByStatus | ✓ |

**10/10** ITs verdes (`R2dbcPatientRepositoryIT`).

```bash
./gradlew :modules:patient-management:patient-infrastructure:test
```

Requiere Docker Desktop (Testcontainers), igual que Organization ITs.

---

## Validación arquitectónica

| Check | Resultado |
|-------|-----------|
| ADR-012 | ✅ solo columnas del contrato congelado |
| ADR-013 | ✅ sin repos/FK a Org; ReferencePort sigue siendo el camino de validación |
| ADR-003 | ✅ tenant obligatorio en fila e índices |
| Sin acoplamiento IAM | ✅ |
| Sin acoplamiento Organization | ✅ |
| Intentionally small | ✅ |
| Reutilizable por Dental/Vet/Hospital/… | ✅ — identity only |

---

## Fuera de alcance

HTTP · Controllers · OpenAPI · Use cases · Permissions · `PatientReferencePort`

---

## Siguiente paso

**PASO 17.5.1 — Patient Admin API Audit** — autorización cerrada en [PASO-17.5](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md).

---

## Referencias

- [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md)  
- [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [PASO-17.3](PASO-17.3-PATIENT-DOMAIN-FOUNDATION.md)  
- [PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md](PASO-17.5-PATIENT-AUTHORIZATION-CONTRACT.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
- Migración: `V18__create_patient_table.sql`  
