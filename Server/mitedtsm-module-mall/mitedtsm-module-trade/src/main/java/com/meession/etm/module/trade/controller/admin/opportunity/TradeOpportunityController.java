package com.meession.etm.module.trade.controller.admin.opportunity;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.*;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityDO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityItemDO;
import com.meession.etm.module.trade.enums.TradeOpportunityStatusEnum;
import com.meession.etm.module.trade.service.TradeOpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 商机")
@RestController
@RequestMapping("/trade/opportunity")
public class TradeOpportunityController {

    @Resource
    private TradeOpportunityService tradeOpportunityService;

    @PostMapping("/create")
    @Operation(summary = "创建商机")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:create')")
    public CommonResult<Long> createOpportunity(@Valid @RequestBody TradeOpportunitySaveReqVO createReqVO) {
        Long id = tradeOpportunityService.createOpportunity(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新商机")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:update')")
    public CommonResult<Void> updateOpportunity(@Valid @RequestBody TradeOpportunitySaveReqVO updateReqVO) {
        tradeOpportunityService.updateOpportunity(updateReqVO);
        return success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除商机")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:delete')")
    public CommonResult<Void> deleteOpportunity(@Parameter(name = "id", description = "商机编号", required = true) @RequestParam("id") Long id) {
        tradeOpportunityService.deleteOpportunity(id);
        return success();
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获取商机详情")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:query')")
    public CommonResult<TradeOpportunityRespVO> getOpportunityDetail(@Parameter(name = "id", description = "商机编号", required = true) @RequestParam("id") Long id) {
        TradeOpportunityDO opportunity = tradeOpportunityService.getOpportunityWithItems(id);
        return success(BeanUtils.toBean(opportunity, TradeOpportunityRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取商机分页")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:query')")
    public CommonResult<PageResult<TradeOpportunityRespVO>> getOpportunityPage(@Valid TradeOpportunityPageReqVO pageReqVO) {
        PageResult<TradeOpportunityDO> pageResult = tradeOpportunityService.getOpportunityPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TradeOpportunityRespVO.class));
    }

    @GetMapping("/item/list")
    @Operation(summary = "获取商机产品行列表")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:query')")
    public CommonResult<List<TradeOpportunityItemRespVO>> getOpportunityItemList(@Parameter(name = "opportunityId", description = "商机编号", required = true) @RequestParam("opportunityId") Long opportunityId) {
        List<TradeOpportunityItemDO> items = tradeOpportunityService.getOpportunityItems(opportunityId);
        return success(BeanUtils.toBean(items, TradeOpportunityItemRespVO.class));
    }

    @GetMapping("/item/get-detail")
    @Operation(summary = "获取商机产品行详情")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:query')")
    public CommonResult<TradeOpportunityItemRespVO> getOpportunityItemDetail(@Parameter(name = "id", description = "产品行编号", required = true) @RequestParam("id") Long id) {
        TradeOpportunityItemDO item = tradeOpportunityService.getOpportunityItem(id);
        return success(BeanUtils.toBean(item, TradeOpportunityItemRespVO.class));
    }

    @PostMapping("/convert-to-order")
    @Operation(summary = "商机转订单")
    @PreAuthorize("@ss.hasPermission('trade:opportunity:convert')")
    public CommonResult<Long> convertToOrder(@Valid @RequestBody TradeOpportunityToOrderReqVO reqVO) {
        Long orderId = tradeOpportunityService.convertToOrder(reqVO);
        return success(orderId);
    }

}