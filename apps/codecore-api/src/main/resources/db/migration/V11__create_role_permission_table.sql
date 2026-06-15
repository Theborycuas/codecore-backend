-- =============================================================================
-- V11 — Role ↔ Permission (N:M association)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 14.3 — Authorization Foundation
-- =============================================================================

CREATE TABLE iam.role_permission (
    role_id        UUID                     NOT NULL,
    permission_id  UUID                     NOT NULL,
    assigned_at    TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_role_permission
        PRIMARY KEY (role_id, permission_id),

    CONSTRAINT uq_role_permission_role_permission
        UNIQUE (role_id, permission_id),

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
        REFERENCES iam.role (role_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
        REFERENCES iam.permission (permission_id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE iam.role_permission IS
    'N:M link between tenant-scoped Role and global Permission (owned by Role aggregate).';

COMMENT ON COLUMN iam.role_permission.role_id IS
    'Role aggregate id — tenant scope inherited from iam.role (no redundant tenant_id).';

COMMENT ON COLUMN iam.role_permission.permission_id IS
    'Global permission catalog id (domain PermissionId).';
