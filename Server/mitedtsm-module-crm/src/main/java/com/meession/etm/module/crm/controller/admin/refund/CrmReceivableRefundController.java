package com.meession.etm.module.crm.controller.admin.refund;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.collection.CollectionUtils;
import com.meession.etm.framework.common.util.number.NumberUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.refund.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundDO;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.receivable.CrmReceivableService;
import com.meession.etm.module.crm.service.refund.CrmReceivableRefundService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 回款退款/冲销")
@RestController
@RequestMapping("/crm/receivable-refund")
@Validated
public class CrmReceivableRefundController {

    @Resource private CrmReceivableRefundService refundService;
    @Resource private CrmReceivableService receivableService;
    @Resource private CrmContractService contractService;
    @Resource private CrmCustomerService customerService;
    @Resource private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建退款/冲销草稿")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:create')")
    public CommonResult<Long> create(@Valid @RequestBody CrmReceivableRefundSaveReqVO reqVO) {
        return success(refundService.createRefund(reqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新退款/冲销草稿")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody CrmReceivableRefundSaveReqVO reqVO) {
        refundService.updateRefund(reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除退款/冲销新草稿")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        refundService.deleteRefund(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交退款/冲销审批")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:update')")
    public CommonResult<Boolean> submit(@RequestParam Long id) {
        refundService.submitRefund(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得退款/冲销详情")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:query')")
    public CommonResult<CrmReceivableRefundRespVO> get(@RequestParam Long id) {
        return success(buildRespList(Collections.singletonList(refundService.getRefund(id))).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得退款/冲销分页")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:query')")
    public CommonResult<PageResult<CrmReceivableRefundRespVO>> page(@Valid CrmReceivableRefundPageReqVO reqVO) {
        PageResult<CrmReceivableRefundDO> page = refundService.getRefundPage(reqVO, getLoginUserId());
        return success(new PageResult<>(buildRespList(page.getList()), page.getTotal()));
    }

    @GetMapping("/source-summary")
    @Operation(summary = "获得原回款可退金额摘要")
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:query')")
    public CommonResult<CrmReceivableRefundSourceSummaryRespVO> sourceSummary(
            @RequestParam Long receivableId,
            @RequestParam(required = false) Long excludeRefundId) {
        return success(refundService.getSourceSummary(receivableId, excludeRefundId));
    }

    @GetMapping("/action-records")
    @Operation(summary = "获得退款/冲销不可变动作轨迹")
    @Parameter(name = "refundId", required = true)
    @PreAuthorize("@ss.hasPermission('crm:receivable-refund:query')")
    public CommonResult<List<CrmReceivableRefundActionRespVO>> actionRecords(@RequestParam Long refundId) {
        List<CrmReceivableRefundActionRecordDO> records = refundService.getActionRecords(refundId);
        Set<Long> userIds = CollectionUtils.convertSet(records,
                CrmReceivableRefundActionRecordDO::getOperatorUserId,
                record -> record.getOperatorUserId() != null);
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return success(BeanUtils.toBean(records, CrmReceivableRefundActionRespVO.class, item -> {
            AdminUserRespDTO user = users.get(item.getOperatorUserId());
            item.setOperatorUserName(user == null ? null : user.getNickname());
        }));
    }

    private List<CrmReceivableRefundRespVO> buildRespList(List<CrmReceivableRefundDO> refunds) {
        if (refunds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> receivableIds = CollectionUtils.convertSet(refunds, CrmReceivableRefundDO::getReceivableId);
        Map<Long, CrmReceivableDO> receivables = CollectionUtils.convertMap(
                receivableService.getReceivableList(receivableIds), CrmReceivableDO::getId);
        Map<Long, CrmCustomerDO> customers = customerService.getCustomerMap(
                CollectionUtils.convertSet(refunds, CrmReceivableRefundDO::getCustomerId));
        Map<Long, CrmContractDO> contracts = contractService.getContractMap(
                CollectionUtils.convertSet(refunds, CrmReceivableRefundDO::getContractId));
        Collection<Long> userIds = CollectionUtils.convertListByFlatMap(refunds, item ->
                java.util.stream.Stream.of(item.getOwnerUserId(), NumberUtils.parseLong(item.getCreator())));
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(refunds, CrmReceivableRefundRespVO.class, item -> {
            CrmReceivableDO receivable = receivables.get(item.getReceivableId());
            if (receivable != null) {
                item.setReceivableNo(receivable.getNo()).setReceivablePrice(receivable.getPrice());
            }
            CrmCustomerDO customer = customers.get(item.getCustomerId());
            item.setCustomerName(customer == null ? null : customer.getName());
            CrmContractDO contract = contracts.get(item.getContractId());
            if (contract != null) {
                item.setContractNo(contract.getNo()).setContractName(contract.getName());
            }
            AdminUserRespDTO owner = users.get(item.getOwnerUserId());
            item.setOwnerUserName(owner == null ? null : owner.getNickname());
            AdminUserRespDTO creator = users.get(NumberUtils.parseLong(item.getCreator()));
            item.setCreatorName(creator == null ? null : creator.getNickname());
        });
    }
}
