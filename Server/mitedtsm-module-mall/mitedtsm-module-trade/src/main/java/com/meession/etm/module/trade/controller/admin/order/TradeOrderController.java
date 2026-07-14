package com.meession.etm.module.trade.controller.admin.order;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderItemRespVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderPageReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderRespVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderItemDO;
import com.meession.etm.module.trade.enums.TradeOrderStatusEnum;
import com.meession.etm.module.trade.enums.TradeOrderTypeEnum;
import com.meession.etm.module.trade.service.TradeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 交易订单")
@RestController
@RequestMapping("/trade/order")
@Validated
public class TradeOrderController {

    @Resource
    private TradeOrderService tradeOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    @PreAuthorize("@ss.hasPermission('trade:order:create')")
    public CommonResult<Long> createOrder(@Valid @RequestBody TradeOrderSaveReqVO createReqVO) {
        Long orderId = tradeOrderService.createOrder(createReqVO);
        return success(orderId);
    }

    @PutMapping("/update")
    @Operation(summary = "修改订单")
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CommonResult<Boolean> updateOrder(@Valid @RequestBody TradeOrderSaveReqVO updateReqVO) {
        tradeOrderService.updateOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除订单")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('trade:order:delete')")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        tradeOrderService.deleteOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除订单")
    @Parameter(name = "ids", description = "订单编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('trade:order:delete')")
    public CommonResult<Boolean> deleteOrderList(@RequestParam("ids") List<Long> ids) {
        tradeOrderService.deleteOrderList(ids);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取订单分页列表")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<PageResult<TradeOrderRespVO>> getOrderPage(@Validated TradeOrderPageReqVO pageReqVO) {
        PageResult<TradeOrderDO> pageResult = tradeOrderService.getOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TradeOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取订单详情")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<TradeOrderRespVO> getOrder(@RequestParam("id") Long id) {
        TradeOrderDO order = tradeOrderService.getOrder(id);
        TradeOrderRespVO respVO = BeanUtils.toBean(order, TradeOrderRespVO.class);
        if (order != null) {
            respVO.setStatusName(TradeOrderStatusEnum.valueOf(order.getStatus()) != null ? TradeOrderStatusEnum.valueOf(order.getStatus()).getName() : null);
            respVO.setTypeName(TradeOrderTypeEnum.valueOf(order.getType()) != null ? TradeOrderTypeEnum.valueOf(order.getType()).getName() : null);
        }
        return success(respVO);
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获取订单详情（含订单产品行）")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<TradeOrderRespVO> getOrderDetail(@RequestParam("id") Long id) {
        TradeOrderDO order = tradeOrderService.getOrderWithItems(id);
        TradeOrderRespVO respVO = BeanUtils.toBean(order, TradeOrderRespVO.class);
        if (order != null) {
            respVO.setStatusName(TradeOrderStatusEnum.valueOf(order.getStatus()) != null ? TradeOrderStatusEnum.valueOf(order.getStatus()).getName() : null);
            respVO.setTypeName(TradeOrderTypeEnum.valueOf(order.getType()) != null ? TradeOrderTypeEnum.valueOf(order.getType()).getName() : null);
            if (order.getItems() != null) {
                respVO.setItems(BeanUtils.toBean(order.getItems(), TradeOrderItemRespVO.class));
            }
        }
        return success(respVO);
    }

    @GetMapping("/items")
    @Operation(summary = "获取订单产品行列表")
    @Parameter(name = "orderId", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<List<TradeOrderItemRespVO>> getOrderItems(@RequestParam("orderId") Long orderId) {
        List<TradeOrderItemDO> items = tradeOrderService.getOrderItems(orderId);
        return success(BeanUtils.toBean(items, TradeOrderItemRespVO.class));
    }

    @GetMapping("/item/get")
    @Operation(summary = "获取订单产品行详情")
    @Parameter(name = "id", description = "订单产品行编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CommonResult<TradeOrderItemRespVO> getOrderItem(@RequestParam("id") Long id) {
        TradeOrderItemDO item = tradeOrderService.getOrderItem(id);
        return success(BeanUtils.toBean(item, TradeOrderItemRespVO.class));
    }

    @PutMapping("/status/update")
    @Operation(summary = "更新订单状态")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @Parameter(name = "status", description = "订单状态", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CommonResult<Boolean> updateOrderStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        tradeOrderService.updateOrderStatus(id, status);
        return success(true);
    }

}