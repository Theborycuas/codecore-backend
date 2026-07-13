package com.codecore.access.application.query;

import java.util.Objects;

/**
 * List filters for Invitation administration.
 */
public record InvitationListQuery(
        InvitationListFilter status
) {

    public InvitationListQuery {
        status = Objects.requireNonNull(status, "status");
    }

    public static InvitationListQuery of(String status) {
        return new InvitationListQuery(InvitationListFilter.parse(status));
    }
}
