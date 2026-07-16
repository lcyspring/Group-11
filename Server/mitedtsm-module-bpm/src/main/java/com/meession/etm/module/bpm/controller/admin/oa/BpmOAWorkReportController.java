package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportRespVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportUpdateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkReportDO;
import com.meession.etm.module.bpm.service.oa.BpmOAWorkReportService;
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

@Tag(name = "管理后台 - OA工作报告")
@RestController
@RequestMapping("/bpm/oa/work-report")
@Validated
public class BpmOAWorkReportController {

    @Resource
    private BpmOAWorkReportService workReportService;

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bpm:oa-work-report:create')")
    @Operation(summary = "创建工作报告")
    public CommonResult<Long> createWorkReport(@Valid @RequestBody BpmOAWorkReportCreateReqVO createReqVO) {
        return success(workReportService.createWorkReport(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bpm:oa-work-report:update')")
    @Operation(summary = "更新工作报告")
    public CommonResult<Long> updateWorkReport(@Valid @RequestBody BpmOAWorkReportUpdateReqVO updateReqVO) {
        workReportService.updateWorkReport(updateReqVO);
        return success(updateReqVO.getId());
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bpm:oa-work-report:delete')")
    @Operation(summary = "删除工作报告")
    @Parameter(name = "id", description = "工作报告ID", required = true, example = "1")
    public CommonResult<Long> deleteWorkReport(@RequestParam("id") Long id) {
        workReportService.deleteWorkReport(id);
        return success(id);
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bpm:oa-work-report:query')")
    @Operation(summary = "获得工作报告")
    @Parameter(name = "id", description = "工作报告ID", required = true, example = "1")
    public CommonResult<BpmOAWorkReportRespVO> getWorkReport(@RequestParam("id") Long id) {
        BpmOAWorkReportDO report = workReportService.getWorkReport(id);
        return success(BpmOAWorkReportRespVO.build(report));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bpm:oa-work-report:query')")
    @Operation(summary = "获得工作报告分页")
    public CommonResult<PageResult<BpmOAWorkReportRespVO>> getWorkReportPage(@Valid BpmOAWorkReportPageReqVO pageReqVO) {
        PageResult<BpmOAWorkReportDO> pageResult = workReportService.getWorkReportPage(getLoginUserId(), pageReqVO);
        return success(BeanUtils.toBean(pageResult, BpmOAWorkReportRespVO.class));
    }

}