package com.codecore.inventory.interfaces.http.admin;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import com.codecore.inventory.application.command.CreateItemCommand;
import com.codecore.inventory.application.command.UpdateItemCommand;
import com.codecore.inventory.application.port.in.ActivateItemUseCase;
import com.codecore.inventory.application.port.in.ArchiveItemUseCase;
import com.codecore.inventory.application.port.in.CreateItemUseCase;
import com.codecore.inventory.application.port.in.GetItemUseCase;
import com.codecore.inventory.application.port.in.ListItemsUseCase;
import com.codecore.inventory.application.port.in.UpdateItemUseCase;
import com.codecore.inventory.application.query.ItemListQuery;
import com.codecore.inventory.application.query.PageQueryParser;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.interfaces.http.admin.dto.CreateItemRequest;
import com.codecore.inventory.interfaces.http.admin.dto.ItemResponse;
import com.codecore.inventory.interfaces.http.admin.dto.PagedItemResponse;
import com.codecore.inventory.interfaces.http.admin.dto.UpdateItemRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(ItemAdminApiPaths.ITEMS)
@Tag(name = "Items", description = "Item inventoriable catalog administration (`item:*` permissions)")
public class ItemAdminController {

    private final ListItemsUseCase listItemsUseCase;
    private final GetItemUseCase getItemUseCase;
    private final CreateItemUseCase createItemUseCase;
    private final UpdateItemUseCase updateItemUseCase;
    private final ArchiveItemUseCase archiveItemUseCase;
    private final ActivateItemUseCase activateItemUseCase;

    public ItemAdminController(
            ListItemsUseCase listItemsUseCase,
            GetItemUseCase getItemUseCase,
            CreateItemUseCase createItemUseCase,
            UpdateItemUseCase updateItemUseCase,
            ArchiveItemUseCase archiveItemUseCase,
            ActivateItemUseCase activateItemUseCase
    ) {
        this.listItemsUseCase = listItemsUseCase;
        this.getItemUseCase = getItemUseCase;
        this.createItemUseCase = createItemUseCase;
        this.updateItemUseCase = updateItemUseCase;
        this.archiveItemUseCase = archiveItemUseCase;
        this.activateItemUseCase = activateItemUseCase;
    }

    @GetMapping
    @RequiresPermission("item:read")
    public Mono<PagedItemResponse> listItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) UUID primaryOrganizationId
    ) {
        ItemListQuery filter = ItemListQuery.of(status, q, code, primaryOrganizationId);
        return listItemsUseCase
                .execute(filter, PageQueryParser.parseItemPageQuery(page, size, sort))
                .map(PagedItemResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("item:read")
    public Mono<ItemResponse> getItem(@PathVariable UUID id) {
        return getItemUseCase.execute(new ItemId(id)).map(ItemResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("item:create")
    public Mono<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        return createItemUseCase.execute(toCreateCommand(request)).map(ItemResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("item:update")
    public Mono<ItemResponse> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        return updateItemUseCase.execute(toUpdateCommand(id, request)).map(ItemResponse::from);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("item:archive")
    public Mono<ItemResponse> archiveItem(@PathVariable UUID id) {
        return archiveItemUseCase.archive(new ItemId(id)).map(ItemResponse::from);
    }

    @PostMapping("/{id}/activate")
    @RequiresPermission("item:update")
    public Mono<ItemResponse> activateItem(@PathVariable UUID id) {
        return activateItemUseCase.activate(new ItemId(id)).map(ItemResponse::from);
    }

    private static CreateItemCommand toCreateCommand(CreateItemRequest request) {
        return new CreateItemCommand(
                request.displayName(),
                request.code(),
                request.primaryOrganizationId()
        );
    }

    private static UpdateItemCommand toUpdateCommand(UUID id, UpdateItemRequest request) {
        return new UpdateItemCommand(
                new ItemId(id),
                request.displayName(),
                request.code(),
                request.primaryOrganizationId()
        );
    }
}
