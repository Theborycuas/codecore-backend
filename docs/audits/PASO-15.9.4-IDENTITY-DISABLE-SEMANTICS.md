# PASO 15.9.4 — Identity Disable Semantics

**Fecha:** 2026-06-17  
**Estado:** ✅ Cerrado  
**ADR:** ADR-009 (P1), ADR-006

---

## Problema

`DELETE /api/v1/iam/users/{id}` ejecutaba `Identity.disable()` — efecto **global** sobre identity compartida entre tenants, generando ambigüedad multi-tenant (PASO-15.9.1).

---

## Análisis

| Concepto | Alcance | Operación correcta |
|----------|---------|-------------------|
| **Offboarding tenant** | Membership tenant-scoped | `membership.deactivate()` |
| **Offboarding global** | Identity global | `identity.disable()` vía `PUT` status DISABLED |
| **Identity global** | ADR-006 | Email único; identity compartida entre memberships |

### Respuestas de diseño

1. **¿Mantener `Identity.disable()` global?** Sí — vía `PUT /users/{id}` con `status: DISABLED`.
2. **¿Disable membership?** Sí — es la semántica de `DELETE /users/{id}`.
3. **¿Renombrar endpoint?** No — `DELETE` sigue siendo la convención REST de "quitar usuario del tenant"; comportamiento corregido.
4. **¿Alineación ADR-006?** Sí — admin tenant opera sobre membership; identidad global solo se deshabilita explícitamente.

---

## Implementación

### `DELETE /api/v1/iam/users/{id}`

```text
loadIdentityInTenant (membership exists en tenant JWT)
  → OwnershipPolicy
  → membership.deactivate()
  → membershipRepository.save()
```

**No** modifica `Identity.status`.

### `PUT /api/v1/iam/users/{id}` con `status: DISABLED`

Sigue ejecutando `identity.disable()` — offboarding global intencional.

### `DELETE /api/v1/iam/memberships/{id}`

Sin cambio — también desactiva membership (equivalente explícito).

---

## Efectos multi-tenant

| Acción | Tenant A | Tenant B (misma identity) |
|--------|----------|---------------------------|
| DELETE user en A | Membership A INACTIVE | Membership B sin cambio |
| PUT user DISABLED en A | Identity DISABLED globalmente | Login bloqueado en todos los tenants |

---

## Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `UserAdministrationUseCaseImpl.java` | DELETE → membership deactivate |
| `UserAdministrationUseCaseTest.java` | Assert membership INACTIVE, identity ACTIVE |
| `IamUserAdminControllerIT.java` | IT actualizado |

---

## Tests

- Unit: `UserAdministrationUseCaseTest.shouldDeactivateMembershipForUserInTenant`
- IT: `IamUserAdminControllerIT.shouldDeactivateUserWithDeleteEndpoint`
