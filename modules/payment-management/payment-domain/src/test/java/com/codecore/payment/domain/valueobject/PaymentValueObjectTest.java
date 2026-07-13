package com.codecore.payment.domain.valueobject;

import com.codecore.payment.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentValueObjectTest {

    @Test
    void shouldGenerateDistinctPaymentIds() {
        assertThat(PaymentId.generate()).isNotEqualTo(PaymentId.generate());
    }

    @Test
    void shouldParsePaymentIdFromString() {
        UUID uuid = UUID.randomUUID();
        assertThat(new PaymentId(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void shouldEqualTenantIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(new TenantId(uuid)).isEqualTo(new TenantId(uuid.toString()));
    }

    @Test
    void shouldEqualInvoiceIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(InvoiceId.of(uuid)).isEqualTo(new InvoiceId(uuid.toString()));
    }

    @Test
    void shouldCreateMoneyWithNormalizedCurrency() {
        Money money = Money.of("usd", 1500);
        assertThat(money.currency()).isEqualTo("USD");
        assertThat(money.amountMinor()).isEqualTo(1500);
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        assertThatThrownBy(() -> Money.of("US", 100))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("ISO 4217");
        assertThatThrownBy(() -> Money.of("1234", 100))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("ISO 4217");
    }

    @Test
    void shouldRejectZeroOrNegativeAmountMinor() {
        assertThatThrownBy(() -> Money.of("USD", 0))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("amountMinor");
        assertThatThrownBy(() -> Money.of("USD", -1))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("amountMinor");
    }

    @Test
    void shouldTrimPaymentMethodCode() {
        assertThat(PaymentMethodCode.of("  CASH  ").value()).isEqualTo("CASH");
    }

    @Test
    void shouldRejectBlankPaymentMethodCode() {
        assertThatThrownBy(() -> PaymentMethodCode.of(" "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("payment method code");
    }

    @Test
    void shouldRejectOversizedPaymentMethodCode() {
        String oversized = "m".repeat(33);
        assertThatThrownBy(() -> PaymentMethodCode.of(oversized))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("payment method code");
    }

    @Test
    void shouldAcceptMaxLengthPaymentMethodCode() {
        String maxLength = "m".repeat(32);
        assertThat(PaymentMethodCode.of(maxLength).value()).hasSize(32);
    }

    @Test
    void shouldFreezePaymentStatusEnum() {
        assertThat(PaymentStatus.values())
                .containsExactly(PaymentStatus.RECORDED, PaymentStatus.VOIDED);
    }
}
