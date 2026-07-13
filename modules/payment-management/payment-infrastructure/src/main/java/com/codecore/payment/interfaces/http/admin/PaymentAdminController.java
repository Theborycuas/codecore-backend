package com.codecore.payment.interfaces.http.admin;

import com.codecore.payment.application.command.CreatePaymentCommand;
import com.codecore.payment.application.port.in.CreatePaymentUseCase;
import com.codecore.payment.application.port.in.GetPaymentUseCase;
import com.codecore.payment.application.port.in.ListPaymentsUseCase;
import com.codecore.payment.application.port.in.VoidPaymentUseCase;
import com.codecore.payment.application.query.PageQueryParser;
import com.codecore.payment.application.query.PaymentListQuery;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.interfaces.http.admin.dto.CreatePaymentRequest;
import com.codecore.payment.interfaces.http.admin.dto.PagedPaymentResponse;
import com.codecore.payment.interfaces.http.admin.dto.PaymentResponse;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(PaymentAdminApiPaths.PAYMENTS)
@Tag(name = "Payments", description = "Payment settlement-record administration (`payment:*` permissions)")
public class PaymentAdminController {

    private final ListPaymentsUseCase listPaymentsUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final VoidPaymentUseCase voidPaymentUseCase;

    public PaymentAdminController(
            ListPaymentsUseCase listPaymentsUseCase,
            GetPaymentUseCase getPaymentUseCase,
            CreatePaymentUseCase createPaymentUseCase,
            VoidPaymentUseCase voidPaymentUseCase
    ) {
        this.listPaymentsUseCase = listPaymentsUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.createPaymentUseCase = createPaymentUseCase;
        this.voidPaymentUseCase = voidPaymentUseCase;
    }

    @GetMapping
    @RequiresPermission("payment:read")
    public Mono<PagedPaymentResponse> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordedAt,desc") String sort,
            @RequestParam(defaultValue = "RECORDED") String status,
            @RequestParam(required = false) UUID invoiceId
    ) {
        PaymentListQuery filter = PaymentListQuery.of(status, invoiceId);
        return listPaymentsUseCase
                .execute(filter, PageQueryParser.parsePaymentPageQuery(page, size, sort))
                .map(PagedPaymentResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("payment:read")
    public Mono<PaymentResponse> getPayment(@PathVariable UUID id) {
        return getPaymentUseCase.execute(new PaymentId(id)).map(PaymentResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("payment:create")
    public Mono<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return createPaymentUseCase.execute(toCreateCommand(request)).map(PaymentResponse::from);
    }

    @PostMapping("/{id}/void")
    @RequiresPermission("payment:void")
    public Mono<PaymentResponse> voidPayment(@PathVariable UUID id) {
        return voidPaymentUseCase.voidPayment(new PaymentId(id)).map(PaymentResponse::from);
    }

    private static CreatePaymentCommand toCreateCommand(CreatePaymentRequest request) {
        return new CreatePaymentCommand(
                request.invoiceId(),
                request.currency(),
                request.amountMinor(),
                request.paymentMethodCode()
        );
    }
}
