package com.meession.etm.module.crm.framework.invoice;

import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CrmLocalLedgerInvoiceProviderTest {

    private final LocalLedgerCrmInvoiceProvider provider = new LocalLedgerCrmInvoiceProvider();

    @Test
    void localLedgerPreservesEveryCommandIdempotencyKeyWithoutClaimingExternalIssuance() {
        CrmInvoiceDO blue = new CrmInvoiceDO().setId(1L);
        CrmInvoiceDO red = new CrmInvoiceDO().setId(2L);
        assertResult(provider.issue(blue, "invoice:issue:1"), "invoice:issue:1");
        assertResult(provider.redFlush(blue, red, "invoice:red:1:R-1"), "invoice:red:1:R-1");
        assertResult(provider.voidInvoice(blue, "invoice:void:1"), "invoice:void:1");
    }

    private static void assertResult(CrmInvoiceProvider.ProviderResult result, String requestId) {
        assertEquals("local-ledger", result.providerCode());
        assertEquals(requestId, result.requestId());
        assertNull(result.externalInvoiceId());
    }
}
