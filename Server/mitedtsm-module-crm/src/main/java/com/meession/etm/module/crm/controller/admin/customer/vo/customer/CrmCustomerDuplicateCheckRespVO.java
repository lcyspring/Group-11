package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - CRM 客户查重 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerDuplicateCheckRespVO {

    @Schema(description = "是否存在重复客户", example = "true")
    private Boolean hasDuplicate;

    @Schema(description = "重复客户列表")
    private List<DuplicateCustomer> duplicates;

    @Schema(description = "重复客户")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateCustomer {

        @Schema(description = "客户编号", example = "1024")
        private Long id;

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

        @Schema(description = "匹配度（0-100）", example = "80")
        private Integer matchScore;

        @Schema(description = "匹配字段列表", example = "[\"name\", \"mobile\"]")
        private List<String> matchedFields;

    }

}
