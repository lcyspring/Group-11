package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCustomerMapperTest {

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "crm-query-test");
        TableInfoHelper.initTableInfo(assistant, CrmCustomerDO.class);
        TableInfoHelper.initTableInfo(assistant, CrmContactDO.class);
    }

    @Test
    void selectPageDoesNotJoinContactsWithoutContactFilters() {
        MPJLambdaWrapperX<CrmCustomerDO> query = captureQuery(new CrmCustomerPageReqVO().setPool(true));

        assertFalse(query.getSelectDistinct());
        assertFalse(query.getFrom().contains("crm_contact"));
    }

    @Test
    void selectPageFiltersAnyContactNameAndUsesDistinct() {
        MPJLambdaWrapperX<CrmCustomerDO> query = captureQuery(new CrmCustomerPageReqVO()
                .setPool(true).setContactName("张三"));

        assertTrue(query.getSelectDistinct());
        assertTrue(query.getFrom().contains("crm_contact contact_filter"));
        assertTrue(query.getSqlSegment().contains("contact_filter.name LIKE"));
        assertTrue(hasParameterContaining(query, "张三"));
    }

    @Test
    void selectPageFiltersPrimaryContactIndependently() {
        MPJLambdaWrapperX<CrmCustomerDO> query = captureQuery(new CrmCustomerPageReqVO()
                .setPool(true).setContactName("普通联系人").setPrimaryContactName("首联系人"));

        assertTrue(query.getSelectDistinct());
        assertTrue(query.getFrom().contains("crm_contact contact_filter"));
        assertTrue(query.getFrom().contains("crm_contact primary_contact_filter"));
        assertTrue(query.getSqlSegment().contains("primary_contact_filter.primary_contact"));
        assertTrue(query.getSqlSegment().contains("primary_contact_filter.name LIKE"));
        assertTrue(hasParameterContaining(query, "普通联系人"));
        assertTrue(hasParameterContaining(query, "首联系人"));
        assertTrue(query.getParamNameValuePairs().containsValue(true));
    }

    private static boolean hasParameterContaining(MPJLambdaWrapperX<CrmCustomerDO> query, String expected) {
        return query.getParamNameValuePairs().values().stream()
                .anyMatch(value -> value instanceof String string && string.contains(expected));
    }

    @SuppressWarnings("unchecked")
    private static MPJLambdaWrapperX<CrmCustomerDO> captureQuery(CrmCustomerPageReqVO reqVO) {
        AtomicReference<MPJLambdaWrapperX<CrmCustomerDO>> captured = new AtomicReference<>();
        CrmCustomerMapper mapper = (CrmCustomerMapper) Proxy.newProxyInstance(
                CrmCustomerMapper.class.getClassLoader(), new Class<?>[]{CrmCustomerMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectJoinPage")) {
                        captured.set((MPJLambdaWrapperX<CrmCustomerDO>) args[2]);
                        return PageResult.empty();
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                });

        mapper.selectPage(reqVO, 1L);
        return captured.get();
    }

}
