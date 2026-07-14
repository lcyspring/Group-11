package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABorrowDO;
import com.meession.etm.module.bpm.service.oa.BpmOABorrowService;
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

/**
 * 管理后台 - OA 借款申请控制器
 *
 * @author 李春雨
 */
@Tag(name = "管理后台 - OA 借款申请")
@RestController
@RequestMapping("/bpm/oa/borrow")
@Validated
public class BpmOABorrowController {

    @Resource
    private BpmOABorrowService borrowService;

    /**
     * 创建借款申请
     *
     * @param createReqVO 借款申请创建参数
     * @return 借款申请ID
     */
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bpm:oa-borrow:create')")
    @Operation(summary = "创建借款申请")
    public CommonResult<Long> createBorrow(@Valid @RequestBody BpmOABorrowCreateReqVO createReqVO) {
        return success(borrowService.createBorrow(getLoginUserId(), createReqVO));
    }

    /**
     * 获得借款申请详情
     *
     * @param id 借款申请ID
     * @return 借款申请详情
     */
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bpm:oa-borrow:query')")
    @Operation(summary = "获得借款申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<BpmOABorrowRespVO> getBorrow(@RequestParam("id") Long id) {
        BpmOABorrowDO borrow = borrowService.getBorrow(id);
        return success(BeanUtils.toBean(borrow, BpmOABorrowRespVO.class));
    }

    /**
     * 获得借款申请分页列表
     *
     * @param pageVO 分页查询参数
     * @return 借款申请分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bpm:oa-borrow:query')")
    @Operation(summary = "获得借款申请分页")
    public CommonResult<PageResult<BpmOABorrowRespVO>> getBorrowPage(@Valid BpmOABorrowPageReqVO pageVO) {
        PageResult<BpmOABorrowDO> pageResult = borrowService.getBorrowPage(getLoginUserId(), pageVO);
        return success(BeanUtils.toBean(pageResult, BpmOABorrowRespVO.class));
    }

}