# PASO 10.5 — IAM Persistence Adapter (auditoría)

**Fecha:** 2026-06-01  
**Alcance:** Persistencia R2DBC hexagonal (sin endpoints, use cases, JWT).  
**Estado:** Implementado y compilado.

---

## Entregables

| Clase | Paquete |
|-------|---------|
| `IamUserEntity` | `...infrastructure.persistence.entity` |
| `SpringDataIamUserRepository` | `...infrastructure.persistence.repository` |
| `IamUserMapper` | `...infrastructure.persistence.mapper` |
| `R2dbcIdentityRepository` | `...infrastructure.persistence.repository` |
| `IamModuleConfiguration` | `...configuration` (+ `@EnableR2dbcRepositories`, bean `IamUserMapper`) |

**Dependencia añadida:** `spring-boot-starter-data-r2dbc` + `platform-r2dbc` en `modules/identity-access-management/build.gradle.kts`.

---

## Validación arquitectónica

| Capa | Contenido | Depende de Spring |
|------|-----------|-------------------|
| **Dominio** | `Identity`, VOs, reglas de auth | No |
| **Aplicación** | `IdentityRepository` (puerto out), use cases (futuro) | No (solo Reactor en puertos) |
| **Infraestructura** | Entity, Spring Data repo, mapper, `R2dbcIdentityRepository` | Sí |
| **Configuración** | `@EnableR2dbcRepositories`, beans de infra | Sí |

- **El adapter implementa el puerto** porque la aplicación define *qué* necesita persistir (`IdentityRepository`); la infraestructura define *cómo* (R2DBC + `iam.iam_user`).
- **El mapper existe** para mantener el dominio libre de anotaciones `@Table`/`@Column` y de acoplar `Identity` al esquema SQL; solo traduce estructuras.

---

## Riesgos dominio ↔ persistencia

1. **`Username` no está en `iam_user`** — derivado del local-part del email (sanitizado); búsqueda por username usa la misma regla SQL (`regexp_replace` + `split_part`). V3: columna `username`.
2. **`email` vs `normalized_email`** — el dominio solo expone `EmailAddress` normalizado; ambas columnas se persisten con el mismo valor.
3. **`locked` / `enabled`** — no hay columnas; se reconstruyen desde `status` al leer.
4. **`email_verified`** — columna persistida; dominio no tiene VO; se infiere al escribir desde `status != PENDING_VERIFICATION`.
5. **`Credential`** — embebido; `CredentialId` = `IdentityId`; sin `password_changed_at` en BD (V3/V4).
6. **`first_name` / `last_name`** — en tabla, no en `Identity`; no se mapean al dominio en 10.5.
7. **Optimistic lock** — `@Version` en entity; conflicto → excepción Spring Data en `save`.

---

## Verificación

```bash
./gradlew :modules:identity-access-management:test --tests "com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepositoryIT"
```

**Resultado (2026-06-01):** 2/2 tests PASSED (`R2dbcIdentityRepositoryIT` con Testcontainers PostgreSQL 16 + Flyway V1/V2).

### Correcciones aplicadas durante la prueba

1. `spring.r2dbc.properties.schema: iam` en `application.yml` y tests.
2. `IamUserEntity implements Persistable<UUID>` para INSERT con ID preasignado.
3. `R2dbcIdentityRepository.save` distingue insert/update con `findByTenantIdAndId`.
4. Classpath de migraciones en tests: `processTestResources` → `db/migration/`.
