package com.meession.etm.module.crm.controller.admin.workreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmWorkReportRespVO {
    private Long id;
    private Long authorUserId;
    private String authorUserName;
    private Integer reportType;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate reportDate;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate periodStart;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate periodEnd;
    private String title;
    private String completedContent;
    private String pendingContent;
    private String nextPlan;
    private String issues;
    private List<Long> receiverUserIds;
    private List<String> receiverUserNames;
    private List<String> attachmentUrls;
    private Integer status;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
}
