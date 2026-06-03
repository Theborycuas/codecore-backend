package com.codecore.iam.domain.valueobject;

import java.util.UUID;

/**
 * Identifier for {@link com.codecore.iam.domain.model.membership.IdentityTenantMembership}.
 */
public final class MembershipId extends UuidIdentifier {

    public MembershipId(UUID value) {
        super(value);
    }

    public MembershipId(String value) {
        super(value);
    }

    public static MembershipId generate() {
        return new MembershipId(UUID.randomUUID());
    }
}
