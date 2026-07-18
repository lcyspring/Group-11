package com.meession.etm.module.crm.controller.admin.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.meession.etm.framework.apilog.core.annotation.ApiAccessLog;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.collection.CollectionUtils;
import com.meession.etm.framework.common.util.collection.MapUtils;
import com.meession.etm.framework.common.util.number.NumberUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import com.meession.etm.framework.security.core.service.SecurityFrameworkService;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.*;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerLifecycleRecordDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomer360Service;
import com.meession.etm.module.crm.service.customer.CrmCustomerImportPreviewService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.customer.CrmCustomerResponseAssembler;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.meession.etm.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.*;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static java.util.Collections.singletonList;

@Tag(name = "管理后台 - CRM 客户")
@RestController
@RequestMapping("/crm/customer")
@Validated
public class CrmCustomerController {

    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmCustomerImportPreviewService customerImportPreviewService;
    @Resource
    private CrmCustomerResponseAssembler customerResponseAssembler;
    @Resource
    private CrmCustomer360Service customer360Service;
    @Resource
    private CrmContactService contactService;

    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private CrmPermissionService permissionService;

    @PostMapping("/create")
    @Operation(summary = "创建客户")
    @PreAuthorize("@ss.hasPermission('crm:customer:create')")
    public CommonResult<Long> createCustomer(@Valid @RequestBody CrmCustomerSaveReqVO createReqVO) {
        return success(customerService.createCustomer(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户")
    @PreAuthorize("@ss.hasPermission('crm:customer:update')")
    public CommonResult<Boolean> updateCustomer(@Valid @RequestBody CrmCustomerSaveReqVO updateReqVO) {
        customerService.updateCustomer(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-deal-status")
    @Operation(summary = "更新客户的成交状态")
    @Parameters({
            @Parameter(name = "id", description = "客户编号", required = true),
            @Parameter(name = "dealStatus", description = "成交状态", required = true)
    })
    public CommonResult<Boolean> updateCustomerDealStatus(@RequestParam("id") Long id,
                                                          @RequestParam("dealStatus") Boolean dealStatus) {
        customerService.updateCustomerDealStatus(id, dealStatus, getLoginUserId());
        return success(true);
    }

    @PutMapping("/update-lifecycle-status")
    @Operation(summary = "更新客户生命周期状态")
    @PreAuthorize("@ss.hasPermission('crm:customer:update')")
    public CommonResult<Boolean> updateCustomerLifecycleStatus(
            @Valid @RequestBody CrmCustomerLifecycleUpdateReqVO reqVO) {
        customerService.updateCustomerLifecycleStatus(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/lifecycle-record-list")
    @Operation(summary = "获得客户生命周期变更记录")
    @Parameter(name = "customerId", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<List<CrmCustomerLifecycleRecordRespVO>> getCustomerLifecycleRecordList(
            @RequestParam("customerId") Long customerId) {
        List<CrmCustomerLifecycleRecordDO> records = customerService.getCustomerLifecycleRecordList(customerId);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(records, CrmCustomerLifecycleRecordDO::getOperatorUserId));
        return success(BeanUtils.toBean(records, CrmCustomerLifecycleRecordRespVO.class, recordVO ->
                MapUtils.findAndThen(userMap, recordVO.getOperatorUserId(),
                        user -> recordVO.setOperatorUserName(user.getNickname()))));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户")
    @Parameter(name = "id", description = "客户编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer:delete')")
    public CommonResult<Boolean> deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<CrmCustomerRespVO> getCustomer(@RequestParam("id") Long id) {
        // 1. 获取客户
        CrmCustomerDO customer = customerService.getCustomer(id);
        // 2. 拼接数据
        return success(buildCustomerDetail(customer));
    }

    public CrmCustomerRespVO buildCustomerDetail(CrmCustomerDO customer) {
        if (customer == null) {
            return null;
        }
        return buildCustomerDetailList(singletonList(customer)).get(0);
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户分页")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<PageResult<CrmCustomerRespVO>> getCustomerPage(@Valid CrmCustomerPageReqVO pageVO) {
        // 1. 查询客户分页
        PageResult<CrmCustomerDO> pageResult = customerService.getCustomerPage(pageVO, getLoginUserId());
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        // 2. 拼接数据
        return success(new PageResult<>(buildCustomerDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/360-summary")
    @Operation(summary = "获得客户 360 只读聚合摘要")
    @Parameter(name = "customerId", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<CrmCustomer360SummaryRespVO> getCustomer360Summary(
            @RequestParam("customerId") Long customerId) {
        boolean queryAllWorkOrders = securityFrameworkService.hasPermission("crm:work-order:query-all");
        return success(customer360Service.getSummary(customerId, getLoginUserId(), queryAllWorkOrders));
    }

    @GetMapping("/duplicate-check")
    @Operation(summary = "查询疑似重复客户", description = "按名称或手机精确匹配，仅返回当前用户有权查看的客户")
    @PreAuthorize("@ss.hasAnyPermissions('crm:customer:query', 'crm:customer:create')")
    public CommonResult<List<CrmCustomerDuplicateRespVO>> getDuplicateCustomerList(
            @Valid CrmCustomerDuplicateCheckReqVO reqVO) {
        List<CrmCustomerDO> list = customerService.getDuplicateCustomerList(reqVO, getLoginUserId());
        return success(convertList(list, customer -> new CrmCustomerDuplicateRespVO()
                .setId(customer.getId()).setName(customer.getName()).setMobile(customer.getMobile())));
    }

    @GetMapping("/owner-record-list")
    @Operation(summary = "获得客户归属变更记录")
    @Parameter(name = "customerId", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<List<CrmCustomerOwnerRecordRespVO>> getCustomerOwnerRecordList(
            @RequestParam("customerId") Long customerId) {
        // Service 层会先执行 CRM 客户 READ 权限校验，避免按客户编号越权读取历史
        List<CrmCustomerOwnerRecordDO> records = customerService.getCustomerOwnerRecordList(customerId);
        if (CollUtil.isEmpty(records)) {
            return success(java.util.Collections.emptyList());
        }
        Set<Long> userIds = convertSetByFlatMap(records, record -> Stream.of(record.getPreviousOwnerUserId(),
                record.getNewOwnerUserId(), NumberUtils.parseLong(record.getCreator())));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, CrmCustomerOwnerRecordDO> recordMap = convertMap(records, CrmCustomerOwnerRecordDO::getId);
        return success(BeanUtils.toBean(records, CrmCustomerOwnerRecordRespVO.class, recordVO -> {
            CrmCustomerOwnerRecordDO record = recordMap.get(recordVO.getId());
            Long operatorUserId = NumberUtils.parseLong(record.getCreator());
            recordVO.setOperatorUserId(operatorUserId);
            MapUtils.findAndThen(userMap, record.getPreviousOwnerUserId(),
                    user -> recordVO.setPreviousOwnerUserName(user.getNickname()));
            MapUtils.findAndThen(userMap, record.getNewOwnerUserId(),
                    user -> recordVO.setNewOwnerUserName(user.getNickname()));
            MapUtils.findAndThen(userMap, operatorUserId,
                    user -> recordVO.setOperatorUserName(user.getNickname()));
        }));
    }

    public List<CrmCustomerRespVO> buildCustomerDetailList(List<CrmCustomerDO> list) {
        return customerResponseAssembler.buildDetailList(list);
    }

    @GetMapping("/put-pool-remind-page")
    @Operation(summary = "获得待进入公海客户分页")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<PageResult<CrmCustomerRespVO>> getPutPoolRemindCustomerPage(@Valid CrmCustomerPageReqVO pageVO) {
        // 1. 查询客户分页
        PageResult<CrmCustomerDO> pageResult = customerService.getPutPoolRemindCustomerPage(pageVO, getLoginUserId());
        // 2. 拼接数据
        return success(new PageResult<>(buildCustomerDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/put-pool-remind-count")
    @Operation(summary = "获得待进入公海客户数量")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<Long> getPutPoolRemindCustomerCount() {
        return success(customerService.getPutPoolRemindCustomerCount(getLoginUserId()));
    }

    @GetMapping("/today-contact-count")
    @Operation(summary = "获得今日需联系客户数量")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<Long> getTodayContactCustomerCount() {
        return success(customerService.getTodayContactCustomerCount(getLoginUserId()));
    }

    @GetMapping("/follow-count")
    @Operation(summary = "获得分配给我、待跟进的线索数量的客户数量")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<Long> getFollowCustomerCount() {
        return success(customerService.getFollowCustomerCount(getLoginUserId()));
    }

    /**
     * 获取距离进入公海的时间 Map
     *
     * @param list 客户列表
     * @return key 客户编号, value 距离进入公海的时间
     */
    @GetMapping(value = "/simple-list")
    @Operation(summary = "获取客户精简信息列表", description = "只包含有读权限的客户，主要用于前端的下拉选项")
    public CommonResult<List<CrmCustomerRespVO>> getCustomerSimpleList() {
        CrmCustomerPageReqVO reqVO = new CrmCustomerPageReqVO();
        reqVO.setPageSize(PAGE_SIZE_NONE); // 不分页
        List<CrmCustomerDO> list = customerService.getCustomerPage(reqVO, getLoginUserId()).getList();
        return success(convertList(list, customer -> // 只返回 id、name 精简字段
                new CrmCustomerRespVO().setId(customer.getId()).setName(customer.getName())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出客户 Excel")
    @PreAuthorize("@ss.hasPermission('crm:customer:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCustomerExcel(@Valid CrmCustomerPageReqVO pageVO,
                                    HttpServletResponse response) throws IOException {
        pageVO.setPageSize(PAGE_SIZE_NONE); // 不分页
        Long userId = getLoginUserId();
        List<CrmCustomerDO> list = customerService.getCustomerPage(pageVO, userId).getList();
        permissionService.validateExportPermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                convertSet(list, CrmCustomerDO::getId), userId);
        // 导出 Excel
        ExcelUtils.write(response, "客户.xls", "数据", CrmCustomerRespVO.class,
                buildCustomerDetailList(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入客户模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 手动创建导出 demo
        List<CrmCustomerImportExcelVO> list = Arrays.asList(
                CrmCustomerImportExcelVO.builder().name("密讯").industryId(1).level(1).source(1)
                        .mobile("15601691300").telephone("").qq("").wechat("").email("yunai@xxx")
                        .areaId(null).detailAddress("").remark("").build(),
                CrmCustomerImportExcelVO.builder().name("源码").industryId(1).level(1).source(1)
                        .mobile("15601691300").telephone("").qq("").wechat("").email("yunai@xxx")
                        .areaId(null).detailAddress("").remark("").build()
        );
        // 输出
        ExcelUtils.write(response, "客户导入模板.xls", "客户列表", CrmCustomerImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入客户")
    @PreAuthorize("@ss.hasPermission('crm:customer:import')")
    public CommonResult<CrmCustomerImportRespVO> importExcel(@Valid CrmCustomerImportReqVO importReqVO)
            throws Exception {
        List<CrmCustomerImportExcelVO> list = ExcelUtils.read(importReqVO.getFile(), CrmCustomerImportExcelVO.class);
        return success(customerService.importCustomerList(list, importReqVO));
    }

    @PostMapping("/import-preview")
    @Operation(summary = "预检客户导入文件和字段映射")
    @PreAuthorize("@ss.hasPermission('crm:customer:import')")
    public CommonResult<CrmCustomerImportPreviewRespVO> previewImport(
            @Valid CrmCustomerImportPreviewReqVO request) throws IOException {
        return success(customerImportPreviewService.createPreview(request, getLoginUserId()));
    }

    @GetMapping("/import-preview/get")
    @Operation(summary = "获得客户导入预检结果")
    @PreAuthorize("@ss.hasPermission('crm:customer:import')")
    public CommonResult<CrmCustomerImportPreviewRespVO> getImportPreview(@RequestParam("id") Long id) {
        return success(customerImportPreviewService.getPreview(id, getLoginUserId()));
    }

    @PostMapping("/import-preview/confirm")
    @Operation(summary = "确认客户导入预检")
    @PreAuthorize("@ss.hasPermission('crm:customer:import')")
    public CommonResult<CrmCustomerImportRespVO> confirmImportPreview(
            @Valid @RequestBody CrmCustomerImportConfirmReqVO request) {
        return success(customerImportPreviewService.confirmPreview(request.getId(), getLoginUserId()));
    }

    @PutMapping("/transfer")
    @Operation(summary = "转移客户")
    @PreAuthorize("@ss.hasPermission('crm:customer:update')")
    public CommonResult<Boolean> transferCustomer(@Valid @RequestBody CrmCustomerTransferReqVO reqVO) {
        customerService.transferCustomer(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/lock")
    @Operation(summary = "锁定/解锁客户")
    @PreAuthorize("@ss.hasPermission('crm:customer:update')")
    public CommonResult<Boolean> lockCustomer(@Valid @RequestBody CrmCustomerLockReqVO lockReqVO) {
        customerService.lockCustomer(lockReqVO, getLoginUserId());
        return success(true);
    }

    // ==================== 公海相关操作 ====================

    @PutMapping("/put-pool")
    @Operation(summary = "数据放入公海")
    @Parameter(name = "id", description = "客户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:customer:update')")
    public CommonResult<Boolean> putCustomerPool(@RequestParam("id") Long id) {
        customerService.putCustomerPool(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/receive")
    @Operation(summary = "领取公海客户")
    @Parameter(name = "ids", description = "编号数组", required = true, example = "1,2,3")
    @PreAuthorize("@ss.hasPermission('crm:customer:receive')")
    public CommonResult<Boolean> receiveCustomer(@RequestParam(value = "ids") List<Long> ids) {
        customerService.receiveCustomer(ids, getLoginUserId(), Boolean.TRUE);
        return success(true);
    }

    @PutMapping("/distribute")
    @Operation(summary = "分配公海给对应负责人")
    @PreAuthorize("@ss.hasPermission('crm:customer:distribute')")
    public CommonResult<Boolean> distributeCustomer(@Valid @RequestBody CrmCustomerDistributeReqVO distributeReqVO) {
        customerService.receiveCustomer(distributeReqVO.getIds(), distributeReqVO.getOwnerUserId(), Boolean.FALSE);
        return success(true);
    }

}
