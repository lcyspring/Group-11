package com.meession.etm.module.crm.controller.admin.reimbursement;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.*;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.reimbursement.CrmExpenseCategoryService;
import com.meession.etm.module.crm.service.reimbursement.CrmReimbursementService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 报销管理")
@RestController
@RequestMapping("/crm/reimbursement")
@Validated
public class CrmReimbursementController {
    @Resource private CrmReimbursementService reimbursementService;
    @Resource private CrmExpenseCategoryService categoryService;
    @Resource private CrmCustomerService customerService;
    @Resource private CrmContractService contractService;
    @Resource private AdminUserApi adminUserApi;

    @PostMapping("/attachment/upload")
    @Operation(summary = "上传报销受保护附件")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:update')")
    public CommonResult<String> uploadAttachment(@RequestParam Long reimbursementId,
                                                 @RequestParam MultipartFile file) throws IOException {
        return success(reimbursementService.uploadAttachmentFile(reimbursementId, file.getBytes(),
                file.getOriginalFilename(), file.getContentType()));
    }

    @PostMapping("/create")
    @Operation(summary = "创建自己的报销草稿")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:create')")
    public CommonResult<Long> create(@Valid @RequestBody CrmReimbursementSaveReqVO reqVO) {
        return success(reimbursementService.createReimbursement(reqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "修订报销草稿")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody CrmReimbursementSaveReqVO reqVO) {
        reimbursementService.updateReimbursement(reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除从未提交的报销草稿")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        reimbursementService.deleteReimbursement(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交报销审批")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:update')")
    public CommonResult<Boolean> submit(@RequestParam Long id) {
        reimbursementService.submitReimbursement(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报销详情和明细")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:query')")
    public CommonResult<CrmReimbursementRespVO> get(@RequestParam Long id) {
        CrmReimbursementDO reimbursement = reimbursementService.getReimbursement(id);
        CrmReimbursementRespVO response = buildResponses(List.of(reimbursement)).get(0);
        response.setItems(buildItemResponses(reimbursementService.getItems(id)));
        return success(response);
    }

    @GetMapping("/page")
    @Operation(summary = "获得报销分页")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:query')")
    public CommonResult<PageResult<CrmReimbursementRespVO>> page(@Valid CrmReimbursementPageReqVO reqVO) {
        PageResult<CrmReimbursementDO> page = reimbursementService.getReimbursementPage(reqVO, getLoginUserId());
        return success(new PageResult<>(buildResponses(page.getList()), page.getTotal()));
    }

    @GetMapping("/action-records")
    @Operation(summary = "获得报销不可变动作轨迹")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:query')")
    public CommonResult<List<CrmReimbursementActionRespVO>> actionRecords(@RequestParam Long reimbursementId) {
        List<CrmReimbursementActionRecordDO> records = reimbursementService.getActionRecords(reimbursementId);
        Set<Long> userIds = new HashSet<>();
        records.stream().map(CrmReimbursementActionRecordDO::getOperatorUserId).filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return success(BeanUtils.toBean(records, CrmReimbursementActionRespVO.class, item -> {
            AdminUserRespDTO user = users.get(item.getOperatorUserId());
            item.setOperatorUserName(user == null ? null : user.getNickname());
        }));
    }

    @PostMapping("/category/create")
    @Operation(summary = "创建费用分类")
    @PreAuthorize("@ss.hasPermission('crm:expense-category:write')")
    public CommonResult<Long> createCategory(@Valid @RequestBody CrmExpenseCategorySaveReqVO reqVO) {
        return success(categoryService.createCategory(reqVO));
    }

    @PutMapping("/category/update")
    @Operation(summary = "更新费用分类")
    @PreAuthorize("@ss.hasPermission('crm:expense-category:write')")
    public CommonResult<Boolean> updateCategory(@Valid @RequestBody CrmExpenseCategorySaveReqVO reqVO) {
        categoryService.updateCategory(reqVO);
        return success(true);
    }

    @DeleteMapping("/category/delete")
    @Operation(summary = "删除未使用的费用分类")
    @PreAuthorize("@ss.hasPermission('crm:expense-category:write')")
    public CommonResult<Boolean> deleteCategory(@RequestParam Long id) {
        categoryService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/category/list")
    @Operation(summary = "获得费用分类列表")
    @PreAuthorize("@ss.hasPermission('crm:reimbursement:query')")
    public CommonResult<List<CrmExpenseCategoryDO>> categoryList(@RequestParam(required = false) Integer status) {
        return success(categoryService.getCategoryList(status));
    }

    private List<CrmReimbursementRespVO> buildResponses(List<CrmReimbursementDO> reimbursements) {
        if (reimbursements.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> customerIds = new HashSet<>();
        Set<Long> contractIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (CrmReimbursementDO item : reimbursements) {
            if (item.getCustomerId() != null) customerIds.add(item.getCustomerId());
            if (item.getContractId() != null) contractIds.add(item.getContractId());
            userIds.add(item.getApplicantUserId());
        }
        Map<Long, CrmCustomerDO> customers = customerService.getCustomerMap(customerIds);
        Map<Long, CrmContractDO> contracts = contractService.getContractMap(contractIds);
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(reimbursements, CrmReimbursementRespVO.class, item -> {
            AdminUserRespDTO applicant = users.get(item.getApplicantUserId());
            item.setApplicantUserName(applicant == null ? null : applicant.getNickname());
            CrmCustomerDO customer = customers.get(item.getCustomerId());
            item.setCustomerName(customer == null ? null : customer.getName());
            CrmContractDO contract = contracts.get(item.getContractId());
            if (contract != null) {
                item.setContractNo(contract.getNo()).setContractName(contract.getName());
            }
        });
    }

    private List<CrmReimbursementItemRespVO> buildItemResponses(List<CrmReimbursementItemDO> items) {
        Set<Long> categoryIds = new HashSet<>();
        items.forEach(item -> categoryIds.add(item.getCategoryId()));
        Map<Long, String> categoryNames = new HashMap<>();
        categoryService.getCategoryList(null).stream()
                .filter(category -> categoryIds.contains(category.getId()))
                .forEach(category -> categoryNames.put(category.getId(), category.getName()));
        return BeanUtils.toBean(items, CrmReimbursementItemRespVO.class, item -> {
            item.setCategoryName(categoryNames.get(item.getCategoryId()));
            CrmReimbursementItemDO source = items.stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), item.getId())).findFirst().orElseThrow();
            item.setAttachmentUrls(JsonUtils.parseArray(source.getAttachmentUrls(), String.class));
        });
    }
}
