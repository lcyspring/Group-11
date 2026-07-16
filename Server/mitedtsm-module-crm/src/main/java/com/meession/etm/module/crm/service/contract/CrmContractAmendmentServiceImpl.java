package com.meession.etm.module.crm.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.crypto.SecureUtil;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.common.util.number.MoneyUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAmendmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAttachmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAmendmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAttachmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractSigningMapper;
import com.meession.etm.module.crm.dal.mysql.invoice.CrmInvoiceMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceDirectionEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.contract.CrmContractAmendmentProperties;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.product.CrmProductService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.*;
import static com.meession.etm.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

@Service
@Slf4j
public class CrmContractAmendmentServiceImpl implements CrmContractAmendmentService {

    @Resource private CrmContractMapper contractMapper;
    @Resource private CrmContractProductMapper productMapper;
    @Resource private CrmContractSigningMapper signingMapper;
    @Resource private CrmContractAttachmentMapper attachmentMapper;
    @Resource private CrmContractAmendmentMapper amendmentMapper;
    @Resource private CrmReceivableMapper receivableMapper;
    @Resource private CrmReceivablePlanMapper receivablePlanMapper;
    @Resource private CrmInvoiceMapper invoiceMapper;
    @Resource private CrmProductService productService;
    @Resource private CrmContactService contactService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource private CrmContractLifecycleService lifecycleService;
    @Resource private CrmNoRedisDAO noRedisDAO;
    @Resource private CrmContractAmendmentProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#req.contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createAmendment(CrmContractAmendmentSaveReqVO req, Long userId) {
        String requestHash = requestHash(req);
        CrmContractAmendmentDO retry = amendmentMapper.selectByRequestId(req.getClientRequestId());
        if (retry != null) {
            if (ObjUtil.equal(retry.getContractId(), req.getContractId())
                    && ObjUtil.equal(retry.getRequestHash(), requestHash)) {
                return retry.getId();
            }
            throw exception(CONTRACT_AMENDMENT_REQUEST_CONFLICT);
        }
        CrmContractDO contract = requireAmendableContract(req.getContractId());
        if (amendmentMapper.selectOpenByContractId(contract.getId()) != null) {
            throw exception(CONTRACT_AMENDMENT_OPEN_EXISTS);
        }
        int baseVersion = lifecycleService.getCurrentVersion(contract.getId());
        List<CrmContractProductDO> beforeProducts = productMapper.selectListByContractId(contract.getId());
        AmendmentSnapshot after = buildAfterSnapshot(contract, beforeProducts, req);
        validateFinancialFloor(contract.getId(), after.contract().getTotalPrice());

        CrmContractAmendmentDO amendment = new CrmContractAmendmentDO()
                .setContractId(contract.getId()).setNo(noRedisDAO.generateMonthly(properties.getNumberPrefix()))
                .setClientRequestId(req.getClientRequestId()).setRequestHash(requestHash)
                .setBaseVersion(baseVersion).setTargetVersion(baseVersion + 1)
                .setTitle(req.getTitle().trim()).setReason(req.getReason().trim())
                .setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus())
                .setBeforeContractSnapshot(JsonUtils.toJsonString(contract))
                .setBeforeProductSnapshot(JsonUtils.toJsonString(beforeProducts))
                .setAfterContractSnapshot(JsonUtils.toJsonString(after.contract()))
                .setAfterProductSnapshot(JsonUtils.toJsonString(after.products()))
                .setAmountBefore(contract.getTotalPrice()).setAmountAfter(after.contract().getTotalPrice())
                .setAmountDelta(after.contract().getTotalPrice().subtract(contract.getTotalPrice()));
        try {
            amendmentMapper.insert(amendment);
        } catch (DuplicateKeyException ex) {
            CrmContractAmendmentDO concurrent = amendmentMapper.selectByRequestId(req.getClientRequestId());
            if (concurrent != null && ObjUtil.equal(concurrent.getRequestHash(), requestHash)) {
                return concurrent.getId();
            }
            throw exception(CONTRACT_AMENDMENT_REQUEST_CONFLICT);
        }
        lifecycleService.recordChange(contract.getId(), ACTION_AMENDMENT_CREATE,
                amendment.getTargetVersion(), userId, "创建补充协议 " + amendment.getNo());
        return amendment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#req.contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public void updateAmendment(CrmContractAmendmentSaveReqVO req, Long userId) {
        if (req.getId() == null) throw exception(CONTRACT_AMENDMENT_NOT_EXISTS);
        CrmContractDO contract = requireAmendableContract(req.getContractId());
        CrmContractAmendmentDO amendment = requireForUpdate(req.getId());
        requireBelongs(amendment, contract.getId());
        if (!Set.of(CrmAuditStatusEnum.DRAFT.getStatus(), CrmAuditStatusEnum.REJECT.getStatus(),
                CrmAuditStatusEnum.CANCEL.getStatus()).contains(amendment.getAuditStatus())) {
            throw exception(CONTRACT_AMENDMENT_NOT_EDITABLE);
        }
        if (ObjUtil.notEqual(amendment.getBaseVersion(), lifecycleService.getCurrentVersion(contract.getId()))) {
            throw exception(CONTRACT_AMENDMENT_BASE_VERSION_STALE);
        }
        List<CrmContractProductDO> beforeProducts = JsonUtils.parseArray(
                amendment.getBeforeProductSnapshot(), CrmContractProductDO.class);
        AmendmentSnapshot after = buildAfterSnapshot(contract, beforeProducts, req);
        validateFinancialFloor(contract.getId(), after.contract().getTotalPrice());
        amendmentMapper.updateById(new CrmContractAmendmentDO().setId(amendment.getId())
                .setTitle(req.getTitle().trim()).setReason(req.getReason().trim())
                .setRequestHash(requestHash(req)).setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus())
                .setProcessInstanceId(null).setAfterContractSnapshot(JsonUtils.toJsonString(after.contract()))
                .setAfterProductSnapshot(JsonUtils.toJsonString(after.products()))
                .setAmountAfter(after.contract().getTotalPrice())
                .setAmountDelta(after.contract().getTotalPrice().subtract(amendment.getAmountBefore())));
        lifecycleService.recordChange(contract.getId(), ACTION_AMENDMENT_UPDATE,
                amendment.getTargetVersion(), userId, "修改补充协议 " + amendment.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public void submitAmendment(Long contractId, Long id, Long userId) {
        CrmContractAmendmentDO amendment = requireForUpdate(id);
        requireBelongs(amendment, contractId);
        CrmContractDO contract = requireAmendableContract(amendment.getContractId());
        if (ObjUtil.notEqual(amendment.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(CONTRACT_AMENDMENT_NOT_DRAFT);
        }
        if (ObjUtil.notEqual(amendment.getBaseVersion(), lifecycleService.getCurrentVersion(contract.getId()))) {
            throw exception(CONTRACT_AMENDMENT_BASE_VERSION_STALE);
        }
        if (attachmentMapper.selectListByAmendmentId(id).isEmpty()) {
            throw exception(CONTRACT_AMENDMENT_EVIDENCE_REQUIRED);
        }
        validateFinancialFloor(contract.getId(), amendment.getAmountAfter());
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(properties.getProcessDefinitionKey())
                        .setBusinessKey(String.valueOf(id)));
        amendmentMapper.updateById(new CrmContractAmendmentDO().setId(id)
                .setProcessInstanceId(processInstanceId).setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                .setSubmitterUserId(userId).setSubmitTime(LocalDateTime.now()));
        lifecycleService.recordChange(contract.getId(), ACTION_AMENDMENT_SUBMIT,
                amendment.getTargetVersion(), userId, "提交补充协议审批 " + amendment.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuditStatus(Long id, String processInstanceId, Integer bpmResult) {
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        CrmContractAmendmentDO amendment = amendmentMapper.selectByIdForUpdate(id);
        if (amendment == null) throw exception(CONTRACT_AMENDMENT_NOT_EXISTS);
        if (ObjUtil.equal(amendment.getProcessInstanceId(), processInstanceId)
                && ObjUtil.equal(amendment.getAuditStatus(), auditStatus)) return;
        if (ObjUtil.notEqual(amendment.getProcessInstanceId(), processInstanceId)
                || ObjUtil.notEqual(amendment.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.warn("[updateAuditStatus][忽略补充协议({})过期审批事件，当前流程({})，事件流程({})]",
                    id, amendment.getProcessInstanceId(), processInstanceId);
            return;
        }
        LocalDateTime effectiveTime = ObjUtil.equal(auditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                ? LocalDateTime.now() : null;
        if (amendmentMapper.updateAuditStatusIfProcessing(id, processInstanceId, auditStatus, effectiveTime) == 0) {
            log.warn("[updateAuditStatus][补充协议({})审批终态被并发事件更新]", id);
            return;
        }
        // 先以条件更新占有流程终态，再应用合同投影。两步位于同一事务中，投影应用失败会连同
        // 审批终态一起回滚，避免极端并发下出现“合同已变更、补充协议仍审批中”的分裂状态。
        if (ObjUtil.equal(auditStatus, CrmAuditStatusEnum.APPROVE.getStatus())) {
            applyApprovedAmendment(amendment);
        }
        int action = ObjUtil.equal(auditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                ? ACTION_AMENDMENT_EFFECTIVE : ObjUtil.equal(auditStatus, CrmAuditStatusEnum.REJECT.getStatus())
                ? ACTION_AMENDMENT_REJECT : ACTION_AMENDMENT_CANCEL;
        lifecycleService.recordChange(amendment.getContractId(), action, amendment.getTargetVersion(), null,
                "补充协议 " + amendment.getNo() + " 审批状态变更为 " + auditStatus);
    }

    private void applyApprovedAmendment(CrmContractAmendmentDO amendment) {
        CrmContractDO current = contractMapper.selectByIdForUpdate(amendment.getContractId());
        if (current == null) throw exception(CONTRACT_NOT_EXISTS);
        if (ObjUtil.notEqual(amendment.getBaseVersion(), lifecycleService.getCurrentVersion(current.getId()))) {
            throw exception(CONTRACT_AMENDMENT_BASE_VERSION_STALE);
        }
        validateFinancialFloor(current.getId(), amendment.getAmountAfter());
        CrmContractDO after = JsonUtils.parseObject(amendment.getAfterContractSnapshot(), CrmContractDO.class);
        contractMapper.updateById(new CrmContractDO().setId(current.getId()).setName(after.getName())
                .setStartTime(after.getStartTime()).setEndTime(after.getEndTime())
                .setDiscountPercent(after.getDiscountPercent()).setTotalProductPrice(after.getTotalProductPrice())
                .setTotalPrice(after.getTotalPrice()).setTaxAmount(after.getTaxAmount())
                .setGrossAmount(after.getGrossAmount()).setBaseGrossAmount(after.getBaseGrossAmount())
                .setSignContactId(after.getSignContactId()).setSignUserId(after.getSignUserId())
                .setRemark(after.getRemark()));
        List<CrmContractProductDO> oldProducts = productMapper.selectListByContractId(current.getId());
        if (CollUtil.isNotEmpty(oldProducts)) {
            productMapper.deleteByIds(oldProducts.stream().map(CrmContractProductDO::getId).toList());
        }
        List<CrmContractProductDO> afterProducts = JsonUtils.parseArray(
                amendment.getAfterProductSnapshot(), CrmContractProductDO.class);
        afterProducts.forEach(item -> item.setId(null).setContractId(current.getId()));
        if (CollUtil.isNotEmpty(afterProducts)) productMapper.insertBatch(afterProducts);
        attachmentMapper.selectListByAmendmentId(amendment.getId()).forEach(item ->
                attachmentMapper.updateById(new CrmContractAttachmentDO().setId(item.getId()).setImmutable(true)));
    }

    private AmendmentSnapshot buildAfterSnapshot(CrmContractDO current, List<CrmContractProductDO> beforeProducts,
                                                   CrmContractAmendmentSaveReqVO req) {
        if (req.getStartTime() != null && req.getEndTime() != null
                && req.getStartTime().isAfter(req.getEndTime())) {
            throw exception(CONTRACT_AMENDMENT_TIME_RANGE_INVALID);
        }
        if (req.getSignContactId() != null) {
            CrmContactDO contact = contactService.getContact(req.getSignContactId());
            if (contact == null || ObjUtil.notEqual(contact.getCustomerId(), current.getCustomerId())) {
                throw exception(CONTRACT_SIGN_CONTACT_CUSTOMER_MISMATCH);
            }
        }
        if (req.getSignUserId() != null) adminUserApi.validateUser(req.getSignUserId());
        Map<Long, CrmContractProductDO> oldRows = new HashMap<>();
        beforeProducts.forEach(item -> oldRows.put(item.getId(), item));
        List<CrmContractAmendmentSaveReqVO.Product> requested = req.getProducts() == null
                ? List.of() : req.getProducts();
        Set<Long> requestedRowIds = new java.util.HashSet<>();
        requested.stream().map(CrmContractAmendmentSaveReqVO.Product::getId).filter(java.util.Objects::nonNull)
                .forEach(id -> {
                    if (!requestedRowIds.add(id)) throw exception(CONTRACT_PRODUCT_ROW_DUPLICATE);
                });
        Set<Long> newProductIds = requested.stream()
                .filter(item -> item.getId() == null || oldRows.get(item.getId()) == null
                        || ObjUtil.notEqual(oldRows.get(item.getId()).getProductId(), item.getProductId()))
                .map(CrmContractAmendmentSaveReqVO.Product::getProductId).collect(java.util.stream.Collectors.toSet());
        Map<Long, CrmProductDO> productCatalog = new HashMap<>();
        if (CollUtil.isNotEmpty(newProductIds)) {
            productService.validProductList(newProductIds).forEach(item -> productCatalog.put(item.getId(), item));
        }
        List<CrmContractProductDO> products = requested.stream().map(item -> {
            CrmContractProductDO old = item.getId() == null ? null : oldRows.get(item.getId());
            if (item.getId() != null && old == null) throw exception(CONTRACT_PRODUCT_ROW_NOT_BELONGS);
            CrmContractProductDO product = new CrmContractProductDO().setProductId(item.getProductId())
                    .setContractPrice(item.getContractPrice()).setCount(item.getCount())
                    .setTotalPrice(MoneyUtils.priceMultiply(item.getContractPrice(), item.getCount()));
            if (old != null && ObjUtil.equal(old.getProductId(), item.getProductId())) {
                product.setProductNameSnapshot(old.getProductNameSnapshot()).setProductNoSnapshot(old.getProductNoSnapshot())
                        .setProductUnitSnapshot(old.getProductUnitSnapshot())
                        .setProductCategoryIdSnapshot(old.getProductCategoryIdSnapshot())
                        .setProductVersionSnapshot(old.getProductVersionSnapshot()).setProductPrice(old.getProductPrice())
                        .setTaxRatePercent(old.getTaxRatePercent() == null ? BigDecimal.ZERO : old.getTaxRatePercent());
            } else {
                CrmProductDO catalog = productCatalog.get(item.getProductId());
                product.setProductNameSnapshot(catalog.getName()).setProductNoSnapshot(catalog.getNo())
                        .setProductUnitSnapshot(catalog.getUnit()).setProductCategoryIdSnapshot(catalog.getCategoryId())
                        .setProductVersionSnapshot(catalog.getVersion() == null ? 1 : catalog.getVersion())
                        .setProductPrice(catalog.getPrice()).setTaxRatePercent(BigDecimal.ZERO);
            }
            return product;
        }).toList();
        BigDecimal subtotal = products.stream().map(CrmContractProductDO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.subtract(MoneyUtils.priceMultiplyPercent(subtotal, req.getDiscountPercent()));
        if (total.signum() <= 0) throw exception(CONTRACT_AMENDMENT_AMOUNT_INVALID);
        BigDecimal taxAmount = BigDecimal.ZERO;
        for (CrmContractProductDO product : products) {
            BigDecimal lineNet = product.getTotalPrice().subtract(
                    MoneyUtils.priceMultiplyPercent(product.getTotalPrice(), req.getDiscountPercent()));
            BigDecimal lineTax = MoneyUtils.priceMultiplyPercent(lineNet, product.getTaxRatePercent());
            product.setTaxAmount(lineTax).setGrossAmount(lineNet.add(lineTax));
            taxAmount = taxAmount.add(lineTax);
        }
        BigDecimal grossAmount = total.add(taxAmount);
        CrmContractDO after = BeanUtils.toBean(current, CrmContractDO.class).setName(req.getContractName().trim())
                .setStartTime(req.getStartTime()).setEndTime(req.getEndTime())
                .setDiscountPercent(req.getDiscountPercent()).setTotalProductPrice(subtotal).setTotalPrice(total)
                .setTaxAmount(taxAmount).setGrossAmount(grossAmount)
                .setBaseGrossAmount(current.getExchangeRateToBase() == null ? grossAmount
                        : grossAmount.multiply(current.getExchangeRateToBase()))
                .setSignContactId(req.getSignContactId()).setSignUserId(req.getSignUserId()).setRemark(req.getRemark());
        return new AmendmentSnapshot(after, products);
    }

    private void validateFinancialFloor(Long contractId, BigDecimal targetAmount) {
        BigDecimal received = receivableMapper.selectReceivablePriceMapByContractId(List.of(contractId))
                .getOrDefault(contractId, BigDecimal.ZERO);
        BigDecimal planned = receivablePlanMapper.selectListByContractId(contractId).stream()
                .map(item -> item.getPrice() == null ? BigDecimal.ZERO : item.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CrmInvoiceDO> invoices = invoiceMapper.selectEffectiveListByContractId(contractId);
        BigDecimal blue = invoices.stream().filter(item -> ObjUtil.equal(item.getDirection(), CrmInvoiceDirectionEnum.BLUE.getDirection()))
                .map(CrmInvoiceDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal red = invoices.stream().filter(item -> ObjUtil.equal(item.getDirection(), CrmInvoiceDirectionEnum.RED.getDirection()))
                .map(CrmInvoiceDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal floor = received.max(planned).max(blue.subtract(red));
        if (targetAmount.compareTo(floor) < 0) throw exception(CONTRACT_AMENDMENT_FINANCIAL_FLOOR);
    }

    private CrmContractDO requireAmendableContract(Long contractId) {
        CrmContractDO contract = contractMapper.selectByIdForUpdate(contractId);
        if (contract == null) throw exception(CONTRACT_NOT_EXISTS);
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
            throw exception(CONTRACT_AMENDMENT_REQUIRES_APPROVED);
        }
        CrmContractSigningDO signing = signingMapper.selectByContractId(contractId);
        if (signing == null || ObjUtil.notEqual(signing.getStatus(), CrmContractLifecycleEnums.SIGNED)) {
            throw exception(CONTRACT_AMENDMENT_REQUIRES_SIGNED);
        }
        return contract;
    }

    private CrmContractAmendmentDO requireForUpdate(Long id) {
        CrmContractAmendmentDO amendment = amendmentMapper.selectByIdForUpdate(id);
        if (amendment == null) throw exception(CONTRACT_AMENDMENT_NOT_EXISTS);
        return amendment;
    }

    private void requireBelongs(CrmContractAmendmentDO amendment, Long contractId) {
        if (ObjUtil.notEqual(amendment.getContractId(), contractId)) {
            throw exception(CONTRACT_AMENDMENT_NOT_BELONGS);
        }
    }

    private String requestHash(CrmContractAmendmentSaveReqVO req) {
        return SecureUtil.sha256(JsonUtils.toJsonString(req));
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public CrmContractAmendmentDO getAmendment(Long contractId, Long id) {
        CrmContractAmendmentDO result = amendmentMapper.selectById(id);
        if (result == null) throw exception(CONTRACT_AMENDMENT_NOT_EXISTS);
        requireBelongs(result, contractId);
        return result;
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmContractAmendmentDO> getAmendmentList(Long contractId) {
        if (contractMapper.selectById(contractId) == null) throw exception(CONTRACT_NOT_EXISTS);
        return amendmentMapper.selectListByContractId(contractId);
    }

    private record AmendmentSnapshot(CrmContractDO contract, List<CrmContractProductDO> products) {}
}
