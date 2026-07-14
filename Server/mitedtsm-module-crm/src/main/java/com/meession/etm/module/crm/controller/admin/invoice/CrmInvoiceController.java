package com.meession.etm.module.crm.controller.admin.invoice;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.apilog.core.annotation.ApiAccessLog;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.module.crm.controller.admin.invoice.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.invoice.CrmInvoiceService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
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
import java.util.*;

import static com.meession.etm.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 发票")
@RestController
@RequestMapping("/crm/invoice")
@Validated
public class CrmInvoiceController {

    @Resource
    private CrmInvoiceService invoiceService;
    @Resource
    private CrmContractService contractService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建发票草稿")
    @PreAuthorize("@ss.hasPermission('crm:invoice:create')")
    public CommonResult<Long> createInvoice(@Valid @RequestBody CrmInvoiceCreateReqVO reqVO) {
        return success(invoiceService.createInvoice(reqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "修改发票草稿")
    @PreAuthorize("@ss.hasPermission('crm:invoice:update')")
    public CommonResult<Boolean> updateInvoice(@Valid @RequestBody CrmInvoiceUpdateReqVO reqVO) {
        invoiceService.updateInvoice(reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发票草稿")
    @PreAuthorize("@ss.hasPermission('crm:invoice:delete')")
    public CommonResult<Boolean> deleteInvoice(@RequestParam("id") Long id) {
        invoiceService.deleteInvoice(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/issue")
    @Operation(summary = "正式开具发票")
    @PreAuthorize("@ss.hasPermission('crm:invoice:issue')")
    public CommonResult<Boolean> issueInvoice(@Valid @RequestBody CrmInvoiceIssueReqVO reqVO) {
        invoiceService.issueInvoice(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/red-flush")
    @Operation(summary = "创建红票")
    @PreAuthorize("@ss.hasPermission('crm:invoice:red-flush')")
    public CommonResult<Long> redFlushInvoice(@Valid @RequestBody CrmInvoiceRedFlushReqVO reqVO) {
        return success(invoiceService.redFlushInvoice(reqVO, getLoginUserId()));
    }

    @PutMapping("/void")
    @Operation(summary = "作废蓝票或红票")
    @PreAuthorize("@ss.hasPermission('crm:invoice:void')")
    public CommonResult<Boolean> voidInvoice(@Valid @RequestBody CrmInvoiceVoidReqVO reqVO) {
        invoiceService.voidInvoice(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发票详情及生命周期轨迹")
    @PreAuthorize("@ss.hasPermission('crm:invoice:query')")
    public CommonResult<CrmInvoiceRespVO> getInvoice(@RequestParam("id") Long id) {
        CrmInvoiceDO invoice = invoiceService.getInvoice(id);
        CrmInvoiceRespVO result = buildInvoiceList(Collections.singletonList(invoice)).get(0);
        List<CrmInvoiceActionRecordDO> actions = invoiceService.getActionRecordList(id);
        Set<Long> operatorUserIds = convertSet(actions, CrmInvoiceActionRecordDO::getOperatorUserId);
        operatorUserIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = operatorUserIds.isEmpty()
                ? Collections.emptyMap() : adminUserApi.getUserMap(operatorUserIds);
        result.setActionRecords(BeanUtils.toBean(actions, CrmInvoiceRespVO.ActionRecord.class, action -> {
            AdminUserRespDTO user = userMap.get(action.getOperatorUserId());
            if (user != null) {
                action.setOperatorUserName(user.getNickname());
            }
        }));
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得发票分页")
    @PreAuthorize("@ss.hasPermission('crm:invoice:query')")
    public CommonResult<PageResult<CrmInvoiceRespVO>> getInvoicePage(@Valid CrmInvoicePageReqVO reqVO) {
        PageResult<CrmInvoiceDO> page = invoiceService.getInvoicePage(reqVO, getLoginUserId());
        return success(new PageResult<>(buildInvoiceList(page.getList()), page.getTotal()));
    }

    @GetMapping("/contract-summary")
    @Operation(summary = "获得合同开票金额汇总")
    @PreAuthorize("@ss.hasPermission('crm:invoice:query')")
    public CommonResult<CrmInvoiceSummaryRespVO> getContractSummary(@RequestParam("contractId") Long contractId) {
        return success(invoiceService.getContractSummary(contractId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出发票")
    @PreAuthorize("@ss.hasPermission('crm:invoice:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportInvoice(@Valid CrmInvoicePageReqVO reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PAGE_SIZE_NONE);
        List<CrmInvoiceDO> list = invoiceService.getInvoicePage(reqVO, getLoginUserId()).getList();
        ExcelUtils.write(response, "发票.xls", "发票", CrmInvoiceRespVO.class, buildInvoiceList(list));
    }

    private List<CrmInvoiceRespVO> buildInvoiceList(List<CrmInvoiceDO> invoices) {
        if (CollUtil.isEmpty(invoices)) {
            return Collections.emptyList();
        }
        Map<Long, CrmContractDO> contractMap = contractService.getContractMap(
                convertSet(invoices, CrmInvoiceDO::getContractId));
        Map<Long, CrmCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(invoices, CrmInvoiceDO::getCustomerId));
        Set<Long> userIds = new HashSet<>();
        invoices.forEach(invoice -> {
            if (invoice.getOwnerUserId() != null) {
                userIds.add(invoice.getOwnerUserId());
            }
            if (invoice.getHandlerUserId() != null) {
                userIds.add(invoice.getHandlerUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        Set<Long> originalInvoiceIds = convertSet(invoices, CrmInvoiceDO::getOriginalInvoiceId);
        originalInvoiceIds.remove(null);
        Map<Long, CrmInvoiceDO> originalMap = convertMap(
                invoiceService.getInvoiceList(originalInvoiceIds), CrmInvoiceDO::getId);
        return BeanUtils.toBean(invoices, CrmInvoiceRespVO.class, result -> {
            CrmContractDO contract = contractMap.get(result.getContractId());
            if (contract != null) {
                result.setContractNo(contract.getNo()).setContractName(contract.getName());
            }
            CrmCustomerDO customer = customerMap.get(result.getCustomerId());
            if (customer != null) {
                result.setCustomerName(customer.getName());
            }
            AdminUserRespDTO owner = userMap.get(result.getOwnerUserId());
            if (owner != null) {
                result.setOwnerUserName(owner.getNickname());
            }
            AdminUserRespDTO handler = userMap.get(result.getHandlerUserId());
            if (handler != null) {
                result.setHandlerUserName(handler.getNickname());
            }
            CrmInvoiceDO original = originalMap.get(result.getOriginalInvoiceId());
            if (original != null) {
                result.setOriginalInvoiceNo(original.getInvoiceNo());
            }
        });
    }
}
