# PASO 16.0 — Organizations Audit

**Fecha:** 2026-06-17  
**Estado:** 🟡 Preparación — listo para iniciar FASE 16  
**Dependencias:** FASE 15 IAM **FOUNDATION COMPLETE** (15.9.2–15.9.4)

---

## Objetivo

Auditoría inicial para iniciar **FASE 16 — Organizations** sobre la base IAM cerrada, sin reabrir RBAC ni Identity/Membership.

---

## Estado IAM al iniciar FASE 16

| Capacidad | Estado |
|-----------|--------|
| Admin API `/api/v1/iam/**` | ✅ Operativa + E2E (15.9) |
| RBAC `@RequiresPermission` | ✅ ADR-007 |
| Identity global + Membership | ✅ ADR-006 |
| Bootstrap greenfield | ✅ 15.9.2 (ApplicationRunner) |
| Tenant status enforcement | ✅ 15.9.3 |
| User DELETE = tenant offboarding | ✅ 15.9.4 |
| Cross-tenant isolation | ✅ Verificado 15.9.1 |
| Password recovery | ⏳ ADR-009 P1 deferred |
| Audit trail | ⏳ ADR-009 P2 deferred |

**Veredicto:** No hay bloqueantes arquitectónicos para Organizations.

---

## Qué es Organizations en CodeCore (hipótesis inicial)

Organizations extiende el modelo multi-tenant con estructura de negocio:

```text
Tenant
 └── Organization(s)
      └── Office(s) / Location(s)
           └── Staff, Patients, Appointments, …
```

IAM provee:

- `tenantId` desde JWT / `AuthorizationContext`
- Membership + roles para autorización
- Admin API para usuarios y asignaciones

Organizations **no** debe alterar ADR-006 ni ADR-007 sin nueva ADR.

---

## Preguntas a resolver en 16.0 / 16.1

1. ¿Organization es aggregate bajo Tenant o entidad independiente con `tenant_id`?
2. ¿Relación Organization ↔ Membership (staff pertenece a org)?
3. ¿Nuevo módulo Gradle o extensión de `identity-access-management`?
4. ¿Permisos nuevos en catálogo (`organization:*`) o reutilización temporal?
5. ¿Organizations comparte tenant boundary de ADR-003?

---

## Restricciones heredadas

- DDD · Hexagonal · Modular Monolith · WebFlux · R2DBC
- Tenant isolation obligatorio (ADR-003)
- Sin microservicios ni CQRS
- Deuda ADR-009 P1/P2 **no bloquea** diseño Organizations

---

## Artefactos existentes reutilizables

| Artefacto | Uso en FASE 16 |
|-----------|----------------|
| `AuthorizationContext.tenantId()` | Scope queries |
| `TenantOperationalGuard` | Bloqueo si tenant no ACTIVE |
| `IamAdminApiPaths` | Patrón rutas admin |
| `@RequiresPermission` | Protección endpoints org |
| Flyway incremental | Nuevo schema `org` o tablas en módulo |

---

## Riesgos a vigilar

| Riesgo | Mitigación |
|--------|------------|
| Mezclar IAM con dominio org | Módulo separado o paquete `organizations` |
| Romper tenant isolation | Toda query filtrada por `tenantId` |
| Duplicar user/membership | Organizations referencia `identityId` / `membershipId` |

---

## Próximo paso recomendado

**PASO 16.2 — Organization Persistence**

1. Schema `org.organization` (Flyway V14+)
2. Implementación R2DBC de `OrganizationRepository` y `OrganizationQueryPort`
3. Tests de integración persistencia
4. Sin HTTP hasta 16.4

Ver [PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md](PASO-16.1-ORGANIZATIONS-DOMAIN-FOUNDATION.md).

---

## Referencias

- [ROADMAP.md](../architecture/ROADMAP.md)
- [ADR-006](../architecture/ADR-006-IDENTITY-STRATEGY.md)
- [ADR-009](../architecture/ADR-009-PRODUCTION-READINESS-BACKLOG.md)
- [PASO-15.9.1](PASO-15.9.1-IAM-PRODUCTION-READINESS-AUDIT.md)
