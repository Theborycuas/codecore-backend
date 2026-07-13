package com.codecore.billing.application.port.in;

import com.codecore.billing.application.command.UpdateInvoiceCommand;
import com.codecore.billing.application.dto.AdminInvoiceView;
import reactor.core.publisher.Mono;

public interface UpdateInvoiceUseCase {

    Mono<AdminInvoiceView> execute(UpdateInvoiceCommand command);
}
