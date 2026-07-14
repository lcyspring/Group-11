package com.meession.etm.module.crm.service.invoice;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.invoice.vo.*;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Collection;

public interface CrmInvoiceService {

    Long createInvoice(@Valid CrmInvoiceCreateReqVO reqVO, Long userId);

    void updateInvoice(@Valid CrmInvoiceUpdateReqVO reqVO, Long userId);

    void deleteInvoice(Long id, Long userId);

    void issueInvoice(@Valid CrmInvoiceIssueReqVO reqVO, Long userId);

    Long redFlushInvoice(@Valid CrmInvoiceRedFlushReqVO reqVO, Long userId);

    void voidInvoice(@Valid CrmInvoiceVoidReqVO reqVO, Long userId);

    CrmInvoiceDO getInvoice(Long id);

    List<CrmInvoiceDO> getInvoiceList(Collection<Long> ids);

    PageResult<CrmInvoiceDO> getInvoicePage(CrmInvoicePageReqVO reqVO, Long userId);

    List<CrmInvoiceActionRecordDO> getActionRecordList(Long invoiceId);

    CrmInvoiceSummaryRespVO getContractSummary(Long contractId);
}
