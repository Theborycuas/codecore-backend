# PASO 23.2 — Access / IAM Reference Readiness

**Veredicto:** Ports IAM contract **listos** para create/accept Invitation — **sin** reabrir ADR-006/007.

| Invariante | Port | ¿OK? |
|------------|------|------|
| Inviter ACTIVE en tenant | `IamMembershipReferencePort.existsActiveByIdAndTenant` | ✅ |
| Sin Membership ACTIVE email+tenant | `IamActiveMembershipByEmailPort.existsActiveByEmailAndTenant` | ✅ |
| Rol sistema allow-list (≠ OWNER) | `IamSystemRoleReferencePort.existsSystemRoleByCodeAndTenant` | ✅ |
| Identity por email (opcional create) | `IamIdentityEmailReferencePort.existsByEmail` | ✅ |
| Accept → Membership (+ Identity) | `TenantAccessProvisionPort.provision` | ✅ |

No ports clínicos / económicos / Organization / Subscription.

**Listo para 23.3 Domain Foundation.**
