package com.meession.etm.module.crm.service.fulfillment;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpCustomerMappingRespVO;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpMappingSaveReqVO;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpProductMappingRespVO;
import jakarta.validation.Valid;

public interface CrmErpMappingService {

    PageResult<CrmErpCustomerMappingRespVO> getCustomerMappingPage(PageParam pageParam);

    PageResult<CrmErpProductMappingRespVO> getProductMappingPage(PageParam pageParam);

    Long saveCustomerMapping(@Valid CrmErpMappingSaveReqVO request);

    Long saveProductMapping(@Valid CrmErpMappingSaveReqVO request);

    void deleteCustomerMapping(Long id);

    void deleteProductMapping(Long id);
}
