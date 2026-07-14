package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户查重 Request VO")
@Data
public class CrmCustomerDuplicateCheckReqVO {

    @Schema(description = "客户名称", example = "赵六")
    private String name;

    @Schema(description = "手机", example = "18000000000")
    private String mobile;

    @Schema(description = "电话", example = "010-12345678")
    private String telephone;

    @Schema(description = "邮箱", example = "test@example.com")
    private String email;

    @Schema(description = "QQ", example = "123456789")
    private String qq;

    @Schema(description = "微信", example = "wechat_id")
    private String wechat;

    @Schema(description = "是否严格匹配（精确匹配）", example = "false")
    private Boolean strictMatch;

}
