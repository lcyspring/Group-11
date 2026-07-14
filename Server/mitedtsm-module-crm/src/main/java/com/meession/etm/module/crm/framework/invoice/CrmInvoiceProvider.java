package com.meession.etm.module.crm.framework.invoice;

import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;

/**
 * 税务/开票平台适配边界。实现不得直接更新 CRM 发票表，所有状态变更由 InvoiceService 统一提交。
 */
public interface CrmInvoiceProvider {

    String providerCode();

    ProviderResult issue(CrmInvoiceDO invoice, String requestId);

    ProviderResult voidInvoice(CrmInvoiceDO invoice, String requestId);

    ProviderResult redFlush(CrmInvoiceDO original, CrmInvoiceDO redInvoice, String requestId);

    record ProviderResult(String providerCode, String requestId, String externalInvoiceId) {
    }
}
