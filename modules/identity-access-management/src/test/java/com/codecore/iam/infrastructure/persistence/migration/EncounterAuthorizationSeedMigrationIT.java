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
 * Validates Flyway V23 Encounter permission seeds are idempotent.
 */
class EncounterAuthorizationSeedMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("db_codecore")
            .withUsername("codecore")
            .withPassword("codecore0803861400");

    private static final int EXPECTED_TOTAL_PERMISSION_COUNT = 52;
    private static final int EXPECTED_ENCOUNTER_PERMISSION_COUNT = 4;

    private static final String V23_SEED_SQL = """
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
                    ('encounter:create',  'Create occurred care episodes (encounters)'),
                    ('encounter:read',    'Read occurred care episodes (encounters)'),
                    ('encounter:update',  'Update occurred care episodes (refs, time bounds, complete)'),
                    ('encounter:cancel',  'Cancel occurred care episodes (encounters)')
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
    void shouldSeedEncounterPermissionsIdempotently() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(countPermissions()).isEqualTo(EXPECTED_TOTAL_PERMISSION_COUNT);
        assertThat(countEncounterPermissions()).isEqualTo(EXPECTED_ENCOUNTER_PERMISSION_COUNT);
        assertThat(appliedMigrationVersion()).isEqualTo("29");

        executeUpdate(V23_SEED_SQL);
        executeUpdate(V23_SEED_SQL);

        assertThat(countPermissions()).isEqualTo(EXPECTED_TOTAL_PERMISSION_COUNT);
        assertThat(countEncounterPermissions()).isEqualTo(EXPECTED_ENCOUNTER_PERMISSION_COUNT);
    }

    private static int countPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM iam.permission");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int countEncounterPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT COUNT(*) FROM iam.permission WHERE code LIKE 'encounter:%'
                     """);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
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
