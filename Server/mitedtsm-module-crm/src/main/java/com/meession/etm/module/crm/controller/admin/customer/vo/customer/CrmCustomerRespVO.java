package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.meession.etm.framework.excel.core.annotations.DictFormat;
import com.meession.etm.framework.excel.core.convert.DictConvert;
import com.meession.etm.module.infra.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Schema(description = "管理后台 - CRM 客户 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmCustomerRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty("客户名称")
    private String name;

    @Schema(description = "上级客户编号", example = "1024")
    @ExcelProperty("上级客户编号")
    private Long parentCustomerId;

    @Schema(description = "上级客户名称", example = "集团总部")
    @ExcelProperty("上级客户名称")
    private String parentCustomerName;

    @Schema(description = "首联系人姓名", example = "张三")
    @ExcelProperty("首联系人")
    private String primaryContactName;

    @Schema(description = "首联系人手机", example = "18000000000")
    @ExcelProperty("首联系人手机")
    private String primaryContactMobile;

    @Schema(description = "跟进状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty(value = "跟进状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.BOOLEAN_STRING)
    private Boolean followUpStatus;

    @Schema(description = "最后跟进时间")
    @ExcelProperty("最后跟进时间")
    private LocalDateTime contactLastTime;

    @Schema(description = "最后跟进内容", example = "吃饭、睡觉、打逗逗")
    @ExcelProperty("最后跟进内容")
    private String contactLastContent;

    @Schema(description = "下次联系时间")
    @ExcelProperty("下次联系时间")
    private LocalDateTime contactNextTime;

    @Schema(description = "负责人的用户编号", example = "25682")
    @ExcelProperty("负责人的用户编号")
    private Long ownerUserId;
    @Schema(description = "负责人名字", example = "25682")
    @ExcelProperty("负责人名字")
    private String ownerUserName;
    @Schema(description = "负责人部门")
    @ExcelProperty("负责人部门")
    private String ownerUserDeptName;

    @Schema(description = "池状态：0 在管、1 公海、2 垃圾池")
    private Integer poolStatus;
    @Schema(description = "本次进入公海时间")
    private LocalDateTime poolEntryTime;
    @Schema(description = "本次入池前负责人编号")
    private Long poolPreviousOwnerUserId;
    @Schema(description = "本次入池前负责人名称")
    private String poolPreviousOwnerUserName;
    @Schema(description = "本次入池原因编码")
    private String poolReason;
    @Schema(description = "累计进入公海次数")
    private Integer poolCycleCount;
    @Schema(description = "进入垃圾池时间")
    private LocalDateTime garbageTime;
    @Schema(description = "垃圾池原因")
    private String garbageReason;

    @Schema(description = "锁定状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty(value = "锁定状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.BOOLEAN_STRING)
    private Boolean lockStatus;

    @Schema(description = "成交状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty(value = "成交状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.BOOLEAN_STRING)
    private Boolean dealStatus;

    @Schema(description = "客户生命周期状态：10 潜在、20 意向、30 成交、40 流失", example = "20")
    @ExcelProperty("客户生命周期状态")
    private Integer lifecycleStatus;

    @Schema(description = "生命周期状态最后变更时间")
    @ExcelProperty("生命周期状态变更时间")
    private LocalDateTime lifecycleStatusChangeTime;

    @Schema(description = "流失原因")
    @ExcelProperty("流失原因")
    private String lifecycleLostReason;

    @Schema(description = "手机", example = "25682")
    @ExcelProperty("手机")
    private String mobile;

    @Schema(description = "电话", example = "25682")
    @ExcelProperty("电话")
    private String telephone;

    @Schema(description = "QQ", example = "25682")
    @ExcelProperty("QQ")
    private String qq;

    @Schema(description = "wechat", example = "25682")
    @ExcelProperty("wechat")
    private String wechat;

    @Schema(description = "email", example = "25682")
    @ExcelProperty("email")
    private String email;

    @Schema(description = "客户自身生日")
    @ExcelProperty("生日")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Schema(description = "地区编号", example = "1024")
    @ExcelProperty("地区编号")
    private Integer areaId;
    @Schema(description = "地区名称", example = "北京市")
    @ExcelProperty("地区名称")
    private String areaName;
    @Schema(description = "详细地址", example = "北京市成华大道")
    @ExcelProperty("详细地址")
    private String detailAddress;

    @Schema(description = "所属行业", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty(value = "所属行业", converter = DictConvert.class)
    @DictFormat(com.meession.etm.module.crm.enums.DictTypeConstants.CRM_CUSTOMER_INDUSTRY)
    private Integer industryId;

    @Schema(description = "客户等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty(value = "客户等级", converter = DictConvert.class)
    @DictFormat(com.meession.etm.module.crm.enums.DictTypeConstants.CRM_CUSTOMER_LEVEL)
    private Integer level;

    @Schema(description = "客户来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "13563")
    @ExcelProperty(value = "客户来源", converter = DictConvert.class)
    @DictFormat(com.meession.etm.module.crm.enums.DictTypeConstants.CRM_CUSTOMER_SOURCE)
    private Integer source;

    @Schema(description = "负责人的用户编号", example = "25682")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人", example = "1024")
    @ExcelProperty("创建人")
    private String creator;
    @Schema(description = "创建人名字", example = "密讯")
    @ExcelProperty("创建人名字")
    private String creatorName;

    @Schema(description = "距离加入公海时间", example = "1")
    private Long poolDay;

}
