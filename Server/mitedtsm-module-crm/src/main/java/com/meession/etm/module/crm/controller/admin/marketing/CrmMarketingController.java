package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.*;
import com.meession.etm.module.crm.service.marketing.CrmMarketingService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 营销互动")
@RestController
@RequestMapping("/crm/marketing")
@Validated
public class CrmMarketingController {
    @Resource private CrmMarketingService marketingService;
    @Resource private AdminUserApi adminUserApi;

    @PostMapping("/campaign/save")
    @Operation(summary = "新增或更新营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:update')")
    public CommonResult<Long> saveCampaign(@Valid @RequestBody CrmMarketingCampaignSaveReqVO request) {
        return success(marketingService.saveCampaign(request, getLoginUserId()));
    }

    @GetMapping("/campaign/get")
    @Operation(summary = "获得营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:query')")
    public CommonResult<CrmMarketingCampaignRespVO> getCampaign(@RequestParam Long id) {
        return success(buildCampaign(marketingService.getCampaign(id, getLoginUserId())));
    }

    @GetMapping("/campaign/page")
    @Operation(summary = "获得营销活动分页")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:query')")
    public CommonResult<PageResult<CrmMarketingCampaignRespVO>> getCampaignPage(@Valid CrmMarketingCampaignPageReqVO request) {
        PageResult<CrmMarketingCampaignDO> page = marketingService.getCampaignPage(request, getLoginUserId());
        return success(new PageResult<>(page.getList().stream().map(this::buildCampaign).toList(), page.getTotal()));
    }

    @GetMapping("/campaign/relations")
    @Operation(summary = "获得营销活动关联对象")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:query')")
    public CommonResult<List<CrmMarketingRelationReqVO>> getCampaignRelations(@RequestParam Long id) {
        return success(BeanUtils.toBean(marketingService.getCampaignRelations(id, getLoginUserId()),
                CrmMarketingRelationReqVO.class));
    }

    @PutMapping("/campaign/start")
    @Operation(summary = "启动营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:update')")
    public CommonResult<Boolean> startCampaign(@RequestParam Long id) {
        marketingService.startCampaign(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/campaign/lock")
    @Operation(summary = "锁定营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:update')")
    public CommonResult<Boolean> lockCampaign(@RequestParam Long id) {
        marketingService.lockCampaign(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/campaign/terminate")
    @Operation(summary = "终止营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:update')")
    public CommonResult<Boolean> terminateCampaign(@Valid @RequestBody CrmMarketingCampaignActionReqVO request) {
        marketingService.terminateCampaign(request, getLoginUserId());
        return success(true);
    }

    @PutMapping("/campaign/complete")
    @Operation(summary = "完成营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:update')")
    public CommonResult<Boolean> completeCampaign(@Valid @RequestBody CrmMarketingCampaignActionReqVO request) {
        marketingService.completeCampaign(request, getLoginUserId());
        return success(true);
    }

    @PostMapping("/competitor/save")
    @Operation(summary = "新增或更新竞争对手资料")
    @PreAuthorize("@ss.hasPermission('crm:competitor:update')")
    public CommonResult<Long> saveCompetitor(@Valid @RequestBody CrmCompetitorSaveReqVO request) {
        return success(marketingService.saveCompetitor(request, getLoginUserId()));
    }

    @GetMapping("/competitor/page")
    @Operation(summary = "获得竞争对手资料分页")
    @PreAuthorize("@ss.hasPermission('crm:competitor:query')")
    public CommonResult<PageResult<CrmCompetitorRespVO>> getCompetitorPage(@Valid CrmCompetitorPageReqVO request) {
        PageResult<CrmCompetitorDO> page = marketingService.getCompetitorPage(request, getLoginUserId());
        Set<Long> userIds = page.getList().stream().map(CrmCompetitorDO::getOwnerUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        List<CrmCompetitorRespVO> list = BeanUtils.toBean(page.getList(), CrmCompetitorRespVO.class, item -> {
            AdminUserRespDTO user = users.get(item.getOwnerUserId());
            if (user != null) item.setOwnerUserName(user.getNickname());
        });
        return success(new PageResult<>(list, page.getTotal()));
    }

    @DeleteMapping("/competitor/delete")
    @Operation(summary = "删除竞争对手资料")
    @PreAuthorize("@ss.hasPermission('crm:competitor:delete')")
    public CommonResult<Boolean> deleteCompetitor(@RequestParam Long id) {
        marketingService.deleteCompetitor(id, getLoginUserId());
        return success(true);
    }

    private CrmMarketingCampaignRespVO buildCampaign(CrmMarketingCampaignDO row) {
        CrmMarketingCampaignRespVO result = BeanUtils.toBean(row, CrmMarketingCampaignRespVO.class);
        result.setRelations(BeanUtils.toBean(marketingService.getCampaignRelations(row.getId(), getLoginUserId()),
                CrmMarketingRelationReqVO.class));
        AdminUserRespDTO user = adminUserApi.getUserMap(Set.of(row.getOwnerUserId())).get(row.getOwnerUserId());
        if (user != null) result.setOwnerUserName(user.getNickname());
        return result;
    }
}
