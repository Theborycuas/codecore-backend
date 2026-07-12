package com.codecore.inventory.application.port.in;

import com.codecore.inventory.application.dto.AdminItemView;
import com.codecore.inventory.application.dto.PagedResult;
import com.codecore.inventory.application.query.ItemListQuery;
import com.codecore.inventory.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListItemsUseCase {

    Mono<PagedResult<AdminItemView>> execute(ItemListQuery filter, PageQuery pageQuery);
}
