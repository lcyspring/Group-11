package com.meession.etm.module.crm.controller.admin.invoice.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ExcelIgnoreUnannotated
public class CrmInvoiceRespVO {

    @ExcelProperty("编号")
    private Long id;
    @ExcelProperty("申请号")
    private String no;
    private Long contractId;
    @ExcelProperty("合同编号")
    private String contractNo;
    @ExcelProperty("合同名称")
    private String contractName;
    private Long customerId;
    @ExcelProperty("客户名称")
    private String customerName;
    private Long ownerUserId;
    @ExcelProperty("负责人")
    private String ownerUserName;
    private Long handlerUserId;
    @ExcelProperty("经手人")
    private String handlerUserName;
    @ExcelProperty("方向")
    private Integer direction;
    private Long originalInvoiceId;
    private String originalInvoiceNo;
    @ExcelProperty("状态")
    private Integer status;
    @ExcelProperty("票据类型")
    private Integer type;
    @ExcelProperty("开票金额")
    private BigDecimal amount;
    @ExcelProperty("已红冲金额")
    private BigDecimal redAmount;
    @ExcelProperty("税务发票号码")
    private String invoiceNo;
    @ExcelProperty("开票日期")
    private LocalDateTime invoiceDate;
    @ExcelProperty("购方抬头")
    private String title;
    @ExcelProperty("购方税号")
    private String taxNo;
    private String registeredAddress;
    private String registeredPhone;
    private String bankName;
    private String bankAccount;
    private String email;
    @ExcelProperty("开票内容")
    private String content;
    private String externalProvider;
    private String externalRequestId;
    private String externalInvoiceId;
    private String issueRemark;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ActionRecord> actionRecords;

    @Data
    public static class ActionRecord {
        private Long id;
        private Integer actionType;
        private Integer fromStatus;
        private Integer toStatus;
        private Long operatorUserId;
        private String operatorUserName;
        private LocalDateTime actionTime;
        private String providerRequestId;
        private String remark;
    }
}
