# PASO 23.0 — Platform Services Foundation Planning

**Platform Services** no es un Bounded Context único: es la **fase de plataforma** que completa capacidades horizontales del Core (acceso multi-usuario, recuperación de credenciales, y más adelante el modelo comercial SaaS) — **sin** reabrir dominio clínico ni económico, y **sin** convertir “Platform” en un God BC.

**Fecha:** 2026-07-12  
**Estado:** ✅ Planificación cerrada (sin código)  
**Tipo:** Definición de FASE 23 · Umbrella Platform Services · primer BC **Access** (`Invitation`)  
**Dependencias:** FASE 16–22 cerradas · ADR-006 · ADR-007 · ADR-009 · ADR-013 · [DEVELOPMENT-POLICY-FASE-16-PLUS.md](../architecture/DEVELOPMENT-POLICY-FASE-16-PLUS.md) · [CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md](../architecture/CODECORE-PAYMENTS-ARCHITECTURE-REVIEW-2026-07.md) · [PASO-17.0 §1](PASO-17.0-CLINICAL-FOUNDATION-PLANNING.md)

---

## Quick path

1. FASE 23 = **umbrella** · primer BC nuevo = **Access** · primer Aggregate Root = **`Invitation`**  
2. **Password Recovery** = track IAM (aggregate ya existe) — **no** es el BC inaugural  
3. **Subscription** = BC **distinto**, **después** del slice Invitation — **nunca** dentro de Access  
4. Siguiente: **PASO 23.0.1** — Invitation Aggregate Audit  

**Sin código. Sin tablas. Sin endpoints. Sin DTOs. Sin Notification God BC. Sin Email Verification como root. Sin Tenant Onboarding workflow aggregate. Sin Subscription en v1.**

---

## Objetivo

1. **Desafiar** si “Platform Services” es un BC o una fase — y partirlo con rigor DDD.  
2. Elegir el **primer Bounded Context** real y el **primer Aggregate Root**.  
3. Fijar qué **no** entra (Subscription, Notifications, Audit trail, vertical packs).  
4. Definir el plan de pasos (espejo FASE 16–22).  
5. Dejar explícito cómo se conecta con IAM **sin reabrir** Identity / Membership / ADR-006–008.

---

## 0. ¿Qué es FASE 23 — y qué no es?

### Criterio de decisión (filosofía Core Platform)

Una capacidad de plataforma entra en FASE 23 solo si:

1. Es **horizontal** (cualquier producto SaaS sobre CodeCore la necesita).  
2. **No** pertenece al dominio clínico ni al arco económico ya cerrado (Org → Patient → Appointment → Encounter → Item → Invoice → Payment).  
3. Tiene **consumidores claros** (productos, admin, o BCs posteriores).  
4. No requiere inventar un God módulo “Platform” que acumule email + seats + notificaciones + onboarding + billing SaaS.

### Inventario de candidatos (validación previa)

| Candidato | ¿BC propio? | ¿Aggregate Root nuevo? | ¿En FASE 23 v1 (slice Invitation)? | Veredicto |
|-----------|-------------|------------------------|-------------------------------------|-----------|
| **Invitation** (invitar a un Tenant) | **Sí — Access** | **Sí — `Invitation`** | **Sí — inaugural** | **Elegido** |
| **Password Recovery** | **No** — ya es IAM | Ya existe `PasswordResetRequest` | Track paralelo / temprano IAM | Completar deuda ADR-009 P1 **sin** nuevo BC |
| Email verification | Posible token IAM | No ahora | No | Extensión Identity / token — después si producto lo exige |
| Notification orchestration | Posible BC futuro | **No en v1** | No | Outbound port (`SendEmail` / `Notify`) — no Aggregate |
| Platform notifications inbox | BC futuro | No | No | Producto / read-model — no Core v1 |
| **Subscription** (Plan / Seat) | **Sí — BC propio** | Sí (más adelante) | **No** | Modelo comercial SaaS — **otro** BC (política Billing ≠ Subscription) |
| Tenant onboarding workflow | No | No | No | Orquestación (bootstrap ya existe 15.9.2 + Invitation) — no root |
| Audit trail / Observability | Transversal | No | No | **FASE 24** (ADR-009 P2) |
| JWT stale / OpenAPI hardening | Transversal | No | No | **FASE 25** |

### Por qué “Platform Services” no es un Bounded Context

| Si tratáramos FASE 23 como un solo BC “Platform”… | Coste |
|--------------------------------------------------|-------|
| Invitation + PasswordReset + Subscription + Notifications en un módulo | **God Bounded Context** |
| Mezclar acceso (Membership) con seats SaaS | Contamina IAM y viola DEVELOPMENT-POLICY |
| Un schema `platform` catch-all | Pierde límites; imposible consumir por IDs/ports limpios |

**Decisión:** FASE 23 es el **nombre de fase**. Dentro de ella hay **tracks/BCs**:

```text
FASE 23 — Platform Services (umbrella)
 ├── Track A — IAM completion: Password Recovery (ADR-009 P1)     ← no BC nuevo
 ├── Track B — BC Access: Invitation slice (23.0 → 23.8)          ← inaugural
 └── Track C — BC Subscription (posterior a 23.8)                ← otro ADR / otro root
```

Stock (Inventory) sigue pudiendo avanzar **en paralelo de producto**; no forma parte de FASE 23.

### Veredicto de fase

**Sí — abrir FASE 23 ahora es correcto** para el Core Platform: el arco de negocio 16–22 está cerrado y auditado; falta el **aprovisionamiento de acceso multi-usuario** que todo SaaS necesita, más cerrar deuda P1 de password recovery.

```text
IAM → Organization → … → Payments
CLOSED                 CLOSED
  ↘
    FASE 23 Platform Services
      ├── Password Recovery (completa IAM)
      └── Access (Invitation) → (luego Subscription BC)
```

**Regla de oro FASE 23:** no modificar aggregates ni ADRs de Organization / Patient / Appointment / Encounter / Item / Invoice / Payment. Seeds IAM de permisos nuevos = cambio acotado permitido. Identity / Membership **no se rediseñan** (ADR-006/007 intactos); solo se **completan** capacidades y se **publican contratos** (`iam-contract`) donde Acceso lo exija.

---

## 1. Primer Bounded Context: Access

### Respuestas directas (checklist del paso)

| # | Pregunta | Respuesta |
|---|----------|-----------|
| 1 | ¿Cuál es el primer BC? | **Access** (aprovisionamiento de acceso a Tenant) |
| 2 | ¿Por qué antes que los demás? | Ver §1.1 |
| 3 | ¿Qué Aggregate inaugura? | **`Invitation`** |
| 4 | ¿Qué NO pertenece al Aggregate? | Ver §4 |
| 5 | ¿Quién lo consumirá? | Ver §6 |
| 6 | ¿Qué ReferencePorts consume? | Ver §5 |
| 7 | ¿Publicará ReferencePort? | **Sí en closeout** (mínimo boolean) — §6 |
| 8 | ¿Roadmap completo? | §9 |
| 9 | ¿Auditoría 23.0.1? | **Sí — obligatoria** |
| 10 | ¿ADR nuevo? | **ADR-019 — Invitation Domain Model** |

### 1.1 ¿Por qué Access / Invitation antes que el resto?

| Alternativa | ¿Ahora como primer BC? | Motivo |
|-------------|------------------------|--------|
| **Access (`Invitation`)** | **Sí** | Diferido desde FASE 17 con razón correcta (no clínica); completa la historia ADR-006 *invite → membership*; desbloquea onboarding multi-usuario de **cualquier** producto SaaS; consumidores claros (admin, packs, StaffAssignment *después* de membership). |
| Password Recovery como “BC” | **No** | Ya es aggregate IAM (`PasswordResetRequest`); ports in existen; falta persistencia/HTTP. Es **completion**, no BC nuevo. Se hace en FASE 23 como **track A**, no como root inaugural del ecosistema. |
| Subscription primero | **No** | Modelo comercial SaaS (Plan/Seat/lifecycle). Más grande, más tentación God-BC, y **no** desbloquea invitar usuarios. Política: Billing operativo ≠ Subscription. Necesita `iam-contract` limpio + reglas de seats — **después** de Access. |
| Notifications BC | **No** | Infraestructura de entrega. Invitation/PasswordReset necesitan un **port de envío**, no un aggregate Notification. |
| Email verification | **No** | Token de Identity; no prueba Access; no es el cuello de botella de onboarding multi-tenant staff. |
| Tenant onboarding AR | **No** | Workflow (bootstrap + invite + opcional subscription). Orquestación ≠ Aggregate. |
| Stock (Inventory) | Paralelo OK | No es Platform Services; no responde a ADR-009 ni a Invitations. |

**Por qué Invitation gana al “completar Password Recovery primero como narrativa de BC”:**  
Password Recovery **debe** ejecutarse en FASE 23 (deuda P1), pero **no** define un Bounded Context nuevo ni publica un contrato de plataforma comparable a `InvitationId`. Invitation es el primer **nodo nuevo** del grafo de plataforma post–FASE 15.

**Orden pragmático recomendado dentro de la fase:**

1. Arrancar **23.0.1 / ADR-019** (modelo Invitation) en paralelo con un **micro-track Password Recovery** (cerrar HTTP+DB del aggregate IAM existente) — sin mezclar modelos.  
2. Si hay que serializar por capacidad: **Password Recovery primero** (superficie ya dibujada, menor riesgo de frontera) **y luego** Invitation domain — ambos en FASE 23; el **BC inaugural nombrado** sigue siendo Access.

### Nombre del BC

| Candidato | ¿Adoptar? | Motivo |
|-----------|-----------|--------|
| **Access** | **Sí** | Lenguaje de plataforma: aprovisionar quién puede operar en un Tenant |
| Invitations | Débil como nombre de BC | Describe el *primer* aggregate; el BC puede crecer (p. ej. InviteLink / AcceptSession) |
| Platform Services | **No como BC** | Nombre de fase; God-BC si se schema-tiza |
| IAM | **No reabrir como “FASE IAM 2”** | IAM foundation cerrada; Access **crece alrededor** vía contratos |
| Identity | **No** | Identity es global (ADR-006); Invitation es *tenant join intent* |
| Onboarding | **No** | Workflow marketing/producto |

**Decisión:** el primer Bounded Context de FASE 23 se llama **Access**.  
Primer Aggregate Root: **`Invitation`**.

```text
Identity (global) + Membership (tenant belonging)  → IAM            CLOSED (foundation)
Invitation (intent to join a Tenant)               → Access         FASE 23  ← este BC
PasswordResetRequest                               → IAM            Track A (completion)
Subscription / Plan / Seat                         → Subscription   Track C (después)
```

---

## 2. Primer Aggregate Root: `Invitation`

### One-sentence rule

> **Invitation** = la intención pendiente (y su resolución) de que una identidad —existente o por crear— obtenga Membership en un Tenant bajo condiciones definidas por quien invita.

| Interpretación | ¿Es? |
|----------------|------|
| ¿El permiso de unirse al tenant vía token/aceptación? | **Sí** |
| ¿El Membership en sí? | **No** — Membership sigue siendo IAM |
| ¿La Identity global? | **No** — puede *referenciar* o provocar creación vía IAM |
| ¿StaffAssignment / dónde opera? | **No** — Organization; ocurre *después* si aplica |
| ¿El email SMTP / plantilla? | **No** — adapter / port de notificación |
| ¿El Plan SaaS / seat billing? | **No** — Subscription |
| ¿Password reset? | **No** — IAM `PasswordResetRequest` |

Patrón de frases del Core:

| Aggregate | Una frase |
|-----------|-----------|
| Identity | Quién es la persona autenticable (global) |
| Membership | Pertenencia activa de una Identity a un Tenant |
| **Invitation** | Intención de **otorgar** Membership en un Tenant |
| PasswordResetRequest | Intención de **restablecer** credencial de una Identity |

### Análisis de candidatos (uno solo gana)

| Candidato | ¿Root de Access v1? | Motivo |
|-----------|---------------------|--------|
| **`Invitation`** | **Sí** | Frontera transaccional del invite lifecycle (crear → pending → accept/revoke/expire). Universal en todo SaaS multi-tenant. Anticipado en ADR-006. |
| `InviteToken` solo | No | Token es VO/mecanismo; el root es la invitación |
| `MembershipProvision` | No | Nombre técnico; diluye el lenguaje de negocio |
| `PasswordResetRequest` | No — otro track | Ya vive en IAM |
| `Notification` | No | Entrega, no acceso |
| `Subscription` / `Seat` | No | Otro BC |
| `EmailVerification` | No | Verificación de Identity, no join a Tenant |
| `TenantOnboardingCase` | No | Saga/workflow |

**Elegido:** **`Invitation`**.

Detalle irreversible: **PASO 23.0.1** (Aggregate Audit → prep. ADR-019).

### Hipótesis a cerrar en 23.0.1 (no decidir tablas aquí)

| Tema | Trade-off |
|------|-----------|
| **Alcance** | ¿Solo Tenant-scoped (recomendado v1) o también Organization-scoped? — Org-scope empuja StaffAssignment prematuro; preferir **tenant invite** + StaffAssignment **después** |
| **Email / Identity** | Invite por email → Identity existente **o** crear Identity al aceptar — orquestación IAM |
| **Rol inicial** | ¿`roleCode` / `RoleId` en Invitation? — debe poder asignar rol de sistema al aceptar **sin** embeber Role |
| **Lifecycle** | p. ej. `PENDING → ACCEPTED \| REVOKED \| EXPIRED` — sin “re-send” como nuevo estado si basta nueva Invitation |
| **Token** | Hash + expiración (espejo PasswordReset) — nunca token en claro en DB |
| **Quién invita** | `invitedByMembershipId` / IdentityId — auditoría humana |
| **Dónde vive el módulo** | `access-management` + schema `access` **vs** aggregate en módulo IAM + schema `iam` — **cerrar en 23.0.1** con criterio: *quién es dueño de crear Membership* |
| **Escritura a IAM** | ReferencePort es **lectura**. Accept necesita **command port** IAM (`ProvisionMembership` / use case publicado) o co-locación en IAM — **decisión irreversible de 23.0.1** |

### Ubicación del módulo (pre-decisión para audit)

| Opción | Pros | Contras |
|--------|------|---------|
| **A — BC Access separado** (`access-management`, schema `access`) | Respeta “IAM foundation closed”; límites claros; fuerza `iam-contract` | Accept no es mono-aggregate; necesita port de comando IAM |
| **B — Aggregate dentro de IAM** | Accept + Membership en el mismo lenguaje; menos ceremonia | Engorda el módulo IAM; diluye el mensaje “fase Platform / BC Access” |

**Recomendación de planificación (no congelada):** **Opción A** — BC Access separado + extracción/publicación de **`iam-contract`** (lectura + comando mínimo de provisión).  
Motivo Core Platform: Architecture Review ya señaló `MembershipReferencePort` transitorio y `iam-contract` como deuda media; FASE 23 es el primer consumidor que **justifica** abrirlo.  
Si 23.0.1 demuestra que el command port crea más complejidad que valor en v1, **Opción B** queda como fallback documentado — **una** de las dos, no un híbrido.

---

## 3. Track A — Password Recovery (no es el BC inaugural)

| Hecho | Evidencia |
|-------|-----------|
| Aggregate existe | `PasswordResetRequest` en IAM domain |
| Ports in existen | `RequestPasswordResetUseCase` / `CompletePasswordResetUseCase` |
| Deuda | ADR-009 P1 — sin HTTP/DB implementation completa |
| ¿Nuevo ADR de modelo? | **No esperado** — completar contra modelo existente; solo ADR/amend si 23.0.1-PR encuentra hueco |

**En FASE 23:** cerrar persistencia + API pública anti-enumeración + port de email — **sin** mover el aggregate a Access, **sin** mezclar tokens de invite y reset.

---

## 4. Qué NO pertenece a `Invitation` (ni al BC Access v1)

| Concepto | Por qué no |
|----------|------------|
| Membership / Identity ownership | IAM — Invitation solo *provoca* provisión |
| StaffAssignment / Office | Organization — post-membership |
| Password reset / credential change | IAM `PasswordResetRequest` |
| Email templates / SMTP / provider SDK | Infrastructure adapter |
| Notification inbox / preferences | BC futuro o producto |
| Subscription / Plan / Seat / trial | Subscription BC |
| Invoice / Payment | Billing / Payments — cerrados |
| Patient / Appointment / Encounter | Dominio clínico — cerrado |
| Tenant bootstrap first OWNER | Ya resuelto 15.9.2 |
| Audit trail estructurado | FASE 24 |
| Org-scoped RBAC | ADR-007 intacto |
| “Invite to Organization” como root distinto | Diferir; v1 = Tenant |
| Magic-link login genérico | Auth — no Invitation |

**Regla:** si responde *“cómo se entrega el email / qué plan SaaS / dónde trabaja el staff / cuál es la contraseña / qué paciente atiende”* en lugar de *“hay una intención pendiente de unirse a este Tenant”*, **no** pertenece dentro de `Invitation`.

---

## 5. Qué consume (IDs + ports)

| Referencia | ¿En Invitation v1? | Validación |
|------------|--------------------|------------|
| `TenantId` | **Obligatoria** | JWT del invitador / TenantContext — tenant destino |
| Email del invitado | **Obligatoria** | VO; lookup Identity vía port IAM |
| `RoleId` o role code sistema | **Hipótesis 23.0.1** | Port IAM de rol existente / allow-list |
| `invitedBy` (MembershipId o IdentityId) | **Hipótesis 23.0.1** | Quién emitió la invite |
| `OrganizationId` / `OfficeId` | **No en v1** salvo audit lo exija | Evitar acoplar Access a Org structure |
| `PatientId` / clínicos / Invoice | **No** | |

| Port | ¿Crear / tocar? | Nota |
|------|-----------------|------|
| **`iam-contract`** (nuevo / extracción) | **Sí — readiness 23.2** | Sacar `MembershipReferencePort` del sitio transitorio; publicar lecturas Identity/Membership/Role mínimas |
| Command: provision Membership (+ Identity si falta) | **Sí — forma exacta en 23.0.1** | Sin esto Accept no puede vivir fuera de IAM |
| `OrganizationReferencePort` | **No** en v1 | |
| Email/Notification outbound | **Sí — port de infraestructura** | No es ReferencePort de BC; adapter SMTP/outbox mínimo |
| Ports clínicos / Billing / Payments | **No** | |

| Prohibido |
|-----------|
| Embed de Identity / Membership / Role aggregates |
| SQL a `iam.*` desde Access **si** se elige Opción A (solo vía contract/adapters) |
| Inventar `NotificationReferencePort` / `SubscriptionReferencePort` “por si acaso” |
| Reabrir ADR-006 para “Invitation dentro de Membership” |

---

## 6. Qué publica · consumidores futuros

| Artefacto | ¿Cuándo? | Para quién |
|-----------|----------|------------|
| `InvitationId` | Desde domain/contract | Audit, correlacionar email, admin UI, packs |
| `InvitationReferencePort` | Closeout 23.8 | p. ej. `existsPendingByIdAndTenant` — forma exacta en closeout |
| Guía de consumo | Closeout | Productos, Subscription (seats asumen membership), StaffAssignment flows |

**Consumidores claros (obligatorio por filosofía):**

| Consumidor | Uso |
|------------|-----|
| **Admin / product UIs** (todos los verticales) | Invitar usuarios al Tenant sin crear Membership a mano |
| **StaffAssignment** (Organization) | *Después* de accept: asignar dónde opera el membership nuevo — **no** dentro de Invitation |
| **Subscription** (futuro) | Cuenta seats sobre Membership ACTIVE; Invitation alimenta el funnel de alta |
| **Audit / Observability (FASE 24)** | Hechos de invite/accept/revoke |
| Password Recovery | **No consume Invitation** — track independiente |

¿ReferencePort en closeout aunque el consumidor fuerte sea UX?  
**Sí** — misma disciplina que Item/Invoice/Payment: contrato mínimo boolean para no filtrar repositorio. Si 23.8 no encuentra consumidor inmediato de lectura cross-BC, el port sigue siendo la **única** puerta publicada (anti-atajo SQL).

---

## 7. Cómo evitar God Aggregate / God BC

| Defensa | Aplicación en FASE 23 |
|---------|----------------------|
| Umbrella ≠ BC | “Platform Services” no tiene schema catch-all |
| One-sentence Invitation | Solo intención de join a Tenant |
| PasswordReset fuera de Access | Track IAM |
| Subscription fuera | Track C / BC propio + ADR propio |
| Notifications = port | Sin aggregate Notification en v1 |
| Sin Org-scope invite v1 | Evita mezclar Access + StaffAssignment |
| Tests negativos | Negar `assignOffice`, `createSubscription`, `sendSmsCampaign`, `addPatient` en domain tests |
| Permanencia ADR | *Invitation is intentionally small* (congelar en ADR-019) |

---

## 8. Cómo fortalece el Core Platform

| Señal de plataforma | Cómo lo aporta Invitation |
|---------------------|---------------------------|
| SaaS multi-usuario real | Tras bootstrap OWNER, el Tenant puede crecer sin scripts |
| Cierra deuda estratégica FASE 17 | Invitations diferidas con razón; ahora toca |
| Fuerza `iam-contract` | Primer consumidor que justifica contract oficial IAM (ADR-013) |
| Agnóstico vertical | Misma invite para Dental, Vet, ERP, Retail… |
| Separa Access de Subscription | Evita “invitar = cobrar seat” en un solo modelo |
| No toca el arco clínico/económico | Fortalece el corazón sin reabrir 16–22 |

**No** es onboarding dental. **No** es HR. **No** es un mailer.  
Es el **núcleo de aprovisionamiento de acceso** sobre el que los productos construyen UX y, más adelante, Subscription cuenta seats.

---

## 9. FASE 23 — estructura completa (patrón FASE 16+)

**Fase:** Platform Services (umbrella)  
**Primer Bounded context:** Access  
**Primer Aggregate Root:** `Invitation`  
**ADR de modelo:** **ADR-019** (Accepted en 23.1)  
**Módulo Gradle (propuesta):** `modules/access-management/` (domain · application · infrastructure · contract) — **cerrar en 23.0.1** (Opción A/B)  
**Schema SQL (propuesta):** `access` — **cerrar en 23.0.1**  
**HTTP (cerrar en audit API):** p. ej. `/api/v1/access/invitations` — decisión en 23.5.1  

### Pasos del slice Invitation (espejo 17–22)

| Paso | Nombre | Objetivo | Auditoría / ADR | Entregable |
|------|--------|----------|-----------------|------------|
| **23.0** | Platform Services Foundation Planning | Este documento + ROADMAP | Este PASO | Plan FASE 23 |
| **23.0.1** | Invitation Aggregate Audit | Checklist política; lifecycle; Identity/Membership write path; Opción A/B; *intentionally small* | **Obligatoria** | Modelo + prep. ADR-019 |
| **23.1** | Invitation Model ADR | Congelar modelo irreversible | **ADR-019** | ADR Accepted |
| **23.2** | Access / IAM Reference Readiness | Extraer/publicar `iam-contract` mínimo; command port de provisión si Opción A; **sin** ports clínicos/económicos | Contract check | Ports listos para escritura |
| **23.3** | Invitation Domain Foundation | Aggregate + VOs + tests | — | Dominio puro |
| **23.4** | Invitation Persistence | Flyway + R2DBC + ITs | — | Schema decidido |
| **23.5** | Invitation Authorization Contract | `invitation:*` (nombre exacto en audit) + seed | Mínima | Catalog + Flyway |
| **23.5.1** | Invitation Admin/Public API Audit | HTTP invite/accept/revoke; anti-enumeration; paginación | **Obligatoria** | Contrato HTTP |
| **23.6** | Invitation Administration (+ accept) API | Use cases + controllers + ITs + email port | — | API Access |
| **23.7** | Invitation Verification | E2E: IAM ports, RBAC, tenant, OpenAPI, accept→Membership | — | `InvitationVerificationIT` |
| **23.8** | Access Closeout (slice Invitation) | Guía consumo + `InvitationReferencePort` + ROADMAP | — | Slice Invitation cerrado |

### Track A — Password Recovery (paralelo o previo inmediato)

| Paso sugerido | Objetivo |
|---------------|----------|
| **23.PR.1** | Audit rápido del modelo `PasswordResetRequest` existente + gaps HTTP/DB/email |
| **23.PR.2** | Persistence + public API + anti-enumeration + verification |
| **23.PR.3** | Marcar ADR-009 P1 Password Recovery → **Done** (amend backlog) |

No diluye 23.0–23.8: **no** comparte aggregate con Invitation.

### Track C — Subscription (explícitamente fuera del slice v1)

Tras **23.8**, el ROADMAP puede abrir:

| Paso futuro | Objetivo |
|-------------|----------|
| **23.9.0** o fase numerada siguiente | Subscription Foundation Planning (BC propio) |
| Aggregate típico | `Subscription` / `Plan` / `Seat` — **uno** primero vía audit |
| ADR | **ADR-020** (esperado) — **no** reutilizar ADR-019 |

Meter Subscription en 23.0–23.8 convierte Access en comercial SaaS y rompe la separación Billing ≠ Subscription.

### Auditorías / ADR obligatorios

| Trigger (política) | Paso |
|--------------------|------|
| Nuevo Aggregate Root `Invitation` | **23.0.1** |
| Nuevo módulo + BC Access | **23.0.1** |
| Nuevo ADR de modelo | **23.1 → ADR-019** |
| Extracción `iam-contract` / command port | **23.2** (no reescribir ADR-006) |
| Shape API invite/accept | **23.5.1** |
| Subscription (cuando llegue) | Nueva audit + ADR-020 |

### ¿Hace falta 23.0.1?

**Sí — obligatoria.**  
Invitation conecta Identity/Membership (cross-BC write), introduce token lifecycle, y decide Opción A/B. Encaja en DEVELOPMENT-POLICY §3 (aggregate que conecta BCs / invariantes importantes).

---

## 10. Criterio de cierre FASE 23 (slice Invitation)

1. `Invitation` según ADR-019 (frozen) — *intentionally small*.  
2. Consumo IAM **solo** por contracts/ports publicados (lectura + provisión).  
3. Verification E2E verde (incl. accept → Membership ACTIVE).  
4. ROADMAP slice Invitation ✅ · Password Recovery P1 cerrado o plan residual explícito · **Subscription no mezclado**.  
5. Ningún aggregate de Org / Patient / Appointment / Encounter / Item / Invoice / Payment modificado.  
6. `InvitationReferencePort` + guía publicados.  
7. Ningún Notification / Subscription / StaffAssignment embebido en Invitation.  
8. Architecture Review independiente post-closeout (opción A/B/C) antes de abrir Subscription o FASE 24.

---

## 11. Relación con Stock y fases 24–25

| Trabajo | Relación con FASE 23 |
|---------|----------------------|
| **Stock** (Inventory) | Paralelo de producto — **no** bloquea ni pertenece a Platform Services |
| **FASE 24 Audit & Observability** | Consume hechos de Invitation/PasswordReset — **después** |
| **FASE 25 Production Hardening** | JWT stale, OpenAPI — **después** (ADR-009 P2) |

---

## Checklist

- [x] “Platform Services” desafiado como God BC → umbrella  
- [x] Primer BC = **Access** · Root = **Invitation**  
- [x] Password Recovery = track IAM (no BC inaugural)  
- [x] Subscription = BC posterior (no v1)  
- [x] Notifications = port, no aggregate  
- [x] 23.0.1 obligatoria · ADR-019 previsto  
- [x] Plan 23.0 → 23.8 (+ tracks A/C)  
- [x] Sin código / tablas / endpoints / ADR Accepted todavía  

---

## Siguiente

**PASO 23.0.1 — Invitation Aggregate Audit**  
(lifecycle · Identity/Membership provision path · Opción A/B módulo · roles · token · *intentionally small* → prep. ADR-019)

En paralelo (capacidad): **23.PR.1** Password Recovery gap audit (ADR-009 P1).
