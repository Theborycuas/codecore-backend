# PASO 21.7 — Invoice Verification

**`InvoiceVerificationIT`** cierra el slice Invoice con 8 checks end-to-end sobre Testcontainers PostgreSQL + IAM real + Organization + Patient + Inventory (Item) reales, espejo `ItemVerificationIT` (FASE 20.7).

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Verificación E2E (HTTP + seguridad + persistencia real)  
**Dependencias:** [PASO-21.6](PASO-21.6-INVOICE-ADMINISTRATION-API.md)

---

## Entregable

`billing-infrastructure/src/test/.../interfaces/http/admin/InvoiceVerificationIT.java` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` con `InvoiceAdministrationVerificationTestConfiguration` (importa IAM + Organization + Patient + Inventory + Billing stacks completos: seguridad JWT, controllers reales, ReferencePorts reales, persistencia R2DBC real).

`AbstractInvoiceHttpIntegrationTest` — Testcontainers PostgreSQL estático + Flyway completo (`classpath:db/migration`), mismo patrón que `AbstractPostgresIntegrationTest` + soporte HTTP (`WebTestClient`).

### 8 checks de verificación

| # | Test | Verifica |
|---|------|----------|
| 1 | `verification1_fullInvoiceLifecycleJourney` | Journey completo: crear (org issuer + patient bill-to + línea con `ItemId`) → listar (`DRAFT` default) → obtener (`totalAmountMinor` derivado correcto) → actualizar (reemplazo completo de líneas, total recalculado) → **issue** (`DRAFT→ISSUED`) → filtrar por `status=DRAFT`/`ISSUED`/`VOIDED` → **void** (`ISSUED→VOIDED`) |
| 2 | `verification2_readOnlyCannotCreateInvoice` | Rol `READ_ONLY` intenta `POST` → **403** |
| 3 | `verification3_crossTenantAccessReturns404` | Invoice del tenant B, `GET` con token del tenant A → **404** (anti-enumeración) |
| 4 | `verification4_invalidIssuerOrBillToReferencesReturn404` | `issuerOrganizationId` inexistente → 404; `billToPatientId` inexistente → 404 |
| 5 | `verification5_managerCanIssueAndVoidPerMatrix` | Rol `MANAGER` (matriz 21.5) → `issue` 200 y `void` 200 |
| 6 | `verification6_duplicateInvoiceNumberReturns409` | Mismo `invoiceNumber` en el mismo tenant → **409** |
| 7 | `verification7_openApiDocumentsBillingAdministrationSurface` | `GET /v3/api-docs/billing-administration` incluye `/api/v1/billing/invoices` |
| 8 | `verification8_unauthenticatedRequestsReturn401` | `GET` list/get sin `Authorization` → **401** |

```bash
./gradlew :modules:billing-management:billing-infrastructure:test --tests "*Invoice*"
# BUILD SUCCESSFUL — 18 tests (10 R2dbcInvoiceRepositoryIT + 8 InvoiceVerificationIT)
```

### Deviación anotada vs. el enunciado

- El check 6 se implementó como **duplicado de `invoiceNumber` → 409** (la opción explícitamente ofrecida en el enunciado, "Duplicate invoiceNumber 409 OR patient/encounter mismatch 409"). El mismatch Encounter↔Patient bill-to (`InvoicePatientMismatchException`) está cubierto a nivel de aplicación en `InvoiceAdministrationUseCaseTest` (21.6); no se duplicó en el nivel E2E para mantener el conjunto de fixtures (org+patient+item, sin encounter) simple, consistente con la sugerencia del enunciado ("optional encounter line if wiring is heavy").
- Se usó **una línea con `ItemId`** en el journey principal (check 1) para ejercitar `ItemReferencePort` end-to-end, cumpliendo "at least one test with ItemId line and ItemReferencePort".
- No se agregó un `InvoiceAdminControllerIT` de slice aislado — la cobertura HTTP vive íntegramente en `InvoiceVerificationIT` (ver nota en PASO-21.6).

---

## Tests — resumen consolidado FASE 21 (21.3–21.7)

| Módulo | Suite | Tests |
|--------|-------|-------|
| billing-domain | `InvoiceTest` + `InvoiceValueObjectTest` | 38 |
| billing-contract | `InvoicePermissionCatalogTest` | 3 |
| billing-application | `InvoiceAdministrationUseCaseTest` | 14 |
| billing-infrastructure | `R2dbcInvoiceRepositoryIT` + `InvoiceVerificationIT` | 18 |
| identity-access-management | `InvoiceAuthorizationSeedMigrationIT` (+ `SystemRoleTemplateTest`, `IamPermissionCatalog`-related, actualizados) | 1 nuevo + existentes actualizados |
| **Total Invoice-specific** | | **74** |

```bash
./gradlew :modules:billing-management:billing-domain:test
./gradlew :modules:billing-management:billing-application:test
./gradlew :modules:billing-management:billing-contract:test
./gradlew :modules:billing-management:billing-infrastructure:test --tests "*Invoice*"
./gradlew :modules:identity-access-management:test --tests "*Permission*" --tests "*SystemRole*" --tests "*Invoice*" --tests "*ItemAuthorization*"
# Todos BUILD SUCCESSFUL
```

### Nota — fallos IAM pre-existentes detectados y corregidos

Al correr la suite amplia de `identity-access-management:test` (solicitada por el enunciado: "Run all tests and fix until green") se detectaron y corrigieron dos bugs **pre-existentes**, **no relacionados** con el modelo Invoice ni con `git diff` previo de este trabajo — confirmados como deuda latente de una fase anterior (feature `TenantOperationalGuard`, PASO 15.9.3) que nunca se propagó a todos los test configs:

1. **`NoSuchBeanDefinitionException` de `TenantOperationalGuard`** en `AuthorizationHttpIT`, `IamAdministrationControllerIT`, y potencialmente otros ITs de login — `AuthenticateIdentityUseCaseImpl` requiere `TenantOperationalGuard` desde una fase anterior, pero varios `@Import` de test config (`IamAuthorizationHttpIntegrationTestConfiguration`, `IamAuthorizationVerificationTestConfiguration`, `IamSecurityHttpIntegrationTestConfiguration`, `IamLoginHttpIntegrationTestConfiguration`, `AuthenticateIdentityUseCaseIT`) nunca importaron `IamBootstrapConfiguration` (dueño del bean). **Fix:** se agregó `IamBootstrapConfiguration.class` al `@Import` de los 5 archivos. Adicionalmente, `AuthorizationHttpIT` creaba tenants con `TenantId.generate()` sin persistir la fila `iam.tenant` — el guard exige que el tenant exista; se agregó `persistActiveTenant(...)` antes del login en ambos tests.
2. **`DuplicateKeyException`** en `R2dbcPermissionRepositoryIT` (`shouldPersistFindAndCheckExistsByCode`, `shouldEnforceGloballyUniqueCode`, `shouldPersistSystemPermission`) — los fixtures usaban códigos (`user:update`, `user:delete`, `patient:create`) que **ya están sembrados globalmente** por Flyway (V13/V19) en el contenedor Postgres compartido de `AbstractPostgresIntegrationTest`, por lo que el `save()` del test siempre colisionaba con el seed. **Fix:** se renombraron los fixtures a códigos neutros sin colisión (`test-fixture:update|delete|create`).

Ambos fixes son puramente de configuración/fixtures de test IAM, sin tocar el modelo `Invoice`, sin ADR nuevo, y sin ampliar el alcance de FASE 21 más allá de dejar en verde la corrida de tests exigida por el enunciado.

---

## Checklist

- [x] 8/8 checks de verificación E2E verdes
- [x] Journey completo create→list→get→update→issue→filter→void→filter
- [x] RBAC (`READ_ONLY` 403, `MANAGER` issue/void 200) verificado con la matriz real de 21.5
- [x] Aislamiento cross-tenant (404) verificado
- [x] Referencias inválidas (org/patient) → 404 verificado
- [x] `invoiceNumber` duplicado → 409 verificado
- [x] OpenAPI `billing-administration` expone `/api/v1/billing/invoices`
- [x] Sin JWT → 401 verificado
- [x] Fallos IAM pre-existentes identificados, confirmados no relacionados con el modelo Invoice, y corregidos (config de test)

## Siguiente paso

**PASO 21.8 — Billing Closeout** — ✅ [PASO-21.8](PASO-21.8-BILLING-CLOSEOUT.md). **FASE 21 Invoice slice ✅**.
