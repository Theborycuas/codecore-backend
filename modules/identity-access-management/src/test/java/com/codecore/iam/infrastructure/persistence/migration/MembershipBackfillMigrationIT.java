package com.codecore.iam.infrastructure.persistence.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Flyway V8 backfill idempotency against a real PostgreSQL 16 schema.
 */
class MembershipBackfillMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("db_codecore")
            .withUsername("codecore")
            .withPassword("codecore0803861400");

    private static final String BACKFILL_SQL = """
            INSERT INTO iam.identity_tenant_membership (
                membership_id,
                identity_id,
                tenant_id,
                status,
                created_at,
                updated_at
            )
            SELECT
                gen_random_uuid(),
                u.id,
                u.tenant_id,
                'ACTIVE',
                u.created_at,
                u.updated_at
            FROM iam.iam_user u
            WHERE NOT EXISTS (
                SELECT 1
                FROM iam.identity_tenant_membership m
                WHERE m.identity_id = u.id
                  AND m.tenant_id   = u.tenant_id
            )
            """;

    static {
        POSTGRES.start();
    }

    @BeforeEach
    void resetDatabaseThroughV7() {
        Flyway flyway = flywayBuilder().load();
        flyway.clean();
        flywayBuilder()
                .target("7")
                .load()
                .migrate();
        assertThat(appliedMigrationVersion()).isEqualTo("7");
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flywayBuilder() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas("iam")
                .createSchemas(true)
                .cleanDisabled(false);
    }

    @Test
    void shouldCreateMembershipForHistoricalIdentityWithoutMembership() throws Exception {
        UUID identityId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-16T12:00:00Z");

        insertHistoricalIamUser(identityId, tenantId, createdAt, updatedAt);
        assertThat(countMembershipsFor(identityId, tenantId)).isZero();

        migrateToLatest();

        assertThat(countMembershipsFor(identityId, tenantId)).isEqualTo(1);
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT status, created_at, updated_at
                     FROM iam.identity_tenant_membership
                     WHERE identity_id = ? AND tenant_id = ?
                     """)) {
            ps.setObject(1, identityId);
            ps.setObject(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("ACTIVE");
                assertThat(rs.getTimestamp("created_at").toInstant()).isEqualTo(createdAt);
                assertThat(rs.getTimestamp("updated_at").toInstant()).isEqualTo(updatedAt);
            }
        }
    }

    @Test
    void shouldNotDuplicateWhenMembershipAlreadyExists() throws Exception {
        UUID identityId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-02-01T08:00:00Z");
        Instant updatedAt = Instant.parse("2024-02-02T09:00:00Z");
        UUID existingMembershipId = UUID.randomUUID();

        insertHistoricalIamUser(identityId, tenantId, createdAt, updatedAt);
        insertMembership(existingMembershipId, identityId, tenantId, "INACTIVE", createdAt, updatedAt);
        assertThat(countMembershipsFor(identityId, tenantId)).isEqualTo(1);

        migrateToLatest();

        assertThat(countMembershipsFor(identityId, tenantId)).isEqualTo(1);
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT membership_id, status
                     FROM iam.identity_tenant_membership
                     WHERE identity_id = ? AND tenant_id = ?
                     """)) {
            ps.setObject(1, identityId);
            ps.setObject(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getObject("membership_id")).isEqualTo(existingMembershipId);
                assertThat(rs.getString("status")).isEqualTo("INACTIVE");
            }
        }
    }

    @Test
    void shouldBeIdempotentOnBackfillSqlReexecution() throws Exception {
        UUID identityId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-03-10T14:00:00Z");
        Instant updatedAt = Instant.parse("2024-03-11T15:00:00Z");

        insertHistoricalIamUser(identityId, tenantId, createdAt, updatedAt);
        migrateToLatest();
        assertThat(countMembershipsFor(identityId, tenantId)).isEqualTo(1);

        executeUpdate(BACKFILL_SQL);
        assertThat(countMembershipsFor(identityId, tenantId)).isEqualTo(1);

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas("iam")
                .createSchemas(true)
                .load()
                .migrate();
        assertThat(countMembershipsFor(identityId, tenantId)).isEqualTo(1);
        assertThat(appliedMigrationVersion()).isEqualTo("9");
    }

    private static void migrateToLatest() {
        flywayBuilder()
                .load()
                .migrate();
        assertThat(appliedMigrationVersion()).isEqualTo("9");
    }

    private static String appliedMigrationVersion() {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT version FROM iam.flyway_schema_history
                     ORDER BY installed_rank DESC
                     LIMIT 1
                     """)) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString("version");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
    }

    private static void insertHistoricalIamUser(
            UUID identityId,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     INSERT INTO iam.iam_user (
                         id, tenant_id, email, normalized_email, password_hash,
                         status, email_verified, created_at, updated_at, version
                     ) VALUES (?, ?, ?, ?, ?, 'ACTIVE', TRUE, ?, ?, 0)
                     """)) {
            String email = "historical.%s@codecore.local".formatted(identityId);
            ps.setObject(1, identityId);
            ps.setObject(2, tenantId);
            ps.setString(3, email);
            ps.setString(4, email.toLowerCase());
            ps.setString(5, "$2a$10$historicalhash");
            ps.setTimestamp(6, java.sql.Timestamp.from(createdAt));
            ps.setTimestamp(7, java.sql.Timestamp.from(updatedAt));
            ps.executeUpdate();
        }
    }

    private static void insertMembership(
            UUID membershipId,
            UUID identityId,
            UUID tenantId,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     INSERT INTO iam.identity_tenant_membership (
                         membership_id, identity_id, tenant_id, status, created_at, updated_at
                     ) VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            ps.setObject(1, membershipId);
            ps.setObject(2, identityId);
            ps.setObject(3, tenantId);
            ps.setString(4, status);
            ps.setTimestamp(5, java.sql.Timestamp.from(createdAt));
            ps.setTimestamp(6, java.sql.Timestamp.from(updatedAt));
            ps.executeUpdate();
        }
    }

    private static int countMembershipsFor(UUID identityId, UUID tenantId) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT COUNT(*) FROM iam.identity_tenant_membership
                     WHERE identity_id = ? AND tenant_id = ?
                     """)) {
            ps.setObject(1, identityId);
            ps.setObject(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void executeUpdate(String sql) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
