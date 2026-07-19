package com.meession.etm.module.crm.service.fulfillment;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpCustomerMappingRespVO;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpMappingSaveReqVO;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpProductMappingRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpCustomerMappingDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpProductMappingDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmErpCustomerMappingMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmErpProductMappingMapper;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.product.CrmProductService;
import com.meession.etm.module.erp.api.sale.ErpSaleOrderApi;
import com.meession.etm.module.erp.api.sale.dto.ErpCustomerReferenceDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpProductReferenceDTO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmErpMappingServiceImpl implements CrmErpMappingService {

    @Resource
    private CrmErpCustomerMappingMapper customerMappingMapper;
    @Resource
    private CrmErpProductMappingMapper productMappingMapper;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmProductService productService;
    @Resource
    private ErpSaleOrderApi erpSaleOrderApi;

    @Override
    public PageResult<CrmErpCustomerMappingRespVO> getCustomerMappingPage(PageParam pageParam) {
        PageResult<CrmErpCustomerMappingDO> page = customerMappingMapper.selectPage(pageParam);
        if (page.getList().isEmpty()) {
            return new PageResult<>(java.util.List.of(), page.getTotal());
        }
        Map<Long, CrmCustomerDO> crmMap = convertMap(customerService.getCustomerList(
                convertSet(page.getList(), CrmErpCustomerMappingDO::getCrmCustomerId)), CrmCustomerDO::getId);
        Map<Long, ErpCustomerReferenceDTO> erpMap = convertMap(erpSaleOrderApi.getCustomerReferences(
                convertSet(page.getList(), CrmErpCustomerMappingDO::getErpCustomerId)), ErpCustomerReferenceDTO::getId);
        return new PageResult<>(page.getList().stream().map(mapping -> {
            CrmCustomerDO crm = crmMap.get(mapping.getCrmCustomerId());
            ErpCustomerReferenceDTO erp = erpMap.get(mapping.getErpCustomerId());
            return new CrmErpCustomerMappingRespVO().setId(mapping.getId())
                    .setCrmCustomerId(mapping.getCrmCustomerId()).setCrmCustomerName(crm == null ? null : crm.getName())
                    .setErpCustomerId(mapping.getErpCustomerId()).setErpCustomerName(erp == null ? null : erp.getName())
                    .setRemark(mapping.getRemark()).setCreateTime(mapping.getCreateTime()).setUpdateTime(mapping.getUpdateTime());
        }).toList(), page.getTotal());
    }

    @Override
    public PageResult<CrmErpProductMappingRespVO> getProductMappingPage(PageParam pageParam) {
        PageResult<CrmErpProductMappingDO> page = productMappingMapper.selectPage(pageParam);
        if (page.getList().isEmpty()) {
            return new PageResult<>(java.util.List.of(), page.getTotal());
        }
        Map<Long, CrmProductDO> crmMap = convertMap(productService.getProductList(
                convertSet(page.getList(), CrmErpProductMappingDO::getCrmProductId)), CrmProductDO::getId);
        Map<Long, ErpProductReferenceDTO> erpMap = convertMap(erpSaleOrderApi.getProductReferences(
                convertSet(page.getList(), CrmErpProductMappingDO::getErpProductId)), ErpProductReferenceDTO::getId);
        return new PageResult<>(page.getList().stream().map(mapping -> {
            CrmProductDO crm = crmMap.get(mapping.getCrmProductId());
            ErpProductReferenceDTO erp = erpMap.get(mapping.getErpProductId());
            return new CrmErpProductMappingRespVO().setId(mapping.getId())
                    .setCrmProductId(mapping.getCrmProductId()).setCrmProductName(crm == null ? null : crm.getName())
                    .setCrmProductNo(crm == null ? null : crm.getNo()).setErpProductId(mapping.getErpProductId())
                    .setErpProductName(erp == null ? null : erp.getName())
                    .setErpProductBarCode(erp == null ? null : erp.getBarCode()).setRemark(mapping.getRemark())
                    .setCreateTime(mapping.getCreateTime()).setUpdateTime(mapping.getUpdateTime());
        }).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCustomerMapping(CrmErpMappingSaveReqVO request) {
        customerService.validateCustomer(request.getCrmId());
        erpSaleOrderApi.getCustomerReference(request.getErpId());
        CrmErpCustomerMappingDO occupied = customerMappingMapper.selectByErpCustomerId(request.getErpId());
        CrmErpCustomerMappingDO existing = customerMappingMapper.selectByCrmCustomerId(request.getCrmId());
        if (occupied != null && (existing == null || !occupied.getId().equals(existing.getId()))) {
            throw exception(ERP_CUSTOMER_MAPPING_CONFLICT, request.getErpId());
        }
        if (existing == null) {
            existing = new CrmErpCustomerMappingDO().setCrmCustomerId(request.getCrmId())
                    .setErpCustomerId(request.getErpId()).setRemark(request.getRemark());
            try {
                customerMappingMapper.insert(existing);
            } catch (DuplicateKeyException ex) {
                throw exception(ERP_CUSTOMER_MAPPING_CONFLICT, request.getErpId());
            }
        } else {
            customerMappingMapper.updateById(new CrmErpCustomerMappingDO().setId(existing.getId())
                    .setErpCustomerId(request.getErpId()).setRemark(request.getRemark()));
        }
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProductMapping(CrmErpMappingSaveReqVO request) {
        productService.validProductList(java.util.List.of(request.getCrmId()));
        erpSaleOrderApi.getProductReferences(java.util.List.of(request.getErpId()));
        CrmErpProductMappingDO occupied = productMappingMapper.selectByErpProductId(request.getErpId());
        CrmErpProductMappingDO existing = productMappingMapper.selectByCrmProductId(request.getCrmId());
        if (occupied != null && (existing == null || !occupied.getId().equals(existing.getId()))) {
            throw exception(ERP_PRODUCT_MAPPING_CONFLICT, request.getErpId());
        }
        if (existing == null) {
            existing = new CrmErpProductMappingDO().setCrmProductId(request.getCrmId())
                    .setErpProductId(request.getErpId()).setRemark(request.getRemark());
            try {
                productMappingMapper.insert(existing);
            } catch (DuplicateKeyException ex) {
                throw exception(ERP_PRODUCT_MAPPING_CONFLICT, request.getErpId());
            }
        } else {
            productMappingMapper.updateById(new CrmErpProductMappingDO().setId(existing.getId())
                    .setErpProductId(request.getErpId()).setRemark(request.getRemark()));
        }
        return existing.getId();
    }

    @Override
    public void deleteCustomerMapping(Long id) {
        if (customerMappingMapper.deleteById(id) == 0) {
            throw exception(ERP_MAPPING_NOT_EXISTS);
        }
    }

    @Override
    public void deleteProductMapping(Long id) {
        if (productMappingMapper.deleteById(id) == 0) {
            throw exception(ERP_MAPPING_NOT_EXISTS);
        }
    }
}
