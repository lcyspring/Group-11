package com.meession.etm.module.marketing.controller.admin.sms;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateBatchSendReqVO;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateMarketingPageReqVO;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateMarketingRespVO;
import com.meession.etm.module.marketing.service.sms.MarketingSmsTemplateService;
import com.meession.etm.module.system.dal.dataobject.sms.SmsTemplateDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 营销短信模板")
@RestController
@RequestMapping("/marketing/sms-template")
@Validated
public class MarketingSmsTemplateController {

    @Resource
    private MarketingSmsTemplateService marketingSmsTemplateService;

    @GetMapping("/page")
    @Operation(summary = "获得短信模板分页（营销视角）")
    @PreAuthorize("@ss.hasPermission('marketing:sms-template:query')")
    public CommonResult<PageResult<SmsTemplateMarketingRespVO>> getSmsTemplatePage(@Valid SmsTemplateMarketingPageReqVO pageVO) {
        PageResult<SmsTemplateDO> pageResult = marketingSmsTemplateService.getSmsTemplatePage(pageVO);
        return success(BeanUtils.toBean(pageResult, SmsTemplateMarketingRespVO.class));
    }

    @GetMapping("/list-simple")
    @Operation(summary = "获得可用短信模板精简列表（供营销活动选择）")
    @PreAuthorize("@ss.hasPermission('marketing:sms-template:query')")
    public CommonResult<List<SmsTemplateMarketingRespVO>> getSimpleSmsTemplateList() {
        List<SmsTemplateDO> list = marketingSmsTemplateService.getSimpleSmsTemplateList();
        return success(BeanUtils.toBean(list, SmsTemplateMarketingRespVO.class));
    }

    @PostMapping("/batch-send")
    @Operation(summary = "批量发送短信")
    @PreAuthorize("@ss.hasPermission('marketing:sms-template:batch-send')")
    public CommonResult<List<Long>> batchSendSms(@Valid @RequestBody SmsTemplateBatchSendReqVO batchSendReqVO) {
        List<Long> logIds = marketingSmsTemplateService.batchSendSms(batchSendReqVO);
        return success(logIds);
    }

}
