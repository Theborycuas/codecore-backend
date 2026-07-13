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
 * Validates Flyway V21 Appointment permission seeds are idempotent.
 */
class AppointmentAuthorizationSeedMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("db_codecore")
            .withUsername("codecore")
            .withPassword("codecore0803861400");

    private static final int EXPECTED_TOTAL_PERMISSION_COUNT = 49;
    private static final int EXPECTED_APPOINTMENT_PERMISSION_COUNT = 4;

    private static final String V21_SEED_SQL = """
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
                    ('appointment:create',  'Create planned care commitments (appointments)'),
                    ('appointment:read',    'Read planned care commitments (appointments)'),
                    ('appointment:update',  'Update planned care commitments (reschedule, reassign, complete)'),
                    ('appointment:cancel',  'Cancel planned care commitments (appointments)')
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
    void shouldSeedAppointmentPermissionsIdempotently() throws SQLException {
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
        assertThat(countAppointmentPermissions()).isEqualTo(EXPECTED_APPOINTMENT_PERMISSION_COUNT);
        assertThat(appliedMigrationVersion()).isEqualTo("27");

        executeUpdate(V21_SEED_SQL);
        executeUpdate(V21_SEED_SQL);

        assertThat(countPermissions()).isEqualTo(EXPECTED_TOTAL_PERMISSION_COUNT);
        assertThat(countAppointmentPermissions()).isEqualTo(EXPECTED_APPOINTMENT_PERMISSION_COUNT);
    }

    private static int countPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM iam.permission");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int countAppointmentPermissions() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT COUNT(*) FROM iam.permission WHERE code LIKE 'appointment:%'
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
