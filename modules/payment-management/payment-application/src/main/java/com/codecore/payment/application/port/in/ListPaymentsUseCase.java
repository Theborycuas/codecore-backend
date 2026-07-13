package com.codecore.payment.application.port.in;

import com.codecore.payment.application.dto.AdminPaymentView;
import com.codecore.payment.application.dto.PagedResult;
import com.codecore.payment.application.query.PageQuery;
import com.codecore.payment.application.query.PaymentListQuery;
import reactor.core.publisher.Mono;

public interface ListPaymentsUseCase {

    Mono<PagedResult<AdminPaymentView>> execute(PaymentListQuery filter, PageQuery pageQuery);
}
