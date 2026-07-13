# CodeCore — Access Architecture Review (FASE 23)

**Fecha:** 2026-07-13  
**Tipo:** Revisión arquitectónica **independiente** post-cierre (sin modificar ADRs · sin reabrir fase · sin código de corrección · sin nuevas funcionalidades)  
**Alcance:** FASE 23 — Access (`Invitation` slice) + Password Recovery track IAM (ADR-009 P1)  
**Pregunta:** ¿Access quedó como bounded context reutilizable del Core Platform (join-intent), o como contaminación de IAM / embrión de Subscription / God BC “Platform”?

**Método:** contraste código ↔ ADR-019 ↔ ADR-013 ↔ fases 16–22. Los tests verdes **no** se tomaron como prueba de arquitectura; se verificaron módulos Gradle, SQL, aggregate, application, ports y HTTP.

**Autoridad de contraste:** [CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md) · [CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-BILLING-ARCHITECTURE-REVIEW-2026-07.md) · [CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md](CODECORE-CLINICAL-RECORDS-ARCHITECTURE-REVIEW-2026-07.md) · [ADR-019](ADR-019-INVITATION-DOMAIN-MODEL.md) · [ADR-013](ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [ADR-006](ADR-006-IDENTITY-STRATEGY.md) · [ADR-009](ADR-009-PRODUCTION-READINESS-BACKLOG.md) · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md) · PASO 23.0 → 23.8

---

## Executive Summary

**Sí — Access (slice Invitation) es Core Platform, no un rewrite de IAM y no Subscription.**  
`Invitation` permanece *intentionally small* en código: intención de Membership por email, lifecycle `PENDING` → `ACCEPTED`|`REVOKED`|`EXPIRED`, schema `access` **sin FK** a `iam.*`, consumo de IAM solo vía interfaces `com.codecore.iam.contract.*` + `TenantAccessProvisionPort` en accept, publicación de `InvitationReferencePort.existsPendingByIdAndTenant`, y **cero** rastros de Organization/Office/StaffAssignment/Subscription/Notification/Event Bus en el modelo.

Password Recovery vive en `com.codecore.iam` (V32 + `/api/v1/auth/*`) — **sin** mezcla con Invitation.

La deuda **real** no está en el aggregate ni en el ADR: está en **empaquetado Gradle** (`access-application` depende del módulo IAM completo porque no existe `iam-contract` como artefacto separado) y en el **port dual** de Membership (Org transitorio vs IAM contract). Eso es deuda de plataforma conocida/ ampliada — **no** un error de frontera de Invitation que exija reabrir ADR-019.

> **Veredicto operativo: A) Continuar con la siguiente fase sin cambios en Access / Invitation.**

---

## Puntuación (0–10)

| Dimensión | Nota | Comentario |
|-----------|------|------------|
| Bounded Context | **9.5** | Access ≠ IAM ownership; umbrella Platform no contaminó el schema |
| DDD | **9.5** | Root correcto; permanence ADR-019 §3; tests negativos de superficie |
| ReferencePorts | **9** | SQL solo `access.*`; publish boolean; consume contracts — packaging Gradle flojo |
| Reutilización | **9** | Invite tenant-scoped + system roles; cualquier vertical SaaS |
| Core Platform | **9.5** | Multi-user onboarding sin verticalizar ni mezclar seats |
| Escalabilidad | **9** | Subscription/Staff/Audit crecen **alrededor** de MembershipId |
| Evolución futura | **9** | ADR-019 freeze aguanta seats/org-scope/custom roles como *nuevos* ADRs |
| Consistencia | **8.5** | HTTP/closeout espejo Payment; delta: app→IAM full vs Payment→billing-contract |
| Documentación | **9** | ADR + PASOs + guía + closeout alineados al código auditado |
| **Global FASE 23 Access** | **9.1** | Modelo excelente; packaging IAM un punto por debajo de Payments (9.2) |

Comparado con Payments 22 (**9.2**): Access **iguala** la disciplina de dominio/schema y **cede** ligeramente en higiene de dependencias Gradle hacia el provider (IAM monolito vs `*-contract` módulo).

---

## Veredicto final

| Pregunta | Respuesta |
|----------|-----------|
| ¿Reabrir FASE 23 / slice Invitation? | **No** |
| ¿Modificar ADR-019? | **No** |
| ¿Puede planificarse Subscription / Stock? | **Sí** |
| ¿Deuda P0 en Access? | **Ninguna** |
| ¿Deuda P1 de modelo Invitation? | **Ninguna** |
| Opción | **A) Continuar sin cambios en Invitation** |

---

## 1. Bounded Context

### ¿Access merece ser un BC?

**Sí.** Invitation no es Membership (pertenencia efectiva) ni Identity (persona autenticable). Es el **join-intent** — exactamente el hueco que ADR-006 anticipó (“Invite email → create membership”) sin contaminar los aggregates foundation.

| Evidencia en código | Lectura |
|---------------------|---------|
| Schema `access` · `access-management` · `/api/v1/access/invitations` | Límite modular claro |
| V30: comentario explícito *No FK to iam.** | Desacoplamiento de schema lifecycle |
| IAM sigue dueño de Identity/Membership/Role | Access **orquesta**, no posee |
| FASE 23 = umbrella; Access = BC del slice | “Platform Services” no tiene schema catch-all |

### Contaminación IAM / Organization / Subscription

| Riesgo | ¿Presente en Access main? | Evidencia |
|--------|---------------------------|-----------|
| Ownership Membership en Invitation | **No** | `accept` setea solo `resultingMembershipId`; provisión en `TenantAccessProvisionAdapter` (IAM) |
| SQL `iam.*` / `org.*` desde access-* | **No** | Grep production: limpio; adapters solo `access.invitation` |
| `OrganizationId` / `OfficeId` / StaffAssignment | **No** | Aggregate + negative tests |
| Plan / Seat / SubscriptionId | **No** | Permission catalog + domain API |
| PasswordReset dentro de Access | **No** | Vive en IAM |

**Conclusión §1:** BC correcto. El peligro futuro es **inflar Access** (seats, org-invite, notification inbox) o **meter Invitation dentro de IAM** “para simplificar” — no el diseño actual.

---

## 2. Aggregate `Invitation`

**Sigue siendo intentionally small.** Superficie real (`Invitation.java`):

- Identidad: `InvitationId`, `TenantId` (inmutable)
- Destino: `invitedEmail`, `invitedRoleCode` (≠ OWNER)
- Actor: `invitedByMembershipId`
- Secreto: `tokenHash` + `expiresAt`
- Lifecycle: `create` → PENDING → `accept` | `revoke` | `expire`
- Trazas: `resultingMembershipId?`, `acceptedAt?`, `revokedAt?`
- Métodos públicos de mutación: **solo** `accept` / `revoke` / `expire`

| Pregunta | Respuesta |
|----------|-----------|
| ¿Intentionally small? | **Sí** — alineado ADR-019 §3 |
| ¿God Aggregate? | **No** — `InvitationNegativeApiTest` niega assignOffice/subscription/password/email/OWNER |
| ¿Lógica que debería estar en Application? | Validaciones cross-BC (membership ACTIVE, email duplicado, role exists, provision) **ya** están en application — correcto |
| ¿El aggregate “conoce” IAM? | **No** — solo VOs Access (`MembershipId` local) |

**Conclusión §2:** No reabrir. No mover responsabilidades al aggregate.

---

## 3. ReferencePorts (ADR-013)

### Consumo (Access → IAM)

| Port | Uso | ¿Suficiente v1? |
|------|-----|-----------------|
| `IamMembershipReferencePort.existsActiveByIdAndTenant` | Inviter en create | ✅ |
| `IamActiveMembershipByEmailPort.existsActiveByEmailAndTenant` | Guard create/accept | ✅ |
| `IamSystemRoleReferencePort.existsSystemRoleByCodeAndTenant` | Role allow-list | ✅ |
| `TenantAccessProvisionPort.provision` | Accept (command — no ReferencePort) | ✅ |

### Publicación

| Port | Forma |
|------|-------|
| `InvitationReferencePort.existsPendingByIdAndTenant` | Boolean-first (ADR-013) |

| Prohibido verificado | Estado |
|----------------------|--------|
| `MembershipRepository` / Identity repos en access-application main | **Ausente** |
| SQL `FROM iam.` en access-* main | **Ausente** |
| FK `access.invitation` → `iam.*` (V30) | **Ausente** |
| Revalidación IAM en `revoke` | **Omitida** — correcto (espejo void Payment) |

### Acoplamiento tipado

Application mapea VOs Access → VOs IAM al llamar ports — espejo Payment→Billing. Esperado bajo `api(domain)` / VO-por-BC.

### Deuda de empaquetado (no de puerto)

Los ports viven en paquetes `com.codecore.iam.contract.*` **dentro** del jar `identity-access-management`. No existe módulo Gradle `iam-contract`. Por eso `access-application` declara:

```kotlin
implementation(projects.modules.identityAccessManagement)  // full module
```

mientras Payment declara solo `billingContract`.

**En runtime Access solo usa contratos.** En **classpath** puede ver repos/HTTP IAM — riesgo de acoplamiento accidental futuro, no una violación actual en el código auditado.

**Conclusión §3:** ADR-013 respetado a nivel de diseño/SQL. Extracción de `iam-contract` como módulo = higiene plataforma (ver §9 P1) — **no** reopen de Invitation.

---

## 4. Integración con IAM — ¿quién posee Membership?

| Rol | Dueño |
|-----|-------|
| Identity / Membership / Role / Credential | **IAM** |
| Invitation (join-intent) | **Access** |
| Provisión al accept | **IAM** vía `TenantAccessProvisionAdapter` |

Secuencia accept (application Access):

1. Resolve Invitation por token hash · guard PENDING · expire-if-needed  
2. `TenantAccessProvisionPort.provision(...)` → MembershipId  
3. `invitation.accept(now, membershipId)` · persist  

El adapter IAM usa `IdentityRepository`, `MembershipRepository`, `RoleRepository`, `IdentityRegistrationOrchestrator` **dentro de IAM** — ownership correcto.

**Acoplamiento peligroso encontrado:** ninguno en el flujo de dominio. El único riesgo estructural es el classpath Gradle (§3).

**Dual port legacy:** Organization aún usa `organization.application.port.out.MembershipReferencePort` (transitorio ADR-013) con SQL a `iam.identity_tenant_membership`, en paralelo a `IamMembershipReferencePort`. Access hizo lo correcto; Org **no** migró. Deuda preexistente **amplificada** por FASE 23 (ahora hay dos puertas).

---

## 5. Password Recovery

| Pregunta | Evidencia |
|----------|-----------|
| ¿Pertenece a IAM? | **Sí** — `PasswordResetRequest`, V32 `iam.password_reset_request`, `/api/v1/auth/forgot-password` · `/reset-password` |
| ¿Mezcla con Invitation? | **No** — cero imports cruzados Access ↔ PasswordReset |
| ¿Tipos de token compartidos? | **No** — `InvitationTokenHash` / hasher Access vs `ResetTokenHash` / hasher IAM (duplicación consciente P2, no acoplamiento) |
| ¿ADR-009? | P1 Password Recovery marcado **Done** |

Correcto mantenerlo fuera de Access: reset de credencial ≠ join a tenant.

---

## 6. API — consistencia con la cadena Core

| BC | Patrón | Access |
|----|--------|--------|
| Path | `/api/v1/{bc}/…` | `/api/v1/access/invitations` ✅ |
| Soft transitions | `POST …/{id}/{verb}` | `revoke` ✅ · accept público por token ✅ |
| Sin PUT contenido inmutable | Payment/Invoice | ✅ |
| Sin DELETE físico | cadena | ✅ |
| Permisos `resource:action` | cadena | `invitation:create\|read\|revoke` ✅ |
| OpenAPI group | `*-administration` | `access-administration` ✅ |
| Exception handler bodies vacíos | Payment/Invoice | **Mismo P2 plataforma** |
| `tenantId` en response | Invoice/Payment | Presente — alineado |
| Default list status = cola vigente | Invoice DRAFT / Payment RECORDED | `PENDING` ✅ |

**Delta legítimo:** accept `permitAll` (poseedor del token aún no es Membership). No es inconsistencia; es requisito de onboarding.

---

## 7. Consistencia global vs FASE 16–22

| Convención | ¿Access la rompe? |
|------------|-------------------|
| Modular monolith 4 capas + contract | **No** |
| Schema dedicado sin FK cross-BC | **No** — ejemplar |
| ADR model frozen + consumption guide | **No** |
| ReferencePort boolean closeout | **No** |
| Application → peer **solo** `*-contract` Gradle | **Sí, parcialmente** — depende de IAM monolito (Payment/Billing sí separan contract) |
| Seeds IAM `resource:*` + SystemRoleTemplate | **No** (ALL 52→55) |
| Naming `*PermissionCatalog` / VerificationIT 8 | **No** |

Ninguna excepción de modelo injustificada. La única desviación estructural es packaging IAM.

---

## 8. Evolución futura — ¿soporta sin reabrir ADR-019?

| Necesidad futura | ¿Sin reabrir ADR-019? | Cómo |
|------------------|----------------------|------|
| **Subscription / seats** | **Sí** | Contar Membership ACTIVE; policy port opcional en *application* Access más adelante = **nuevo** ADR si se congela en Core |
| Seat check al invitar | **Sí** (diferido) | No meter `seatLimit` en Invitation; policy Subscription |
| **StaffAssignment** post-accept | **Sí** | Usa `MembershipId` resultante · Org API |
| **Audit** | **Sí** | Consume hechos InvitationId / accept |
| **Notifications** richer | **Sí** | Sustituir `SendInvitationEmailPort` adapter — no el aggregate |
| Org-scoped invite | **Nuevo ADR** | Frontera distinta — freeze §8 lo dice |
| Custom roles en invite | **Nuevo ADR** | Allow-list system congelada |
| Meter PasswordReset en Access | **Violación** | No hacerlo |

**Conclusión §8:** Access **sí** aguanta la evolución prevista **alrededor** de Membership + InvitationId. Reabrir ADR-019 solo si se cambia la one-sentence o se embebe Org/Subscription.

---

## 9. Riesgos (solo reales)

### P0 — Crítica

**Ninguna.**

### P1 — Alta (plataforma / higiene — no modelo Invitation)

| Ítem | Evidencia | ¿Bloquea siguiente fase? |
|------|-----------|--------------------------|
| `access-application` → IAM **full** module (sin `iam-contract` Gradle) | `access-application/build.gradle.kts` | **No** — corregir al tocar IAM/Subscription packaging |
| Dual `MembershipReferencePort` (Org transitional + `IamMembershipReferencePort`) | org-application vs iam.contract | **No** — migrar Org cuando se extraiga `iam-contract` |

Estos P1 **no** son defectos del aggregate ni violaciones ADR-019. **No** justifican opción B sobre Invitation.

### P2 — Baja

| Ítem | Evidencia | Acción futura |
|------|-----------|---------------|
| Email create: `.onErrorResume(ex -> Mono.empty())` | Invite persistida; mail puede fallar en silencio | Observabilidad / política fail en 24+ |
| Hasher de token duplicado Access vs IAM | Dos SHA-256 | Shared util si duele |
| `api(domain)` en access-contract | Igual Payment/Billing | Thin-contract platform-wide |
| R2DBC starter en application | Igual Payment (`TransactionalOperator`) | Quitar al tocar módulo |
| HTTP error bodies vacíos | `InvitationHttpExceptionHandler` | Política platform web |
| TX anidada provision + accept | Ambos usan `TransactionalOperator` | Monitorear; no defecto confirmado |

### P3

No material.

---

## 10. Nota arquitectónica (detalle)

| Dimensión pedida | Nota | Justificación breve |
|------------------|------|---------------------|
| Bounded Context | **9.5** | Access justificado; sin God “Platform” |
| DDD | **9.5** | Invitation delgado; ownership Membership en IAM |
| ReferencePorts | **9** | SQL/ports correctos; packaging contract incompleto |
| Reutilización | **9** | Multi-vertical sin DentalInvite |
| Core Platform | **9.5** | Onboarding SaaS horizontal |
| Escalabilidad | **9** | Cadena Membership → Staff/Subscription |
| Evolución futura | **9** | Freeze aguanta; seats fuera |
| Consistencia | **8.5** | Cadena HTTP OK; delta Gradle IAM |
| Documentación | **9** | ADR/PASO/guía/closeout coherentes con código |
| **Final** | **9.1 / 10** | |

---

## Opciones de cierre

| Opción | Criterio | ¿Aplica? |
|--------|----------|----------|
| **A) Continuar sin cambios** | Sin P0/P1 de **modelo** Invitation; BC correcto | **Sí** |
| B) Corregir antes de continuar | P1 de modelo / frontera | No — P1 es packaging IAM/Org, no Invitation |
| C) Reabrir fase | Error arquitectónico importante en Access | No |

Extraer `iam-contract` y migrar el port Org es trabajo de **plataforma / pre-Subscription**, no reopen de FASE 23.

---

## Aspectos destacados

1. **Join-intent ≠ Membership** — demostrado en aggregate + provision port.  
2. **Schema `access` sin FK a IAM** — ADR-013 mantenido bajo write path real (más duro que Payment read-only).  
3. **Password Recovery fuera de Access** — umbrella FASE 23 sin God BC.  
4. **Tests negativos de superficie** — intentionally small enforceable.  
5. **Accept público por token** — semántica correcta de onboarding.  
6. **Primer command port cross-BC serio** (`TenantAccessProvisionPort`) sin filtrar repos IAM a Access.

---

## Conclusión

FASE 23 entrega un **Bounded Context Access cerrado y audit-ready**: Invitation *intentionally small*, desacoplado de IAM por contratos/SQL, Password Recovery correctamente en IAM, listo para Subscription/Staff/Audit **alrededor** — no dentro.

**Congelar ADR-019. No reabrir el slice Invitation.**  
**Veredicto: A) Continuar con la siguiente fase sin cambios.**

Referencias: [PASO-23.8](../audits/PASO-23.8-ACCESS-CLOSEOUT.md) · [ACCESS-CONSUMPTION-GUIDE.md](ACCESS-CONSUMPTION-GUIDE.md) · [ADR-019](ADR-019-INVITATION-DOMAIN-MODEL.md) · [ADR-009](ADR-009-PRODUCTION-READINESS-BACKLOG.md).
