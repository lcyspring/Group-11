package com.meession.etm.module.crm.controller.admin.workorder.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderPriorityEnum;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderSourceTypeEnum;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderStatusEnum;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;

/** 工单归档导出模型，避免把前端使用的状态编码直接暴露给离线报表用户。 */
@Data
@ExcelIgnoreUnannotated
public class CrmWorkOrderExportRespVO {

    @ExcelProperty("工单编号")
    private String no;
    @ExcelProperty("工单标题")
    private String title;
    @ExcelProperty("客户")
    private String customerName;
    @ExcelProperty("类型")
    private String typeName;
    @ExcelProperty("优先级")
    private String priorityName;
    @ExcelProperty("状态")
    private String statusName;
    @ExcelProperty("来源类型")
    private String sourceTypeName;
    @ExcelProperty("来源编号")
    private Long sourceId;
    @ExcelProperty("处理组")
    private String groupName;
    @ExcelProperty("处理人")
    private String handlerUserName;
    @ExcelProperty("创建人")
    private String creatorName;
    @ExcelProperty("抄送人")
    private String ccUserNames;
    @ExcelProperty("工单描述")
    private String description;
    @ExcelProperty("解决方案")
    private String solution;
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    @ExcelProperty("开始处理时间")
    private LocalDateTime processTime;
    @ExcelProperty("完结时间")
    private LocalDateTime completeTime;

    public static CrmWorkOrderExportRespVO from(CrmWorkOrderRespVO source) {
        return new CrmWorkOrderExportRespVO()
                .setNo(source.getNo()).setTitle(source.getTitle()).setCustomerName(source.getCustomerName())
                .setTypeName(typeName(source.getType())).setPriorityName(priorityName(source.getPriority()))
                .setStatusName(statusName(source.getStatus())).setSourceTypeName(sourceTypeName(source.getSourceType()))
                .setSourceId(source.getSourceId()).setGroupName(source.getGroupName())
                .setHandlerUserName(source.getHandlerUserName()).setCreatorName(source.getCreatorName())
                .setCcUserNames(source.getCcUserNames() == null ? null : String.join("、", source.getCcUserNames()))
                .setDescription(source.getDescription()).setSolution(source.getSolution())
                .setCreateTime(source.getCreateTime()).setProcessTime(source.getProcessTime())
                .setCompleteTime(source.getCompleteTime());
    }

    private static String typeName(Integer value) {
        return Arrays.stream(CrmWorkOrderTypeEnum.values()).filter(item -> item.getType().equals(value))
                .map(CrmWorkOrderTypeEnum::getName).findFirst().orElse(String.valueOf(value));
    }

    private static String priorityName(Integer value) {
        return Arrays.stream(CrmWorkOrderPriorityEnum.values()).filter(item -> item.getPriority().equals(value))
                .map(CrmWorkOrderPriorityEnum::getName).findFirst().orElse(String.valueOf(value));
    }

    private static String statusName(Integer value) {
        return Arrays.stream(CrmWorkOrderStatusEnum.values()).filter(item -> item.getStatus().equals(value))
                .map(CrmWorkOrderStatusEnum::getName).findFirst().orElse(String.valueOf(value));
    }

    private static String sourceTypeName(Integer value) {
        return Arrays.stream(CrmWorkOrderSourceTypeEnum.values()).filter(item -> item.getType().equals(value))
                .map(CrmWorkOrderSourceTypeEnum::getName).findFirst().orElse(String.valueOf(value));
    }
}
