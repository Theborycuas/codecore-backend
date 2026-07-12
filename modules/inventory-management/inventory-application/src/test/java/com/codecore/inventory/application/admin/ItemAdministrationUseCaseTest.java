package com.codecore.inventory.application.admin;

import com.codecore.inventory.application.command.CreateItemCommand;
import com.codecore.inventory.application.dto.AdminItemView;
import com.codecore.inventory.application.port.out.ItemAdminQueryRepository;
import com.codecore.inventory.application.port.out.ItemQueryPort;
import com.codecore.inventory.application.port.out.ItemRepository;
import com.codecore.inventory.application.port.out.TenantContextAccessor;
import com.codecore.inventory.domain.exception.PrimaryOrganizationNotFoundException;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.TenantId;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.domain.valueobject.OrganizationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemAdministrationUseCaseTest {

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private ItemAdminQueryRepository itemAdminQueryRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemQueryPort itemQueryPort;
    @Mock
    private OrganizationReferencePort organizationReferencePort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private ItemAdministrationUseCaseImpl useCase;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        useCase = new ItemAdministrationUseCaseImpl(
                tenantContextAccessor,
                itemAdminQueryRepository,
                itemRepository,
                itemQueryPort,
                organizationReferencePort,
                transactionalOperator
        );
        tenantId = TenantId.generate();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateItemWithoutPrimaryOrganization() {
        when(itemRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreateItemCommand command = new CreateItemCommand(
                "Gauze Roll",
                null,
                null
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    org.assertj.core.api.Assertions.assertThat(view.displayName()).isEqualTo("Gauze Roll");
                    org.assertj.core.api.Assertions.assertThat(view.primaryOrganizationId()).isNull();
                    org.assertj.core.api.Assertions.assertThat(view.tenantId()).isEqualTo(tenantId);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectInactivePrimaryOrganization() {
        UUID orgId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(OrganizationId.class), any()))
                .thenReturn(Mono.just(false));

        CreateItemCommand command = new CreateItemCommand(
                "Buddy",
                null,
                orgId
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(PrimaryOrganizationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldCreateItemWithActivePrimaryOrganization() {
        UUID orgId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(OrganizationId.class), any()))
                .thenReturn(Mono.just(true));
        when(itemRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreateItemCommand command = new CreateItemCommand(
                ItemDisplayName.of("Anaesthetic Cartridge").value(),
                "SKU-1",
                orgId
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    org.assertj.core.api.Assertions.assertThat(view.primaryOrganizationId().value()).isEqualTo(orgId);
                    org.assertj.core.api.Assertions.assertThat(view.code()).isEqualTo("SKU-1");
                })
                .verifyComplete();
    }
}
