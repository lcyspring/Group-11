package com.meession.etm.module.crm.service.reimbursement;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementItemSaveReqVO;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementPageReqVO;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.*;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.reimbursement.*;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.enums.reimbursement.CrmReimbursementActionTypeEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.framework.reimbursement.CrmReimbursementProperties;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import com.meession.etm.module.infra.api.file.FileApi;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

@Service
@Validated
@Slf4j
public class CrmReimbursementServiceImpl implements CrmReimbursementService {
    private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("999999999999999999.999999");
    private static final Set<Integer> EDITABLE_STATUSES = Set.of(
            CrmAuditStatusEnum.DRAFT.getStatus(), CrmAuditStatusEnum.REJECT.getStatus(),
            CrmAuditStatusEnum.CANCEL.getStatus());

    @Resource private CrmReimbursementMapper reimbursementMapper;
    @Resource private CrmReimbursementItemMapper itemMapper;
    @Resource private CrmReimbursementActionRecordMapper actionMapper;
    @Resource private CrmExpenseCategoryMapper categoryMapper;
    @Resource private CrmCustomerMapper customerMapper;
    @Resource private CrmContractMapper contractMapper;
    @Resource private CrmNoRedisDAO noRedisDAO;
    @Resource private CrmPermissionService permissionService;
    @Resource private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource private AdminUserApi adminUserApi;
    @Resource private FileApi fileApi;
    @Resource private CrmReimbursementProperties properties;

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#reimbursementId",
            level = CrmPermissionLevelEnum.WRITE)
    public String uploadAttachmentFile(Long reimbursementId, byte[] content, String fileName, String contentType) {
        validateExists(reimbursementId);
        return fileApi.createFile(content, fileName, getProtectedDirectory(reimbursementId), contentType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReimbursement(CrmReimbursementSaveReqVO reqVO, Long userId) {
        AdminUserRespDTO applicant = adminUserApi.getUser(userId);
        if (applicant == null) {
            adminUserApi.validateUser(userId);
        }
        LinkSnapshot link = validateLink(reqVO.getCustomerId(), reqVO.getContractId(), userId);
        BigDecimal total = validateAndSumItems(reqVO);
        String no = noRedisDAO.generateMonthly(properties.getNumberPrefix());
        if (reimbursementMapper.selectByNo(no) != null) {
            throw exception(REIMBURSEMENT_NO_EXISTS);
        }
        CrmReimbursementDO reimbursement = new CrmReimbursementDO()
                .setNo(no).setApplicantUserId(userId).setOwnerUserId(userId)
                .setDepartmentId(applicant == null ? null : applicant.getDeptId())
                .setCustomerId(link.customerId()).setContractId(link.contractId())
                .setCurrency(properties.getDefaultCurrency()).setTotalAmount(total)
                .setExpenseStartDate(reqVO.getExpenseStartDate()).setExpenseEndDate(reqVO.getExpenseEndDate())
                .setReason(reqVO.getReason()).setRemark(reqVO.getRemark())
                .setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus()).setVersion(0);
        reimbursementMapper.insert(reimbursement);
        validateAttachments(reimbursement.getId(), reqVO.getItems());
        replaceItems(reimbursement.getId(), reqVO.getItems());
        permissionService.createPermission(new CrmPermissionCreateReqBO()
                .setBizType(CrmBizTypeEnum.CRM_REIMBURSEMENT.getType()).setBizId(reimbursement.getId())
                .setUserId(userId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
        insertAction(reimbursement, CrmReimbursementActionTypeEnum.CREATE, null,
                CrmAuditStatusEnum.DRAFT.getStatus(), userId, null, reqVO.getReason());
        return reimbursement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#reqVO.id",
            level = CrmPermissionLevelEnum.WRITE)
    public void updateReimbursement(CrmReimbursementSaveReqVO reqVO, Long userId) {
        CrmReimbursementDO old = validateExistsForUpdate(reqVO.getId());
        if (!EDITABLE_STATUSES.contains(old.getAuditStatus())) {
            throw exception(REIMBURSEMENT_EDIT_STATUS_INVALID);
        }
        LinkSnapshot link = validateLink(reqVO.getCustomerId(), reqVO.getContractId(), userId);
        BigDecimal total = validateAndSumItems(reqVO);
        CrmReimbursementDO update = new CrmReimbursementDO().setId(old.getId()).setVersion(old.getVersion())
                .setCustomerId(link.customerId()).setContractId(link.contractId()).setTotalAmount(total)
                .setExpenseStartDate(reqVO.getExpenseStartDate()).setExpenseEndDate(reqVO.getExpenseEndDate())
                .setReason(reqVO.getReason()).setRemark(reqVO.getRemark())
                .setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        validateAttachments(old.getId(), reqVO.getItems());
        if (reimbursementMapper.updateContentIfVersion(update, old.getVersion()) == 0) {
            throw exception(REIMBURSEMENT_CONCURRENT_CHANGE);
        }
        replaceItems(old.getId(), reqVO.getItems());
        update.setProcessInstanceId(old.getProcessInstanceId());
        insertAction(update, CrmReimbursementActionTypeEnum.UPDATE, old.getAuditStatus(),
                CrmAuditStatusEnum.DRAFT.getStatus(), userId, old.getProcessInstanceId(), reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#id",
            level = CrmPermissionLevelEnum.OWNER)
    public void deleteReimbursement(Long id, Long userId) {
        CrmReimbursementDO current = validateExistsForUpdate(id);
        long submitCount = actionMapper.selectCountByReimbursementIdAndAction(
                id, CrmReimbursementActionTypeEnum.SUBMIT.getType());
        if (ObjUtil.notEqual(current.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus()) || submitCount > 0) {
            throw exception(REIMBURSEMENT_DELETE_STATUS_INVALID);
        }
        insertAction(current, CrmReimbursementActionTypeEnum.DELETE, current.getAuditStatus(),
                null, userId, current.getProcessInstanceId(), "删除新草稿");
        itemMapper.deleteByReimbursementId(id);
        reimbursementMapper.deleteById(id);
        permissionService.deletePermission(CrmBizTypeEnum.CRM_REIMBURSEMENT.getType(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#id",
            level = CrmPermissionLevelEnum.WRITE)
    public void submitReimbursement(Long id, Long userId) {
        CrmReimbursementDO current = validateExistsForUpdate(id);
        if (ObjUtil.notEqual(current.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(REIMBURSEMENT_SUBMIT_STATUS_INVALID);
        }
        List<CrmReimbursementItemDO> items = itemMapper.selectListByReimbursementId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(REIMBURSEMENT_ITEM_REQUIRED);
        }
        BigDecimal actualTotal = items.stream().map(CrmReimbursementItemDO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (actualTotal.compareTo(current.getTotalAmount()) != 0) {
            throw exception(REIMBURSEMENT_CONCURRENT_CHANGE);
        }
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(properties.getProcessDefinitionKey())
                        .setBusinessKey(String.valueOf(id)));
        if (reimbursementMapper.submitIfDraftAndVersion(id, current.getVersion(), processInstanceId) == 0) {
            throw exception(REIMBURSEMENT_CONCURRENT_CHANGE);
        }
        current.setProcessInstanceId(processInstanceId);
        insertAction(current, CrmReimbursementActionTypeEnum.SUBMIT, CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus(), userId, processInstanceId, current.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuditStatus(Long id, String processInstanceId, Integer bpmResult) {
        Integer target = convertBpmResultToAuditStatus(bpmResult);
        CrmReimbursementDO current = validateExists(id);
        if (ObjUtil.equal(current.getProcessInstanceId(), processInstanceId)
                && ObjUtil.equal(current.getAuditStatus(), target)) {
            return;
        }
        if (ObjUtil.notEqual(current.getProcessInstanceId(), processInstanceId)
                || ObjUtil.notEqual(current.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.warn("[updateAuditStatus][忽略报销单({})过期或乱序事件，当前流程({})、事件流程({})]",
                    id, current.getProcessInstanceId(), processInstanceId);
            return;
        }
        if (reimbursementMapper.updateAuditStatusIfProcessing(id, processInstanceId, target) == 0) {
            throw exception(REIMBURSEMENT_CONCURRENT_CHANGE);
        }
        CrmReimbursementActionTypeEnum action = ObjUtil.equal(target, CrmAuditStatusEnum.APPROVE.getStatus())
                ? CrmReimbursementActionTypeEnum.APPROVE
                : ObjUtil.equal(target, CrmAuditStatusEnum.REJECT.getStatus())
                ? CrmReimbursementActionTypeEnum.REJECT : CrmReimbursementActionTypeEnum.CANCEL;
        insertAction(current, action, CrmAuditStatusEnum.PROCESS.getStatus(), target,
                null, processInstanceId, null);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#id",
            level = CrmPermissionLevelEnum.READ)
    public CrmReimbursementDO getReimbursement(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<CrmReimbursementDO> getReimbursementPage(CrmReimbursementPageReqVO reqVO, Long userId) {
        return reimbursementMapper.selectPage(reqVO, userId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#reimbursementId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmReimbursementItemDO> getItems(Long reimbursementId) {
        validateExists(reimbursementId);
        return itemMapper.selectListByReimbursementId(reimbursementId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_REIMBURSEMENT, bizId = "#reimbursementId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmReimbursementActionRecordDO> getActionRecords(Long reimbursementId) {
        validateExists(reimbursementId);
        return actionMapper.selectListByReimbursementId(reimbursementId);
    }

    private BigDecimal validateAndSumItems(CrmReimbursementSaveReqVO reqVO) {
        if (reqVO.getExpenseStartDate().isAfter(reqVO.getExpenseEndDate())) {
            throw exception(REIMBURSEMENT_DATE_RANGE_INVALID);
        }
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(REIMBURSEMENT_ITEM_REQUIRED);
        }
        Set<Long> categoryIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CrmReimbursementItemSaveReqVO item : reqVO.getItems()) {
            if (item.getAmount() == null || item.getAmount().signum() <= 0) {
                throw exception(REIMBURSEMENT_ITEM_AMOUNT_INVALID);
            }
            if (item.getOccurredDate().isBefore(reqVO.getExpenseStartDate())
                    || item.getOccurredDate().isAfter(reqVO.getExpenseEndDate())) {
                throw exception(REIMBURSEMENT_ITEM_DATE_INVALID);
            }
            categoryIds.add(item.getCategoryId());
            total = total.add(item.getAmount());
        }
        Map<Long, CrmExpenseCategoryDO> categories = new HashMap<>();
        categoryMapper.selectByIds(categoryIds).forEach(category -> categories.put(category.getId(), category));
        for (Long categoryId : categoryIds) {
            CrmExpenseCategoryDO category = categories.get(categoryId);
            if (category == null) {
                throw exception(REIMBURSEMENT_CATEGORY_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(category.getStatus())) {
                throw exception(REIMBURSEMENT_CATEGORY_DISABLED, category.getName());
            }
        }
        if (total.signum() <= 0 || total.compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw exception(REIMBURSEMENT_TOTAL_AMOUNT_INVALID);
        }
        return total;
    }

    private LinkSnapshot validateLink(Long customerId, Long contractId, Long userId) {
        Long resolvedCustomerId = customerId;
        if (contractId != null) {
            CrmContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                throw exception(REIMBURSEMENT_CONTRACT_NOT_EXISTS);
            }
            requireReadPermission(CrmBizTypeEnum.CRM_CONTRACT, contractId, userId);
            if (resolvedCustomerId != null && ObjUtil.notEqual(resolvedCustomerId, contract.getCustomerId())) {
                throw exception(REIMBURSEMENT_CONTRACT_CUSTOMER_MISMATCH);
            }
            resolvedCustomerId = contract.getCustomerId();
        }
        if (resolvedCustomerId != null) {
            if (customerMapper.selectById(resolvedCustomerId) == null) {
                throw exception(REIMBURSEMENT_CUSTOMER_NOT_EXISTS);
            }
            requireReadPermission(CrmBizTypeEnum.CRM_CUSTOMER, resolvedCustomerId, userId);
        }
        return new LinkSnapshot(resolvedCustomerId, contractId);
    }

    private void requireReadPermission(CrmBizTypeEnum bizType, Long bizId, Long userId) {
        if (!permissionService.hasPermission(bizType.getType(), bizId, userId, CrmPermissionLevelEnum.READ)) {
            throw exception(CRM_PERMISSION_DENIED, bizType.getName());
        }
    }

    private void replaceItems(Long reimbursementId, List<CrmReimbursementItemSaveReqVO> reqItems) {
        itemMapper.deleteByReimbursementId(reimbursementId);
        List<CrmReimbursementItemDO> items = new ArrayList<>(reqItems.size());
        for (int i = 0; i < reqItems.size(); i++) {
            CrmReimbursementItemSaveReqVO req = reqItems.get(i);
            items.add(new CrmReimbursementItemDO().setReimbursementId(reimbursementId)
                    .setCategoryId(req.getCategoryId()).setOccurredDate(req.getOccurredDate())
                    .setAmount(req.getAmount()).setDescription(req.getDescription()).setInvoiceNo(req.getInvoiceNo())
                    .setAttachmentUrls(JsonUtils.toJsonString(CollUtil.emptyIfNull(req.getAttachmentUrls())))
                    .setSort(i));
        }
        itemMapper.insertBatch(items);
    }

    private void validateAttachments(Long reimbursementId, List<CrmReimbursementItemSaveReqVO> reqItems) {
        String protectedDirectory = getProtectedDirectory(reimbursementId) + "/";
        for (CrmReimbursementItemSaveReqVO item : reqItems) {
            List<String> canonicalUrls = new ArrayList<>();
            for (String url : CollUtil.emptyIfNull(item.getAttachmentUrls())) {
                FileRespDTO file;
                try {
                    file = fileApi.getFileByUrl(url);
                } catch (RuntimeException ex) {
                    throw exception(REIMBURSEMENT_ATTACHMENT_NOT_MANAGED);
                }
                if (file == null || !StrUtil.startWith(file.getPath(), protectedDirectory)) {
                    throw exception(REIMBURSEMENT_ATTACHMENT_NOT_PROTECTED);
                }
                canonicalUrls.add(file.getUrl());
            }
            item.setAttachmentUrls(canonicalUrls);
        }
    }

    private String getProtectedDirectory(Long reimbursementId) {
        return StrUtil.removeSuffix(properties.getProtectedFileDirectory(), "/") + "/" + reimbursementId;
    }

    private CrmReimbursementDO validateExists(Long id) {
        CrmReimbursementDO reimbursement = reimbursementMapper.selectById(id);
        if (reimbursement == null) {
            throw exception(REIMBURSEMENT_NOT_EXISTS);
        }
        return reimbursement;
    }

    private CrmReimbursementDO validateExistsForUpdate(Long id) {
        CrmReimbursementDO reimbursement = reimbursementMapper.selectByIdForUpdate(id);
        if (reimbursement == null) {
            throw exception(REIMBURSEMENT_NOT_EXISTS);
        }
        return reimbursement;
    }

    private void insertAction(CrmReimbursementDO reimbursement, CrmReimbursementActionTypeEnum action,
                              Integer fromStatus, Integer toStatus, Long operatorUserId,
                              String processInstanceId, String remark) {
        actionMapper.insert(new CrmReimbursementActionRecordDO()
                .setReimbursementId(reimbursement.getId()).setActionType(action.getType())
                .setFromStatus(fromStatus).setToStatus(toStatus).setAmountSnapshot(reimbursement.getTotalAmount())
                .setOperatorUserId(operatorUserId).setActionTime(LocalDateTime.now())
                .setProcessInstanceId(processInstanceId).setRemark(remark));
    }

    private record LinkSnapshot(Long customerId, Long contractId) {}
}
