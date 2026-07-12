package com.codecore.inventory.application.port.in;

import com.codecore.inventory.application.dto.AdminItemView;
import com.codecore.inventory.domain.valueobject.ItemId;
import reactor.core.publisher.Mono;

public interface ArchiveItemUseCase {

    Mono<AdminItemView> archive(ItemId itemId);
}
