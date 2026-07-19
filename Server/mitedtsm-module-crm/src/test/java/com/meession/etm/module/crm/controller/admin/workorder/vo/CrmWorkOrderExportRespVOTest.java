package com.meession.etm.module.crm.controller.admin.workorder.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmWorkOrderExportRespVOTest {

    @Test
    void convertsCodesAndCollaboratorsToReadableArchiveValues() {
        CrmWorkOrderRespVO source = new CrmWorkOrderRespVO();
        source.setNo("WO-202607-001");
        source.setTitle("设备故障处理");
        source.setType(1);
        source.setPriority(3);
        source.setStatus(20);
        source.setSourceType(2);
        source.setCcUserNames(List.of("张三", "李四"));

        CrmWorkOrderExportRespVO result = CrmWorkOrderExportRespVO.from(source);

        assertEquals("问题", result.getTypeName());
        assertEquals("高", result.getPriorityName());
        assertEquals("处理中", result.getStatusName());
        assertEquals("合同", result.getSourceTypeName());
        assertEquals("张三、李四", result.getCcUserNames());
    }
}
