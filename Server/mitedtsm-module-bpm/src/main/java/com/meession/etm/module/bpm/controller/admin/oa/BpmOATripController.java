package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATripDO;
import com.meession.etm.module.bpm.service.oa.BpmOATripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - OA 出差申请")
@RestController
@RequestMapping("/bpm/oa/trip")
@Validated
public class BpmOATripController {

    @Resource
    private BpmOATripService tripService;

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bpm:oa-trip:create')")
    @Operation(summary = "创建出差申请并发起审批")
    public CommonResult<Long> createTrip(@Valid @RequestBody BpmOATripCreateReqVO request) {
        return success(tripService.createTrip(getLoginUserId(), request));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bpm:oa-trip:query')")
    @Operation(summary = "获得本人出差申请")
    public CommonResult<BpmOATripRespVO> getTrip(@RequestParam Long id) {
        return success(BeanUtils.toBean(tripService.getTrip(getLoginUserId(), id), BpmOATripRespVO.class));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bpm:oa-trip:query')")
    @Operation(summary = "获得本人出差申请分页")
    public CommonResult<PageResult<BpmOATripRespVO>> getTripPage(@Valid BpmOATripPageReqVO request) {
        PageResult<BpmOATripDO> page = tripService.getTripPage(getLoginUserId(), request);
        return success(BeanUtils.toBean(page, BpmOATripRespVO.class));
    }

    @GetMapping("/reimbursable-list")
    @PreAuthorize("@ss.hasPermission('bpm:oa-trip:query')")
    @Operation(summary = "获得本人已通过且已结束的可报销出差")
    public CommonResult<List<BpmOATripRespVO>> getReimbursableTrips() {
        return success(BeanUtils.toBean(tripService.getReimbursableTrips(getLoginUserId()), BpmOATripRespVO.class));
    }
}
