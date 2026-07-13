package com.codecore.iam.infrastructure.persistence.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Flyway V13 IAM permission seeds are idempotent.
 */
class AuthorizationSeedMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("db_codecore")
            .withUsername("codecore")
            .withPassword("codecore0803861400");

    private static final int EXPECTED_PERMISSION_COUNT = 52;

    private static final String V13_SEED_SQL = """
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
                    ('tenant:read',        'Read tenant metadata'),
                    ('tenant:update',      'Update tenant metadata'),
                    ('membership:read',    'Read tenant memberships'),
                    ('membership:create',  'Create tenant memberships'),
                    ('membership:update',  'Update tenant memberships'),
                    ('membership:delete',  'Delete tenant memberships'),
                    ('role:read',          'Read tenant roles'),
                    ('role:create',        'Create tenant roles'),
                    ('role:update',        'Update tenant roles'),
                    ('role:delete',        'Delete tenant roles'),
                    ('permission:read',    'Read global permission catalog'),
                    ('permission:assign',  'Assign permissions to roles'),
                    ('user:read',          'Read tenant users'),
                    ('user:create',        'Create tenant users'),
                    ('user:update',        'Update tenant users'),
                    ('user:delete',        'Delete tenant users')
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
    void shouldSeedIamPermissionsIdempotently() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas("iam")
                .createSchemas(true)
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas("iam")
                .createSchemas(true)
                .load()
                .migrate();

        assertThat(countPermissions()).isEqualTo(EXPECTED_PERMISSION_COUNT);
        assertThat(countSystemPermissions()).isEqualTo(EXPECTED_PERMISSION_COUNT);
        assertThat(appliedMigrationVersion()).isEqualTo("29");

        executeUpdate(V13_SEED_SQL);
        executeUpdate(V13_SEED_SQL);

        assertThat(countPermissions()).isEqualTo(EXPECTED_PERMISSION_COUNT);
    }

    private static int countPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM iam.permission");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int countSystemPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT COUNT(*) FROM iam.permission WHERE system_permission = TRUE")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String appliedMigrationVersion() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT version FROM iam.flyway_schema_history ORDER BY installed_rank DESC LIMIT 1");
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
