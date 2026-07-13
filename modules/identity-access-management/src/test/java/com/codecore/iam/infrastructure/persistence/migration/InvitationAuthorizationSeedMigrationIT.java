package com.codecore.iam.infrastructure.persistence.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Flyway V31 Invitation permission seeds are idempotent, and that the migration
 * backfills {@code invitation:*} grants onto system roles provisioned by earlier tenants
 * (i.e. tenants whose {@code iam.role} rows already existed before V31 ran).
 */
class InvitationAuthorizationSeedMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("db_codecore")
            .withUsername("codecore")
            .withPassword("codecore0803861400");

    private static final int EXPECTED_TOTAL_PERMISSION_COUNT = 55;
    private static final int EXPECTED_INVITATION_PERMISSION_COUNT = 3;

    private static final String V31_SEED_SQL = """
            INSERT INTO iam.permission (
                permission_id,
                code,
                description,
                system_permission,
                created_at,
                updated_at
            )
            SELECT
                gen_random_uuid(),
                v.code,
                v.description,
                TRUE,
                NOW(),
                NOW()
            FROM (
                VALUES
                    ('invitation:create', 'Create a Tenant invitation for an email + system role'),
                    ('invitation:read',   'Read Tenant invitations'),
                    ('invitation:revoke', 'Revoke a PENDING Tenant invitation')
            ) AS v(code, description)
            WHERE NOT EXISTS (
                SELECT 1
                FROM iam.permission p
                WHERE p.code = v.code
            )
            """;

    static {
        POSTGRES.start();
    }

    @Test
    void shouldSeedInvitationPermissionsIdempotently() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("30"))
                .load()
                .migrate();

        UUID tenantId = seedPreExistingTenantWithSystemRoles();

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(countPermissions()).isEqualTo(EXPECTED_TOTAL_PERMISSION_COUNT);
        assertThat(countInvitationPermissions()).isEqualTo(EXPECTED_INVITATION_PERMISSION_COUNT);
        assertThat(appliedMigrationVersion()).isEqualTo("32");

        executeUpdate(V31_SEED_SQL);
        executeUpdate(V31_SEED_SQL);

        assertThat(countPermissions()).isEqualTo(EXPECTED_TOTAL_PERMISSION_COUNT);
        assertThat(countInvitationPermissions()).isEqualTo(EXPECTED_INVITATION_PERMISSION_COUNT);

        assertThat(countRoleInvitationPermissions(tenantId, "OWNER")).isEqualTo(3);
        assertThat(countRoleInvitationPermissions(tenantId, "ADMIN")).isEqualTo(3);
        assertThat(countRoleInvitationPermissions(tenantId, "MANAGER")).isEqualTo(3);
        assertThat(countRoleInvitationPermissions(tenantId, "USER")).isEqualTo(1);
        assertThat(countRoleInvitationPermissions(tenantId, "READ_ONLY")).isEqualTo(1);
    }

    private static UUID seedPreExistingTenantWithSystemRoles() throws SQLException {
        UUID tenantId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO iam.tenant (tenant_id, name, status, created_at, updated_at)
                    VALUES (?, 'Pre-V31 Tenant', 'ACTIVE', NOW(), NOW())
                    """)) {
                ps.setObject(1, tenantId);
                ps.executeUpdate();
            }
            for (String roleCode : new String[] {"OWNER", "ADMIN", "MANAGER", "USER", "READ_ONLY"}) {
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO iam.role (role_id, tenant_id, code, name, status, system_role, created_at, updated_at)
                        VALUES (?, ?, ?, ?, 'ACTIVE', TRUE, NOW(), NOW())
                        """)) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, tenantId);
                    ps.setString(3, roleCode);
                    ps.setString(4, roleCode);
                    ps.executeUpdate();
                }
            }
        }
        return tenantId;
    }

    private static int countPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM iam.permission");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int countInvitationPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT COUNT(*) FROM iam.permission WHERE code LIKE 'invitation:%'
                     """);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int countRoleInvitationPermissions(UUID tenantId, String roleCode) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM iam.role_permission rp
                     JOIN iam.role r ON r.role_id = rp.role_id
                     JOIN iam.permission p ON p.permission_id = rp.permission_id
                     WHERE r.system_role = TRUE
                       AND r.tenant_id = ?
                       AND r.code = ?
                       AND p.code LIKE 'invitation:%'
                     """)) {
            ps.setObject(1, tenantId);
            ps.setString(2, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String appliedMigrationVersion() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void executeUpdate(String sql) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
    }
}
