package com.meession.etm.module.marketing.controller.admin.mail;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateBatchSendReqVO;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateMarketingPageReqVO;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateMarketingRespVO;
import com.meession.etm.module.marketing.service.mail.MarketingMailTemplateService;
import com.meession.etm.module.system.dal.dataobject.mail.MailTemplateDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 营销邮件模板")
@RestController
@RequestMapping("/marketing/mail-template")
@Validated
public class MarketingMailTemplateController {

    @Resource
    private MarketingMailTemplateService marketingMailTemplateService;

    @GetMapping("/page")
    @Operation(summary = "获得邮件模板分页（营销视角）")
    @PreAuthorize("@ss.hasPermission('marketing:mail-template:query')")
    public CommonResult<PageResult<MailTemplateMarketingRespVO>> getMailTemplatePage(@Valid MailTemplateMarketingPageReqVO pageVO) {
        PageResult<MailTemplateDO> pageResult = marketingMailTemplateService.getMailTemplatePage(pageVO);
        return success(BeanUtils.toBean(pageResult, MailTemplateMarketingRespVO.class));
    }

    @GetMapping("/list-simple")
    @Operation(summary = "获得可用邮件模板精简列表（供营销活动选择）")
    @PreAuthorize("@ss.hasPermission('marketing:mail-template:query')")
    public CommonResult<List<MailTemplateMarketingRespVO>> getSimpleMailTemplateList() {
        List<MailTemplateDO> list = marketingMailTemplateService.getSimpleMailTemplateList();
        return success(BeanUtils.toBean(list, MailTemplateMarketingRespVO.class));
    }

    @PostMapping("/batch-send")
    @Operation(summary = "批量发送邮件")
    @PreAuthorize("@ss.hasPermission('marketing:mail-template:batch-send')")
    public CommonResult<List<Long>> batchSendMail(@Valid @RequestBody MailTemplateBatchSendReqVO batchSendReqVO) {
        List<Long> logIds = marketingMailTemplateService.batchSendMail(batchSendReqVO);
        return success(logIds);
    }

}
