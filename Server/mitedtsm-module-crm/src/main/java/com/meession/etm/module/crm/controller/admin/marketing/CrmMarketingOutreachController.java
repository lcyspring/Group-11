package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.service.marketing.CrmMarketingOutreachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 合规群发")
@RestController
@RequestMapping("/crm/marketing/outreach")
@Validated
public class CrmMarketingOutreachController {
    @Resource private CrmMarketingOutreachService service;

    @PostMapping("/consent/save")
    @Operation(summary = "保存客户营销同意或退订")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:consent')")
    public CommonResult<Boolean> saveConsent(@Valid @RequestBody CrmMarketingConsentSaveReqVO request) {
        service.saveConsent(request, getLoginUserId()); return success(true);
    }

    @PostMapping("/broadcast/save")
    @Operation(summary = "保存营销群发任务")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:update')")
    public CommonResult<Long> saveBroadcast(@Valid @RequestBody CrmMarketingBroadcastSaveReqVO request) {
        return success(service.saveBroadcast(request, getLoginUserId()));
    }

    @GetMapping("/broadcast/page")
    @Operation(summary = "获得群发任务分页")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:query')")
    public CommonResult<PageResult<CrmMarketingBroadcastRespVO>> getBroadcastPage(@Valid CrmMarketingBroadcastPageReqVO request) {
        PageResult<CrmMarketingBroadcastDO> page = service.getBroadcastPage(request);
        return success(new PageResult<>(BeanUtils.toBean(page.getList(), CrmMarketingBroadcastRespVO.class), page.getTotal()));
    }

    @GetMapping("/broadcast/recipients")
    @Operation(summary = "获得群发收件人结果")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:query')")
    public CommonResult<PageResult<CrmMarketingRecipientRespVO>> getRecipientPage(@Valid CrmMarketingRecipientPageReqVO request) {
        PageResult<CrmMarketingBroadcastRecipientDO> page = service.getRecipientPage(request);
        return success(new PageResult<>(BeanUtils.toBean(page.getList(), CrmMarketingRecipientRespVO.class), page.getTotal()));
    }

    @PutMapping("/broadcast/submit-review")
    @Operation(summary = "提交群发审核")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:update')")
    public CommonResult<Boolean> submitReview(@RequestParam Long id) { service.submitReview(id, getLoginUserId()); return success(true); }

    @PutMapping("/broadcast/approve")
    @Operation(summary = "通过群发审核")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:review')")
    public CommonResult<Boolean> approve(@Valid @RequestBody CrmMarketingReviewReqVO request) { service.review(request, getLoginUserId(), true); return success(true); }

    @PutMapping("/broadcast/reject")
    @Operation(summary = "驳回群发审核")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:review')")
    public CommonResult<Boolean> reject(@Valid @RequestBody CrmMarketingReviewReqVO request) { service.review(request, getLoginUserId(), false); return success(true); }

    @PutMapping("/broadcast/send")
    @Operation(summary = "发送已审核群发")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:send')")
    public CommonResult<Boolean> send(@RequestParam Long id) { service.send(id); return success(true); }

    @PutMapping("/broadcast/retry")
    @Operation(summary = "重试失败收件人")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:send')")
    public CommonResult<Boolean> retry(@RequestParam Long id) { service.retryFailed(id); return success(true); }
}
