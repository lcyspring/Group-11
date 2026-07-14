package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignPageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignRespVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import com.meession.etm.module.crm.service.marketing.CrmMarketingCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 营销活动")
@RestController
@RequestMapping("/crm/marketing-campaign")
@Validated
public class CrmMarketingCampaignController {

    @Resource
    private CrmMarketingCampaignService campaignService;

    @PostMapping("/create")
    @Operation(summary = "创建营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:create')")
    public CommonResult<Long> createCampaign(@Valid @RequestBody CrmMarketingCampaignSaveReqVO createReqVO) {
        return success(campaignService.createCampaign(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新营销活动")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:update')")
    public CommonResult<Boolean> updateCampaign(@Valid @RequestBody CrmMarketingCampaignSaveReqVO updateReqVO) {
        campaignService.updateCampaign(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除营销活动")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:delete')")
    public CommonResult<Boolean> deleteCampaign(@RequestParam("id") Long id) {
        campaignService.deleteCampaign(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得营销活动")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:query')")
    public CommonResult<CrmMarketingCampaignRespVO> getCampaign(@RequestParam("id") Long id) {
        CrmMarketingCampaignDO campaign = campaignService.getCampaign(id);
        return success(BeanUtils.toBean(campaign, CrmMarketingCampaignRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得营销活动分页")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:query')")
    public CommonResult<PageResult<CrmMarketingCampaignRespVO>> getCampaignPage(@Valid CrmMarketingCampaignPageReqVO pageVO) {
        PageResult<CrmMarketingCampaignDO> pageResult = campaignService.getCampaignPage(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmMarketingCampaignRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出营销活动 Excel")
    @PreAuthorize("@ss.hasPermission('crm:marketing-campaign:export')")
    public void exportCampaignExcel(@Valid CrmMarketingCampaignPageReqVO exportReqVO,
                                     HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CrmMarketingCampaignDO> list = campaignService.getCampaignPage(exportReqVO).getList();
        ExcelUtils.write(response, "营销活动.xls", "数据", CrmMarketingCampaignRespVO.class,
                BeanUtils.toBean(list, CrmMarketingCampaignRespVO.class));
    }

}
