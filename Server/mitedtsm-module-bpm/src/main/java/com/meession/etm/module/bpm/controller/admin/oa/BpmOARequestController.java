package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOARequestDO;
import com.meession.etm.module.bpm.service.oa.BpmOARequestService;
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

@Tag(name = "管理后台 - OA请示审批")
@RestController
@RequestMapping("/bpm/oa/request")
@Validated
public class BpmOARequestController {

    @Resource
    private BpmOARequestService requestService;

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bpm:oa-request:create')")
    @Operation(summary = "创建请示审批")
    public CommonResult<Long> createRequest(@Valid @RequestBody BpmOARequestCreateReqVO createReqVO) {
        return success(requestService.createRequest(getLoginUserId(), createReqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bpm:oa-request:query')")
    @Operation(summary = "获得请示审批")
    @Parameter(name = "id", description = "请示审批ID", required = true, example = "1")
    public CommonResult<BpmOARequestRespVO> getRequest(@RequestParam("id") Long id) {
        BpmOARequestDO request = requestService.getRequest(id);
        return success(BpmOARequestRespVO.build(request));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bpm:oa-request:query')")
    @Operation(summary = "获得请示审批分页")
    public CommonResult<PageResult<BpmOARequestRespVO>> getRequestPage(@Valid BpmOARequestPageReqVO pageReqVO) {
        PageResult<BpmOARequestDO> pageResult = requestService.getRequestPage(getLoginUserId(), pageReqVO);
        return success(BeanUtils.toBean(pageResult, BpmOARequestRespVO.class));
    }

}