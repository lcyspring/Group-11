package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABusinessTripDO;
import com.meession.etm.module.bpm.service.oa.BpmOABusinessTripService;
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

@Tag(name = "管理后台 - OA 出差申请")
@RestController
@RequestMapping("/bpm/oa/business-trip")
@Validated
public class BpmOABusinessTripController {

    @Resource
    private BpmOABusinessTripService businessTripService;

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bpm:oa-business-trip:create')")
    @Operation(summary = "创建出差申请")
    public CommonResult<Long> createBusinessTrip(@Valid @RequestBody BpmOABusinessTripCreateReqVO createReqVO) {
        return success(businessTripService.createBusinessTrip(getLoginUserId(), createReqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bpm:oa-business-trip:query')")
    @Operation(summary = "获得出差申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<BpmOABusinessTripRespVO> getBusinessTrip(@RequestParam("id") Long id) {
        BpmOABusinessTripDO businessTrip = businessTripService.getBusinessTrip(id);
        return success(BeanUtils.toBean(businessTrip, BpmOABusinessTripRespVO.class));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bpm:oa-business-trip:query')")
    @Operation(summary = "获得出差申请分页")
    public CommonResult<PageResult<BpmOABusinessTripRespVO>> getBusinessTripPage(@Valid BpmOABusinessTripPageReqVO pageVO) {
        PageResult<BpmOABusinessTripDO> pageResult = businessTripService.getBusinessTripPage(getLoginUserId(), pageVO);
        return success(BeanUtils.toBean(pageResult, BpmOABusinessTripRespVO.class));
    }

}