package com.codecore.billing.application.port.in;

import com.codecore.billing.application.dto.AdminInvoiceView;
import com.codecore.billing.domain.valueobject.InvoiceId;
import reactor.core.publisher.Mono;

public interface IssueInvoiceUseCase {

    Mono<AdminInvoiceView> issue(InvoiceId invoiceId);
}
