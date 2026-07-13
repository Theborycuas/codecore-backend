package com.codecore.billing.domain.valueobject;

import com.codecore.billing.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceValueObjectTest {

    @Test
    void shouldGenerateDistinctInvoiceIds() {
        assertThat(InvoiceId.generate()).isNotEqualTo(InvoiceId.generate());
    }

    @Test
    void shouldParseInvoiceIdFromString() {
        UUID uuid = UUID.randomUUID();
        assertThat(new InvoiceId(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void shouldGenerateDistinctInvoiceLineIds() {
        assertThat(InvoiceLineId.generate()).isNotEqualTo(InvoiceLineId.generate());
    }

    @Test
    void shouldEqualTenantIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(new TenantId(uuid)).isEqualTo(new TenantId(uuid.toString()));
    }

    @Test
    void shouldEqualOrganizationIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(OrganizationId.of(uuid)).isEqualTo(new OrganizationId(uuid.toString()));
    }

    @Test
    void shouldEqualBillToPatientIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(BillToPatientId.of(uuid)).isEqualTo(new BillToPatientId(uuid.toString()));
    }

    @Test
    void shouldEqualBillToOrganizationIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(BillToOrganizationId.of(uuid)).isEqualTo(new BillToOrganizationId(uuid.toString()));
    }

    @Test
    void shouldEqualItemIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(ItemId.of(uuid)).isEqualTo(new ItemId(uuid.toString()));
    }

    @Test
    void shouldEqualEncounterIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(EncounterId.of(uuid)).isEqualTo(new EncounterId(uuid.toString()));
    }

    @Test
    void shouldTrimInvoiceNumber() {
        assertThat(InvoiceNumber.of("  INV-0001  ").value()).isEqualTo("INV-0001");
    }

    @Test
    void shouldRejectBlankInvoiceNumber() {
        assertThatThrownBy(() -> InvoiceNumber.of(" "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("invoice number");
    }

    @Test
    void shouldRejectOversizedInvoiceNumber() {
        String oversized = "n".repeat(65);
        assertThatThrownBy(() -> InvoiceNumber.of(oversized))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("invoice number");
    }

    @Test
    void shouldTrimLineDescription() {
        assertThat(LineDescription.of("  Consultation fee  ").value()).isEqualTo("Consultation fee");
    }

    @Test
    void shouldRejectBlankLineDescription() {
        assertThatThrownBy(() -> LineDescription.of(" "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("line description");
    }

    @Test
    void shouldRejectOversizedLineDescription() {
        String oversized = "d".repeat(501);
        assertThatThrownBy(() -> LineDescription.of(oversized))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("line description");
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
    void shouldFreezeInvoiceStatusEnum() {
        assertThat(InvoiceStatus.values())
                .containsExactly(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED, InvoiceStatus.VOIDED);
    }

    @Test
    void shouldBuildPatientBillTo() {
        BillToPatientId patientId = BillToPatientId.of(UUID.randomUUID());
        BillTo billTo = BillTo.patient(patientId);

        assertThat(billTo.isPatient()).isTrue();
        assertThat(billTo.isOrganization()).isFalse();
        assertThat(billTo.patientId()).contains(patientId);
        assertThat(billTo.organizationId()).isEmpty();
    }

    @Test
    void shouldBuildOrganizationBillTo() {
        BillToOrganizationId organizationId = BillToOrganizationId.of(UUID.randomUUID());
        BillTo billTo = BillTo.organization(organizationId);

        assertThat(billTo.isOrganization()).isTrue();
        assertThat(billTo.isPatient()).isFalse();
        assertThat(billTo.organizationId()).contains(organizationId);
        assertThat(billTo.patientId()).isEmpty();
    }

    @Test
    void shouldRejectBillToOfWithBothOrNeitherPresent() {
        assertThatThrownBy(() -> BillTo.of(null, null))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("Exactly one");

        assertThatThrownBy(() -> BillTo.of(
                BillToPatientId.of(UUID.randomUUID()),
                BillToOrganizationId.of(UUID.randomUUID())
        )).isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("Exactly one");
    }
}
