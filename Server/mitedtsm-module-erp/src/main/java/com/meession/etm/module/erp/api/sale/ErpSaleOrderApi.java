package com.meession.etm.module.erp.api.sale;

import com.meession.etm.module.erp.api.sale.dto.ErpCustomerReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpProductReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderRespDTO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * ERP 销售履约应用接口。
 *
 * <p>跨域调用必须通过本接口，不允许 CRM 直接访问 ERP Mapper 或内部 Controller VO。</p>
 */
public interface ErpSaleOrderApi {

    ErpSaleOrderRespDTO createOrGetExternalSaleOrder(@Valid ErpSaleOrderCreateReqDTO request);

    ErpSaleOrderRespDTO getSaleOrder(Long id);

    ErpCustomerReferenceDTO getCustomerReference(Long id);

    List<ErpCustomerReferenceDTO> getCustomerReferences(Collection<Long> ids);

    List<ErpProductReferenceDTO> getProductReferences(Collection<Long> ids);
}
