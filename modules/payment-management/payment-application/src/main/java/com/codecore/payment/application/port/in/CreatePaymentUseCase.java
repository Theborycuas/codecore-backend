package com.codecore.payment.application.port.in;

import com.codecore.payment.application.command.CreatePaymentCommand;
import com.codecore.payment.application.dto.AdminPaymentView;
import reactor.core.publisher.Mono;

public interface CreatePaymentUseCase {

    Mono<AdminPaymentView> execute(CreatePaymentCommand command);
}
