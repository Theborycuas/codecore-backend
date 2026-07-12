package com.codecore.inventory.application.admin;

import com.codecore.inventory.application.command.CreateItemCommand;
import com.codecore.inventory.application.command.UpdateItemCommand;
import com.codecore.inventory.application.dto.AdminItemView;
import com.codecore.inventory.application.dto.PagedResult;
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
import com.codecore.inventory.application.query.ItemListQuery;
import com.codecore.inventory.application.query.PageQuery;
import com.codecore.inventory.domain.exception.InvalidDomainValueException;
import com.codecore.inventory.domain.exception.ItemNotFoundException;
import com.codecore.inventory.domain.exception.PrimaryOrganizationNotFoundException;
import com.codecore.inventory.domain.model.item.Item;
import com.codecore.inventory.domain.valueobject.ItemCode;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.domain.valueobject.OrganizationId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ItemAdministrationUseCaseImpl
        implements ListItemsUseCase,
        GetItemUseCase,
        CreateItemUseCase,
        UpdateItemUseCase,
        ArchiveItemUseCase,
        ActivateItemUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final ItemAdminQueryRepository itemAdminQueryRepository;
    private final ItemRepository itemRepository;
    private final ItemQueryPort itemQueryPort;
    private final OrganizationReferencePort organizationReferencePort;
    private final TransactionalOperator transactionalOperator;

    public ItemAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            ItemAdminQueryRepository itemAdminQueryRepository,
            ItemRepository itemRepository,
            ItemQueryPort itemQueryPort,
            OrganizationReferencePort organizationReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.itemAdminQueryRepository = Objects.requireNonNull(
                itemAdminQueryRepository,
                "itemAdminQueryRepository"
        );
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository");
        this.itemQueryPort = Objects.requireNonNull(itemQueryPort, "itemQueryPort");
        this.organizationReferencePort = Objects.requireNonNull(
                organizationReferencePort,
                "organizationReferencePort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminItemView>> execute(ItemListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> itemAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> itemAdminQueryRepository
                                .findByTenantId(tenantId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminItemView> execute(ItemId itemId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, itemId).map(this::toView));
    }

    @Override
    public Mono<AdminItemView> execute(CreateItemCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> {
                    ItemDisplayName displayName = requireDisplayName(command.displayName());
                    ItemCode code = toOptionalCode(command.code());
                    return resolvePrimaryOrganization(tenantId, command.primaryOrganizationId())
                            .flatMap(primaryOrg -> {
                                Item item = Item.create(
                                        ItemId.generate(),
                                        tenantId,
                                        displayName,
                                        code,
                                        primaryOrg.orElse(null),
                                        Instant.now()
                                );
                                return itemRepository.save(item).map(this::toView);
                            });
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminItemView> execute(UpdateItemCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.itemId())
                        .flatMap(item -> {
                            item.rename(requireDisplayName(command.displayName()));
                            String codeValue = blankToNull(command.code());
                            if (codeValue == null) {
                                item.clearCode();
                            } else {
                                item.assignCode(ItemCode.of(codeValue));
                            }
                            return resolvePrimaryOrganization(tenantId, command.primaryOrganizationId())
                                    .flatMap(primaryOrg -> {
                                        if (primaryOrg.isPresent()) {
                                            item.assignPrimaryOrganization(primaryOrg.get());
                                        } else {
                                            item.removePrimaryOrganization();
                                        }
                                        return itemRepository.save(item);
                                    });
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminItemView> archive(ItemId itemId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, itemId)
                        .flatMap(item -> {
                            item.archive();
                            return itemRepository.save(item);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminItemView> activate(ItemId itemId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, itemId)
                        .flatMap(item -> {
                            item.activate();
                            return itemRepository.save(item);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Item> loadInTenant(TenantId tenantId, ItemId itemId) {
        return itemQueryPort.findByIdAndTenantId(itemId, tenantId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(
                        "Item not found in tenant context")));
    }

    private Mono<Optional<PrimaryOrganizationId>> resolvePrimaryOrganization(
            TenantId tenantId,
            UUID primaryOrganizationId
    ) {
        if (primaryOrganizationId == null) {
            return Mono.just(Optional.empty());
        }
        OrganizationId organizationId = new OrganizationId(primaryOrganizationId);
        com.codecore.organization.domain.valueobject.TenantId orgTenantId =
                new com.codecore.organization.domain.valueobject.TenantId(tenantId.value());
        return organizationReferencePort.existsActiveByIdAndTenant(organizationId, orgTenantId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new PrimaryOrganizationNotFoundException(
                                "Primary organization not found or not ACTIVE in tenant"));
                    }
                    return Mono.just(Optional.of(PrimaryOrganizationId.of(primaryOrganizationId)));
                });
    }

    private static ItemDisplayName requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new InvalidDomainValueException("displayName is required");
        }
        return ItemDisplayName.of(displayName);
    }

    private static ItemCode toOptionalCode(String code) {
        String normalized = blankToNull(code);
        return normalized == null ? null : ItemCode.of(normalized);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdminItemView toView(Item item) {
        return new AdminItemView(
                item.id(),
                item.tenantId(),
                item.displayName().value(),
                item.code().map(ItemCode::value).orElse(null),
                item.primaryOrganizationId().orElse(null),
                item.status(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
