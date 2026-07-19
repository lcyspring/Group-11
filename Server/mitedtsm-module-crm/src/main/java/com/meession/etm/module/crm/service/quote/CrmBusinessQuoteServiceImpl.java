package com.meession.etm.module.crm.service.quote;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteItemDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.dal.mysql.quote.CrmBusinessQuoteActionRecordMapper;
import com.meession.etm.module.crm.dal.mysql.quote.CrmBusinessQuoteItemMapper;
import com.meession.etm.module.crm.dal.mysql.quote.CrmBusinessQuoteMapper;
import com.meession.etm.module.crm.enums.quote.CrmQuoteActionTypeEnum;
import com.meession.etm.module.crm.enums.quote.CrmQuoteStatusEnum;
import com.meession.etm.module.crm.framework.quote.CrmQuoteProperties;
import com.meession.etm.module.crm.service.product.CrmProductService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmBusinessQuoteServiceImpl implements CrmBusinessQuoteService {

    @Resource private CrmBusinessQuoteMapper quoteMapper;
    @Resource private CrmBusinessQuoteItemMapper itemMapper;
    @Resource private CrmBusinessQuoteActionRecordMapper actionMapper;
    @Resource private CrmBusinessMapper businessMapper;
    @Resource private CrmProductService productService;
    @Resource private CrmQuoteProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
    public Long createInitialDraft(Long businessId, CrmBusinessSaveReqVO reqVO, Long userId) {
        if (quoteMapper.selectLatest(businessId) != null) {
            throw exception(QUOTE_VERSION_CONCURRENT);
        }
        CrmBusinessQuoteDO quote = new CrmBusinessQuoteDO().setBusinessId(businessId).setVersionNo(1)
                .setStatus(CrmQuoteStatusEnum.DRAFT.getStatus()).setVersion(0);
        applyDraft(quote, reqVO, buildItems(reqVO), userId, CrmQuoteActionTypeEnum.CREATE, "创建报价草稿");
        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
    public Long requireDraftForUpdate(Long businessId) {
        CrmBusinessQuoteDO quote = quoteMapper.selectLatestForUpdate(businessId);
        if (quote == null) throw exception(QUOTE_NOT_EXISTS);
        if (!Objects.equals(quote.getStatus(), CrmQuoteStatusEnum.DRAFT.getStatus())) {
            throw exception(QUOTE_LOCKED_IMMUTABLE);
        }
        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
    public void syncDraft(Long quoteId, CrmBusinessSaveReqVO reqVO, Long userId) {
        CrmBusinessQuoteDO latest = quoteMapper.selectLatestForUpdate(reqVO.getId());
        if (latest == null || !Objects.equals(latest.getId(), quoteId)
                || !Objects.equals(latest.getStatus(), CrmQuoteStatusEnum.DRAFT.getStatus())) {
            throw exception(QUOTE_LOCKED_IMMUTABLE);
        }
        applyDraft(latest, reqVO, buildItems(reqVO), userId, CrmQuoteActionTypeEnum.UPDATE, "更新报价草稿");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmBusinessQuoteDO lockQuote(Long businessId, String remark, Long userId) {
        CrmBusinessDO business = businessMapper.selectByIdForUpdate(businessId);
        if (business == null) throw exception(BUSINESS_NOT_EXISTS);
        if (business.getEndStatus() != null) throw exception(QUOTE_REOPEN_BUSINESS_ENDED);
        CrmBusinessQuoteDO quote = quoteMapper.selectLatestForUpdate(businessId);
        if (quote == null) throw exception(QUOTE_NOT_EXISTS);
        if (!Objects.equals(quote.getStatus(), CrmQuoteStatusEnum.DRAFT.getStatus())) {
            throw exception(QUOTE_LOCK_STATUS_INVALID);
        }
        List<CrmBusinessQuoteItemDO> items = itemMapper.selectByQuoteId(quote.getId());
        if (CollUtil.isEmpty(items)) throw exception(QUOTE_ITEM_REQUIRED);
        LocalDateTime now = LocalDateTime.now();
        if (quoteMapper.lockDraft(quote.getId(), quote.getVersion(), userId, now) != 1) {
            throw exception(QUOTE_VERSION_CONCURRENT);
        }
        appendAction(quote.getId(), CrmQuoteActionTypeEnum.LOCK, CrmQuoteStatusEnum.DRAFT.getStatus(),
                CrmQuoteStatusEnum.LOCKED.getStatus(), userId, requireRemark(remark));
        return quote.setStatus(CrmQuoteStatusEnum.LOCKED.getStatus()).setLockedBy(userId).setLockedAt(now)
                .setVersion(quote.getVersion() + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmBusinessQuoteDO reopenQuote(Long businessId, String remark, Long userId) {
        CrmBusinessDO business = businessMapper.selectByIdForUpdate(businessId);
        if (business == null) throw exception(BUSINESS_NOT_EXISTS);
        if (business.getEndStatus() != null) throw exception(QUOTE_REOPEN_BUSINESS_ENDED);
        CrmBusinessQuoteDO old = quoteMapper.selectLatestForUpdate(businessId);
        if (old == null) throw exception(QUOTE_NOT_EXISTS);
        if (!Objects.equals(old.getStatus(), CrmQuoteStatusEnum.LOCKED.getStatus())) {
            throw exception(QUOTE_REOPEN_STATUS_INVALID);
        }
        if (old.getVersionNo() >= properties.getMaxVersionsPerBusiness()) {
            throw exception(QUOTE_VERSION_LIMIT, properties.getMaxVersionsPerBusiness());
        }
        String normalizedRemark = requireRemark(remark);
        if (quoteMapper.supersedeLocked(old.getId(), old.getVersion()) != 1) {
            throw exception(QUOTE_VERSION_CONCURRENT);
        }
        appendAction(old.getId(), CrmQuoteActionTypeEnum.REOPEN, CrmQuoteStatusEnum.LOCKED.getStatus(),
                CrmQuoteStatusEnum.SUPERSEDED.getStatus(), userId, normalizedRemark);

        CrmBusinessQuoteDO draft = copyHeader(old).setId(null).setVersionNo(old.getVersionNo() + 1)
                .setStatus(CrmQuoteStatusEnum.DRAFT.getStatus()).setSourceQuoteId(old.getId())
                .setLockedBy(null).setLockedAt(null).setVersion(0);
        try {
            quoteMapper.insert(draft);
        } catch (DuplicateKeyException ex) {
            throw exception(QUOTE_VERSION_CONCURRENT);
        }
        List<CrmBusinessQuoteItemDO> clonedItems = itemMapper.selectByQuoteId(old.getId()).stream()
                .map(this::copyItem).peek(item -> item.setId(null).setQuoteId(draft.getId())).toList();
        if (CollUtil.isNotEmpty(clonedItems)) itemMapper.insertBatch(clonedItems);
        appendAction(draft.getId(), CrmQuoteActionTypeEnum.REOPEN, null,
                CrmQuoteStatusEnum.DRAFT.getStatus(), userId,
                "从报价 V" + old.getVersionNo() + " 重开：" + normalizedRemark);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
    public void terminateCurrent(Long businessId, String remark, Long userId) {
        CrmBusinessQuoteDO quote = quoteMapper.selectLatestForUpdate(businessId);
        if (quote == null) throw exception(QUOTE_NOT_EXISTS);
        if (!Objects.equals(quote.getStatus(), CrmQuoteStatusEnum.DRAFT.getStatus())
                && !Objects.equals(quote.getStatus(), CrmQuoteStatusEnum.LOCKED.getStatus())) {
            throw exception(QUOTE_TERMINATE_STATUS_INVALID);
        }
        if (quoteMapper.terminateCurrent(quote.getId(), quote.getVersion(), quote.getStatus()) != 1) {
            throw exception(QUOTE_VERSION_CONCURRENT);
        }
        appendAction(quote.getId(), CrmQuoteActionTypeEnum.TERMINATE, quote.getStatus(),
                CrmQuoteStatusEnum.TERMINATED.getStatus(), userId, requireRemark(remark));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuoteSnapshot requireCurrentLocked(Long businessId) {
        CrmBusinessQuoteDO quote = quoteMapper.selectLatestForUpdate(businessId);
        if (quote == null || !Objects.equals(quote.getStatus(), CrmQuoteStatusEnum.LOCKED.getStatus())) {
            throw exception(QUOTE_CURRENT_NOT_LOCKED);
        }
        List<CrmBusinessQuoteItemDO> items = itemMapper.selectByQuoteId(quote.getId());
        if (CollUtil.isEmpty(items)) throw exception(QUOTE_ITEM_REQUIRED);
        return new QuoteSnapshot(quote, items);
    }

    @Override
    public CrmBusinessQuoteDO getCurrent(Long businessId) {
        return quoteMapper.selectLatest(businessId);
    }

    @Override
    public List<CrmBusinessQuoteDO> getVersions(Long businessId) {
        return quoteMapper.selectVersions(businessId);
    }

    @Override
    public List<CrmBusinessQuoteItemDO> getItems(Long quoteId) {
        return itemMapper.selectByQuoteId(quoteId);
    }

    @Override
    public List<CrmBusinessQuoteActionRecordDO> getActions(Long quoteId) {
        return actionMapper.selectByQuoteId(quoteId);
    }

    private void applyDraft(CrmBusinessQuoteDO quote, CrmBusinessSaveReqVO reqVO,
                            List<CrmBusinessQuoteItemDO> items, Long userId,
                            CrmQuoteActionTypeEnum action, String remark) {
        String currency = CrmQuoteProperties.normalize(StrUtil.blankToDefault(
                reqVO.getCurrencyCode(), properties.getDefaultCurrency()));
        BigDecimal rate;
        try {
            rate = properties.requireExchangeRate(currency);
        } catch (IllegalArgumentException ex) {
            throw exception(QUOTE_CURRENCY_UNSUPPORTED, currency);
        }
        BigDecimal discount = reqVO.getDiscountPercent() == null ? BigDecimal.ZERO : reqVO.getDiscountPercent();
        if (discount.signum() < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            throw exception(QUOTE_DISCOUNT_INVALID);
        }
        Totals totals = calculate(items, discount, rate);
        quote.setCurrencyCode(currency).setBaseCurrencyCode(properties.getBaseCurrency())
                .setExchangeRateToBase(scale(rate)).setDiscountPercent(scale(discount))
                .setSubtotal(totals.subtotal()).setDiscountAmount(totals.discountAmount())
                .setNetAmount(totals.netAmount()).setTaxAmount(totals.taxAmount())
                .setGrossAmount(totals.grossAmount()).setBaseGrossAmount(totals.baseGrossAmount());
        if (quote.getId() == null) quoteMapper.insert(quote); else quoteMapper.updateById(quote);
        itemMapper.deleteByQuoteId(quote.getId());
        items.forEach(item -> item.setId(null).setQuoteId(quote.getId()));
        if (CollUtil.isNotEmpty(items)) itemMapper.insertBatch(items);
        appendAction(quote.getId(), action,
                action == CrmQuoteActionTypeEnum.CREATE ? null : CrmQuoteStatusEnum.DRAFT.getStatus(),
                CrmQuoteStatusEnum.DRAFT.getStatus(), userId, remark);
    }

    private List<CrmBusinessQuoteItemDO> buildItems(CrmBusinessSaveReqVO reqVO) {
        List<CrmBusinessSaveReqVO.BusinessProduct> requests = reqVO.getProducts();
        if (CollUtil.isEmpty(requests)) return new ArrayList<>();
        Collection<Long> productIds = requests.stream().map(CrmBusinessSaveReqVO.BusinessProduct::getProductId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<Long, CrmProductDO> products = convertMap(productService.validProductList(productIds), CrmProductDO::getId);
        List<CrmBusinessQuoteItemDO> items = new ArrayList<>(requests.size());
        for (CrmBusinessSaveReqVO.BusinessProduct request : requests) {
            CrmProductDO product = products.get(request.getProductId());
            BigDecimal unitPrice = request.getBusinessPrice();
            BigDecimal count = request.getCount();
            if (unitPrice == null || unitPrice.signum() < 0 || count == null || count.signum() <= 0) {
                throw exception(QUOTE_ITEM_AMOUNT_INVALID);
            }
            BigDecimal taxRate = request.getTaxRatePercent() == null
                    ? properties.getDefaultTaxRate() : request.getTaxRatePercent();
            boolean allowed = properties.getAllowedTaxRates().stream()
                    .anyMatch(rate -> rate.compareTo(taxRate) == 0);
            if (!allowed) throw exception(QUOTE_TAX_RATE_UNSUPPORTED, taxRate);
            items.add(new CrmBusinessQuoteItemDO().setProductId(product.getId())
                    .setProductNameSnapshot(product.getName()).setProductNoSnapshot(product.getNo())
                    .setProductUnitSnapshot(product.getUnit()).setProductCategoryIdSnapshot(product.getCategoryId())
                    .setProductVersionSnapshot(product.getVersion() == null ? 1 : product.getVersion())
                    .setListPrice(scale(product.getPrice())).setBusinessPrice(scale(unitPrice)).setCount(scale(count))
                    .setTaxRatePercent(scale(taxRate)));
        }
        return items;
    }

    private Totals calculate(List<CrmBusinessQuoteItemDO> items, BigDecimal discountPercent,
                             BigDecimal exchangeRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal grossAmount = BigDecimal.ZERO;
        BigDecimal factor = discountPercent.movePointLeft(2);
        for (CrmBusinessQuoteItemDO item : items) {
            BigDecimal lineSubtotal = scale(item.getBusinessPrice().multiply(item.getCount()));
            BigDecimal lineDiscount = scale(lineSubtotal.multiply(factor));
            BigDecimal lineNet = scale(lineSubtotal.subtract(lineDiscount));
            BigDecimal lineTax = scale(lineNet.multiply(item.getTaxRatePercent().movePointLeft(2)));
            BigDecimal lineGross = scale(lineNet.add(lineTax));
            item.setLineSubtotal(lineSubtotal).setLineDiscountAmount(lineDiscount).setNetAmount(lineNet)
                    .setTaxAmount(lineTax).setGrossAmount(lineGross);
            subtotal = subtotal.add(lineSubtotal);
            discountAmount = discountAmount.add(lineDiscount);
            netAmount = netAmount.add(lineNet);
            taxAmount = taxAmount.add(lineTax);
            grossAmount = grossAmount.add(lineGross);
        }
        return new Totals(scale(subtotal), scale(discountAmount), scale(netAmount), scale(taxAmount),
                scale(grossAmount), scale(grossAmount.multiply(exchangeRate)));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(properties.getAmountScale(), properties.resolvedRoundingMode());
    }

    private void appendAction(Long quoteId, CrmQuoteActionTypeEnum action, Integer fromStatus,
                              Integer toStatus, Long userId, String remark) {
        actionMapper.insert(new CrmBusinessQuoteActionRecordDO().setQuoteId(quoteId).setActionType(action.getType())
                .setFromStatus(fromStatus).setToStatus(toStatus).setOperatorUserId(userId).setRemark(remark));
    }

    private String requireRemark(String remark) {
        String normalized = StrUtil.trim(remark);
        if (StrUtil.isBlank(normalized)) throw exception(QUOTE_ACTION_REMARK_REQUIRED);
        return normalized;
    }

    private CrmBusinessQuoteDO copyHeader(CrmBusinessQuoteDO source) {
        return new CrmBusinessQuoteDO().setBusinessId(source.getBusinessId()).setCurrencyCode(source.getCurrencyCode())
                .setBaseCurrencyCode(source.getBaseCurrencyCode()).setExchangeRateToBase(source.getExchangeRateToBase())
                .setDiscountPercent(source.getDiscountPercent()).setSubtotal(source.getSubtotal())
                .setDiscountAmount(source.getDiscountAmount()).setNetAmount(source.getNetAmount())
                .setTaxAmount(source.getTaxAmount()).setGrossAmount(source.getGrossAmount())
                .setBaseGrossAmount(source.getBaseGrossAmount());
    }

    private CrmBusinessQuoteItemDO copyItem(CrmBusinessQuoteItemDO source) {
        return new CrmBusinessQuoteItemDO().setProductId(source.getProductId())
                .setProductNameSnapshot(source.getProductNameSnapshot()).setProductNoSnapshot(source.getProductNoSnapshot())
                .setProductUnitSnapshot(source.getProductUnitSnapshot())
                .setProductCategoryIdSnapshot(source.getProductCategoryIdSnapshot())
                .setProductVersionSnapshot(source.getProductVersionSnapshot()).setListPrice(source.getListPrice())
                .setBusinessPrice(source.getBusinessPrice()).setCount(source.getCount())
                .setTaxRatePercent(source.getTaxRatePercent()).setLineSubtotal(source.getLineSubtotal())
                .setLineDiscountAmount(source.getLineDiscountAmount()).setNetAmount(source.getNetAmount())
                .setTaxAmount(source.getTaxAmount()).setGrossAmount(source.getGrossAmount());
    }

    private record Totals(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal netAmount,
                          BigDecimal taxAmount, BigDecimal grossAmount, BigDecimal baseGrossAmount) {
    }
}
