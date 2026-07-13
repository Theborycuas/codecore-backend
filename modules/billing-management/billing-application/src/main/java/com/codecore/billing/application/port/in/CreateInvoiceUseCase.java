package com.codecore.billing.application.port.in;

import com.codecore.billing.application.command.CreateInvoiceCommand;
import com.codecore.billing.application.dto.AdminInvoiceView;
import reactor.core.publisher.Mono;

public interface CreateInvoiceUseCase {

    Mono<AdminInvoiceView> execute(CreateInvoiceCommand command);
}
