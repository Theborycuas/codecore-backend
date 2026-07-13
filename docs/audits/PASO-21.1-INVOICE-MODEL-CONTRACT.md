# PASO 21.1 — Invoice Model Contract (ADR-017)

**Invoice** queda congelado: reclamación comercial, *intentionally small*, listo para que FASE 21 implemente sin reabrir el dominio.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Aceptación ADR (sin código)  
**Dependencias:** [PASO-21.0.1](PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md) · [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md)

---

## Entregable

| Artefacto | Estado |
|-----------|--------|
| [ADR-017 — Invoice Domain Model](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md) | **Accepted** |

Sin tablas · sin HTTP · sin persistencia · sin módulos Gradle en este paso.

---

## Contrato congelado (resumen)

| Elemento | Valor |
|----------|-------|
| Definición | Commercial claim that an amount is owed by a bill-to party to an issuing organization within a Tenant |
| Principio | **Invoice is intentionally small** (permanente) |
| Emisor | `OrganizationId` obligatorio |
| Bill-to | Exactamente Patient **xor** Organization (≠ issuer) |
| Estados | `DRAFT` · `ISSUED` · `VOIDED` — sin `PAID` |
| Identidad | `InvoiceId` · `invoiceNumber?` soft-unique |
| Líneas | description + Money + `ItemId?` + `EncounterId?` |
| Dinero | currency + amountMinor; una currency; total = suma |
| Ports | Org · Patient? · Item? · Encounter findLinkable? |
| Schema / módulo | `billing` · `billing-management` |
| HTTP | `/api/v1/billing/invoices` |
| Permisos | `invoice:read\|create\|update\|issue\|void` |

---

## Checklist de aceptación

- [x] Checklist política §8 verde (21.0.1)  
- [x] Permanencia §3 en ADR  
- [x] Lifecycle sin PAID congelado  
- [x] Bill-to multi-vertical cerrado  
- [x] Compatibilidad ADR-003/006/007/010–016 sin modificar ADRs previos  
- [x] Freeze rule publicada  

---

## Siguiente paso

**PASO 21.2 — Billing Reference Readiness**.

---

## Referencias

- [ADR-017-INVOICE-DOMAIN-MODEL.md](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md)  
- [PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md](PASO-21.0.1-INVOICE-AGGREGATE-AUDIT.md)  
- [ROADMAP.md](../architecture/ROADMAP.md)  
