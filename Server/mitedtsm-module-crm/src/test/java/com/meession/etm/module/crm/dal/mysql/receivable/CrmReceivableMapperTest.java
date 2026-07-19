package com.meession.etm.module.crm.dal.mysql.receivable;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.receivable.vo.receivable.CrmReceivablePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.receivable.CrmReceivableReferenceStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmReceivableMapperTest {

    @BeforeAll
    static void initTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "crm-receivable-query-test"),
                CrmReceivableDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "crm-customer-query-test"),
                CrmCustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "crm-contract-query-test"),
                CrmContractDO.class);
    }

    @Test
    void referenceStatusFilterUsesValidCustomerAndContractJoins() {
        MPJLambdaWrapperX<CrmReceivableDO> valid = new MPJLambdaWrapperX<>();
        CrmReceivableMapper.appendReferenceStatusFilter(valid,
                CrmReceivableReferenceStatusEnum.VALID.getStatus());
        assertTrue(valid.getFrom().contains("crm_customer"));
        assertTrue(valid.getFrom().contains("crm_contract"));
        assertEquals(2, occurrences(valid.getSqlSegment(), "IS NOT NULL"));

        MPJLambdaWrapperX<CrmReceivableDO> bothMissing = new MPJLambdaWrapperX<>();
        CrmReceivableMapper.appendReferenceStatusFilter(bothMissing,
                CrmReceivableReferenceStatusEnum.BOTH_INVALID.getStatus());
        assertTrue(bothMissing.getFrom().contains("customer_id"));
        assertEquals(2, occurrences(bothMissing.getSqlSegment(), "IS NULL"));
    }

    @Test
    void referenceStatusResolutionDistinguishesMissingObjects() {
        assertEquals(CrmReceivableReferenceStatusEnum.VALID,
                CrmReceivableReferenceStatusEnum.resolve(true, true));
        assertEquals(CrmReceivableReferenceStatusEnum.CUSTOMER_MISSING,
                CrmReceivableReferenceStatusEnum.resolve(false, true));
        assertEquals(CrmReceivableReferenceStatusEnum.CONTRACT_INVALID,
                CrmReceivableReferenceStatusEnum.resolve(true, false));
        assertEquals(CrmReceivableReferenceStatusEnum.BOTH_INVALID,
                CrmReceivableReferenceStatusEnum.resolve(false, false));
    }

    @Test
    void permissionScopedPageKeepsCustomerFilter() {
        CrmReceivablePageReqVO reqVO = new CrmReceivablePageReqVO();
        reqVO.setCustomerId(88L);
        MPJLambdaWrapperX<CrmReceivableDO> query = new MPJLambdaWrapperX<>();

        CrmReceivableMapper.appendPageFilter(query, reqVO);

        assertTrue(query.getSqlSegment().contains("customer_id"));
        assertTrue(query.getParamNameValuePairs().containsValue(88L));
    }

    @Test
    void receivedAmountIncludesOnlyApprovedReceivables() {
        AtomicReference<QueryWrapper<CrmReceivableDO>> captured = new AtomicReference<>();
        CrmReceivableMapper mapper = (CrmReceivableMapper) Proxy.newProxyInstance(
                CrmReceivableMapper.class.getClassLoader(), new Class<?>[]{CrmReceivableMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectMaps")) {
                        @SuppressWarnings("unchecked")
                        QueryWrapper<CrmReceivableDO> query = (QueryWrapper<CrmReceivableDO>) args[0];
                        captured.set(query);
                        return Collections.<Map<String, Object>>emptyList();
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                });

        mapper.selectReceivablePriceMapByContractId(List.of(99L));

        assertTrue(captured.get().getSqlSegment().contains("audit_status ="));
        assertTrue(captured.get().getParamNameValuePairs().containsValue(CrmAuditStatusEnum.APPROVE.getStatus()));
        assertFalse(captured.get().getParamNameValuePairs().containsValue(CrmAuditStatusEnum.DRAFT.getStatus()));
        assertFalse(captured.get().getParamNameValuePairs().containsValue(CrmAuditStatusEnum.PROCESS.getStatus()));
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }

}
