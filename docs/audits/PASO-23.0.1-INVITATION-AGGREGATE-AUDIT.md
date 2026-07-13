# PASO 23.0.1 — Invitation Aggregate Audit (DDD Estratégico)

**Invitation** es la intención de otorgar Membership en un Tenant — *intentionally small*, multi-vertical, y el primer Aggregate Root del BC **Access**.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado (solo arquitectura)  
**Tipo:** Auditoría obligatoria — Aggregate Root nuevo + Bounded Context Access  
**Dependencias:** [PASO-23.0](PASO-23.0-PLATFORM-SERVICES-FOUNDATION-PLANNING.md) · ADR-003 · ADR-006 · ADR-007 · ADR-009 · ADR-013 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md)

---

## Objetivo

Definir el modelo correcto del Aggregate Root **`Invitation`** para que CodeCore, como Core Platform, permita aprovisionar acceso multi-usuario a cualquier producto SaaS — **sin** reabrir Identity/Membership, **sin** acoplar Access a Organization/StaffAssignment, **sin** mezclar Subscription/seats, y **sin** convertir Invitation en un mailer, onboarding workflow o HR module.

**Sin código. Sin tablas. Sin endpoints. Sin migraciones. Sin DTOs. Sin OpenAPI. Sin Password Recovery (track IAM aparte). Sin Subscription.**

---

## Checklist política (§8) — verdicto previo

| # | Ítem | ✓ | Nota |
|---|------|---|------|
| 1 | Aggregate Root identificado | ✅ | `Invitation` |
| 2 | Ownership definido | ✅ | BC Access |
| 3 | Invariantes definidas | ✅ | § Invariantes |
| 4 | Lifecycle definido | ✅ | § Lifecycle |
| 5 | Estados definidos | ✅ | `PENDING` · `ACCEPTED` · `REVOKED` · `EXPIRED` |
| 6 | Permisos definidos | ✅ | Borrador `invitation:*` (+ accept público por token) |
| 7 | Relaciones solo mediante IDs | ✅ | § Referencias · § Provisión IAM |
| 8 | Bounded Context correcto | ✅ | Access (≠ IAM ownership, ≠ Org, ≠ Subscription) |
| 9 | No rompe ADR vigentes | ✅ | 003/006/007/009/013–018 intactos; BCs 16–22 cerrados |
| 10 | Escalable multi-tenant | ✅ | |
| 11 | Escalable multi-organization | ✅ | Invite **tenant-scoped**; Org assignment = post-accept |
| 12 | Escalable millones de registros | ✅ | Aggregate delgado; token hash; sin hijos |

**Veredicto:** checklist en verde → listo para **ADR-019**.

---

## Decisiones irreversibles (resumen ejecutivo)

| Decisión | Elección |
|----------|----------|
| Naturaleza | **Intención de otorgar Membership** en un Tenant — no es Membership, no es Identity, no es email |
| Aggregate Root | **`Invitation`** — única representación Core de esa intención |
| BC propietario | **Access** |
| Ubicación módulo | **Opción A** — `access-management` · schema `access` · **no** embeber en módulo IAM |
| Tenant | **Siempre** — `TenantId` obligatorio e **inmutable** |
| Destinatario | **`invitedEmail`** obligatorio (VO email normalizado) |
| Identity previa | **No requerida** en create — puede o no existir; se resuelve en **accept** |
| Rol otorgado | **`invitedRoleCode`** — código de **system role** del tenant · allow-list **sin OWNER** |
| Quién invita | **`invitedByMembershipId`** obligatorio (Membership del actor autenticado) |
| Organization / Office | **Prohibidos** en Invitation v1 |
| StaffAssignment | **Prohibido** — ocurre *después*, con `MembershipId` resultante |
| Subscription / seats | **Prohibido** en el aggregate — policy futura fuera de Access |
| Token | Hash + expiración; raw token **nunca** persistido |
| Lifecycle | `PENDING` → `ACCEPTED` \| `REVOKED` \| `EXPIRED` — **sin DRAFT** · **sin un-revoke** · **sin delete físico** |
| Accept | Orquestación application: marca Invitation + **command port IAM** de provisión |
| Resend | **Nueva** Invitation (revocar o dejar expirar la anterior) — no estado `RESENT` |
| Unicidad dura | Solo **`InvitationId` (UUID)** |
| Soft-unique | Como máximo **una** `PENDING` por `(tenantId, invitedEmail)` |
| Módulo / schema / HTTP (propuesta) | `access-management` · `access` · `/api/v1/access/invitations` |
| Permisos | `invitation:create\|read\|revoke` · accept = endpoint público por token |

Borrador formal: **ADR-019 — Invitation Domain Model** — a aceptar en PASO 23.1.

---

## Respuestas directas (checklist del paso)

| # | Pregunta | Respuesta |
|---|----------|-----------|
| 1 | ¿Responsabilidad exacta? | Representar y resolver la **intención de unirse a un Tenant** (invitar → aceptar/revocar/expirar) |
| 2 | ¿One-sentence? | Ver §1 |
| 3 | ¿Lifecycle? | Ver § Lifecycle |
| 4 | ¿Estados reales? | `PENDING`, `ACCEPTED`, `REVOKED`, `EXPIRED` |
| 5 | ¿Invariantes del Aggregate? | Ver § Invariantes |
| 6 | ¿Qué NO pertenece? | Ver § Fuera |
| 7 | ¿Qué consume de IAM? | Lecturas + **comando de provisión** vía `iam-contract` — § IAM |
| 8 | ¿Cómo se crea Membership al aceptar? | Application orquesta `Invitation.accept` + `TenantAccessProvisionPort` — § Accept |
| 9 | ¿Debe existir Identity antes? | **No** en create; en accept se **reutiliza o crea** |
| 10 | ¿Evitar StaffAssignment? | Sin `OrganizationId`/`OfficeId` en Invitation; post-accept en Org |
| 11 | ¿Evitar Subscription? | Sin seats/plan/trial en modelo; sin port Subscription en v1 |
| 12 | ¿Evitar God BC Access? | Solo Invitation en v1; PasswordReset/Subscription/Notifications fuera |
| 13 | ¿ReferencePorts? | Consumo IAM (lectura) + publish Invitation en closeout |
| 14 | ¿Publicará InvitationReferencePort? | **Sí** en 23.8 — boolean mínimo |
| 15 | ¿VOs importantes? | Ver § Value Objects |
| 16 | ¿Modelo para ADR-019? | Ver § Prep. ADR-019 |
| 17 | ¿Sobreingeniería? | Ver § Riesgos |
| 18 | ¿Corto de más? | Ver § Riesgos |
| 19 | ¿10+ años? | **Sí**, si permanece *intentionally small* |
| 20 | ¿Pruebas negativas? | Ver § Tests negativos |

---

## 1. Naturaleza del Aggregate

### ¿Qué representa Invitation?

**Invitation es la intención pendiente (y su resolución) de que una persona identificada por email obtenga Membership en un Tenant, con un rol de sistema acordado, bajo quien invita.**

| Interpretación | ¿Es? | Por qué |
|----------------|------|---------|
| ¿El permiso/intent de unirse al tenant? | **Sí** | Definición operativa |
| ¿El Membership? | **No** | IAM posee pertenencia |
| ¿La Identity global? | **No** | Puede provocar su creación vía IAM |
| ¿StaffAssignment / locus org? | **No** | Organization — después |
| ¿El envío de email / plantilla? | **No** | Outbound port / adapter |
| ¿Password reset? | **No** | IAM `PasswordResetRequest` |
| ¿Seat / plan SaaS? | **No** | Subscription |
| ¿Algo dental / ERP específico? | **No** | Core Platform |

**Regla de exclusividad:** dentro del Tenant, la **intención de otorgar Membership vía invite** vive en Access como `Invitation`. Ningún vertical debe crear un “DentalInvite” / “ClinicStaffInvite” paralelo en el Core — los verticales **referencian** `InvitationId` o usan la API Access.

**Intentionally small:** Invitation no crece con offices, suscripciones, notificaciones inbox, HR profiles, ni magic-link login genérico.

### One-sentence rule

> **Invitation** = la intención de otorgar Membership en un Tenant a un destinatario identificado por email.

Si la frase necesita añadir “y asignarlo a un Office / y cobrar un seat / y verificar el email de marketing / y resetear password”, el modelo ya está engordando.

---

## 2. Aggregate Root

### ¿Por qué Invitation debe ser Aggregate Root?

- Boundary transaccional del **invite lifecycle** (crear, aceptar, revocar, expirar).  
- Ciclo de vida propio e independiente de Membership (Membership puede existir sin Invitation — admin create FASE 15).  
- `InvitationId` estable para audit, correlacionar emails, admin UI, packs.  
- Sus invariantes **no** incluyen consistencia de seats ni de StaffAssignment (política §5).

### Principio de permanencia (para ADR-019)

> **Invitation is intentionally small.**

Decisión **permanente** (no limitación de FASE 23): Invitation solo representa la intención de acceso + invariantes propias. Membership/Identity ownership, StaffAssignment, Subscription, Notifications y PasswordReset viven **fuera**. Embebidos en Invitation = violación (**God Aggregate**).

### ¿Por qué no Membership “con estado INVITED”?

Contaminaría ADR-006. Membership es pertenencia **efectiva** (ACTIVE/INACTIVE…). Un invite pendiente **no** es pertenencia. Mezclarlos obliga a half-memberships y rompe StaffAssignment/RBAC que asumen Membership real.

### ¿Por qué no Identity?

Identity es global y autenticable (ADR-006). Invitation es **tenant join intent**. Una misma Identity puede recibir invites a muchos tenants.

### ¿Por qué no Notification / OnboardingCase?

Entrega y workflow ≠ intención de acceso.

---

## 3. Ownership

| Rol | Actor / BC |
|-----|------------|
| **Propietario del modelo** | Bounded Context **Access** (`access-management`) |
| **Quién crea** | Membership con `invitation:create` |
| **Quién lee (admin)** | `invitation:read` |
| **Quién revoca** | `invitation:revoke` |
| **Quién acepta** | Poseedor del **raw token** (público autenticado por secreto, no por RBAC de tenant) |
| **Quién NO es dueño** | IAM (posee Identity/Membership), Organization, Billing, Payments, Subscription |

**Borrador permisos (23.5):**  
`invitation:read` · `invitation:create` · `invitation:revoke`

**No** `invitation:accept` como permiso RBAC de tenant — accept es capacidad **pública por token** (espejo password-reset complete).  
**No** verbos verticales (`invitation:dental-staff`, `invitation:hire`).  
**No** `invitation:assign-office` / `invitation:subscribe`.

---

## 4. Bounded Context · Opción A (congelada)

| Pregunta | Respuesta |
|----------|-----------|
| ¿BC? | **Access** |
| ¿Parte de IAM? | **No como ownership** — Access **orquesta** provisión vía contract |
| ¿Parte de Organization? | **No** |
| ¿Parte de Subscription? | **No** |
| ¿Módulo Gradle? | `modules/access-management/` (domain · application · infrastructure · contract) |
| ¿Schema? | `access` — **no** meter invitations en `org` / clínicos / `billing` |

### Por qué Opción A (BC separado) y no Opción B (dentro de IAM)

| Criterio | Opción A (elegida) | Opción B (descartada para v1) |
|----------|--------------------|-------------------------------|
| IAM foundation “cerrada” | Se respeta; IAM publica contratos | Engorda el módulo foundation |
| ADR-013 / `iam-contract` | **Justifica** extracción real | Sigue ocultando puertos |
| God-BC “Platform” | Access solo Invitation | IAM + invite + recovery → mega-módulo |
| Accept write path | Command port explícito | Más simple, peor frontera |

**Opción B queda documentada como no elegida** — no híbrido. Si en implementación el command port se vuelve absurdamente costoso, haría falta **nuevo audit + ADR**; no se “mete Invitation en IAM” en silencio.

```text
iam.*     → Identity / Membership / Role / PasswordResetRequest   FOUNDATION
access.*  → Invitation                                            FASE 23
org.*     → Organization / Office / StaffAssignment               CLOSED
```

Access v1 **no** añade PasswordReset ni Subscription al schema `access`.

---

## 5. Tenant

| Pregunta | Respuesta |
|----------|-----------|
| ¿Invitation pertenece al Tenant? | **Sí** — el tenant **destino** del Membership |
| ¿Siempre? | **Sí** |
| ¿Puede cambiar de Tenant? | **Nunca** |

`TenantId` se fija en `create` y es inmutable. Cross-tenant → 404 en admin; accept valida que el token pertenece a esa Invitation (tenant implícito en el registro).

---

## 6. Destinatario · Identity · Membership (cerrado aquí)

### Problema

Exigir Identity previa **verticaliza el onboarding** hacia “admin crea usuario a mano primero”. Crear siempre Identity en `create` **sin password** inventa cuentas zombie. Ignorar Membership ACTIVE existente genera invites inútiles o dobles.

### Decisiones

| Momento | Regla |
|---------|-------|
| **create** | Requiere `invitedEmail` válido. **No** exige Identity. **Rechaza** si ya existe Membership **ACTIVE** para ese email en el tenant (vía port IAM). |
| **create** | Soft-unique: no segunda Invitation `PENDING` para mismo `(tenantId, invitedEmail)`. |
| **accept** | Resuelve Identity por email: **si existe** → proveer Membership sobre ella; **si no** → IAM crea Identity + Credential (password aportado en accept) + Membership. |
| **accept** | **Rechaza** si ya hay Membership ACTIVE para esa Identity/email en el tenant (carrera / invite obsoleto). |

### ¿Debe existir Identity antes?

**No.**  
Identity-before-invite es un *modo* que el admin puede lograr creando Membership a mano (FASE 15); no es invariante de Invitation.

### Resultado tras accept

| Campo | ¿En aggregate? | Nota |
|-------|----------------|------|
| `resultingMembershipId` | **Sí — set-once** al aceptar | Trazabilidad; no implica ownership de Membership |
| `IdentityId` del aceptante | **No obligatorio en v1** | Recuperable vía Membership; evitar denormalizar de más |

---

## 7. Rol otorgado

### Decisión

Invitation lleva **`invitedRoleCode`** (VO): código de **system role** del tenant.

| Permitido en v1 (allow-list) | ¿Invite? |
|------------------------------|----------|
| `ADMIN` | Sí |
| `MANAGER` | Sí |
| `USER` | Sí |
| `READ_ONLY` | Sí |
| `OWNER` | **No** — OWNER solo bootstrap / proceso IAM excepcional |

**Por qué code y no `RoleId` UUID:** los system roles se provisionan por tenant; el **código** es estable en el lenguaje de plataforma (ADR-007). El resolve a `RoleId` ocurre en IAM al proveer.

**Validación en create:** port IAM confirma que el role code existe como system role en el tenant (tras provisionamiento de tenant).  
**Invitation no embebe** el aggregate Role ni la matriz de permisos.

**Custom roles (no system):** **fuera de v1** — evita Access → RBAC designer. Futuro solo con audit+ADR.

---

## 8. Quién invita

| Campo | Regla |
|-------|-------|
| `invitedByMembershipId` | **Obligatorio** · Membership del actor autenticado en el **mismo** tenant |
| IdentityId del invitador | **No** como campo primario — el actor operativo es Membership |

No se revalida “Membership sigue ACTIVE” en `revoke`/`expire` (espejo void Payment). Sí se exige ACTIVE al **create**.

---

## 9. Token · seguridad

| Elemento | Regla |
|----------|-------|
| Raw token | Generado en create; **devuelto una vez** al invitador/API; **nunca** persistido |
| Almacenado | `InvitationTokenHash` (hash del raw) |
| Expiración | `expiresAt` (`Instant`) — duración desde config application, no magia de dominio |
| Comparación | Constant-time / equals de hash en application |
| Accept | Requiere raw token + (si Identity nueva) password que cumpla política IAM |

Invitation **no** implementa SMTP. Tras create, application llama outbound `SendInvitationEmailPort` (best-effort / fail policy en 23.5.1 — no parte del modelo de dominio).

---

## 10. Lifecycle

| Estado | Significado |
|--------|-------------|
| `PENDING` | Invite vigente; aún no aceptada |
| `ACCEPTED` | Destinatario aceptó; Membership provisionado; terminal |
| `REVOKED` | Anulada por admin antes de accept; terminal |
| `EXPIRED` | Venció `expiresAt` sin accept; terminal |

```text
(create) → PENDING
              ├── accept  → ACCEPTED   (solo si PENDING ∧ not past expiresAt ∧ token ok)
              ├── revoke  → REVOKED    (solo desde PENDING)
              └── expire  → EXPIRED    (PENDING ∧ now ≥ expiresAt; en accept attempt o job)

ACCEPTED / REVOKED / EXPIRED → terminal (sin un-revoke / un-expire / re-accept en v1)
```

| Operación | ¿Mutación de contenido? |
|-----------|-------------------------|
| create | Entra en `PENDING` con email, role, token hash, expiresAt, invitedBy |
| accept | Solo transición + `resultingMembershipId` (+ timestamps) |
| revoke | Solo transición |
| expire | Solo transición |
| update email/role | **Prohibido** — revocar + nueva Invitation |
| delete físico | **Prohibido** |
| resend | **Nueva** Invitation (recomendación: revoke previa PENDING) |

**¿Por qué EXPIRED es estado y no solo “PENDING viejo”?**  
Clarifica consultas admin, ReferencePort (`existsPending…` = false), y evita re-accept accidental tras expiry sin marcar el hecho.

---

## 11. Invariantes (normativas del Aggregate)

1. Exactamente un `TenantId`, **inmutable**.  
2. Status ∈ {`PENDING`, `ACCEPTED`, `REVOKED`, `EXPIRED`}.  
3. `invitedEmail` siempre presente, normalizado, no blank.  
4. `invitedRoleCode` ∈ allow-list system (**≠ OWNER**).  
5. `invitedByMembershipId` siempre presente.  
6. `tokenHash` + `expiresAt` siempre presentes desde create.  
7. Contenido de negocio (email, role, token, invitedBy, tenant) **inmutable** tras create.  
8. Solo `PENDING` puede pasar a `ACCEPTED` / `REVOKED` / `EXPIRED`.  
9. `accept` exige `now < expiresAt`; si no → `expire` (o rechazo equivalente) — no accept tardío.  
10. `resultingMembershipId` solo se setea en `ACCEPTED`, una vez.  
11. Invitation **no** embebe Identity, Membership, Role, Organization, Office, Subscription, Patient.  
12. Cross-tenant imposible.  
13. Única “invite-to-tenant” role en Core — no “DentalInvitation” paralelo.

**Invariantes de application (no del aggregate puro, pero normativas FASE 23):**

- create: invitador Membership ACTIVE en tenant.  
- create: no Membership ACTIVE para `invitedEmail` en tenant.  
- create: no otra Invitation `PENDING` mismo email+tenant.  
- create: role code resoluble en tenant.  
- accept: token raw valida hash; provisión IAM atómica respecto al mark ACCEPTED (ver § Accept).

---

## 12. Qué NO pertenece a Invitation

| Concepto | Por qué |
|----------|---------|
| Ownership de Identity / Membership / Role | IAM |
| PasswordResetRequest | IAM track |
| StaffAssignment / Office / OrganizationId | Organization — post-accept |
| Seat count / Plan / Trial / SubscriptionId | Subscription BC |
| SMTP / templates / provider SDKs | Infrastructure |
| Notification inbox / preferences | Futuro / producto |
| Email verification marketing | Identity / growth — no join intent |
| Patient / Encounter / Invoice / Payment | Dominios cerrados |
| Audit trail estructurado | FASE 24 |
| Magic-link login sin Membership | Auth abuse — fuera |
| Multi-role / custom role designer | RBAC — fuera v1 |
| Invite a Organization como locus | Diferido; v1 = Tenant |

---

## 13. Integración IAM — consumo exacto

### Lectura (`iam-contract` — Reference / query ports)

| Capacidad | Uso |
|-----------|-----|
| ¿Membership ACTIVE para email/identity en tenant? | create + accept guards |
| ¿Identity por email? | accept path (reuse vs create) |
| ¿System role code existe en tenant? | create |
| ¿Membership ACTIVE del invitador? | create |

Forma exacta de métodos → **PASO 23.2** (boolean-first ADR-013).  
Migrar `MembershipReferencePort` transitorio (org-application) hacia `iam-contract` es **parte de readiness**, no reopen de Org.

### Escritura (command port — **no** es ReferencePort)

| Port (nombre de planificación) | Responsabilidad |
|--------------------------------|-----------------|
| **`TenantAccessProvisionPort`** (nombre final en 23.2) | Dado tenant + email + roleCode + password? → asegura Identity + Membership ACTIVE + assignment del role system · retorna `MembershipId` |

**Invitation domain no llama repositorios IAM.**  
Solo application Access, tras validar token/estado, invoca el command port y luego (o en la misma unidad de trabajo de application) persiste `Invitation.accept(...)`.

### Cómo se crea Membership al aceptar (secuencia normativa)

```text
1. Load Invitation by id (or by token hash lookup)
2. Verify PENDING ∧ token ∧ now < expiresAt
3. Call TenantAccessProvisionPort.provision(...)
      - Identity exists? link : create Identity+Credential
      - Create Membership ACTIVE in tenant
      - Assign invitedRoleCode (system role)
      - Fail if ACTIVE membership already exists
4. invitation.accept(now, membershipId) → ACCEPTED
5. Persist Invitation
```

**Consistencia:** application debe tratar 3+4 como **fallar juntos** (transacción o compensación documentada). Preferencia: transacción que abarque persistencia Access + efectos IAM **si** el modular monolith lo permite vía un solo `TransactionalOperator` y adapters; si no, **saga corta** (provision → accept; si accept falla → compensar Membership) — detalle de implementación en 23.6, **no** engordar el aggregate.

**Invitation no marca Identity ACTIVE/DISABLED** más allá de lo que el port de provisión ya haga según ADR-006.

---

## 14. Evitar acoplamiento StaffAssignment

| Defensa | Cómo |
|---------|------|
| Sin `OrganizationId` / `OfficeId` en Invitation | Congelado |
| Sin `StaffAssignmentId` | Congelado |
| Accept solo produce `MembershipId` | StaffAssignment se crea **después** en Organization API |
| Tests negativos | `assignOffice`, `createStaffAssignment` ausentes en domain API |
| Guía de consumo | “After accept → optional StaffAssignment with MembershipId” |

---

## 15. Evitar acoplamiento Subscription

| Defensa | Cómo |
|---------|------|
| Sin `PlanId` / `SubscriptionId` / `seatLimit` | Congelado |
| Sin llamada a Subscription port en v1 | No inventar seat-check “por si acaso” |
| Seats futuros | Subscription BC consulta Membership ACTIVE; puede **después** publicar policy port que Access application consulte — **nuevo audit**, no campo en Invitation |
| Tests negativos | `reserveSeat`, `attachPlan`, `startTrial` ausentes |

Si un producto necesita “no invitar si no hay seats”, eso es **policy de producto/Subscription**, no responsabilidad del aggregate Invitation.

---

## 16. Evitar God BC Access

| Tentación | Respuesta |
|-----------|-----------|
| Meter PasswordReset en `access` | Track IAM — aggregate ya existe |
| Meter Notification aggregate | Port de envío solamente |
| Meter Subscription | Track C / BC propio + ADR-020 |
| Meter “UserProfile” / HR | Pack / otro BC |
| Schema `platform` catch-all | Prohibido — schema `access` solo Invitation v1 |
| Invite + Org + Billing en un módulo | Prohibido |

**Access v1 = Invitation y nada más.**

---

## 17. ReferencePorts

### Consume

| Port | ¿v1? |
|------|------|
| IAM membership/identity/role **read** ports (`iam-contract`) | **Sí** |
| `TenantAccessProvisionPort` (command) | **Sí** — no ReferencePort |
| `SendInvitationEmailPort` (infra) | **Sí** — no ReferencePort |
| Organization / Patient / Invoice / Payment / Item ports | **No** |
| Subscription ports | **No** |

### Publica (closeout 23.8)

| Artefacto | Forma esperada |
|-----------|----------------|
| `InvitationId` | contract/domain VO |
| `InvitationReferencePort` | p. ej. `existsPendingByIdAndTenant` — **boolean-first** |
| Guía | ACCESS-CONSUMPTION-GUIDE (nombre final en 23.8) |

Consumidores del port: Audit FASE 24, admin cross-check, packs; StaffAssignment **no** necesita el port (usa MembershipId).

---

## 18. Value Objects importantes

| VO | Rol |
|----|-----|
| `InvitationId` | Identidad dura |
| `TenantId` | Aislamiento |
| `EmailAddress` / `InvitedEmail` | Destinatario normalizado |
| `InvitationStatus` | PENDING / ACCEPTED / REVOKED / EXPIRED |
| `InvitationRoleCode` | System role allow-list |
| `MembershipId` | `invitedByMembershipId` · `resultingMembershipId` |
| `InvitationTokenHash` | Secreto almacenado |
| `InvitationExpiration` / `expiresAt` | Ventana de validez |

**No VO en v1:** OrganizationId, OfficeId, PlanId, NotificationId, raw token persistido.

---

## 19. Modelo exacto que congelará ADR-019

### Definición

Invitation es la intención de otorgar Membership en un Tenant a un email, con un system role permitido, emitida por un Membership invitador.

### Permanence

*Invitation is intentionally small.*

### Campos lógicos (dominio)

| Campo | Cardinalidad |
|-------|--------------|
| `id` | 1 |
| `tenantId` | 1 · inmutable |
| `invitedEmail` | 1 · inmutable |
| `invitedRoleCode` | 1 · inmutable · ≠ OWNER |
| `invitedByMembershipId` | 1 · inmutable |
| `tokenHash` | 1 · inmutable |
| `expiresAt` | 1 · inmutable |
| `status` | 1 |
| `resultingMembershipId` | 0..1 · solo ACCEPTED |
| `createdAt` / `updatedAt` | auditoría técnica |
| `acceptedAt` / `revokedAt` | opcionales según transición |

### Lifecycle

`PENDING` → `ACCEPTED` | `REVOKED` | `EXPIRED`

### Integración

- Lectura IAM + `TenantAccessProvisionPort` en accept.  
- Sin Organization/Subscription en el aggregate.

### Freeze rule

Cambios de frontera (añadir Org-scope, seats, custom roles, embeber Membership, meter PasswordReset) requieren **nuevo ADR**.

### Permisos

`invitation:create|read|revoke` (+ accept público por token)

### Módulo

`access-management` · schema `access` · HTTP `/api/v1/access/invitations` (shape exacto en 23.5.1)

---

## 20. Riesgos

### Sobreingeniería (evitar)

| Riesgo | Mitigación |
|--------|------------|
| Notification BC / outbox enterprise en v1 | Un port de email mínimo |
| Seat reservation en invite | Diferir a Subscription |
| Org-scoped invite + StaffAssignment atómico | Diferir; post-accept |
| Custom role invite | Allow-list system only |
| Identity must exist + email verification chain | No requerir Identity en create |
| Saga distribuida con Event Bus | Modular monolith; TX/compensación corta sin bus preventivo |
| Opción A+B híbrida | Una sola: **A** |

### Corto de más (evitar)

| Riesgo | Mitigación |
|--------|------------|
| Invite sin role (siempre USER) | `invitedRoleCode` obligatorio |
| Sin `invitedBy` | Auditoría humana perdida — campo obligatorio |
| Sin EXPIRED | Ambiguity admin/port — estado explícito |
| Sin `resultingMembershipId` | Trazabilidad accept débil — set-once |
| Identity-only-if-preexists | Bloquea onboarding SaaS real — create-on-accept permitido |
| Accept sin password path para Identity nueva | Obliga cuentas sin credencial — password en accept si create Identity |

### Longevidad 10+ años

**Sí**, si:

1. One-sentence se mantiene.  
2. Membership sigue siendo el hecho de pertenencia (IAM).  
3. Organization/Subscription crecen **alrededor** de `MembershipId`, no dentro de Invitation.  
4. Token/email siguen siendo mecanismo, no el modelo.

Invitation en 2036 debería seguir leyéndose: *“alguien fue invitado a este tenant y aceptó o no”* — no *“el usuario HR del módulo dental con seat Gold en la clínica Norte”*.

---

## 21. Pruebas negativas (domain) — intentionally small

Demostrar **ausencia** de API/campos/comportamientos:

| # | Debe fallar / no existir |
|---|--------------------------|
| 1 | `assignOffice` / `assignOrganization` / `createStaffAssignment` |
| 2 | `attachSubscription` / `reserveSeat` / `startTrial` / `setPlan` |
| 3 | `setPassword` / `resetPassword` / `completePasswordReset` en Invitation |
| 4 | `sendEmail` / `retryNotification` como método de dominio |
| 5 | `addPatient` / `linkEncounter` / `addInvoice` |
| 6 | `inviteOwner` / role code `OWNER` aceptado |
| 7 | `updateEmail` / `updateRole` en PENDING (mutación de contenido) |
| 8 | `accept` desde `REVOKED` / `EXPIRED` / `ACCEPTED` |
| 9 | `unrevoke` / `reactivate` / `reopen` |
| 10 | `delete` físico |
| 11 | Campos `organizationId`, `officeId`, `planId`, `subscriptionId` |
| 12 | Persistencia de raw token en reconstitución |

---

## 22. Permisos · matriz borrador (23.5)

| Rol sistema | create | read | revoke | accept (token) |
|-------------|--------|------|--------|----------------|
| OWNER | ✅ | ✅ | ✅ | n/a (público) |
| ADMIN | ✅ | ✅ | ✅ | n/a |
| MANAGER | ✅ | ✅ | ✅ | n/a |
| USER | ❌ | ✅ (opcional: solo propias / denegar list — cerrar en 23.5) | ❌ | n/a |
| READ_ONLY | ❌ | ✅ | ❌ | n/a |

Detalle fino USER list-scope → **23.5** / **23.5.1**. No bloquea ADR-019.

---

## 23. Conformidad ADRs

| ADR | ¿Respeta? |
|-----|-----------|
| ADR-003 Multi-tenant | Sí — TenantId inmutable |
| ADR-006 Identity + Membership | Sí — Invitation no sustituye Membership; provisión vía IAM |
| ADR-007 RBAC | Sí — system role codes; sin org-scoped RBAC |
| ADR-009 | Sí — Password Recovery sigue track aparte |
| ADR-013 Reference Contracts | Sí — `iam-contract` + InvitationReferencePort closeout |
| ADR-010…018 | Sí — no reabre BCs cerrados |

---

## Prep. ADR-019

Congelar en ADR-019:

1. Definición + one-sentence + permanence (*intentionally small*).  
2. BC Access · Opción A (`access-management` / schema `access`).  
3. Campos: tenant, email, roleCode (≠ OWNER), invitedByMembershipId, tokenHash, expiresAt, status, resultingMembershipId?.  
4. Lifecycle PENDING → ACCEPTED | REVOKED | EXPIRED.  
5. Identity no requerida en create; create-or-link en accept vía `TenantAccessProvisionPort`.  
6. Prohibiciones: Org/Office/StaffAssignment/Subscription/PasswordReset/Notification aggregate.  
7. Permisos `invitation:create|read|revoke` + accept por token.  
8. Freeze rule (nuevo ADR para ampliar frontera).

---

## Checklist final

- [x] Responsabilidad y one-sentence  
- [x] Lifecycle y estados mínimos reales  
- [x] Invariantes aggregate vs application  
- [x] Opción A congelada  
- [x] Path accept → Membership sin embeber IAM  
- [x] Identity opcional en create  
- [x] Anti-StaffAssignment / anti-Subscription / anti-God-BC  
- [x] Ports de consumo y publicación  
- [x] VOs y modelo ADR-019  
- [x] Riesgos corto/largo + tests negativos  
- [x] Sin código / tablas / endpoints  

---

## Siguiente

**PASO 23.1 — Invitation Model ADR (ADR-019 Accepted).**
