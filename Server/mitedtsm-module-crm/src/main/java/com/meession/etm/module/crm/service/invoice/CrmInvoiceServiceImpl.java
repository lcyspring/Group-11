package com.meession.etm.module.crm.service.invoice;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.common.util.object.ObjectUtils;
import com.meession.etm.module.crm.controller.admin.invoice.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import com.meession.etm.module.crm.dal.mysql.invoice.CrmInvoiceActionRecordMapper;
import com.meession.etm.module.crm.dal.mysql.invoice.CrmInvoiceMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.invoice.*;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.invoice.CrmInvoiceProvider;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;

@Service
@Validated
public class CrmInvoiceServiceImpl implements CrmInvoiceService {

    @Resource
    private CrmInvoiceMapper invoiceMapper;
    @Resource
    private CrmInvoiceActionRecordMapper actionRecordMapper;
    @Resource
    private CrmNoRedisDAO noRedisDAO;
    @Resource
    private CrmContractService contractService;
    @Resource
    private CrmPermissionService permissionService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private CrmInvoiceProvider invoiceProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#reqVO.contractId",
            level = CrmPermissionLevelEnum.WRITE)
    @LogRecord(type = CRM_INVOICE_TYPE, subType = CRM_INVOICE_CREATE_SUB_TYPE,
            bizNo = "{{#invoice.id}}", success = CRM_INVOICE_CREATE_SUCCESS)
    public Long createInvoice(CrmInvoiceCreateReqVO reqVO, Long userId) {
        CrmContractDO contract = validateContractForInvoice(reqVO.getContractId());
        validateBuyerInfo(reqVO);
        adminUserApi.validateUser(reqVO.getHandlerUserId());
        validateDraftAmount(contract, reqVO.getAmount());
        String no = noRedisDAO.generateMonthly(CrmNoRedisDAO.INVOICE_PREFIX);
        if (invoiceMapper.selectByNo(no) != null) {
            throw exception(INVOICE_NO_EXISTS);
        }
        CrmInvoiceDO invoice = BeanUtils.toBean(reqVO, CrmInvoiceDO.class)
                .setNo(no)
                .setCustomerId(contract.getCustomerId())
                .setOwnerUserId(contract.getOwnerUserId())
                .setDirection(CrmInvoiceDirectionEnum.BLUE.getDirection())
                .setStatus(CrmInvoiceStatusEnum.DRAFT.getStatus())
                .setRedAmount(BigDecimal.ZERO);
        invoiceMapper.insert(invoice);
        permissionService.createPermission(new CrmPermissionCreateReqBO()
                .setBizType(CrmBizTypeEnum.CRM_INVOICE.getType()).setBizId(invoice.getId())
                .setUserId(invoice.getOwnerUserId()).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
        insertAction(invoice.getId(), CrmInvoiceActionTypeEnum.CREATE, null,
                CrmInvoiceStatusEnum.DRAFT.getStatus(), userId, null, reqVO.getRemark());
        LogRecordContext.putVariable("invoice", invoice);
        return invoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#reqVO.id",
            level = CrmPermissionLevelEnum.WRITE)
    @LogRecord(type = CRM_INVOICE_TYPE, subType = CRM_INVOICE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}", success = CRM_INVOICE_UPDATE_SUCCESS)
    public void updateInvoice(CrmInvoiceUpdateReqVO reqVO, Long userId) {
        CrmInvoiceDO old = validateInvoiceForUpdate(reqVO.getId());
        if (ObjUtil.notEqual(old.getStatus(), CrmInvoiceStatusEnum.DRAFT.getStatus())
                || ObjUtil.notEqual(old.getDirection(), CrmInvoiceDirectionEnum.BLUE.getDirection())) {
            throw exception(INVOICE_DRAFT_ONLY);
        }
        validateBuyerInfo(reqVO);
        adminUserApi.validateUser(reqVO.getHandlerUserId());
        CrmContractDO contract = validateContractForInvoice(old.getContractId());
        validateDraftAmount(contract, reqVO.getAmount());
        CrmInvoiceDO update = new CrmInvoiceDO().setId(reqVO.getId())
                .setHandlerUserId(reqVO.getHandlerUserId()).setType(reqVO.getType())
                .setAmount(reqVO.getAmount()).setTitle(reqVO.getTitle()).setTaxNo(reqVO.getTaxNo())
                .setRegisteredAddress(reqVO.getRegisteredAddress()).setRegisteredPhone(reqVO.getRegisteredPhone())
                .setBankName(reqVO.getBankName()).setBankAccount(reqVO.getBankAccount())
                .setEmail(reqVO.getEmail()).setContent(reqVO.getContent()).setRemark(reqVO.getRemark());
        invoiceMapper.updateById(update);
        insertAction(old.getId(), CrmInvoiceActionTypeEnum.UPDATE, old.getStatus(), old.getStatus(),
                userId, null, reqVO.getRemark());
        LogRecordContext.putVariable("invoiceNo", old.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#id",
            level = CrmPermissionLevelEnum.OWNER)
    @LogRecord(type = CRM_INVOICE_TYPE, subType = CRM_INVOICE_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = CRM_INVOICE_DELETE_SUCCESS)
    public void deleteInvoice(Long id, Long userId) {
        CrmInvoiceDO invoice = validateInvoiceForUpdate(id);
        if (ObjUtil.notEqual(invoice.getStatus(), CrmInvoiceStatusEnum.DRAFT.getStatus())) {
            throw exception(INVOICE_DRAFT_ONLY);
        }
        insertAction(id, CrmInvoiceActionTypeEnum.DELETE, invoice.getStatus(), null,
                userId, null, "删除草稿");
        invoiceMapper.deleteById(id);
        permissionService.deletePermission(CrmBizTypeEnum.CRM_INVOICE.getType(), id);
        LogRecordContext.putVariable("invoiceNo", invoice.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#reqVO.id",
            level = CrmPermissionLevelEnum.WRITE)
    @LogRecord(type = CRM_INVOICE_TYPE, subType = CRM_INVOICE_ISSUE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}", success = CRM_INVOICE_ISSUE_SUCCESS)
    public void issueInvoice(CrmInvoiceIssueReqVO reqVO, Long userId) {
        CrmInvoiceDO current = validateInvoiceExists(reqVO.getId());
        CrmContractDO contract = contractService.validateContractForUpdate(current.getContractId());
        CrmInvoiceDO invoice = validateInvoiceForUpdate(reqVO.getId());
        if (ObjUtil.notEqual(invoice.getStatus(), CrmInvoiceStatusEnum.DRAFT.getStatus())) {
            throw exception(INVOICE_ISSUE_STATUS_INVALID);
        }
        validateApprovedContract(contract);
        validateFiscalNoUnique(null, reqVO.getInvoiceNo());
        adminUserApi.validateUser(reqVO.getHandlerUserId());
        validateIssueAmount(contract, invoice.getAmount());

        String requestId = "invoice:issue:" + invoice.getId();
        invoice.setInvoiceNo(reqVO.getInvoiceNo()).setInvoiceDate(reqVO.getInvoiceDate())
                .setHandlerUserId(reqVO.getHandlerUserId()).setIssueRemark(reqVO.getRemark());
        CrmInvoiceProvider.ProviderResult providerResult = invoiceProvider.issue(invoice, requestId);
        validateProviderResult(providerResult, requestId);
        CrmInvoiceDO update = new CrmInvoiceDO().setId(invoice.getId())
                .setStatus(CrmInvoiceStatusEnum.ISSUED.getStatus())
                .setInvoiceNo(reqVO.getInvoiceNo()).setInvoiceDate(reqVO.getInvoiceDate())
                .setHandlerUserId(reqVO.getHandlerUserId()).setIssueRemark(reqVO.getRemark())
                .setExternalProvider(providerResult.providerCode())
                .setExternalRequestId(providerResult.requestId())
                .setExternalInvoiceId(providerResult.externalInvoiceId());
        try {
            invoiceMapper.updateById(update);
        } catch (DuplicateKeyException ex) {
            throw exception(INVOICE_FISCAL_NO_EXISTS);
        }
        insertAction(invoice.getId(), CrmInvoiceActionTypeEnum.ISSUE, invoice.getStatus(),
                CrmInvoiceStatusEnum.ISSUED.getStatus(), userId, requestId, reqVO.getRemark());
        LogRecordContext.putVariable("invoiceNo", invoice.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#reqVO.originalInvoiceId",
            level = CrmPermissionLevelEnum.WRITE)
    @LogRecord(type = CRM_INVOICE_TYPE, subType = CRM_INVOICE_RED_SUB_TYPE,
            bizNo = "{{#reqVO.originalInvoiceId}}", success = CRM_INVOICE_RED_SUCCESS)
    public Long redFlushInvoice(CrmInvoiceRedFlushReqVO reqVO, Long userId) {
        CrmInvoiceDO current = validateInvoiceExists(reqVO.getOriginalInvoiceId());
        contractService.validateContractForUpdate(current.getContractId());
        CrmInvoiceDO original = validateInvoiceForUpdate(reqVO.getOriginalInvoiceId());
        if (ObjUtil.notEqual(original.getDirection(), CrmInvoiceDirectionEnum.BLUE.getDirection())) {
            throw exception(INVOICE_RED_CANNOT_RED);
        }
        if (!ObjectUtils.equalsAny(original.getStatus(), CrmInvoiceStatusEnum.ISSUED.getStatus(),
                CrmInvoiceStatusEnum.PARTIALLY_RED.getStatus())) {
            throw exception(INVOICE_RED_ORIGINAL_INVALID);
        }
        BigDecimal remaining = original.getAmount().subtract(original.getRedAmount());
        if (reqVO.getAmount().compareTo(remaining) > 0) {
            throw exception(INVOICE_RED_AMOUNT_EXCEEDS, remaining);
        }
        validateFiscalNoUnique(null, reqVO.getInvoiceNo());
        adminUserApi.validateUser(reqVO.getHandlerUserId());

        String no = noRedisDAO.generateMonthly(CrmNoRedisDAO.INVOICE_PREFIX);
        CrmInvoiceDO redInvoice = new CrmInvoiceDO()
                .setNo(no).setContractId(original.getContractId()).setCustomerId(original.getCustomerId())
                .setOwnerUserId(original.getOwnerUserId()).setHandlerUserId(reqVO.getHandlerUserId())
                .setDirection(CrmInvoiceDirectionEnum.RED.getDirection()).setOriginalInvoiceId(original.getId())
                .setStatus(CrmInvoiceStatusEnum.ISSUED.getStatus()).setType(original.getType())
                .setAmount(reqVO.getAmount()).setRedAmount(BigDecimal.ZERO)
                .setInvoiceNo(reqVO.getInvoiceNo()).setInvoiceDate(reqVO.getInvoiceDate())
                .setTitle(original.getTitle()).setTaxNo(original.getTaxNo())
                .setRegisteredAddress(original.getRegisteredAddress()).setRegisteredPhone(original.getRegisteredPhone())
                .setBankName(original.getBankName()).setBankAccount(original.getBankAccount())
                .setEmail(original.getEmail()).setContent(original.getContent())
                .setIssueRemark(reqVO.getReason()).setRemark(reqVO.getReason());
        try {
            invoiceMapper.insert(redInvoice);
        } catch (DuplicateKeyException ex) {
            throw exception(INVOICE_FISCAL_NO_EXISTS);
        }
        // 红票行在事务回滚后可能获得新的自增 ID；税务红票号才是跨重试稳定的业务标识。
        String requestId = "invoice:red:" + original.getId() + ":" + reqVO.getInvoiceNo();
        CrmInvoiceProvider.ProviderResult providerResult = invoiceProvider.redFlush(original, redInvoice, requestId);
        validateProviderResult(providerResult, requestId);
        invoiceMapper.updateById(new CrmInvoiceDO().setId(redInvoice.getId())
                .setExternalProvider(providerResult.providerCode())
                .setExternalRequestId(providerResult.requestId())
                .setExternalInvoiceId(providerResult.externalInvoiceId()));
        BigDecimal newRedAmount = original.getRedAmount().add(reqVO.getAmount());
        Integer newStatus = newRedAmount.compareTo(original.getAmount()) == 0
                ? CrmInvoiceStatusEnum.FULLY_RED.getStatus() : CrmInvoiceStatusEnum.PARTIALLY_RED.getStatus();
        invoiceMapper.updateById(new CrmInvoiceDO().setId(original.getId())
                .setRedAmount(newRedAmount).setStatus(newStatus));
        permissionService.createPermission(new CrmPermissionCreateReqBO()
                .setBizType(CrmBizTypeEnum.CRM_INVOICE.getType()).setBizId(redInvoice.getId())
                .setUserId(redInvoice.getOwnerUserId()).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
        insertAction(redInvoice.getId(), CrmInvoiceActionTypeEnum.RED_FLUSH, null,
                CrmInvoiceStatusEnum.ISSUED.getStatus(), userId, requestId, reqVO.getReason());
        insertAction(original.getId(), CrmInvoiceActionTypeEnum.RED_FLUSH, original.getStatus(),
                newStatus, userId, requestId, "生成红票：" + redInvoice.getNo());
        LogRecordContext.putVariable("invoiceNo", original.getNo());
        return redInvoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#reqVO.id",
            level = CrmPermissionLevelEnum.WRITE)
    @LogRecord(type = CRM_INVOICE_TYPE, subType = CRM_INVOICE_VOID_SUB_TYPE,
            bizNo = "{{#reqVO.id}}", success = CRM_INVOICE_VOID_SUCCESS)
    public void voidInvoice(CrmInvoiceVoidReqVO reqVO, Long userId) {
        CrmInvoiceDO current = validateInvoiceExists(reqVO.getId());
        contractService.validateContractForUpdate(current.getContractId());
        CrmInvoiceDO invoice = validateInvoiceForUpdate(reqVO.getId());
        if (ObjUtil.notEqual(invoice.getStatus(), CrmInvoiceStatusEnum.ISSUED.getStatus())) {
            throw exception(INVOICE_VOID_STATUS_INVALID);
        }
        if (ObjUtil.equal(invoice.getDirection(), CrmInvoiceDirectionEnum.BLUE.getDirection())
                && !invoiceMapper.selectActiveRedList(invoice.getId()).isEmpty()) {
            throw exception(INVOICE_VOID_HAS_RED);
        }
        String requestId = "invoice:void:" + invoice.getId();
        CrmInvoiceProvider.ProviderResult providerResult = invoiceProvider.voidInvoice(invoice, requestId);
        validateProviderResult(providerResult, requestId);
        invoiceMapper.updateById(new CrmInvoiceDO().setId(invoice.getId())
                .setStatus(CrmInvoiceStatusEnum.VOIDED.getStatus()).setIssueRemark(reqVO.getReason()));
        CrmInvoiceActionTypeEnum actionType = ObjUtil.equal(invoice.getDirection(), CrmInvoiceDirectionEnum.RED.getDirection())
                ? CrmInvoiceActionTypeEnum.VOID_RED : CrmInvoiceActionTypeEnum.VOID;
        insertAction(invoice.getId(), actionType, invoice.getStatus(), CrmInvoiceStatusEnum.VOIDED.getStatus(),
                userId, requestId, reqVO.getReason());
        if (ObjUtil.equal(invoice.getDirection(), CrmInvoiceDirectionEnum.RED.getDirection())) {
            restoreOriginalAfterRedVoid(invoice, userId, requestId);
        }
        LogRecordContext.putVariable("invoiceNo", invoice.getNo());
    }

    private void restoreOriginalAfterRedVoid(CrmInvoiceDO redInvoice, Long userId, String requestId) {
        CrmInvoiceDO original = validateInvoiceForUpdate(redInvoice.getOriginalInvoiceId());
        BigDecimal newRedAmount = original.getRedAmount().subtract(redInvoice.getAmount());
        Integer newStatus = newRedAmount.signum() == 0 ? CrmInvoiceStatusEnum.ISSUED.getStatus()
                : CrmInvoiceStatusEnum.PARTIALLY_RED.getStatus();
        invoiceMapper.updateById(new CrmInvoiceDO().setId(original.getId())
                .setRedAmount(newRedAmount).setStatus(newStatus));
        insertAction(original.getId(), CrmInvoiceActionTypeEnum.VOID_RED, original.getStatus(), newStatus,
                userId, requestId, "红票作废：" + redInvoice.getNo());
    }

    private CrmContractDO validateContractForInvoice(Long contractId) {
        CrmContractDO contract = contractService.validateContract(contractId);
        validateApprovedContract(contract);
        return contract;
    }

    private void validateApprovedContract(CrmContractDO contract) {
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
            throw exception(INVOICE_CONTRACT_NOT_APPROVED);
        }
    }

    private void validateBuyerInfo(CrmInvoiceBaseReqVO reqVO) {
        if (ObjUtil.notEqual(reqVO.getType(), CrmInvoiceTypeEnum.VAT_SPECIAL.getType())) {
            return;
        }
        if (StrUtil.hasBlank(reqVO.getTaxNo(), reqVO.getRegisteredAddress(), reqVO.getRegisteredPhone(),
                reqVO.getBankName(), reqVO.getBankAccount())) {
            throw exception(INVOICE_SPECIAL_BUYER_INFO_REQUIRED);
        }
    }

    private void validateDraftAmount(CrmContractDO contract, BigDecimal amount) {
        BigDecimal available = contract.getTotalPrice().subtract(calculateNetAmount(contract.getId()));
        if (amount.compareTo(available) > 0) {
            throw exception(INVOICE_AMOUNT_EXCEEDS_CONTRACT, available);
        }
    }

    private void validateIssueAmount(CrmContractDO contract, BigDecimal amount) {
        validateDraftAmount(contract, amount);
    }

    private BigDecimal calculateNetAmount(Long contractId) {
        return invoiceMapper.selectEffectiveListByContractId(contractId).stream()
                .map(invoice -> ObjUtil.equal(invoice.getDirection(), CrmInvoiceDirectionEnum.RED.getDirection())
                        ? invoice.getAmount().negate() : invoice.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateFiscalNoUnique(Long id, String invoiceNo) {
        CrmInvoiceDO existing = invoiceMapper.selectByInvoiceNo(invoiceNo);
        if (existing != null && ObjUtil.notEqual(existing.getId(), id)) {
            throw exception(INVOICE_FISCAL_NO_EXISTS);
        }
    }

    private void validateProviderResult(CrmInvoiceProvider.ProviderResult result, String requestId) {
        if (result == null || StrUtil.isBlank(result.providerCode())
                || !StrUtil.equals(requestId, result.requestId())) {
            throw exception(INVOICE_PROVIDER_RESULT_INVALID);
        }
    }

    private CrmInvoiceDO validateInvoiceExists(Long id) {
        CrmInvoiceDO invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        return invoice;
    }

    private CrmInvoiceDO validateInvoiceForUpdate(Long id) {
        CrmInvoiceDO invoice = invoiceMapper.selectByIdForUpdate(id);
        if (invoice == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        return invoice;
    }

    private void insertAction(Long invoiceId, CrmInvoiceActionTypeEnum actionType, Integer fromStatus,
                              Integer toStatus, Long userId, String requestId, String remark) {
        actionRecordMapper.insert(new CrmInvoiceActionRecordDO()
                .setInvoiceId(invoiceId).setActionType(actionType.getActionType())
                .setFromStatus(fromStatus).setToStatus(toStatus).setOperatorUserId(userId)
                .setActionTime(LocalDateTime.now()).setProviderRequestId(requestId).setRemark(remark));
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#id",
            level = CrmPermissionLevelEnum.READ)
    public CrmInvoiceDO getInvoice(Long id) {
        return validateInvoiceExists(id);
    }

    @Override
    public List<CrmInvoiceDO> getInvoiceList(Collection<Long> ids) {
        return invoiceMapper.selectListByIds(ids);
    }

    @Override
    public PageResult<CrmInvoiceDO> getInvoicePage(CrmInvoicePageReqVO reqVO, Long userId) {
        return invoiceMapper.selectPage(reqVO, userId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_INVOICE, bizId = "#invoiceId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmInvoiceActionRecordDO> getActionRecordList(Long invoiceId) {
        validateInvoiceExists(invoiceId);
        return actionRecordMapper.selectListByInvoiceId(invoiceId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public CrmInvoiceSummaryRespVO getContractSummary(Long contractId) {
        CrmContractDO contract = contractService.validateContract(contractId);
        List<CrmInvoiceDO> effective = invoiceMapper.selectEffectiveListByContractId(contractId);
        BigDecimal blue = effective.stream()
                .filter(invoice -> ObjUtil.equal(invoice.getDirection(), CrmInvoiceDirectionEnum.BLUE.getDirection()))
                .map(CrmInvoiceDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal red = effective.stream()
                .filter(invoice -> ObjUtil.equal(invoice.getDirection(), CrmInvoiceDirectionEnum.RED.getDirection()))
                .map(CrmInvoiceDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = blue.subtract(red);
        return new CrmInvoiceSummaryRespVO(contract.getTotalPrice(), blue, red, net,
                contract.getTotalPrice().subtract(net));
    }
}
