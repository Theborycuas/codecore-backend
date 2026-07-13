package com.codecore.payment.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPermissionCatalogTest {

    @Test
    void shouldExposeThreePaymentLifecyclePermissions() {
        assertThat(PaymentPermissionCatalog.ALL).hasSize(3);
        assertThat(PaymentPermissionCatalog.PAYMENT_READ_ONLY)
                .containsExactly(PaymentPermissionCatalog.PAYMENT_READ);
    }

    @Test
    void shouldUseCreateReadVoidNotRefundOrCaptureOrPost() {
        assertThat(PaymentPermissionCatalog.ALL)
                .contains(PaymentPermissionCatalog.PAYMENT_CREATE)
                .contains(PaymentPermissionCatalog.PAYMENT_READ)
                .contains(PaymentPermissionCatalog.PAYMENT_VOID)
                .doesNotContain("payment:refund")
                .doesNotContain("payment:capture")
                .doesNotContain("payment:post")
                .doesNotContain("payment:update")
                .doesNotContain("payment:delete")
                .doesNotContain("payment:unvoid");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(PaymentPermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("retail")
                        || code.contains("tax")
                        || code.contains("ledger")
                        || code.contains("subscription")
        );
    }
}
