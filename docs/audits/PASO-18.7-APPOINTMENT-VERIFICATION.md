# PASO 18.7 — Appointment Verification & Core Validation

**Veredicto:** Appointment es una pieza **estable** del Core Platform. Scheduling (planned commitment v1) está listo para closeout y consumo futuro **sin reabrir** ADR-014.

**Fecha:** 2026-07-11  
**Estado:** ✅ Completado  
**Tipo:** Verificación + consistencia + cierre de BC (sin nuevas capacidades)  
**Dependencias:** [PASO-18.6](PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md) · [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-011](../architecture/ADR-011-ORGANIZATION-INTEGRATION-PATTERNS.md) · [ADR-012](../architecture/ADR-012-PATIENT-DOMAIN-MODEL.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md)

---

## Quick path

1. Suite E2E: `AppointmentVerificationIT` (8 checks) — verde  
2. Dominio / persistencia / permisos / API auditados contra ADR-014/013  
3. Siguiente: **PASO 18.8 — Scheduling Closeout**

```bash
./gradlew :modules:appointment-management:appointment-infrastructure:test \
  --tests "com.codecore.appointment.interfaces.http.admin.AppointmentVerificationIT"
```

---

## Objetivo

Certificar que el primer bounded context de Scheduling de CodeCore cumple el contrato acumulado (PASO 18.0 → 18.6) y fortalece el **Core reutilizable**, no una app vertical.

**No** se implementaron endpoints, campos, permisos ni ADRs nuevos.  
**Sí** se consolidó evidencia ejecutable + auditoría documental.

---

## Suite E2E

| # | Verificación | Resultado |
|---|--------------|-----------|
| 1 | Journey: create → list → filter org → update → cancel → complete (+ status filters) | ✅ |
| 2 | RBAC — `READ_ONLY` no crea | 403 ✅ |
| 3 | Cross-tenant GET | 404 ✅ |
| 4 | Patient/Org inexistente (`ReferencePorts`) | 404 ✅ |
| 5 | MANAGER cancela Appointment (matriz 18.5) | 200 ✅ |
| 6 | Coherencia StaffAssignment↔Organization rota | 409 ✅ |
| 7 | OpenAPI grupo `scheduling-administration` | ✅ |
| 8 | Sin JWT | 401 ✅ |

Patrón espejo: [PASO-17.7-PATIENT-VERIFICATION.md](PASO-17.7-PATIENT-VERIFICATION.md).

---

## Checklist — Aggregate “intentionally small”

| Responsabilidad | ¿En Appointment? | Evidencia |
|-----------------|------------------|-----------|
| Compromiso planificado de cuidado | ✅ Sí (única) | ADR-014 · `Appointment` aggregate |
| Time window + status lifecycle | ✅ Sí | Domain VOs |
| Refs IDs (Patient/Staff/Org/Office) | ✅ Sí | Solo UUIDs lógicos |
| Encounter / Visit | ❌ | Ausente |
| MedicalRecord / Notes / SOAP / Odontogram | ❌ | Ausente |
| Billing / Inventory | ❌ | Ausente |
| Slot / Availability / Calendar engine | ❌ | Ausente en domain/API/SQL |
| Recurrence / Waitlist | ❌ | Ausente |
| IAM Identity / Membership como provider | ❌ | Solo `StaffAssignmentId` |
| Lógica de vertical (dental chair, surgery, …) | ❌ | Permisos `appointment:*` neutros |

**One-sentence rule:** *Appointment = el compromiso planificado de prestar un servicio a un sujeto de cuidado en un tiempo y contexto operativo determinados.*  
Sigue cabiendo en una frase → no es God Aggregate / God Engine.

---

## Validación ADR-014 (congelado)

| Constraint | ¿Cumple? |
|------------|----------|
| Modelo pequeño; sin embed clínico/capacidad/vertical | ✅ |
| `TenantId` requerido e inmutable | ✅ |
| Estados `SCHEDULED\|CANCELLED\|COMPLETED` | ✅ |
| Sin DELETE físico / sin reactivate | ✅ |
| `OrganizationId` denormalizado | ✅ |
| Coherencia StaffAssignment en application | ✅ |
| Sin double-booking en dominio | ✅ |
| Validación ACTIVE vía ReferencePorts | ✅ |
| Sin verbos de vertical en permisos | ✅ |

**Observación:** ninguna decisión de este paso exige modificar el modelo. ADR-014 permanece intacto.

---

## Validación ADR-013 (Reference Contracts)

| Constraint | ¿Cumple? |
|------------|----------|
| Consume `patient-contract` + `organization-contract` | ✅ `appointment-application` |
| `PatientReferencePort` / Org / Office / StaffAssignment | ✅ use case |
| Nunca repos provider en main | ✅ |
| Nunca SQL `clinical.*` / `org.*` desde Scheduling | ✅ solo `scheduling.appointment` |
| Port `false` / empty → excepción consumidor → HTTP 404 | ✅ |
| Coherencia §7 → 409 | ✅ |
| Cancel/complete sin revalidar ports | ✅ |

Appointment es el **primer consumidor multi-port real** de ADR-013 bajo carga HTTP.

---

## Validación Development Policy (FASE 16+)

| Principio | ¿Cumple? |
|-----------|----------|
| Dominio antes que DB/controller | ✅ (18.0.1 → 18.3 → 18.4 → 18.6) |
| Consistencia de Administration API con Patient/Org | ✅ mismos patrones HTTP/tenant/RBAC |
| Sin sobreingeniería (CQRS, ES, microservicios, slot engine) | ✅ |
| Auditoría antes de HTTP | ✅ 18.0.1 / 18.5.1 |
| Framework de negocio unificado | ✅ ciudadano nativo del Core |

---

## Persistencia

`scheduling.appointment` (V20):

| Columna / concern | ¿Presente? |
|-------------------|------------|
| Commitment identity + time + status + refs | ✅ |
| `tenant_id` (sin FK IAM) | ✅ |
| Refs Patient/Org/Office/Staff (sin FK cross-BC) | ✅ |
| Encounter / Notes / Billing / Slot / Recurrence | ❌ |

Schema = planned commitment. Nada más.

---

## Autorización

| Permiso | Existe |
|---------|--------|
| `appointment:create` | ✅ |
| `appointment:read` | ✅ |
| `appointment:update` | ✅ |
| `appointment:cancel` | ✅ |
| `appointment:complete` (dedicado) / verticales | ❌ |

Complete → `appointment:update` (contrato 18.5). Catalog + V21 alineados. Matrix: OWNER/ADMIN/MANAGER = 4; USER/READ_ONLY = read.

---

## API vs Patient

| Aspecto | Patient | Appointment | Paridad |
|---------|---------|-------------|---------|
| Soft CRUD + lifecycle POST | archive/activate | cancel/complete | ✅ forma |
| Cross-tenant | 404 | 404 | ✅ |
| Sin JWT | 401 | 401 | ✅ |
| Sin permiso | 403 | 403 | ✅ |
| Conflict | 409 (ext id) | 409 (coherence/state) | ✅ |
| Paginación `page/size/sort/status` | ✅ | ✅ + filtros 18.5.1 | ✅ |
| `tenantId` en response | No | No | ✅ |
| Multi-ReferencePort | 1 (Org) | 4 (Patient/Org/Office/Staff) | ✅ evolución Core |

---

## Reutilización multi-vertical

| Vertical | ¿Reutilizable sin cambiar Appointment? | Por qué |
|----------|----------------------------------------|---------|
| Dental | ✅ | Compromiso genérico; chair/odontogram fuera |
| Veterinaria | ✅ | Mismo planned commitment |
| Hospital | ✅ | Encounter fuera |
| Laboratorio | ✅ | Órdenes = otro BC |
| Psicología / Fisio | ✅ | Sin notes clínicas en Appointment |
| Vertical futuro | ✅ | Contrato pequeño + permisos neutros + ReferencePorts |

Appointment pertenece al **Core**, no a Dental ni a ningún producto.

---

## Conclusiones

1. **Scheduling (Appointment v1) está verificado** de dominio a HTTP.  
2. **ADR-014 / 013 se mantienen** — sin presión de cambio de modelo.  
3. **Closeout 18.8** puede publicar `AppointmentReferencePort` + guía de consumo.  
4. CodeCore sale **más fuerte como plataforma**, no como app de agenda.

---

## Riesgos encontrados

| Riesgo | Severidad | Mitigación / nota |
|--------|-----------|-------------------|
| Tentación de meter slots/double-booking “solo un poco” | Media futura | ADR-014 §3 — rechazar; nuevo aggregate/BC |
| Ausencia aún de `AppointmentReferencePort` | Baja | Planificado en closeout 18.8 |
| Labels enriquecidos Patient/Org en response | Baja | Read-model futuro; no inflar DTO |

Ningún riesgo obliga a modificar el dominio ahora.

---

## Checklist DoD

- [x] Checklist arquitectónico completo  
- [x] Validación ADR-014 / ADR-013  
- [x] Validación Development Policy  
- [x] Suite `AppointmentVerificationIT` verde (8/8)  
- [x] Documentación PASO-18.7  
- [x] ROADMAP → 18.7 ✅ · siguiente 18.8  
- [x] Sin cambios de modelo / ADR / endpoints nuevos  

---

## Siguiente paso

**PASO 18.8 — Scheduling Closeout** — `AppointmentReferencePort` · guía · FASE 18 ✅ — ver [PASO-18.8](PASO-18.8-SCHEDULING-CLOSEOUT.md).

---

## Referencias

- [PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md](PASO-18.6-APPOINTMENT-ADMINISTRATION-API.md)  
- [PASO-17.7-PATIENT-VERIFICATION.md](PASO-17.7-PATIENT-VERIFICATION.md)  
- [ADR-014](../architecture/ADR-014-APPOINTMENT-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
