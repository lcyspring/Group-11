package com.meession.etm.module.crm.dal.mysql.workorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmWorkOrderMapperTest {

    @BeforeAll
    static void initTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "work-order-query-test"),
                CrmWorkOrderDO.class);
    }

    @Test
    void explicitCreatedSceneStillAppliesForQueryAllUser() {
        LambdaQueryWrapper<CrmWorkOrderDO> query = captureQuery(
                new CrmWorkOrderPageReqVO().setSceneType(1), 9L, true);

        assertTrue(query.getSqlSegment().contains("creator"));
        assertTrue(query.getParamNameValuePairs().containsValue("9"));
    }

    @Test
    void explicitHandledSceneStillAppliesForQueryAllUser() {
        LambdaQueryWrapper<CrmWorkOrderDO> query = captureQuery(
                new CrmWorkOrderPageReqVO().setSceneType(2), 9L, true);

        assertTrue(query.getSqlSegment().contains("handler_user_id"));
        assertTrue(query.getParamNameValuePairs().containsValue(9L));
    }

    @Test
    void queryAllWithoutSceneDoesNotForcePersonalScope() {
        LambdaQueryWrapper<CrmWorkOrderDO> query = captureQuery(new CrmWorkOrderPageReqVO(), 9L, true);

        assertFalse(query.getSqlSegment().contains("creator"));
        assertFalse(query.getSqlSegment().contains("handler_user_id"));
    }

    @Test
    void copiedSceneUsesExplicitOrderIds() {
        LambdaQueryWrapper<CrmWorkOrderDO> query = captureQuery(
                new CrmWorkOrderPageReqVO().setSceneType(3), 9L, false, Set.of(), Set.of(), Set.of(81L));
        assertTrue(query.getSqlSegment().contains("id"));
        assertTrue(query.getParamNameValuePairs().containsValue(81L));
    }

    @Test
    void selectedExportIdsRemainInsideAuthorizedQuery() {
        LambdaQueryWrapper<CrmWorkOrderDO> query = captureQuery(
                new CrmWorkOrderPageReqVO().setIds(Set.of(81L, 82L)), 9L, false,
                Set.of(7L), Set.of(), Set.of());
        String queryDetails = query.getSqlSegment() + " params=" + query.getParamNameValuePairs();
        assertTrue(query.getParamNameValuePairs().containsValue(81L), queryDetails);
        assertTrue(query.getParamNameValuePairs().containsValue(82L), queryDetails);
        assertTrue(query.getParamNameValuePairs().containsValue(7L), queryDetails);
    }

    @Test
    void personalScopeIncludesManagedAndUnassignedMemberGroups() {
        LambdaQueryWrapper<CrmWorkOrderDO> query = captureQuery(new CrmWorkOrderPageReqVO(), 9L, false,
                Set.of(7L), Set.of(8L), Set.of(81L));
        String queryDetails = query.getSqlSegment() + " params=" + query.getParamNameValuePairs();
        assertTrue(query.getParamNameValuePairs().containsValue(7L), queryDetails);
        assertTrue(query.getParamNameValuePairs().containsValue(8L), queryDetails);
        assertTrue(query.getParamNameValuePairs().containsValue(81L), queryDetails);
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<CrmWorkOrderDO> captureQuery(CrmWorkOrderPageReqVO reqVO,
                                                                   Long userId, boolean queryAll) {
        return captureQuery(reqVO, userId, queryAll, Set.of(), Set.of(), Set.of());
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<CrmWorkOrderDO> captureQuery(CrmWorkOrderPageReqVO reqVO,
                                                                   Long userId, boolean queryAll,
                                                                   Set<Long> managedGroups, Set<Long> memberGroups,
                                                                   Set<Long> ccOrders) {
        AtomicReference<LambdaQueryWrapper<CrmWorkOrderDO>> captured = new AtomicReference<>();
        CrmWorkOrderMapper mapper = (CrmWorkOrderMapper) Proxy.newProxyInstance(
                CrmWorkOrderMapper.class.getClassLoader(), new Class<?>[]{CrmWorkOrderMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectPage") && args.length == 2 && args[0] instanceof IPage<?>) {
                        captured.set((LambdaQueryWrapper<CrmWorkOrderDO>) args[1]);
                        return args[0];
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                });

        mapper.selectPage(reqVO, userId, queryAll, managedGroups, memberGroups, ccOrders);
        return captured.get();
    }
}
