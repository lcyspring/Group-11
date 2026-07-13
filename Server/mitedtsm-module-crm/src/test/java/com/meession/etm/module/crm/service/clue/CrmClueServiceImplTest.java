package com.meession.etm.module.crm.service.clue;

import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransferReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.CrmClueTransformReqVO;
import com.meession.etm.module.crm.controller.admin.contact.vo.CrmContactSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.followup.CrmFollowUpRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CLUE_TRANSFORM_FAIL_ALREADY;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CLUE_UPDATE_FAIL_TRANSFORMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmClueServiceImplTest {

    @Test
    void transformedClueRejectsDirectWrites() {
        CrmClueServiceImpl service = new CrmClueServiceImpl();
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByIdForUpdate")) {
                        return clue().setTransformStatus(true).setCustomerId(99L);
                    }
                    throw new AssertionError("已转换线索不应继续调用 " + method.getName());
                }));

        assertServiceException(() -> service.updateClue(new CrmClueSaveReqVO().setId(10L)),
                CLUE_UPDATE_FAIL_TRANSFORMED);
        assertServiceException(() -> service.updateClueFollowUp(10L, LocalDateTime.now(), "继续跟进"),
                CLUE_UPDATE_FAIL_TRANSFORMED);
        assertServiceException(() -> service.deleteClue(10L), CLUE_UPDATE_FAIL_TRANSFORMED);
        assertServiceException(() -> service.transferClue(
                new CrmClueTransferReqVO().setId(10L).setNewOwnerUserId(2L), 1L),
                CLUE_UPDATE_FAIL_TRANSFORMED);
    }

    @Test
    void transformClueStopsWhenAtomicClaimFails() {
        CrmClueServiceImpl service = new CrmClueServiceImpl();
        AtomicBoolean customerCreated = new AtomicBoolean();
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByIdForUpdate" -> clue();
                    case "updateTransformStatusByIdAndTransformStatus" -> 0;
                    default -> throw new AssertionError("抢占失败后不应继续调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> {
                    customerCreated.set(true);
                    throw new AssertionError("抢占失败后不应创建客户");
                }));

        assertServiceException(() -> service.transformClue(transformReq(), 1L), CLUE_TRANSFORM_FAIL_ALREADY);
        assertFalse(customerCreated.get());
    }

    @Test
    void transformClueClaimsBeforeCreatingAndLinksCustomer() {
        CrmClueServiceImpl service = new CrmClueServiceImpl();
        List<String> calls = new ArrayList<>();
        AtomicBoolean claimed = new AtomicBoolean();
        AtomicReference<CrmClueDO> linkedClue = new AtomicReference<>();
        AtomicReference<CrmContactSaveReqVO> createdContact = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "clueMapper", proxy(CrmClueMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByIdForUpdate" -> clue();
                    case "updateTransformStatusByIdAndTransformStatus" -> {
                        calls.add("claim");
                        claimed.set(true);
                        yield 1;
                    }
                    case "updateById" -> {
                        calls.add("link");
                        linkedClue.set((CrmClueDO) args[0]);
                        yield 1;
                    }
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "customerService", proxy(CrmCustomerService.class,
                (proxy, method, args) -> {
                    assertTrue(claimed.get(), "必须先原子抢占再创建客户");
                    calls.add("customer");
                    return 99L;
                }));
        ReflectionTestUtils.setField(service, "contactService", proxy(CrmContactService.class,
                (proxy, method, args) -> {
                    if (!method.getName().equals("createContact")) {
                        throw new AssertionError("未预期的联系人调用 " + method.getName());
                    }
                    calls.add("contact");
                    createdContact.set((CrmContactSaveReqVO) args[0]);
                    assertEquals(1L, args[1]);
                    return 88L;
                }));
        ReflectionTestUtils.setField(service, "followUpRecordService", proxy(CrmFollowUpRecordService.class,
                (proxy, method, args) -> Collections.emptyList()));

        service.transformClue(transformReq(), 1L);

        assertTrue(claimed.get());
        assertEquals(List.of("claim", "customer", "contact", "link"), calls);
        assertEquals("首联系人", createdContact.get().getName());
        assertEquals("13800138000", createdContact.get().getMobile());
        assertEquals(99L, createdContact.get().getCustomerId());
        assertEquals(1L, createdContact.get().getOwnerUserId());
        assertEquals("010-12345678", createdContact.get().getTelephone());
        assertTrue(createdContact.get().getPrimaryContact());
        assertFalse(createdContact.get().getMaster());
        assertEquals(10L, linkedClue.get().getId());
        assertEquals(99L, linkedClue.get().getCustomerId());
    }

    private static CrmClueTransformReqVO transformReq() {
        return new CrmClueTransformReqVO().setId(10L).setContactName("首联系人")
                .setContactMobile("13800138000");
    }

    private static CrmClueDO clue() {
        return new CrmClueDO().setId(10L).setName("待转换线索").setTransformStatus(false)
                .setTelephone("010-12345678").setWechat("contact-wechat").setEmail("contact@example.com");
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
