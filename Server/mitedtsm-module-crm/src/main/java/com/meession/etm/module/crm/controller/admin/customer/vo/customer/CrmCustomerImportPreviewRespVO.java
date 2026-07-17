package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 客户导入预检 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerImportPreviewRespVO {

    private Long id;
    private String fileName;
    private Integer status;
    private LocalDateTime expiresAt;
    private Integer totalCount;
    private Integer createCount;
    private Integer updateCount;
    private Integer failureCount;
    private Map<String, String> fieldMapping;
    private List<String> headers;
    private List<Field> fields;
    private List<Row> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Field {
        private String key;
        private String label;
        private Boolean required;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private Integer rowNumber;
        private String action;
        private Long existingCustomerId;
        private List<String> errors;
        private CrmCustomerImportExcelVO customer;
    }
}
