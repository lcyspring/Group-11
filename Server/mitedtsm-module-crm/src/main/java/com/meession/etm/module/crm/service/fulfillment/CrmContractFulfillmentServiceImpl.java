package com.meession.etm.module.crm.service.fulfillment;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmContractFulfillmentRespVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmContractFulfillmentDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpCustomerMappingDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpProductMappingDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractSigningMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmContractFulfillmentMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmErpCustomerMappingMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmErpProductMappingMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.fulfillment.CrmErpFulfillmentProperties;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.erp.api.sale.ErpSaleOrderApi;
import com.meession.etm.module.erp.api.sale.dto.ErpCustomerReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpProductReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.SIGNED;
import static com.meession.etm.module.crm.enums.fulfillment.CrmContractFulfillmentStatus.CREATED;

@Service
@Validated
public class CrmContractFulfillmentServiceImpl implements CrmContractFulfillmentService {

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractProductMapper contractProductMapper;
    @Resource
    private CrmContractSigningMapper signingMapper;
    @Resource
    private CrmContractFulfillmentMapper fulfillmentMapper;
    @Resource
    private CrmErpCustomerMappingMapper customerMappingMapper;
    @Resource
    private CrmErpProductMappingMapper productMappingMapper;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmContractFulfillmentStateService stateService;
    @Resource
    private ErpSaleOrderApi erpSaleOrderApi;
    @Resource
    private CrmErpFulfillmentProperties properties;

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public CrmContractFulfillmentRespVO getFulfillment(Long contractId) {
        CrmContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        CrmContractSigningDO signing = signingMapper.selectByContractId(contractId);
        List<CrmContractProductDO> products = contractProductMapper.selectListByContractId(contractId).stream()
                .sorted(Comparator.comparing(CrmContractProductDO::getId)).toList();
        CrmContractFulfillmentDO record = fulfillmentMapper.selectByContractId(contractId);
        CrmContractFulfillmentRespVO response = new CrmContractFulfillmentRespVO()
                .setEnabled(properties.isEnabled()).setPolicyVersion(properties.getVersion())
                .setCurrencyMode(properties.getCurrencyMode().name()).setErpCurrencyCode(properties.getErpCurrency())
                .setAllowedSourceCurrencies(new ArrayList<>(properties.getAllowedSourceCurrencies()))
                .setContractId(contractId).setContractVersion(signing == null ? null : signing.getContractVersion())
                .setCrmCustomerId(contract.getCustomerId());
        CrmCustomerDO customer = customerService.getCustomer(contract.getCustomerId());
        response.setCrmCustomerName(customer == null ? null : customer.getName());

        CrmErpCustomerMappingDO customerMapping = customerMappingMapper.selectByCrmCustomerId(contract.getCustomerId());
        if (customerMapping != null) {
            response.setErpCustomerId(customerMapping.getErpCustomerId());
            try {
                ErpCustomerReferenceDTO erpCustomer = erpSaleOrderApi.getCustomerReference(customerMapping.getErpCustomerId());
                response.setErpCustomerName(erpCustomer.getName());
            } catch (ServiceException ex) {
                response.getBlockers().add("ERP_CUSTOMER_REFERENCE_INVALID");
            }
        }
        Map<Long, CrmErpProductMappingDO> mappings = products.isEmpty() ? Map.of() : convertMap(
                productMappingMapper.selectByCrmProductIds(convertSet(products, CrmContractProductDO::getProductId)),
                CrmErpProductMappingDO::getCrmProductId);
        Map<Long, ErpProductReferenceDTO> erpProducts = loadErpProducts(mappings);
        for (CrmContractProductDO product : products) {
            CrmErpProductMappingDO mapping = mappings.get(product.getProductId());
            ErpProductReferenceDTO erpProduct = mapping == null ? null : erpProducts.get(mapping.getErpProductId());
            response.getProductMappings().add(new CrmContractFulfillmentRespVO.ProductMapping()
                    .setCrmProductId(product.getProductId()).setCrmProductName(product.getProductNameSnapshot())
                    .setCrmProductNo(product.getProductNoSnapshot())
                    .setErpProductId(mapping == null ? null : mapping.getErpProductId())
                    .setErpProductName(erpProduct == null ? null : erpProduct.getName())
                    .setMapped(mapping != null && erpProduct != null));
        }
        response.setRecord(toRecord(record));
        populateEligibility(response, contract, signing, products, record);
        return response;
    }

    private Map<Long, ErpProductReferenceDTO> loadErpProducts(Map<Long, CrmErpProductMappingDO> mappings) {
        if (mappings.isEmpty()) {
            return Map.of();
        }
        try {
            return convertMap(erpSaleOrderApi.getProductReferences(
                    convertSet(mappings.values(), CrmErpProductMappingDO::getErpProductId)),
                    ErpProductReferenceDTO::getId);
        } catch (ServiceException ex) {
            return Map.of();
        }
    }

    private void populateEligibility(CrmContractFulfillmentRespVO response, CrmContractDO contract,
                                     CrmContractSigningDO signing, List<CrmContractProductDO> products,
                                     CrmContractFulfillmentDO record) {
        if (!properties.isEnabled()) {
            response.getBlockers().add("FULFILLMENT_DISABLED");
        }
        if (!CrmAuditStatusEnum.APPROVE.getStatus().equals(contract.getAuditStatus())) {
            response.getBlockers().add("CONTRACT_NOT_APPROVED");
        }
        if (signing == null || signing.getStatus() == null || signing.getStatus() != SIGNED) {
            response.getBlockers().add("CONTRACT_NOT_SIGNED");
        }
        if (products.isEmpty()) {
            response.getBlockers().add("CONTRACT_PRODUCTS_EMPTY");
        }
        boolean frozenRetry = record != null && record.getStatus() != CREATED;
        if (!frozenRetry) {
            if (response.getErpCustomerId() == null) {
                response.getBlockers().add("CUSTOMER_MAPPING_MISSING");
            }
            if (response.getProductMappings().stream().anyMatch(item -> !item.isMapped())) {
                response.getBlockers().add("PRODUCT_MAPPING_MISSING");
            }
        }
        String sourceCurrency = CrmErpFulfillmentProperties.normalizeCurrency(contract.getCurrencyCode());
        if (!properties.supportsSourceCurrency(sourceCurrency)) {
            response.getBlockers().add("SOURCE_CURRENCY_UNSUPPORTED");
        } else if (properties.getCurrencyMode() == CrmErpFulfillmentProperties.CurrencyMode.REQUIRE_SAME_CURRENCY
                && !sourceCurrency.equals(CrmErpFulfillmentProperties.normalizeCurrency(properties.getErpCurrency()))) {
            response.getBlockers().add("CURRENCY_CONVERSION_DISABLED");
        } else if (properties.getCurrencyMode() == CrmErpFulfillmentProperties.CurrencyMode.CONVERT_TO_ERP_CURRENCY
                && !CrmErpFulfillmentProperties.normalizeCurrency(contract.getBaseCurrencyCode())
                .equals(CrmErpFulfillmentProperties.normalizeCurrency(properties.getErpCurrency()))) {
            response.getBlockers().add("BASE_CURRENCY_MISMATCH");
        }
        response.setSourceInvalidated(record != null && record.getStatus() == CREATED
                && (!CrmAuditStatusEnum.APPROVE.getStatus().equals(contract.getAuditStatus())
                || signing == null || signing.getStatus() == null || signing.getStatus() != SIGNED));
        response.setEligible(response.getBlockers().isEmpty() || (record != null && record.getStatus() == CREATED));
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public CrmContractFulfillmentRespVO createOrRetry(Long contractId) {
        CrmContractFulfillmentStateService.Preparation preparation = stateService.prepare(contractId);
        if (preparation.alreadyCreated()) {
            return getFulfillment(contractId);
        }
        try {
            ErpSaleOrderRespDTO order = erpSaleOrderApi.createOrGetExternalSaleOrder(preparation.request());
            stateService.markCreated(preparation.fulfillment().getId(), preparation.fulfillment().getRequestHash(), order);
            return getFulfillment(contractId);
        } catch (RuntimeException ex) {
            String code = ex instanceof ServiceException serviceException
                    ? String.valueOf(serviceException.getCode()) : ex.getClass().getSimpleName();
            stateService.markFailed(preparation.fulfillment().getId(), code, ex.getMessage());
            throw exception(CONTRACT_FULFILLMENT_CREATE_FAILED, ex.getMessage() == null ? "未知错误" : ex.getMessage());
        }
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public CrmContractFulfillmentRespVO refresh(Long contractId) {
        CrmContractFulfillmentDO record = fulfillmentMapper.selectByContractId(contractId);
        if (record == null) {
            throw exception(CONTRACT_FULFILLMENT_NOT_EXISTS);
        }
        if (record.getStatus() != CREATED || record.getErpOrderId() == null) {
            throw exception(CONTRACT_FULFILLMENT_NOT_CREATED);
        }
        ErpSaleOrderRespDTO order = erpSaleOrderApi.getSaleOrder(record.getErpOrderId());
        if (order == null) {
            throw exception(CONTRACT_FULFILLMENT_ERP_ORDER_MISSING);
        }
        stateService.syncFromErp(order);
        return getFulfillment(contractId);
    }

    private static CrmContractFulfillmentRespVO.FulfillmentRecord toRecord(CrmContractFulfillmentDO record) {
        if (record == null) {
            return null;
        }
        return new CrmContractFulfillmentRespVO.FulfillmentRecord().setId(record.getId())
                .setStatus(record.getStatus()).setRequestId(record.getRequestId()).setRequestHash(record.getRequestHash())
                .setAttemptCount(record.getAttemptCount()).setErpOrderId(record.getErpOrderId())
                .setErpOrderNo(record.getErpOrderNo()).setErpOrderStatus(record.getErpOrderStatus())
                .setSourceCurrencyCode(record.getSourceCurrencyCode()).setErpCurrencyCode(record.getErpCurrencyCode())
                .setExchangeRate(record.getExchangeRate()).setSourceGrossAmount(record.getSourceGrossAmount())
                .setErpTotalAmount(record.getErpTotalAmount()).setTotalCount(record.getTotalCount())
                .setOutCount(record.getOutCount()).setReturnCount(record.getReturnCount())
                .setLastErrorCode(record.getLastErrorCode()).setLastErrorMessage(record.getLastErrorMessage())
                .setLastAttemptTime(record.getLastAttemptTime()).setCompletedTime(record.getCompletedTime())
                .setLastSyncTime(record.getLastSyncTime());
    }
}
