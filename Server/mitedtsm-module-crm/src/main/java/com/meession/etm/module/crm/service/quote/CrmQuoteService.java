package com.meession.etm.module.crm.service.quote;

import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteCalculateReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteCalculateRespVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteDiscountCalculateReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteDiscountCalculateRespVO;

public interface CrmQuoteService {

    CrmQuoteCalculateRespVO calculateQuote(CrmQuoteCalculateReqVO reqVO);

    CrmQuoteDiscountCalculateRespVO calculateDiscount(CrmQuoteDiscountCalculateReqVO reqVO);

}
