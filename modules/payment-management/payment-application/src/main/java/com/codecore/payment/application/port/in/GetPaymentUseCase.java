package com.codecore.payment.application.port.in;

import com.codecore.payment.application.dto.AdminPaymentView;
import com.codecore.payment.domain.valueobject.PaymentId;
import reactor.core.publisher.Mono;

public interface GetPaymentUseCase {

    Mono<AdminPaymentView> execute(PaymentId paymentId);
}
