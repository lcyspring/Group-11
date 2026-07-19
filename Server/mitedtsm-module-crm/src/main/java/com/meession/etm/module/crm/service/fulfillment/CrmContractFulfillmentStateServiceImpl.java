package com.meession.etm.module.crm.service.fulfillment;

import cn.hutool.crypto.digest.DigestUtil;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
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
import com.meession.etm.module.crm.framework.fulfillment.CrmErpFulfillmentProperties;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.SIGNED;
import static com.meession.etm.module.crm.enums.fulfillment.CrmContractFulfillmentStatus.*;

@Service
public class CrmContractFulfillmentStateServiceImpl implements CrmContractFulfillmentStateService {

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractProductMapper contractProductMapper;
    @Resource
    private CrmContractSigningMapper signingMapper;
    @Resource
    private CrmErpCustomerMappingMapper customerMappingMapper;
    @Resource
    private CrmErpProductMappingMapper productMappingMapper;
    @Resource
    private CrmContractFulfillmentMapper fulfillmentMapper;
    @Resource
    private CrmErpFulfillmentProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Preparation prepare(Long contractId) {
        if (!properties.isEnabled()) {
            throw exception(ERP_FULFILLMENT_DISABLED);
        }
        CrmContractDO contract = contractMapper.selectByIdForUpdate(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        validateContractState(contract);
        CrmContractSigningDO signing = signingMapper.selectByContractId(contractId);
        if (signing == null || signing.getStatus() == null || signing.getStatus() != SIGNED) {
            throw exception(CONTRACT_FULFILLMENT_REQUIRES_SIGNED);
        }
        CrmContractFulfillmentDO existing = fulfillmentMapper.selectByContractIdForUpdate(contractId);
        if (existing != null) {
            if (existing.getStatus() == CREATED) {
                return new Preparation(existing, null, true);
            }
            ErpSaleOrderCreateReqDTO frozenRequest = parseFrozenRequest(existing);
            fulfillmentMapper.markRetrying(existing.getId(), existing.getAttemptCount() + 1, LocalDateTime.now());
            existing.setStatus(CREATING).setAttemptCount(existing.getAttemptCount() + 1)
                    .setLastAttemptTime(LocalDateTime.now()).setLastErrorCode(null).setLastErrorMessage(null);
            return new Preparation(existing, frozenRequest, false);
        }

        List<CrmContractProductDO> products = contractProductMapper.selectListByContractId(contractId).stream()
                .sorted(Comparator.comparing(CrmContractProductDO::getId)).toList();
        if (products.isEmpty()) {
            throw exception(CONTRACT_FULFILLMENT_PRODUCT_REQUIRED);
        }
        CrmErpCustomerMappingDO customerMapping = customerMappingMapper.selectByCrmCustomerId(contract.getCustomerId());
        if (customerMapping == null) {
            throw exception(ERP_CUSTOMER_MAPPING_NOT_EXISTS, contract.getCustomerId());
        }
        Map<Long, CrmErpProductMappingDO> productMappings = convertMap(productMappingMapper.selectByCrmProductIds(
                convertSet(products, CrmContractProductDO::getProductId)), CrmErpProductMappingDO::getCrmProductId);
        for (CrmContractProductDO product : products) {
            if (!productMappings.containsKey(product.getProductId())) {
                throw exception(ERP_PRODUCT_MAPPING_NOT_EXISTS, product.getProductNameSnapshot());
            }
        }

        CurrencyConversion conversion = resolveCurrency(contract);
        ErpSaleOrderCreateReqDTO request = buildRequest(contract, signing, products, customerMapping,
                productMappings, conversion);
        String hash = DigestUtil.sha256Hex(JsonUtils.toJsonString(request));
        request.setRequestHash(hash);
        String snapshot = JsonUtils.toJsonString(request);
        CrmContractFulfillmentDO fulfillment = new CrmContractFulfillmentDO().setContractId(contractId)
                .setContractVersion(signing.getContractVersion()).setRequestId(request.getRequestId())
                .setRequestHash(hash).setRequestSnapshot(snapshot).setStatus(CREATING)
                .setSourceCurrencyCode(request.getSourceCurrencyCode()).setErpCurrencyCode(request.getCurrencyCode())
                .setExchangeRate(request.getExchangeRateToOrderCurrency())
                .setSourceGrossAmount(request.getSourceGrossAmount()).setAttemptCount(1)
                .setLastAttemptTime(LocalDateTime.now());
        fulfillmentMapper.insert(fulfillment);
        return new Preparation(fulfillment, request, false);
    }

    private void validateContractState(CrmContractDO contract) {
        if (!CrmAuditStatusEnum.APPROVE.getStatus().equals(contract.getAuditStatus())) {
            throw exception(CONTRACT_FULFILLMENT_REQUIRES_APPROVED);
        }
    }

    private ErpSaleOrderCreateReqDTO parseFrozenRequest(CrmContractFulfillmentDO existing) {
        try {
            ErpSaleOrderCreateReqDTO request = JsonUtils.parseObject(existing.getRequestSnapshot(),
                    ErpSaleOrderCreateReqDTO.class);
            if (request == null || !existing.getRequestHash().equals(request.getRequestHash())
                    || !existing.getRequestId().equals(request.getRequestId())) {
                throw exception(CONTRACT_FULFILLMENT_SNAPSHOT_CONFLICT);
            }
            return request;
        } catch (com.meession.etm.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw exception(CONTRACT_FULFILLMENT_REQUEST_CORRUPTED);
        }
    }

    private CurrencyConversion resolveCurrency(CrmContractDO contract) {
        String source = CrmErpFulfillmentProperties.normalizeCurrency(contract.getCurrencyCode());
        String target = CrmErpFulfillmentProperties.normalizeCurrency(properties.getErpCurrency());
        if (!properties.supportsSourceCurrency(source)) {
            throw exception(CONTRACT_FULFILLMENT_CURRENCY_UNSUPPORTED, source);
        }
        if (properties.getCurrencyMode() == CrmErpFulfillmentProperties.CurrencyMode.REQUIRE_SAME_CURRENCY) {
            if (!source.equals(target)) {
                throw exception(CONTRACT_FULFILLMENT_CURRENCY_MISMATCH, source, target);
            }
            return new CurrencyConversion(source, target, BigDecimal.ONE);
        }
        String base = CrmErpFulfillmentProperties.normalizeCurrency(contract.getBaseCurrencyCode());
        if (!base.equals(target)) {
            throw exception(CONTRACT_FULFILLMENT_BASE_CURRENCY_MISMATCH, base, target);
        }
        BigDecimal rate = contract.getExchangeRateToBase();
        if (rate == null || rate.signum() <= 0) {
            throw exception(CONTRACT_FULFILLMENT_RATE_INVALID);
        }
        return new CurrencyConversion(source, target, rate);
    }

    private ErpSaleOrderCreateReqDTO buildRequest(CrmContractDO contract, CrmContractSigningDO signing,
                                                   List<CrmContractProductDO> products,
                                                   CrmErpCustomerMappingDO customerMapping,
                                                   Map<Long, CrmErpProductMappingDO> productMappings,
                                                   CurrencyConversion conversion) {
        List<ErpSaleOrderCreateReqDTO.Item> items = products.stream().map(product ->
                new ErpSaleOrderCreateReqDTO.Item()
                        .setProductId(productMappings.get(product.getProductId()).getErpProductId())
                        .setProductPrice(scale(product.getContractPrice().multiply(conversion.rate())))
                        .setCount(product.getCount()).setTaxPercent(zeroIfNull(product.getTaxRatePercent()))
                        .setRemark(product.getProductNoSnapshot() + " / " + product.getProductNameSnapshot())).toList();
        BigDecimal discount = zeroIfNull(contract.getDiscountPercent());
        BigDecimal expected = calculateErpTotal(items, discount);
        BigDecimal sourceGross = positiveOrFallback(contract.getGrossAmount(), contract.getTotalPrice());
        BigDecimal expectedContractBase = positiveOrFallback(contract.getBaseGrossAmount(),
                scale(sourceGross.multiply(conversion.rate())));
        if (expected.subtract(expectedContractBase).abs().compareTo(properties.getTotalTolerance()) > 0) {
            throw exception(CONTRACT_FULFILLMENT_AMOUNT_MISMATCH, expected, expectedContractBase,
                    properties.getTotalTolerance());
        }
        return new ErpSaleOrderCreateReqDTO().setSourceSystem(properties.getSourceSystem())
                .setSourceType(properties.getSourceType()).setSourceId(contract.getId())
                .setRequestId("CRM-CONTRACT-" + contract.getId() + "-V" + signing.getContractVersion())
                .setCustomerId(customerMapping.getErpCustomerId()).setOrderTime(signing.getSignedTime())
                .setSaleUserId(contract.getOwnerUserId()).setAccountId(properties.getDefaultAccountId())
                .setDiscountPercent(discount).setCurrencyCode(conversion.targetCurrency())
                .setSourceCurrencyCode(conversion.sourceCurrency())
                .setExchangeRateToOrderCurrency(conversion.rate()).setSourceGrossAmount(sourceGross)
                .setExpectedTotalPrice(expected).setRemark("CRM合同 " + contract.getNo() + " / " + contract.getName())
                .setItems(items);
    }

    private BigDecimal calculateErpTotal(List<ErpSaleOrderCreateReqDTO.Item> items, BigDecimal discount) {
        BigDecimal gross = BigDecimal.ZERO;
        for (ErpSaleOrderCreateReqDTO.Item item : items) {
            BigDecimal line = scale(item.getProductPrice().multiply(item.getCount()));
            BigDecimal tax = scale(line.multiply(item.getTaxPercent())
                    .divide(BigDecimal.valueOf(100), properties.getAmountScale(), properties.getRoundingMode()));
            gross = gross.add(line).add(tax);
        }
        BigDecimal discountAmount = scale(gross.multiply(discount)
                .divide(BigDecimal.valueOf(100), properties.getAmountScale(), properties.getRoundingMode()));
        return scale(gross.subtract(discountAmount));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(properties.getAmountScale(), properties.getRoundingMode());
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal positiveOrFallback(BigDecimal value, BigDecimal fallback) {
        return value != null && value.signum() > 0 ? value : zeroIfNull(fallback);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markCreated(Long fulfillmentId, String requestHash, ErpSaleOrderRespDTO order) {
        CrmContractFulfillmentDO fulfillment = fulfillmentMapper.selectById(fulfillmentId);
        if (fulfillment == null || !fulfillment.getRequestHash().equals(requestHash)) {
            throw exception(CONTRACT_FULFILLMENT_SNAPSHOT_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        fulfillmentMapper.updateById(new CrmContractFulfillmentDO().setId(fulfillmentId).setStatus(CREATED)
                .setErpOrderId(order.getId()).setErpOrderNo(order.getNo()).setErpOrderStatus(order.getStatus())
                .setErpTotalAmount(order.getTotalPrice()).setTotalCount(order.getTotalCount())
                .setOutCount(order.getOutCount()).setReturnCount(order.getReturnCount())
                .setCompletedTime(fulfillment.getCompletedTime() == null ? now : fulfillment.getCompletedTime())
                .setLastSyncTime(now).setLastErrorCode(null).setLastErrorMessage(null));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long fulfillmentId, String errorCode, String errorMessage) {
        CrmContractFulfillmentDO fulfillment = fulfillmentMapper.selectById(fulfillmentId);
        if (fulfillment == null || fulfillment.getStatus() == CREATED) {
            return;
        }
        fulfillmentMapper.updateById(new CrmContractFulfillmentDO().setId(fulfillmentId).setStatus(FAILED)
                .setLastErrorCode(errorCode).setLastErrorMessage(truncate(errorMessage)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void syncFromErp(ErpSaleOrderRespDTO order) {
        if (order == null || order.getId() == null) {
            return;
        }
        CrmContractFulfillmentDO fulfillment = fulfillmentMapper.selectByErpOrderId(order.getId());
        if (fulfillment == null) {
            return;
        }
        fulfillmentMapper.updateById(new CrmContractFulfillmentDO().setId(fulfillment.getId())
                .setErpOrderNo(order.getNo()).setErpOrderStatus(order.getStatus())
                .setErpTotalAmount(order.getTotalPrice()).setTotalCount(order.getTotalCount())
                .setOutCount(order.getOutCount()).setReturnCount(order.getReturnCount())
                .setLastSyncTime(LocalDateTime.now()));
    }

    private String truncate(String message) {
        String value = message == null ? "未知错误" : message;
        return value.length() <= properties.getMaxErrorMessageLength() ? value
                : value.substring(0, properties.getMaxErrorMessageLength());
    }

    private record CurrencyConversion(String sourceCurrency, String targetCurrency, BigDecimal rate) {
    }
}
