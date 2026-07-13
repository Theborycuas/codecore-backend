# PASO 21.2 — Billing Reference Readiness

**Contracts existentes** cubren lo que Invoice necesita — sin evolucionar ports, sin reabrir ADR-010…016 ni aggregates congelados.

**Fecha:** 2026-07-12  
**Estado:** ✅ Completado  
**Tipo:** Contract check (ADR-013 · ADR-017) — **sin código nuevo de ports**  
**Dependencias:** [ADR-017](../architecture/ADR-017-INVOICE-DOMAIN-MODEL.md) · [ADR-013](../architecture/ADR-013-BOUNDED-CONTEXT-REFERENCE-CONTRACTS.md) · [PASO-21.1](PASO-21.1-INVOICE-MODEL-CONTRACT.md)

---

## Veredicto

| Port | Método | ¿Suficiente? |
|------|--------|--------------|
| `OrganizationReferencePort` | `existsActiveByIdAndTenant` | ✅ issuer + bill-to Org |
| `PatientReferencePort` | `existsActiveByIdAndTenant` | ✅ bill-to Patient |
| `ItemReferencePort` | `existsActiveByIdAndTenant` | ✅ línea Item |
| `EncounterReferencePort` | `findLinkableByIdAndTenant` | ✅ línea Encounter + `patientId` para coherencia |

| Pregunta | Respuesta |
|----------|-----------|
| ¿Evolución de port en 21.2? | **Ninguna** |
| ¿Appointment / Office / Stock / Payment ports? | **No** |
| ¿`InvoiceReferencePort`? | Closeout **21.8** — no ahora |

**Listo para 21.3 Domain Foundation** sin trabajo de contract.

---

## Coherencia application (futuro 21.6)

1. Issuer Org ACTIVE  
2. Bill-to Patient **xor** Org ACTIVE (Org ≠ issuer)  
3. Por cada línea con Item → Item ACTIVE  
4. Por cada línea con Encounter → `findLinkable` presente; si bill-to Patient → `view.patientId` match  
5. Port `false`/empty → 404 (o 409 si mismatch patient — espejo Encounter)

---

## Checklist

- [x] Org / Patient / Item / Encounter ports suficientes  
- [x] Sin evolución ADR-013  
- [x] Sin ports “por si acaso”  

## Siguiente paso

**PASO 21.3 — Invoice Domain Foundation**.
