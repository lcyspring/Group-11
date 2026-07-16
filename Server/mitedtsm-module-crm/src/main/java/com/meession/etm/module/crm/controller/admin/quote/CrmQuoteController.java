package com.meession.etm.module.crm.controller.admin.quote;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteCalculateReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteCalculateRespVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteDiscountCalculateReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteDiscountCalculateRespVO;
import com.meession.etm.module.crm.service.quote.CrmQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 产品报价")
@RestController
@RequestMapping("/crm/quote")
@Validated
public class CrmQuoteController {

    @Resource
    private CrmQuoteService quoteService;

    @PostMapping("/calculate")
    @Operation(summary = "计算产品报价")
    @PreAuthorize("@ss.hasPermission('crm:quote:calculate')")
    public CommonResult<CrmQuoteCalculateRespVO> calculateQuote(@Valid @RequestBody CrmQuoteCalculateReqVO reqVO) {
        return success(quoteService.calculateQuote(reqVO));
    }

    @PostMapping("/discount-calculate")
    @Operation(summary = "计算报价折扣")
    @PreAuthorize("@ss.hasPermission('crm:quote:discount-calculate')")
    public CommonResult<CrmQuoteDiscountCalculateRespVO> calculateDiscount(@Valid @RequestBody CrmQuoteDiscountCalculateReqVO reqVO) {
        return success(quoteService.calculateDiscount(reqVO));
    }

}
