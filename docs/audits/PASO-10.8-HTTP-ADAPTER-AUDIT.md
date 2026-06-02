# PASO 10.8 — HTTP Adapter (auditoría previa)

**Fecha:** 2026-06-02

## Fuentes revisadas

- `codecore-backend/.cursor/architecture.md`, `rules.md`, `workflow.md`
- `codecore-specifications/.../identity-access-management/` (overview, aggregates, api-contracts, repositories, workflows, security-rules, testing-strategy)

## Alineado

| Tema | Doc | Código |
|------|-----|--------|
| Hexagonal: HTTP en `interfaces` | architecture + package-info | `interfaces.http` |
| WebFlux reactivo | architecture | platform-webflux en API |
| `RegisterIdentityUseCase` | — | 10.6 listo |
| Email-first, sin perfil en registro | overview, user-mgmt separation | Command solo tenant/email/password |
| Unicidad tenant+email | repositories | 409 vía `IdentityAlreadyExistsException` |
| Testcontainers IT | testing-strategy §6.5 | `AbstractPostgresIntegrationTest` |
| Sin JWT/login en este paso | security-rules / alcance | No auth endpoints |

## No alineado / decisión

| Tema | Nota | Decisión 10.8 |
|------|------|----------------|
| Base path blueprint `/api/v1/iam` | api-contracts §3.1 | Ruta explícita del paso: **`POST /api/v1/identities`** |
| Registro no listado en api-contracts | Sin endpoint documentado | Añadir § registro en api-contracts |
| `platform-security` en API app | Genera password dev | IT IAM sin security starter → sin 401 en tests |
| HTTP contract tests | strategy menciona contract | Solo `RegisterIdentityControllerIT` en 10.8 |

## Riesgos

| ID | Descripción | Nivel |
|----|-------------|-------|
| R1 | Path `/api/v1/identities` vs `/api/v1/iam/*` | Bajo — documentar |
| R2 | Security en `codecore-api` puede bloquear POST en runtime | Medio — futuro `permitAll` registro |
| R3 | Validación doble (Bean Validation + dominio) | Bajo — aceptable |

## Autorización implementar

- Controller + DTOs + handler mínimo (409, 400 dominio)
- `RegisterIdentityControllerIT` (WebTestClient + Testcontainers)
- Actualizar api-contracts + testing-strategy

## Fuera de alcance

JWT, login, OAuth, `@PreAuthorize`, error framework genérico, user profile fields.
