package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CrmReceivableServiceImplTest {

    @Test
    void submitReceivableRevalidatesRemainingContractAmountAfterLock() {
        long receivableId = 10L;
        long contractId = 20L;
        AtomicBoolean contractLocked = new AtomicBoolean();
        AtomicBoolean processCreated = new AtomicBoolean();
        AtomicInteger receivableReads = new AtomicInteger();
        CrmReceivableDO draft = new CrmReceivableDO().setId(receivableId).setContractId(contractId)
                .setNo("HK-10").setPrice(new BigDecimal("50"))
                .setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        CrmReceivableDO processing = new CrmReceivableDO().setId(11L).setContractId(contractId)
                .setPrice(new BigDecimal("60")).setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus());

        CrmReceivableServiceImpl service = new CrmReceivableServiceImpl();
        ReflectionTestUtils.setField(service, "receivableMapper", proxy(CrmReceivableMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> {
                        receivableReads.incrementAndGet();
                        yield draft;
                    }
                    case "selectByIdForUpdate" -> {
                        if (!contractLocked.get()) {
                            throw new AssertionError("锁定回款前必须先锁定合同");
                        }
                        receivableReads.incrementAndGet();
                        yield draft;
                    }
                    case "selectContractIdForUpdate" -> {
                        assertEquals(contractId, args[0]);
                        contractLocked.set(true);
                        yield contractId;
                    }
                    case "selectListByContractIdAndStatus" -> {
                        if (!contractLocked.get()) {
                            throw new AssertionError("额度校验前必须锁定合同");
                        }
                        yield new ArrayList<>(List.of(processing));
                    }
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "contractService", proxy(CrmContractService.class,
                (proxy, method, args) -> new CrmContractDO().setId(contractId).setTotalPrice(new BigDecimal("100"))));
        ReflectionTestUtils.setField(service, "bpmProcessInstanceApi", proxy(BpmProcessInstanceApi.class,
                (proxy, method, args) -> {
                    processCreated.set(true);
                    throw new AssertionError("额度不足时不应创建审批流程");
                }));

        assertServiceException(() -> service.submitReceivable(receivableId, 1L),
                RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT, new BigDecimal("40"));

        assertEquals(2, receivableReads.get(), "锁定合同后必须重新读取草稿状态");
        assertFalse(processCreated.get());
    }

    @Test
    void submitReceivableNeverReportsNegativeRemainingAmountForLegacyOverbooking() {
        long receivableId = 12L;
        long contractId = 22L;
        CrmReceivableDO draft = new CrmReceivableDO().setId(receivableId).setContractId(contractId)
                .setNo("HK-12").setPrice(BigDecimal.ONE)
                .setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        CrmReceivableDO approved = new CrmReceivableDO().setId(13L).setContractId(contractId)
                .setPrice(new BigDecimal("110")).setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());

        CrmReceivableServiceImpl service = new CrmReceivableServiceImpl();
        ReflectionTestUtils.setField(service, "receivableMapper", proxy(CrmReceivableMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById", "selectByIdForUpdate" -> draft;
                    case "selectContractIdForUpdate" -> contractId;
                    case "selectListByContractIdAndStatus" -> new ArrayList<>(List.of(approved));
                    default -> throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                }));
        ReflectionTestUtils.setField(service, "contractService", proxy(CrmContractService.class,
                (proxy, method, args) -> new CrmContractDO().setId(contractId).setTotalPrice(new BigDecimal("100"))));
        ReflectionTestUtils.setField(service, "bpmProcessInstanceApi", proxy(BpmProcessInstanceApi.class,
                (proxy, method, args) -> {
                    throw new AssertionError("额度不足时不应创建审批流程");
                }));

        assertServiceException(() -> service.submitReceivable(receivableId, 1L),
                RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT, BigDecimal.ZERO);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
