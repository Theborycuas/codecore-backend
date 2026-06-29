# PASO 16.9 — Organization Verification

**Fecha:** 2026-06-22  
**Estado:** ✅ Completado  
**Dependencias:** PASO-16.7 · PASO-16.8 · ADR-011

---

## Objetivo

Demostrar que toda la capa Organization Management (`/api/v1/org/**`) funciona de extremo a extremo: journey multi-org, offices, staff assignments, RBAC, aislamiento tenant, guards de archive y contrato OpenAPI.

---

## Suite

`OrganizationVerificationIT` — 8 verificaciones explícitas.

```bash
./gradlew :modules:organization-management:organization-infrastructure:test \
  --tests "com.codecore.organization.interfaces.http.admin.OrganizationVerificationIT"
```

| # | Verificación | Resultado esperado |
|---|--------------|------------------|
| 1 | Journey completo (3 orgs → office → assignment → archive → delete) | ✅ |
| 2 | RBAC — READ_ONLY no crea org | 403 |
| 3 | Cross-tenant GET org | 404 |
| 4 | Archive org con offices ACTIVE | 409 |
| 5 | MANAGER lee pero no archiva | 200 / 403 |
| 6 | Staff assignment scope duplicado | 409 |
| 7 | OpenAPI grupo `org-administration` | paths org/offices/staff-assignments |
| 8 | Sin JWT | 401 |

---

## Fix técnico incluido

`PlatformSecurityAutoConfiguration`: deshabilitados security headers por defecto — conflicto `ReadOnlyHttpHeaders` con WebTestClient (también afectaba ITs IAM/org previos). HSTS/CSP en producción → reverse proxy (ADR-009).

---

## Referencias

- Patrón: [PASO-15.9-IAM-ADMINISTRATION-VERIFICATION.md](PASO-15.9-IAM-ADMINISTRATION-VERIFICATION.md)
- Config test: `OrganizationAdministrationVerificationTestConfiguration`
