package com.meession.etm.module.erp.api.sale;

import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.module.erp.api.sale.dto.ErpCustomerReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpProductReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderRespDTO;
import com.meession.etm.module.erp.dal.dataobject.product.ErpProductDO;
import com.meession.etm.module.erp.dal.dataobject.sale.ErpCustomerDO;
import com.meession.etm.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import com.meession.etm.module.erp.service.product.ErpProductService;
import com.meession.etm.module.erp.service.sale.ErpCustomerService;
import com.meession.etm.module.erp.service.sale.ErpSaleOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;
import static com.meession.etm.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static com.meession.etm.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_ENABLE;

@Service
@Validated
public class ErpSaleOrderApiImpl implements ErpSaleOrderApi {

    @Resource
    private ErpSaleOrderService saleOrderService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;

    @Override
    public ErpSaleOrderRespDTO createOrGetExternalSaleOrder(ErpSaleOrderCreateReqDTO request) {
        return saleOrderService.createOrGetExternalSaleOrder(request);
    }

    @Override
    public ErpSaleOrderRespDTO getSaleOrder(Long id) {
        ErpSaleOrderDO order = saleOrderService.getSaleOrder(id);
        return order == null ? null : saleOrderService.toApiResponse(order);
    }

    @Override
    public ErpCustomerReferenceDTO getCustomerReference(Long id) {
        return getCustomerReferences(List.of(id)).get(0);
    }

    @Override
    public List<ErpCustomerReferenceDTO> getCustomerReferences(Collection<Long> ids) {
        List<ErpCustomerDO> customers = customerService.getCustomerList(ids);
        if (customers.size() != ids.size()) {
            ids.forEach(customerService::validateCustomer);
        }
        for (ErpCustomerDO customer : customers) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(customer.getStatus())) {
                throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
            }
        }
        return convertList(customers, customer -> new ErpCustomerReferenceDTO().setId(customer.getId())
                .setName(customer.getName()).setStatus(customer.getStatus()));
    }

    @Override
    public List<ErpProductReferenceDTO> getProductReferences(Collection<Long> ids) {
        List<ErpProductDO> products = productService.validProductList(ids);
        for (ErpProductDO product : products) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(product.getStatus())) {
                throw exception(PRODUCT_NOT_ENABLE, product.getName());
            }
        }
        return convertList(products, product -> new ErpProductReferenceDTO().setId(product.getId())
                .setName(product.getName()).setBarCode(product.getBarCode()).setUnitId(product.getUnitId())
                .setStatus(product.getStatus()));
    }
}
