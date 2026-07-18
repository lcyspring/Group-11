package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingRecipientRespVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CrmMarketingRecipientResponseAssemblerTest {

    @Test
    void resolvesHistoricalRecipientNamesWithoutCurrentTargetOptions() {
        CrmMarketingRecipientResponseAssembler assembler = new CrmMarketingRecipientResponseAssembler();
        ReflectionTestUtils.setField(assembler, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("getCustomerMap")) {
                        return Map.of(14L, new CrmCustomerDO().setId(14L).setName("沉默客户甲"),
                                15L, new CrmCustomerDO().setId(15L).setName("沉默客户乙"));
                    }
                    throw new AssertionError("不应访问其他客户服务方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(assembler, "contactService", proxy(CrmContactService.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("getContactMap")) {
                        return Map.of(17L, new CrmContactDO().setId(17L).setName("联系人十七"));
                    }
                    throw new AssertionError("不应访问其他联系人服务方法: " + method.getName());
                }));
        PageResult<CrmMarketingBroadcastRecipientDO> page = new PageResult<>(List.of(
                new CrmMarketingBroadcastRecipientDO().setId(1L).setCustomerId(14L).setContactId(17L)
                        .setCustomerName("发送时客户").setContactName("发送时联系人"),
                new CrmMarketingBroadcastRecipientDO().setId(2L).setCustomerId(15L)), 2L);

        PageResult<CrmMarketingRecipientRespVO> result = assembler.assemble(page);

        assertAll(
                () -> assertEquals(2L, result.getTotal()),
                () -> assertEquals("发送时客户", result.getList().get(0).getCustomerName()),
                () -> assertEquals("发送时联系人", result.getList().get(0).getContactName()),
                () -> assertEquals("沉默客户乙", result.getList().get(1).getCustomerName()),
                () -> assertNull(result.getList().get(1).getContactName())
        );
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
