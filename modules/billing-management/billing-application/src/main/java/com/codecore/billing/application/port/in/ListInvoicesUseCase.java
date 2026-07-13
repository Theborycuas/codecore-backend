package com.codecore.billing.application.port.in;

import com.codecore.billing.application.dto.AdminInvoiceView;
import com.codecore.billing.application.dto.PagedResult;
import com.codecore.billing.application.query.InvoiceListQuery;
import com.codecore.billing.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListInvoicesUseCase {

    Mono<PagedResult<AdminInvoiceView>> execute(InvoiceListQuery filter, PageQuery pageQuery);
}
