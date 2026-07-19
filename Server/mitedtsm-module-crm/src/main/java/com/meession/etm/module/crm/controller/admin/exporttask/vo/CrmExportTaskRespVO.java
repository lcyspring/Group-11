package com.meession.etm.module.crm.controller.admin.exporttask.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmExportTaskRespVO {
    private Long id;
    private String objectType;
    private Integer status;
    private Integer totalCount;
    private String fileName;
    private String failureReason;
    private Boolean downloadAvailable;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime downloadedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;
}
