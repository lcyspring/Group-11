package com.meession.etm.module.crm.controller.admin.quote;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteActionReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteRespVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuotePolicyRespVO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.framework.quote.CrmQuoteProperties;
import com.meession.etm.module.crm.service.quote.CrmBusinessQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 商机报价")
@RestController
@RequestMapping("/crm/business-quote")
@Validated
public class CrmBusinessQuoteController {
    @Resource private CrmBusinessQuoteService quoteService;
    @Resource private CrmQuoteProperties quoteProperties;

    @GetMapping("/policy")
    @Operation(summary = "获得 YAML 报价计算策略")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<CrmQuotePolicyRespVO> getPolicy() {
        return success(new CrmQuotePolicyRespVO().setVersion(quoteProperties.getVersion())
                .setBaseCurrency(quoteProperties.getBaseCurrency())
                .setDefaultCurrency(quoteProperties.getDefaultCurrency())
                .setExchangeRatesToBase(quoteProperties.getExchangeRatesToBase())
                .setAllowedTaxRates(quoteProperties.getAllowedTaxRates())
                .setDefaultTaxRate(quoteProperties.getDefaultTaxRate())
                .setAmountScale(quoteProperties.getAmountScale())
                .setMaxVersionsPerBusiness(quoteProperties.getMaxVersionsPerBusiness()));
    }

    @GetMapping("/current")
    @Operation(summary = "获得商机当前报价版本")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#businessId",
            level = CrmPermissionLevelEnum.READ)
    public CommonResult<CrmQuoteRespVO> getCurrent(@RequestParam Long businessId) {
        CrmBusinessQuoteDO quote = quoteService.getCurrent(businessId);
        return success(quote == null ? null : buildDetail(quote));
    }

    @GetMapping("/versions")
    @Operation(summary = "获得商机报价版本列表")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#businessId",
            level = CrmPermissionLevelEnum.READ)
    public CommonResult<List<CrmQuoteRespVO>> getVersions(@RequestParam Long businessId) {
        return success(quoteService.getVersions(businessId).stream().map(this::buildDetail).toList());
    }

    @PutMapping("/lock")
    @Operation(summary = "锁定当前报价版本")
    @PreAuthorize("@ss.hasPermission('crm:business:quote:lock')")
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#reqVO.businessId",
            level = CrmPermissionLevelEnum.WRITE)
    public CommonResult<CrmQuoteRespVO> lock(@Valid @RequestBody CrmQuoteActionReqVO reqVO) {
        return success(buildDetail(quoteService.lockQuote(reqVO.getBusinessId(), reqVO.getRemark(), getLoginUserId())));
    }

    @PutMapping("/reopen")
    @Operation(summary = "从锁定报价重开新版本")
    @PreAuthorize("@ss.hasPermission('crm:business:quote:reopen')")
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#reqVO.businessId",
            level = CrmPermissionLevelEnum.WRITE)
    public CommonResult<CrmQuoteRespVO> reopen(@Valid @RequestBody CrmQuoteActionReqVO reqVO) {
        return success(buildDetail(quoteService.reopenQuote(reqVO.getBusinessId(), reqVO.getRemark(), getLoginUserId())));
    }

    private CrmQuoteRespVO buildDetail(CrmBusinessQuoteDO quote) {
        CrmQuoteRespVO response = BeanUtils.toBean(quote, CrmQuoteRespVO.class);
        response.setItems(BeanUtils.toBean(quoteService.getItems(quote.getId()), CrmQuoteRespVO.Item.class));
        response.setActions(BeanUtils.toBean(quoteService.getActions(quote.getId()), CrmQuoteRespVO.Action.class));
        return response;
    }
}
