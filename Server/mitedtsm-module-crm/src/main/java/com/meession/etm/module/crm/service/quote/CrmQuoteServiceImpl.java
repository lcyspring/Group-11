package com.meession.etm.module.crm.service.quote;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.common.util.number.MoneyUtils;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteCalculateReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteCalculateRespVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteDiscountCalculateReqVO;
import com.meession.etm.module.crm.controller.admin.quote.vo.CrmQuoteDiscountCalculateRespVO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.service.product.CrmProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;

@Service
public class CrmQuoteServiceImpl implements CrmQuoteService {

    @Resource
    private CrmProductService productService;

    @Override
    public CrmQuoteCalculateRespVO calculateQuote(CrmQuoteCalculateReqVO reqVO) {
        List<CrmQuoteCalculateReqVO.Product> products = reqVO.getProducts();
        Set<Long> productIds = convertSet(products, CrmQuoteCalculateReqVO.Product::getProductId);
        productService.validProductList(productIds);
        Map<Long, CrmProductDO> productMap = productService.getProductMap(productIds);

        BigDecimal totalProductPrice = BigDecimal.ZERO;
        List<CrmQuoteCalculateRespVO.Product> productList = new ArrayList<>();

        for (CrmQuoteCalculateReqVO.Product productReq : products) {
            CrmProductDO product = productMap.get(productReq.getProductId());
            BigDecimal productPrice = product.getPrice();
            BigDecimal count = productReq.getCount();
            BigDecimal totalPrice = MoneyUtils.priceMultiply(productPrice, count);

            totalProductPrice = totalProductPrice.add(totalPrice);

            CrmQuoteCalculateRespVO.Product productVO = new CrmQuoteCalculateRespVO.Product();
            productVO.setProductId(product.getId());
            productVO.setProductName(product.getName());
            productVO.setProductNo(product.getNo());
            productVO.setProductPrice(productPrice);
            productVO.setCount(count);
            productVO.setTotalPrice(totalPrice);
            productList.add(productVO);
        }

        BigDecimal discountPercent = reqVO.getDiscountPercent() != null ? reqVO.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountAmount = MoneyUtils.priceMultiplyPercent(totalProductPrice, discountPercent);
        BigDecimal totalPrice = totalProductPrice.subtract(discountAmount);

        CrmQuoteCalculateRespVO respVO = new CrmQuoteCalculateRespVO();
        respVO.setTotalProductPrice(totalProductPrice);
        respVO.setDiscountPercent(discountPercent);
        respVO.setDiscountAmount(discountAmount);
        respVO.setTotalPrice(totalPrice);
        respVO.setProducts(productList);

        return respVO;
    }

    @Override
    public CrmQuoteDiscountCalculateRespVO calculateDiscount(CrmQuoteDiscountCalculateReqVO reqVO) {
        BigDecimal totalAmount = reqVO.getTotalAmount();
        BigDecimal totalDiscountPercent = BigDecimal.ZERO;
        List<CrmQuoteDiscountCalculateRespVO.DiscountRule> rules = new ArrayList<>();

        BigDecimal amountDiscount = calculateAmountDiscount(totalAmount);
        if (amountDiscount.compareTo(BigDecimal.ZERO) > 0) {
            totalDiscountPercent = totalDiscountPercent.add(amountDiscount);
            CrmQuoteDiscountCalculateRespVO.DiscountRule rule = new CrmQuoteDiscountCalculateRespVO.DiscountRule();
            rule.setRuleName("金额阶梯折扣");
            rule.setDiscountPercent(amountDiscount);
            rule.setDescription(getAmountDiscountDescription(totalAmount, amountDiscount));
            rules.add(rule);
        }

        if (reqVO.getCustomerLevel() != null) {
            BigDecimal customerDiscount = calculateCustomerLevelDiscount(reqVO.getCustomerLevel());
            if (customerDiscount.compareTo(BigDecimal.ZERO) > 0) {
                totalDiscountPercent = totalDiscountPercent.add(customerDiscount);
                CrmQuoteDiscountCalculateRespVO.DiscountRule rule = new CrmQuoteDiscountCalculateRespVO.DiscountRule();
                rule.setRuleName("客户等级折扣");
                rule.setDiscountPercent(customerDiscount);
                rule.setDescription(getCustomerLevelDiscountDescription(reqVO.getCustomerLevel(), customerDiscount));
                rules.add(rule);
            }
        }

        if (reqVO.getProductCount() != null) {
            BigDecimal quantityDiscount = calculateQuantityDiscount(reqVO.getProductCount());
            if (quantityDiscount.compareTo(BigDecimal.ZERO) > 0) {
                totalDiscountPercent = totalDiscountPercent.add(quantityDiscount);
                CrmQuoteDiscountCalculateRespVO.DiscountRule rule = new CrmQuoteDiscountCalculateRespVO.DiscountRule();
                rule.setRuleName("数量折扣");
                rule.setDiscountPercent(quantityDiscount);
                rule.setDescription(getQuantityDiscountDescription(reqVO.getProductCount(), quantityDiscount));
                rules.add(rule);
            }
        }

        if (totalDiscountPercent.compareTo(new BigDecimal("50")) > 0) {
            totalDiscountPercent = new BigDecimal("50");
        }

        BigDecimal discountAmount = MoneyUtils.priceMultiplyPercent(totalAmount, totalDiscountPercent);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        CrmQuoteDiscountCalculateRespVO respVO = new CrmQuoteDiscountCalculateRespVO();
        respVO.setDiscountPercent(totalDiscountPercent);
        respVO.setDiscountAmount(discountAmount);
        respVO.setFinalAmount(finalAmount);
        respVO.setRules(rules);

        return respVO;
    }

    private BigDecimal calculateAmountDiscount(BigDecimal totalAmount) {
        if (totalAmount.compareTo(new BigDecimal("10000")) >= 0) {
            return new BigDecimal("15");
        } else if (totalAmount.compareTo(new BigDecimal("5000")) >= 0) {
            return new BigDecimal("10");
        } else if (totalAmount.compareTo(new BigDecimal("1000")) >= 0) {
            return new BigDecimal("5");
        }
        return BigDecimal.ZERO;
    }

    private String getAmountDiscountDescription(BigDecimal totalAmount, BigDecimal discount) {
        if (totalAmount.compareTo(new BigDecimal("10000")) >= 0) {
            return "满10000打85折";
        } else if (totalAmount.compareTo(new BigDecimal("5000")) >= 0) {
            return "满5000打9折";
        } else if (totalAmount.compareTo(new BigDecimal("1000")) >= 0) {
            return "满1000打95折";
        }
        return "";
    }

    private BigDecimal calculateCustomerLevelDiscount(Integer customerLevel) {
        switch (customerLevel) {
            case 4:
                return new BigDecimal("8");
            case 3:
                return new BigDecimal("5");
            case 2:
                return new BigDecimal("3");
            default:
                return BigDecimal.ZERO;
        }
    }

    private String getCustomerLevelDiscountDescription(Integer customerLevel, BigDecimal discount) {
        String levelName;
        switch (customerLevel) {
            case 4:
                levelName = "钻石会员";
                break;
            case 3:
                levelName = "金卡会员";
                break;
            case 2:
                levelName = "银卡会员";
                break;
            default:
                levelName = "普通会员";
                break;
        }
        return levelName + "额外" + discount + "%折扣";
    }

    private BigDecimal calculateQuantityDiscount(Integer productCount) {
        if (productCount >= 50) {
            return new BigDecimal("5");
        } else if (productCount >= 10) {
            return new BigDecimal("2");
        }
        return BigDecimal.ZERO;
    }

    private String getQuantityDiscountDescription(Integer productCount, BigDecimal discount) {
        if (productCount >= 50) {
            return "购买50件以上打95折";
        } else if (productCount >= 10) {
            return "购买10件以上打98折";
        }
        return "";
    }

}
