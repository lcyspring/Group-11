package com.meession.etm.module.crm.service.quote;

import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteItemDO;

import java.util.List;

public interface CrmBusinessQuoteService {
    Long createInitialDraft(Long businessId, CrmBusinessSaveReqVO reqVO, Long userId);

    Long requireDraftForUpdate(Long businessId);

    void syncDraft(Long quoteId, CrmBusinessSaveReqVO reqVO, Long userId);

    CrmBusinessQuoteDO lockQuote(Long businessId, String remark, Long userId);

    CrmBusinessQuoteDO reopenQuote(Long businessId, String remark, Long userId);

    void terminateCurrent(Long businessId, String remark, Long userId);

    QuoteSnapshot requireCurrentLocked(Long businessId);

    CrmBusinessQuoteDO getCurrent(Long businessId);

    List<CrmBusinessQuoteDO> getVersions(Long businessId);

    List<CrmBusinessQuoteItemDO> getItems(Long quoteId);

    List<CrmBusinessQuoteActionRecordDO> getActions(Long quoteId);

    record QuoteSnapshot(CrmBusinessQuoteDO quote, List<CrmBusinessQuoteItemDO> items) {
    }
}
