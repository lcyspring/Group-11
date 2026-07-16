package com.meession.etm.module.marketing.controller.admin.campaign;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignPageReqVO;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignRespVO;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.service.campaign.MarketingCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 营销活动")
@RestController
@RequestMapping("/marketing/campaign")
@Validated
public class MarketingCampaignController {

    @Resource
    private MarketingCampaignService campaignService;

    @PostMapping("/create")
    @Operation(summary = "创建营销活动")
    @PreAuthorize("@ss.hasPermission('marketing:campaign:create')")
    public CommonResult<Long> createCampaign(@Valid @RequestBody CampaignSaveReqVO createReqVO) {
        return success(campaignService.createCampaign(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新营销活动")
    @PreAuthorize("@ss.hasPermission('marketing:campaign:update')")
    public CommonResult<Boolean> updateCampaign(@Valid @RequestBody CampaignSaveReqVO updateReqVO) {
        campaignService.updateCampaign(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除营销活动")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('marketing:campaign:delete')")
    public CommonResult<Boolean> deleteCampaign(@RequestParam("id") Long id) {
        campaignService.deleteCampaign(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得营销活动")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('marketing:campaign:query')")
    public CommonResult<CampaignRespVO> getCampaign(@RequestParam("id") Long id) {
        MarketingCampaignDO campaign = campaignService.getCampaign(id);
        return success(BeanUtils.toBean(campaign, CampaignRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得营销活动分页")
    @PreAuthorize("@ss.hasPermission('marketing:campaign:query')")
    public CommonResult<PageResult<CampaignRespVO>> getCampaignPage(@Valid CampaignPageReqVO pageVO) {
        PageResult<MarketingCampaignDO> pageResult = campaignService.getCampaignPage(pageVO);
        return success(BeanUtils.toBean(pageResult, CampaignRespVO.class));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交审核")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('marketing:campaign:submit')")
    public CommonResult<String> submitApproval(@RequestParam("id") Long id) {
        String processInstanceId = campaignService.submitApproval(id, getLoginUserId());
        return success(processInstanceId);
    }

    @PutMapping("/start")
    @Operation(summary = "启动营销活动")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('marketing:campaign:start')")
    public CommonResult<Boolean> startCampaign(@RequestParam("id") Long id) {
        campaignService.startCampaign(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消营销活动")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('marketing:campaign:cancel')")
    public CommonResult<Boolean> cancelCampaign(@RequestParam("id") Long id) {
        campaignService.cancelCampaign(id);
        return success(true);
    }

}
