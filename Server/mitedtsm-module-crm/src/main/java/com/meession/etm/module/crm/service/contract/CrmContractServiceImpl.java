package com.meession.etm.module.crm.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.number.MoneyUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.common.util.object.ObjectUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.contract.CrmContractSaveReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.contract.CrmContractTransferReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractConfigDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.dataobject.permission.CrmPermissionDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import com.meession.etm.module.crm.service.product.CrmProductService;
import com.meession.etm.module.crm.service.quote.CrmBusinessQuoteService;
import com.meession.etm.module.crm.service.receivable.CrmReceivableService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.*;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.*;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.meession.etm.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

/**
 * CRM 合同 Service 实现类
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmContractServiceImpl implements CrmContractService {

    /**
     * BPM 合同审批流程标识
     */
    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-contract-audit";

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractProductMapper contractProductMapper;

    @Resource
    private CrmNoRedisDAO noRedisDAO;

    @Resource
    private CrmPermissionService crmPermissionService;
    @Resource
    private CrmProductService productService;
    @Resource
    private CrmBusinessQuoteService quoteService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmBusinessService businessService;
    @Resource
    private CrmContactService contactService;
    @Resource
    private CrmContractConfigService contractConfigService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private CrmReceivableService receivableService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private CrmContractLifecycleService contractLifecycleService;
    @Resource
    private CrmAuthorizationService authorizationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_CREATE_SUB_TYPE, bizNo = "{{#contract.id}}",
            success = CRM_CONTRACT_CREATE_SUCCESS)
    public Long createContract(CrmContractSaveReqVO createReqVO, Long userId) {
        if (createReqVO.getBusinessId() != null) {
            throw exception(CONTRACT_CREATE_BUSINESS_REQUIRES_CONVERSION);
        }
        return createContractInternal(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_CREATE_SUB_TYPE, bizNo = "{{#contract.id}}",
            success = CRM_CONTRACT_CREATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#createReqVO.businessId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createContractFromBusiness(CrmContractSaveReqVO createReqVO, Long userId) {
        Assert.notNull(createReqVO.getBusinessId(), "来源商机编号不能为空");
        return createContractInternal(createReqVO);
    }

    private Long createContractInternal(CrmContractSaveReqVO createReqVO) {
        // 1.1 商机转合同时校验赢单状态、复用已有合同，并强制继承客户和负责人
        CrmContractDO existingContract = prepareBusinessConversion(createReqVO);
        if (existingContract != null) {
            LogRecordContext.putVariable("contract", existingContract);
            return existingContract.getId();
        }
        // 1.2 商机转换只接受当前锁定报价，不信任客户端再次提交的产品、价格、币种或税率。
        CrmBusinessQuoteService.QuoteSnapshot sourceQuote = createReqVO.getBusinessId() == null
                ? null : quoteService.requireCurrentLocked(createReqVO.getBusinessId());
        List<CrmContractProductDO> contractProducts = sourceQuote == null
                ? buildContractProducts(createReqVO.getProducts(), null)
                : buildContractProductsFromQuote(sourceQuote);
        if (sourceQuote != null) {
            createReqVO.setDiscountPercent(sourceQuote.quote().getDiscountPercent());
        }
        // 1.3 校验关联字段
        validateRelationDataExists(createReqVO);
        // 1.4 生成序号
        String no = noRedisDAO.generate(CrmNoRedisDAO.CONTRACT_NO_PREFIX);
        if (contractMapper.selectByNo(no) != null) {
            throw exception(CONTRACT_NO_EXISTS);
        }

        // 2.1 插入合同
        CrmContractDO contract = BeanUtils.toBean(createReqVO, CrmContractDO.class).setNo(no)
                .setSourceBusinessId(createReqVO.getBusinessId())
                .setSourceQuoteId(sourceQuote == null ? null : sourceQuote.quote().getId());
        if (sourceQuote == null) {
            calculateTotalPrice(contract, contractProducts);
        } else {
            var quote = sourceQuote.quote();
            contract.setTotalProductPrice(quote.getSubtotal()).setTotalPrice(quote.getNetAmount())
                    .setCurrencyCode(quote.getCurrencyCode()).setBaseCurrencyCode(quote.getBaseCurrencyCode())
                    .setExchangeRateToBase(quote.getExchangeRateToBase()).setTaxAmount(quote.getTaxAmount())
                    .setGrossAmount(quote.getGrossAmount()).setBaseGrossAmount(quote.getBaseGrossAmount());
        }
        try {
            contractMapper.insert(contract);
        } catch (DuplicateKeyException ex) {
            if (createReqVO.getBusinessId() == null) {
                throw ex;
            }
            CrmContractDO concurrentContract = contractMapper.selectBySourceBusinessIdForUpdate(
                    createReqVO.getBusinessId());
            if (concurrentContract != null) {
                LogRecordContext.putVariable("contract", concurrentContract);
                return concurrentContract.getId();
            }
            throw exception(CONTRACT_CREATE_FROM_BUSINESS_CONCURRENT);
        }
        // 2.2 插入合同关联商品
        if (CollUtil.isNotEmpty(contractProducts)) {
            contractProducts.forEach(item -> item.setContractId(contract.getId()));
            contractProductMapper.insertBatch(contractProducts);
        }

        // 3. 创建数据权限
        crmPermissionService.createPermission(new CrmPermissionCreateReqBO().setUserId(contract.getOwnerUserId())
                .setBizType(CrmBizTypeEnum.CRM_CONTRACT.getType()).setBizId(contract.getId())
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        contractLifecycleService.recordChange(contract.getId(), ACTION_CREATE, 1, contract.getOwnerUserId(), "创建合同");

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("contract", contract);
        return contract.getId();
    }

    private CrmContractDO prepareBusinessConversion(CrmContractSaveReqVO createReqVO) {
        if (createReqVO.getBusinessId() == null) {
            return null;
        }
        CrmBusinessDO business = businessService.validateBusiness(createReqVO.getBusinessId());
        if (!CrmBusinessEndStatusEnum.WIN.getStatus().equals(business.getEndStatus())) {
            throw exception(CONTRACT_CREATE_FAIL_BUSINESS_NOT_WON);
        }
        CrmContractDO existingContract = contractMapper.selectFirstByBusinessId(business.getId());
        if (existingContract != null) {
            return existingContract;
        }
        createReqVO.setCustomerId(business.getCustomerId()).setOwnerUserId(business.getOwnerUserId());
        return null;
    }

    private List<CrmContractProductDO> buildContractProductsFromQuote(
            CrmBusinessQuoteService.QuoteSnapshot snapshot) {
        return convertList(snapshot.items(), item -> new CrmContractProductDO().setProductId(item.getProductId())
                .setProductNameSnapshot(item.getProductNameSnapshot())
                .setProductNoSnapshot(item.getProductNoSnapshot())
                .setProductUnitSnapshot(item.getProductUnitSnapshot())
                .setProductCategoryIdSnapshot(item.getProductCategoryIdSnapshot())
                .setProductVersionSnapshot(item.getProductVersionSnapshot())
                .setProductPrice(item.getListPrice()).setContractPrice(item.getBusinessPrice())
                .setCount(item.getCount()).setTotalPrice(item.getLineSubtotal())
                .setTaxRatePercent(item.getTaxRatePercent()).setTaxAmount(item.getTaxAmount())
                .setGrossAmount(item.getGrossAmount()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_CONTRACT_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateContract(CrmContractSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "合同编号不能为空");
        updateReqVO.setOwnerUserId(null); // 不允许更新的字段
        // 1.1 锁定并校验存在，避免编辑和审批回调并发串写
        CrmContractDO oldContract = validateContractExistsForUpdate(updateReqVO.getId());
        // 1.2 只有草稿、审核不通过、已取消，可以编辑
        if (!ObjectUtils.equalsAny(oldContract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.REJECT.getStatus(), CrmAuditStatusEnum.CANCEL.getStatus())) {
            throw exception(CONTRACT_UPDATE_FAIL_NOT_EDITABLE);
        }
        // 1.3 商机转换合同的来源、客户和负责人不可通过更新接口改写
        if (oldContract.getSourceBusinessId() != null) {
            updateReqVO.setBusinessId(oldContract.getBusinessId()).setCustomerId(oldContract.getCustomerId());
        }
        // 1.4 商机转换合同始终复用原锁定报价，避免修订时用客户端产品行改写报价来源。
        List<CrmContractProductDO> oldContractProducts =
                contractProductMapper.selectListByContractId(updateReqVO.getId());
        CrmBusinessQuoteService.QuoteSnapshot sourceQuote = oldContract.getSourceQuoteId() == null ? null
                : quoteService.requireCurrentLocked(oldContract.getSourceBusinessId());
        if (sourceQuote != null && ObjUtil.notEqual(sourceQuote.quote().getId(), oldContract.getSourceQuoteId())) {
            throw exception(CONTRACT_SOURCE_QUOTE_CHANGED);
        }
        List<CrmContractProductDO> contractProducts = sourceQuote == null
                ? buildContractProducts(updateReqVO.getProducts(), oldContractProducts)
                : buildContractProductsFromQuote(sourceQuote);
        if (sourceQuote != null) updateReqVO.setDiscountPercent(sourceQuote.quote().getDiscountPercent());
        // 1.5 校验关联字段
        validateRelationDataExists(updateReqVO);

        // 2.1 更新合同
        CrmContractDO updateObj = BeanUtils.toBean(updateReqVO, CrmContractDO.class);
        if (ObjectUtils.equalsAny(oldContract.getAuditStatus(), CrmAuditStatusEnum.REJECT.getStatus(),
                CrmAuditStatusEnum.CANCEL.getStatus())) {
            updateObj.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        }
        if (sourceQuote == null) {
            calculateTotalPrice(updateObj, contractProducts);
        } else {
            var quote = sourceQuote.quote();
            updateObj.setSourceQuoteId(oldContract.getSourceQuoteId()).setTotalProductPrice(quote.getSubtotal())
                    .setTotalPrice(quote.getNetAmount()).setCurrencyCode(quote.getCurrencyCode())
                    .setBaseCurrencyCode(quote.getBaseCurrencyCode()).setExchangeRateToBase(quote.getExchangeRateToBase())
                    .setTaxAmount(quote.getTaxAmount()).setGrossAmount(quote.getGrossAmount())
                    .setBaseGrossAmount(quote.getBaseGrossAmount());
        }
        contractMapper.updateById(updateObj);
        // 2.2 更新合同关联商品
        updateContractProduct(updateReqVO.getId(), oldContractProducts, contractProducts);

        int newVersion = contractLifecycleService.getCurrentVersion(updateReqVO.getId()) + 1;
        contractLifecycleService.recordChange(updateReqVO.getId(), ACTION_UPDATE, newVersion,
                getLoginUserId(), "修改合同");

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldContract.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldContract, CrmContractSaveReqVO.class));
        LogRecordContext.putVariable("contractName", oldContract.getName());
    }

    private void updateContractProduct(Long id, List<CrmContractProductDO> oldList,
                                       List<CrmContractProductDO> newList) {
        List<List<CrmContractProductDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setContractId(id));
            contractProductMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            contractProductMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            contractProductMapper.deleteByIds(convertSet(diffList.get(2), CrmContractProductDO::getId));
        }
    }

    /**
     * 校验关联数据是否存在
     *
     * @param reqVO 请求
     */
    private void validateRelationDataExists(CrmContractSaveReqVO reqVO) {
        // 1. 校验客户
        if (reqVO.getCustomerId() != null) {
            customerService.validateCustomer(reqVO.getCustomerId());
        }
        // 2. 校验负责人
        if (reqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(reqVO.getOwnerUserId());
        }
        // 3. 如果有关联商机，则需要校验存在
        if (reqVO.getBusinessId() != null) {
            businessService.validateBusiness(reqVO.getBusinessId());
        }
        // 4. 校验签约相关字段
        if (reqVO.getSignContactId() != null) {
            CrmContactDO signContact = contactService.getContact(reqVO.getSignContactId());
            if (signContact == null) {
                throw exception(CONTACT_NOT_EXISTS);
            }
            if (ObjUtil.notEqual(signContact.getCustomerId(), reqVO.getCustomerId())) {
                throw exception(CONTRACT_SIGN_CONTACT_CUSTOMER_MISMATCH);
            }
        }
        if (reqVO.getSignUserId() != null) {
            adminUserApi.validateUser(reqVO.getSignUserId());
        }
    }

    /**
     * 根据产品目录生成成交快照。创建时忽略请求中的行编号；更新已有行且产品未变化时，
     * 继续使用原快照，避免产品改名、改编码、改单位、改价或下架影响历史合同。
     */
    private List<CrmContractProductDO> buildContractProducts(List<CrmContractSaveReqVO.Product> list,
                                                              List<CrmContractProductDO> oldList) {
        if (CollUtil.isEmpty(list)) {
            return List.of();
        }
        boolean creating = oldList == null;
        Map<Long, CrmContractProductDO> oldMap = creating ? Map.of()
                : convertMap(oldList, CrmContractProductDO::getId);
        Set<Long> seenRowIds = new HashSet<>();
        Set<Long> currentProductIds = new HashSet<>();

        for (CrmContractSaveReqVO.Product requestProduct : list) {
            if (creating || requestProduct.getId() == null) {
                currentProductIds.add(requestProduct.getProductId());
                continue;
            }
            if (!seenRowIds.add(requestProduct.getId())) {
                throw exception(CONTRACT_PRODUCT_ROW_DUPLICATE);
            }
            CrmContractProductDO oldProduct = oldMap.get(requestProduct.getId());
            if (oldProduct == null) {
                throw exception(CONTRACT_PRODUCT_ROW_NOT_BELONGS);
            }
            if (ObjUtil.notEqual(oldProduct.getProductId(), requestProduct.getProductId())) {
                currentProductIds.add(requestProduct.getProductId());
            }
        }

        Map<Long, CrmProductDO> currentProductMap = CollUtil.isEmpty(currentProductIds) ? Map.of() : convertMap(
                productService.validProductList(currentProductIds), CrmProductDO::getId);
        return convertList(list, requestProduct -> {
            CrmContractProductDO item = BeanUtils.toBean(requestProduct, CrmContractProductDO.class);
            if (creating) {
                item.setId(null); // 商机产品行编号不能成为合同产品行编号
            }
            CrmContractProductDO oldProduct = creating || requestProduct.getId() == null
                    ? null : oldMap.get(requestProduct.getId());
            if (oldProduct != null && ObjUtil.equal(oldProduct.getProductId(), requestProduct.getProductId())) {
                item.setProductNameSnapshot(oldProduct.getProductNameSnapshot())
                        .setProductNoSnapshot(oldProduct.getProductNoSnapshot())
                        .setProductUnitSnapshot(oldProduct.getProductUnitSnapshot())
                        .setProductPrice(oldProduct.getProductPrice());
            } else {
                CrmProductDO currentProduct = currentProductMap.get(requestProduct.getProductId());
                item.setProductNameSnapshot(currentProduct.getName())
                        .setProductNoSnapshot(currentProduct.getNo())
                        .setProductUnitSnapshot(currentProduct.getUnit())
                        .setProductCategoryIdSnapshot(currentProduct.getCategoryId())
                        .setProductVersionSnapshot(currentProduct.getVersion() == null ? 1 : currentProduct.getVersion())
                        .setProductPrice(currentProduct.getPrice());
            }
            item.setTaxRatePercent(BigDecimal.ZERO).setTaxAmount(BigDecimal.ZERO);
            item.setGrossAmount(MoneyUtils.priceMultiply(item.getContractPrice(), item.getCount()));
            return item.setTotalPrice(MoneyUtils.priceMultiply(item.getContractPrice(), item.getCount()));
        });
    }

    private void calculateTotalPrice(CrmContractDO contract, List<CrmContractProductDO> contractProducts) {
        contract.setTotalProductPrice(getSumValue(contractProducts, CrmContractProductDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        BigDecimal discountPrice = MoneyUtils.priceMultiplyPercent(contract.getTotalProductPrice(), contract.getDiscountPercent());
        contract.setTotalPrice(contract.getTotalProductPrice().subtract(discountPrice));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CONTRACT_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteContract(Long id) {
        // 1.1 锁定并校验存在
        CrmContractDO contract = validateContractExistsForUpdate(id);
        // 1.2 仅从未提交审批的草稿允许删除，保留全部审批历史
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())
                || contract.getProcessInstanceId() != null) {
            throw exception(CONTRACT_DELETE_FAIL_NOT_NEW_DRAFT);
        }
        // 1.3 如果被 CrmReceivableDO 所使用，则不允许删除
        if (receivableService.getReceivableCountByContractId(contract.getId()) > 0) {
            throw exception(CONTRACT_DELETE_FAIL);
        }

        // 2.1 删除合同
        contractMapper.deleteById(id);
        // 2.2 删除数据权限
        crmPermissionService.deletePermission(CrmBizTypeEnum.CRM_CONTRACT.getType(), id);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("contractName", contract.getName());
    }

    private CrmContractDO validateContractExists(Long id) {
        CrmContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private CrmContractDO validateContractExistsForUpdate(Long id) {
        CrmContractDO contract = contractMapper.selectByIdForUpdate(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CONTRACT_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferContract(CrmContractTransferReqVO reqVO, Long userId) {
        // 1. 校验合同是否存在
        CrmContractDO contract = validateContractExists(reqVO.getId());

        // 2.1 数据权限转移
        crmPermissionService.transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        // 2.2 设置负责人
        contractMapper.updateById(new CrmContractDO().setId(reqVO.getId()).setOwnerUserId(reqVO.getNewOwnerUserId()));

        // 3. 记录转移日志
        LogRecordContext.putVariable("contract", contract);
    }

    @Override
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CONTRACT_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateContractFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        // 1. 校验存在
        CrmContractDO contract = validateContractExists(id);

        // 2. 更新联系人的跟进信息
        contractMapper.updateById(new CrmContractDO().setId(id).setContactLastTime(LocalDateTime.now()));

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("contractName", contract.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_SUBMIT_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CONTRACT_SUBMIT_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void submitContract(Long id, Long userId) {
        // 1. 锁定合同，防止重复请求创建多个审批流程
        CrmContractDO contract = validateContractExistsForUpdate(id);
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(CONTRACT_SUBMIT_FAIL_NOT_DRAFT);
        }

        // 2. 创建合同审批流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY).setBusinessKey(String.valueOf(id)));

        // 3. 更新合同工作流编号
        contractMapper.updateById(new CrmContractDO().setId(id).setProcessInstanceId(processInstanceId)
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));

        contractLifecycleService.recordChange(id, ACTION_SUBMIT,
                contractLifecycleService.getCurrentVersion(id), userId, "提交合同审批");

        // 3. 记录日志
        LogRecordContext.putVariable("contractName", contract.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractAuditStatus(Long id, String processInstanceId, Integer bpmResult) {
        // 1.1 转换 BPM 流程实例终态并校验合同是否存在
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        CrmContractDO contract = validateContractExists(id);
        // 1.2 重复终态事件直接成功，旧流程或乱序事件不得覆盖当前合同状态
        if (ObjUtil.equal(contract.getProcessInstanceId(), processInstanceId)
                && ObjUtil.equal(contract.getAuditStatus(), auditStatus)) {
            return;
        }
        if (ObjUtil.notEqual(contract.getProcessInstanceId(), processInstanceId)
                || ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.warn("[updateContractAuditStatus][忽略合同({})的过期或乱序审批事件，当前流程({})、事件流程({})、当前状态({})、目标状态({})]",
                    contract.getId(), contract.getProcessInstanceId(), processInstanceId,
                    contract.getAuditStatus(), auditStatus);
            return;
        }

        // 2. CAS 更新合同审批结果；并发终态事件只有首个生效
        if (contractMapper.updateAuditStatusIfProcessing(id, processInstanceId, auditStatus) == 0) {
            log.warn("[updateContractAuditStatus][合同({})审批状态已被并发事件更新，忽略目标状态({})]", id, auditStatus);
            return;
        }
        int action = ObjUtil.equal(auditStatus, CrmAuditStatusEnum.APPROVE.getStatus()) ? ACTION_APPROVE
                : ObjUtil.equal(auditStatus, CrmAuditStatusEnum.REJECT.getStatus()) ? ACTION_REJECT : ACTION_CANCEL;
        contractLifecycleService.recordChange(id, action, contractLifecycleService.getCurrentVersion(id), null,
                "合同审批状态变更为 " + auditStatus);
    }

    // ======================= 查询相关 =======================

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmContractDO getContract(Long id) {
        return contractMapper.selectById(id);
    }

    @Override
    public CrmContractDO validateContract(Long id) {
        return validateContractExists(id);
    }

    @Override
    public CrmContractDO validateContractForUpdate(Long id) {
        return validateContractExistsForUpdate(id);
    }

    @Override
    public List<CrmContractDO> getContractList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return contractMapper.selectByIds(ids);
    }

    @Override
    public PageResult<CrmContractDO> getContractPage(CrmContractPageReqVO pageReqVO, Long userId) {
        // 1. 即将到期，需要查询合同配置
        CrmContractConfigDO config = null;
        if (CrmContractPageReqVO.EXPIRY_TYPE_ABOUT_TO_EXPIRE.equals(pageReqVO.getExpiryType())) {
            config = contractConfigService.getContractConfig();
            if (config != null && Boolean.FALSE.equals(config.getNotifyEnabled())) {
                config = null;
            }
            if (config == null) {
                return PageResult.empty();
            }
        }
        // 2. 查询分页
        return contractMapper.selectPage(pageReqVO, userId, config);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#pageReqVO.customerId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmContractDO> getContractPageByCustomerId(CrmContractPageReqVO pageReqVO) {
        return contractMapper.selectPageByCustomerId(pageReqVO);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#pageReqVO.businessId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmContractDO> getContractPageByBusinessId(CrmContractPageReqVO pageReqVO) {
        return contractMapper.selectPageByBusinessId(pageReqVO);
    }

    @Override
    public Long getContractCountByContactId(Long contactId) {
        return contractMapper.selectCountByContactId(contactId);
    }

    @Override
    public Long getContractCountByCustomerId(Long customerId) {
        return contractMapper.selectCount(CrmContractDO::getCustomerId, customerId);
    }

    @Override
    public Long getContractCountByBusinessId(Long businessId) {
        return contractMapper.selectCountByBusinessId(businessId);
    }

    @Override
    public List<CrmContractProductDO> getContractProductListByContractId(Long contactId) {
        return contractProductMapper.selectListByContractId(contactId);
    }

    @Override
    public Long getAuditContractCount(Long userId) {
        return contractMapper.selectCountByAudit(userId);
    }

    @Override
    public Long getRemindContractCount(Long userId) {
        CrmContractConfigDO config = contractConfigService.getContractConfig();
        if (config == null || Boolean.FALSE.equals(config.getNotifyEnabled())) {
            return 0L;
        }
        return contractMapper.selectCountByRemind(userId, config);
    }

    @Override
    public List<CrmContractDO> getContractListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return contractMapper.selectListByCustomerIdOwnerUserId(customerId, ownerUserId);
    }

    @Override
    public List<CrmContractDO> getReceivableCandidateList(Long customerId, Long userId) {
        List<CrmContractDO> candidates = contractMapper.selectReceivableCandidateList(customerId);
        if (CollUtil.isEmpty(candidates) || authorizationService.isCrmAdmin(userId)) {
            return candidates;
        }
        Set<Long> writableContractIds = convertSet(
                CollUtil.emptyIfNull(crmPermissionService.getPermissionListByBizTypeAndUserId(
                        CrmBizTypeEnum.CRM_CONTRACT.getType(), userId)).stream()
                        .filter(permission -> CrmPermissionLevelEnum.isOwner(permission.getLevel())
                                || CrmPermissionLevelEnum.isWrite(permission.getLevel()))
                        .toList(),
                CrmPermissionDO::getBizId);
        return candidates.stream()
                .filter(contract -> ObjUtil.equal(contract.getOwnerUserId(), userId)
                        || writableContractIds.contains(contract.getId()))
                .toList();
    }

}
