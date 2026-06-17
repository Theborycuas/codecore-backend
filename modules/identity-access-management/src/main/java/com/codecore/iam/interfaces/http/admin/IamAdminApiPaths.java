package com.codecore.iam.interfaces.http.admin;

/**
 * Base paths for IAM administration HTTP API (ADR-008).
 */
public final class IamAdminApiPaths {

    public static final String BASE = "/api/v1/iam";

    public static final String ADMINISTRATION = BASE + "/administration";

    public static final String USERS = BASE + "/users";

    public static final String MEMBERSHIPS = BASE + "/memberships";

    private IamAdminApiPaths() {
    }
}
