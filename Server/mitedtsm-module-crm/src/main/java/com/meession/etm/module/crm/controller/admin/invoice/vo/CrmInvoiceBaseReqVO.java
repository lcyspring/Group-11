package com.meession.etm.module.crm.controller.admin.invoice.vo;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 发票草稿可维护字段。合同、客户和归属信息不在此处暴露，防止生命周期外篡改。 */
@Data
public class CrmInvoiceBaseReqVO {

    @NotNull(message = "经手人不能为空")
    private Long handlerUserId;

    @NotNull(message = "发票类型不能为空")
    @InEnum(CrmInvoiceTypeEnum.class)
    private Integer type;

    @NotNull(message = "开票金额不能为空")
    @DecimalMin(value = "0.01", message = "开票金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "购方抬头不能为空")
    @Size(max = 200, message = "购方抬头不能超过 200 个字符")
    private String title;

    @Size(max = 64, message = "税号不能超过 64 个字符")
    private String taxNo;
    @Size(max = 255, message = "注册地址不能超过 255 个字符")
    private String registeredAddress;
    @Size(max = 32, message = "注册电话不能超过 32 个字符")
    private String registeredPhone;
    @Size(max = 128, message = "开户行不能超过 128 个字符")
    private String bankName;
    @Size(max = 64, message = "银行账号不能超过 64 个字符")
    private String bankAccount;
    @Email(message = "接收邮箱格式不正确")
    @Size(max = 128, message = "接收邮箱不能超过 128 个字符")
    private String email;

    @NotBlank(message = "开票内容不能为空")
    @Size(max = 500, message = "开票内容不能超过 500 个字符")
    private String content;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
