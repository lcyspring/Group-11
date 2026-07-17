package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.service.marketing.CrmMarketingOutreachService;
import com.meession.etm.framework.security.core.service.SecurityFrameworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "管理后台 - CRM 合规群发")
@RestController
@RequestMapping("/crm/marketing/outreach")
@Validated
public class CrmMarketingOutreachController {
    @Resource private CrmMarketingOutreachService service;
    @Resource private SecurityFrameworkService securityFrameworkService;

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

    @GetMapping("/broadcast/target-options")
    @Operation(summary = "获得当前用户可触达的客户和联系人")
    @PreAuthorize("@ss.hasAnyPermissions('crm:marketing-outreach:query', 'crm:marketing-outreach:update')")
    public CommonResult<CrmMarketingTargetOptionsRespVO> getTargetOptions() {
        Long userId = getLoginUserId();
        List<com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO> customers =
                service.getTargetCustomers(userId);
        List<com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO> contacts =
                service.getTargetContacts(customers);
        return success(new CrmMarketingTargetOptionsRespVO()
                .setCustomers(BeanUtils.toBean(customers, CrmMarketingTargetOptionsRespVO.Customer.class))
                .setContacts(BeanUtils.toBean(contacts, CrmMarketingTargetOptionsRespVO.Contact.class)));
    }

    @GetMapping("/broadcast/get")
    @Operation(summary = "获得群发任务详情")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:query')")
    public CommonResult<CrmMarketingBroadcastRespVO> getBroadcast(@RequestParam Long id) {
        Long userId = getLoginUserId();
        boolean privilegedReader = canReadAllBroadcasts();
        CrmMarketingBroadcastDO row = service.getBroadcast(id, userId, privilegedReader);
        CrmMarketingBroadcastRespVO response = toResponse(row);
        response.setCustomerIds(new ArrayList<>()).setContactIds(new ArrayList<>());
        service.getBroadcastRecipients(id, userId, privilegedReader).stream().forEach(recipient -> {
            if (recipient.getContactId() == null && recipient.getCustomerId() != null
                    && !response.getCustomerIds().contains(recipient.getCustomerId())) {
                response.getCustomerIds().add(recipient.getCustomerId());
            }
            if (recipient.getContactId() != null && !response.getContactIds().contains(recipient.getContactId())) {
                response.getContactIds().add(recipient.getContactId());
            }
        });
        return success(response);
    }

    @DeleteMapping("/broadcast/delete")
    @Operation(summary = "删除群发草稿")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:update')")
    public CommonResult<Boolean> deleteBroadcast(@RequestParam Long id) {
        service.deleteBroadcast(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/broadcast/page")
    @Operation(summary = "获得群发任务分页")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:query')")
    public CommonResult<PageResult<CrmMarketingBroadcastRespVO>> getBroadcastPage(@Valid CrmMarketingBroadcastPageReqVO request) {
        PageResult<CrmMarketingBroadcastDO> page = service.getBroadcastPage(
                request, getLoginUserId(), canReadAllBroadcasts());
        return success(new PageResult<>(page.getList().stream().map(this::toResponse).toList(), page.getTotal()));
    }

    private CrmMarketingBroadcastRespVO toResponse(CrmMarketingBroadcastDO row) {
        return BeanUtils.toBean(row, CrmMarketingBroadcastRespVO.class)
                .setCreatorUserId(service.parseCreatorUserId(row.getCreator()));
    }

    @GetMapping("/broadcast/recipients")
    @Operation(summary = "获得群发收件人结果")
    @PreAuthorize("@ss.hasPermission('crm:marketing-outreach:query')")
    public CommonResult<PageResult<CrmMarketingRecipientRespVO>> getRecipientPage(@Valid CrmMarketingRecipientPageReqVO request) {
        PageResult<CrmMarketingBroadcastRecipientDO> page = service.getRecipientPage(
                request, getLoginUserId(), canReadAllBroadcasts());
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

    private boolean canReadAllBroadcasts() {
        return securityFrameworkService.hasAnyPermissions(
                "crm:marketing-outreach:review", "crm:marketing-outreach:send");
    }
}
