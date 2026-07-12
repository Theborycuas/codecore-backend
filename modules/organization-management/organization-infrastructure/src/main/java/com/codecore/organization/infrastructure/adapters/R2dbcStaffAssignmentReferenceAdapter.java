package com.codecore.organization.infrastructure.adapters;

import com.codecore.organization.contract.reference.StaffAssignmentReferencePort;
import com.codecore.organization.contract.reference.StaffAssignmentReferenceView;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * In-process adapter for {@link StaffAssignmentReferencePort} (ADR-013).
 */
@Component
public class R2dbcStaffAssignmentReferenceAdapter implements StaffAssignmentReferencePort {

    private final DatabaseClient databaseClient;

    public R2dbcStaffAssignmentReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Optional<StaffAssignmentReferenceView>> findScopeByIdAndTenant(
            StaffAssignmentId staffAssignmentId,
            TenantId tenantId
    ) {
        return databaseClient.sql("""
                        SELECT assignment_id, organization_id, office_id
                        FROM org.staff_assignment
                        WHERE assignment_id = :assignmentId
                          AND tenant_id = :tenantId
                        """)
                .bind("assignmentId", staffAssignmentId.value())
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> {
                    UUID officeUuid = row.get("office_id", UUID.class);
                    return new StaffAssignmentReferenceView(
                            new StaffAssignmentId(row.get("assignment_id", UUID.class)),
                            new OrganizationId(row.get("organization_id", UUID.class)),
                            officeUuid == null ? null : new OfficeId(officeUuid)
                    );
                })
                .one()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }
}
