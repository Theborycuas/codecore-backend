package com.codecore.billing.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePermissionCatalogTest {

    @Test
    void shouldExposeFiveInvoiceLifecyclePermissions() {
        assertThat(InvoicePermissionCatalog.ALL).hasSize(5);
        assertThat(InvoicePermissionCatalog.INVOICE_READ_ONLY)
                .containsExactly(InvoicePermissionCatalog.INVOICE_READ);
    }

    @Test
    void shouldUseIssueAndVoidNotPayOrPost() {
        assertThat(InvoicePermissionCatalog.ALL)
                .contains(InvoicePermissionCatalog.INVOICE_ISSUE)
                .contains(InvoicePermissionCatalog.INVOICE_VOID)
                .doesNotContain("invoice:pay")
                .doesNotContain("invoice:post")
                .doesNotContain("invoice:tax")
                .doesNotContain("invoice:delete")
                .doesNotContain("invoice:unvoid");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(InvoicePermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("retail")
                        || code.contains("tax")
                        || code.contains("ledger")
                        || code.contains("subscription")
                        || code.contains("payment")
        );
    }
}
