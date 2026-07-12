package com.codecore.inventory.application.port.in;

import com.codecore.inventory.application.command.UpdateItemCommand;
import com.codecore.inventory.application.dto.AdminItemView;
import reactor.core.publisher.Mono;

public interface UpdateItemUseCase {

    Mono<AdminItemView> execute(UpdateItemCommand command);
}
