package com.meession.etm.module.crm.framework.invoice;

import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** CRM 内部台账适配器；不假称已向税务平台开票。 */
@Component
@ConditionalOnProperty(prefix = "mitedtsm.crm.invoice", name = "provider", havingValue = "local-ledger")
public class LocalLedgerCrmInvoiceProvider implements CrmInvoiceProvider {

    @Override
    public String providerCode() {
        return "local-ledger";
    }

    @Override
    public ProviderResult issue(CrmInvoiceDO invoice, String requestId) {
        return new ProviderResult(providerCode(), requestId, null);
    }

    @Override
    public ProviderResult voidInvoice(CrmInvoiceDO invoice, String requestId) {
        return new ProviderResult(providerCode(), requestId, null);
    }

    @Override
    public ProviderResult redFlush(CrmInvoiceDO original, CrmInvoiceDO redInvoice, String requestId) {
        return new ProviderResult(providerCode(), requestId, null);
    }
}
