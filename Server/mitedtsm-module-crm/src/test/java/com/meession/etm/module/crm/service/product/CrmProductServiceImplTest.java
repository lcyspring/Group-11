package com.meession.etm.module.crm.service.product;

import com.meession.etm.module.crm.controller.admin.product.vo.product.CrmProductSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductCategoryDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.mysql.product.CrmProductMapper;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmProductServiceImplTest {

    @Mock private CrmProductMapper productMapper;
    @Mock private CrmProductCategoryService productCategoryService;
    @Mock private CrmPermissionService permissionService;
    @Mock private AdminUserApi adminUserApi;
    @InjectMocks private CrmProductServiceImpl service;

    @Test
    void updateProductLocksCurrentRowAndIncrementsSnapshotVersion() {
        CrmProductDO current = new CrmProductDO().setId(8L).setName("旧产品").setNo("SKU-8")
                .setCategoryId(3L).setPrice(new BigDecimal("10.00")).setStatus(1).setVersion(7);
        when(productMapper.selectByIdForUpdate(8L)).thenReturn(current);
        when(productMapper.selectByNo("SKU-8")).thenReturn(current);
        when(productCategoryService.getProductCategory(3L))
                .thenReturn(new CrmProductCategoryDO().setId(3L).setName("分类"));

        service.updateProduct(new CrmProductSaveReqVO().setId(8L).setName("新产品").setNo("SKU-8")
                .setCategoryId(3L).setPrice(new BigDecimal("11.25")).setStatus(1).setUnit(2)
                .setOwnerUserId(99L));

        ArgumentCaptor<CrmProductDO> captor = ArgumentCaptor.forClass(CrmProductDO.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(8, captor.getValue().getVersion());
        assertEquals(new BigDecimal("11.25"), captor.getValue().getPrice());
    }
}
