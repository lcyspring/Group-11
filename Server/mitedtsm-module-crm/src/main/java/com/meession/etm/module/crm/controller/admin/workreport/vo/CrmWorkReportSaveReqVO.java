package com.meession.etm.module.crm.controller.admin.workreport.vo;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CrmWorkReportSaveReqVO {
    private Long id;
    @NotNull private Integer reportType;
    @NotNull private LocalDate reportDate;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank @Size(max = 5000) private String completedContent;
    @Size(max = 5000) private String pendingContent;
    @NotBlank @Size(max = 5000) private String nextPlan;
    @Size(max = 3000) private String issues;
    @NotEmpty private List<Long> receiverUserIds;
    private List<String> attachmentUrls;
}
