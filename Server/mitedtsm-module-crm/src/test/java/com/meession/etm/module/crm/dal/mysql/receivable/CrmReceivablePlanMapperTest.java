package com.meession.etm.module.crm.dal.mysql.receivable;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmReceivablePlanMapperTest {

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "crm-plan-query-test");
        TableInfoHelper.initTableInfo(assistant, CrmReceivablePlanDO.class);
        TableInfoHelper.initTableInfo(assistant, CrmReceivableDO.class);
    }

    @Test
    void pendingReminderTreatsUnapprovedReceivableAsUnreceived() {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();

        CrmReceivablePlanMapper.appendPendingReminderCondition(query, LocalDateTime.of(2026, 7, 14, 0, 0));

        assertTrue(query.getSqlSegment().contains("audit_status"));
        assertTrue(query.getParamNameValuePairs().containsValue(CrmAuditStatusEnum.APPROVE.getStatus()));
        assertTrue(query.getSqlSegment().contains("remind_time"));
        assertFalse(query.getSqlSegment().contains("return_time"));
    }

    @Test
    void receivedFilterRequiresApprovedStatus() {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();

        CrmReceivablePlanMapper.appendReceivedCondition(query);

        assertTrue(query.getSqlSegment().contains("audit_status"));
        assertTrue(query.getParamNameValuePairs().containsValue(CrmAuditStatusEnum.APPROVE.getStatus()));
    }

}
