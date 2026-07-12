package com.codecore.inventory.configuration;

import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.inventory.application.admin.ItemAdministrationUseCaseImpl;
import com.codecore.inventory.application.port.in.ActivateItemUseCase;
import com.codecore.inventory.application.port.in.ArchiveItemUseCase;
import com.codecore.inventory.application.port.in.CreateItemUseCase;
import com.codecore.inventory.application.port.in.GetItemUseCase;
import com.codecore.inventory.application.port.in.ListItemsUseCase;
import com.codecore.inventory.application.port.in.UpdateItemUseCase;
import com.codecore.inventory.application.port.out.ItemAdminQueryRepository;
import com.codecore.inventory.application.port.out.ItemQueryPort;
import com.codecore.inventory.application.port.out.ItemRepository;
import com.codecore.inventory.application.port.out.TenantContextAccessor;
import com.codecore.inventory.infrastructure.adapters.IamTenantContextAccessor;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class InventoryAdministrationConfiguration {

    @Bean
    public TenantContextAccessor itemTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public ItemAdministrationUseCaseImpl itemAdministrationUseCase(
            TenantContextAccessor itemTenantContextAccessor,
            ItemAdminQueryRepository itemAdminQueryRepository,
            ItemRepository itemRepository,
            ItemQueryPort itemQueryPort,
            OrganizationReferencePort organizationReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new ItemAdministrationUseCaseImpl(
                itemTenantContextAccessor,
                itemAdminQueryRepository,
                itemRepository,
                itemQueryPort,
                organizationReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListItemsUseCase listItemsUseCase(ItemAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetItemUseCase getItemUseCase(ItemAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateItemUseCase createItemUseCase(ItemAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateItemUseCase updateItemUseCase(ItemAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ArchiveItemUseCase archiveItemUseCase(ItemAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ActivateItemUseCase activateItemUseCase(ItemAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
