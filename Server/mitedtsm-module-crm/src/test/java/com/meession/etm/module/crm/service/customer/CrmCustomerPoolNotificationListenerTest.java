package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CrmCustomerPoolNotificationListenerTest {

    @Test
    void listenerIsBoundToAfterCommitPhase() throws NoSuchMethodException {
        TransactionalEventListener annotation = CrmCustomerPoolNotificationListener.class
                .getMethod("onPutPool", CrmCustomerPutPoolEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }

    @Test
    void sendsTemplateNotificationToPreviousOwner() {
        AtomicReference<NotifySendSingleToUserReqDTO> requestRef = new AtomicReference<>();
        CrmCustomerPoolNotificationListener listener = new CrmCustomerPoolNotificationListener();
        ReflectionTestUtils.setField(listener, "notifyMessageSendApi", proxy(NotifyMessageSendApi.class,
                (proxy, method, args) -> {
                    requestRef.set((NotifySendSingleToUserReqDTO) args[0]);
                    return 1L;
                }));

        listener.onPutPool(new CrmCustomerPutPoolEvent(20L, "客户", 7L,
                "AUTO_NO_FOLLOW_UP", "超过配置天数未跟进"));

        assertEquals(7L, requestRef.get().getUserId());
        assertEquals(CrmCustomerPoolNotificationListener.PUT_POOL_TEMPLATE, requestRef.get().getTemplateCode());
        assertEquals("客户", requestRef.get().getTemplateParams().get("customerName"));
    }

    @Test
    void skipsNotificationWhenThereWasNoPreviousOwner() {
        AtomicReference<NotifySendSingleToUserReqDTO> requestRef = new AtomicReference<>();
        CrmCustomerPoolNotificationListener listener = new CrmCustomerPoolNotificationListener();
        ReflectionTestUtils.setField(listener, "notifyMessageSendApi", proxy(NotifyMessageSendApi.class,
                (proxy, method, args) -> {
                    requestRef.set((NotifySendSingleToUserReqDTO) args[0]);
                    return 1L;
                }));

        listener.onPutPool(new CrmCustomerPutPoolEvent(20L, "客户", null,
                "IMPORT_UNASSIGNED", "导入时未指定负责人"));

        assertNull(requestRef.get());
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
