package com.codecore.inventory.application.port.in;

import com.codecore.inventory.application.command.CreateItemCommand;
import com.codecore.inventory.application.dto.AdminItemView;
import reactor.core.publisher.Mono;

public interface CreateItemUseCase {

    Mono<AdminItemView> execute(CreateItemCommand command);
}
