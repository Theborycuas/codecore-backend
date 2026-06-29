# PASO 16.8 — Organization Validation & Integration Patterns

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Tipo:** Validación de bounded context + patrones de integración (sin código)  
**Dependencias:** ADR-010 · PASO-16.7 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Objetivo

Cerrar **Organization Management** como bounded context **estable y reutilizable**:

1. Validar el modelo 16.1–16.7 frente a escenarios reales downstream  
2. Definir cómo otros módulos consumen Organization **sin reabrir arquitectura**  
3. Publicar ADR-011 y guía operativa de consumo  

**No hay entregables de código en 16.8.**

---

## Cambio de enfoque (vs roadmap original)

| Original | Adoptado |
|----------|----------|
| Organization Authorization Patterns | **Organization Validation & Integration Patterns** |
| Implementar filtros en use cases | **Documentar** patrones; filtros en cada consumer BC |
| Doc Patient como nota | **ADR-011** + **ORGANIZATION-CONSUMPTION-GUIDE** prescriptivos |

**Rationale:** FASE 16 deja de ser “implementar Organization” y pasa a ser “construir un BC estable”. A partir de FASE 17 (Patient) se **consume** siguiendo ADR-011 — no se rediscute.

---

## Validación del modelo implementado

### Inventario 16.1–16.7

| Componente | Estado | Evidencia |
|------------|--------|-----------|
| Aggregate `Organization` | ✅ | domain + V14 + API |
| Aggregate `Office` | ✅ | domain + V16 + API |
| Aggregate `StaffAssignment` | ✅ | domain + V17 + API |
| Schema `org` | ✅ | Flyway V14–V17 |
| Permisos `organization:*`, `office:*`, `staff-assignment:*` | ✅ | V15 + catalog |
| Cross-BC membership | ✅ | `MembershipReferencePort` — ID only |
| Archive org/office | ✅ | Soft lifecycle |
| Delete assignment | ✅ | Physical — vínculo operativo |
| Cross-tenant → 404 | ✅ | Repository filters |
| Sin org-scoped RBAC | ✅ | ADR-007 unchanged |
| `organization-contract` | ✅ | Permission catalog (ports TBD on first consumer) |

### Escenarios downstream

| Escenario | ¿Lo soporta el modelo? | Notas |
|-----------|-------------------------|-------|
| Tenant con 3 clínicas (orgs) | ✅ | Multi-org per tenant |
| Consultorios por clínica | ✅ | Office under Organization |
| Médico solo en un consultorio | ✅ | StaffAssignment + officeId |
| Médico en toda la clínica | ✅ | StaffAssignment, officeId null |
| Paciente registrado en clínica X | ✅ | Patient aggregate stores `OrganizationId` (FASE 19) |
| Cita con proveedor concreto | ✅ | Appointment stores `StaffAssignmentId` |
| Historia clínica bajo custodia de org | ✅ | MedicalRecord stores `OrganizationId` |
| Factura por clínica | ✅ | Billing stores `OrganizationId` |
| Stock en consultorio | ✅ | Inventory stores `OfficeId` |
| Staff archivado / org archivada — histórico | ✅ | No cascade delete; operational vs historical reads (ADR-011 §5) |

**Veredicto:** modelo **suficiente**. No se requieren aggregates ni tablas adicionales en Organization antes de FASE 17+.

---

## Gaps conscientes (no bloqueantes)

| Gap | Resolución planificada |
|-----|------------------------|
| Query Ports en `organization-contract` | Primera necesidad del consumer (Patient) o 16.9 prep |
| `OrganizationVerificationIT` E2E | PASO 16.9 |
| OpenAPI grupo `org-administration` | PASO 16.10 |
| WebFlux IT `ReadOnlyHttpHeaders` | Backlog técnico — no afecta contrato de integración |

---

## Entregables 16.8

| Documento | Propósito |
|-----------|-----------|
| [ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) | Decisión arquitectónica — cierre BC + reglas |
| [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md) | Guía práctica por módulo (Patient, Appointment, …) |
| Este PASO-16.8 | Validación + cierre del paso |

---

## Reglas publicadas (resumen)

1. Consumidores dependen solo de `organization-contract`  
2. Referencias por ID — nunca repositorios org en otros BC  
3. **StaffAssignmentId** para acciones de staff; **OrganizationId/OfficeId** para datos/ubicación  
4. IAM responde acceso; Organization responde estructura y scope operativo  
5. BC Organization **cerrado** — cambios requieren nuevo ADR  

---

## Próximos pasos

| Paso | Nombre | Foco |
|------|--------|------|
| **16.9** | Organization Verification | E2E journey — `OrganizationVerificationIT` |
| **16.10** | Organization Management Closeout | OpenAPI, ROADMAP fase ✅, historial |

---

## Referencias

- [ADR-010](../architecture/ADR-010-ORGANIZATIONS-MODEL.md)
- [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md)
- [ORGANIZATION-CONSUMPTION-GUIDE.md](../architecture/ORGANIZATION-CONSUMPTION-GUIDE.md)
- [PASO-16.7-STAFF-ORGANIZATIONAL-ASSIGNMENT.md](PASO-16.7-STAFF-ORGANIZATIONAL-ASSIGNMENT.md)
