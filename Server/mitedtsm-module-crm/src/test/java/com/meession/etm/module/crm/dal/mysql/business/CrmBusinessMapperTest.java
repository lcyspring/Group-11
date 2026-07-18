package com.meession.etm.module.crm.dal.mysql.business;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessStagePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmBusinessMapperTest {

    @BeforeAll
    static void initTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "crm-business-query-test"),
                CrmBusinessDO.class);
    }

    @Test
    void stageDetailsSelectOnlyTheRequestedSiblingStatus() {
        AtomicReference<LambdaQueryWrapperX<CrmBusinessDO>> captured = new AtomicReference<>();
        CrmBusinessMapper mapper = (CrmBusinessMapper) Proxy.newProxyInstance(
                CrmBusinessMapper.class.getClassLoader(), new Class<?>[]{CrmBusinessMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectPage") && args.length == 2) {
                        @SuppressWarnings("unchecked")
                        LambdaQueryWrapperX<CrmBusinessDO> query =
                                (LambdaQueryWrapperX<CrmBusinessDO>) args[1];
                        captured.set(query);
                        return PageResult.empty();
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new AssertionError("未预期的 Mapper 调用 " + method.getName());
                });
        CrmStatisticsBusinessStagePageReqVO request = new CrmStatisticsBusinessStagePageReqVO();
        request.setStatusTypeId(20L);
        request.setStatusId(30L);
        request.setUserIds(List.of(10L));
        request.setTimes(new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59, 59)
        });

        mapper.selectStagePage(request);

        String sql = captured.get().getSqlSegment();
        assertTrue(sql.contains("status_type_id ="));
        assertTrue(sql.contains("status_id ="));
        assertTrue(captured.get().getParamNameValuePairs().containsValue(20L));
        assertTrue(captured.get().getParamNameValuePairs().containsValue(30L));
        assertFalse(sql.contains(">="), "状态明细不得按排序范围包含其它同级状态");
    }
}
