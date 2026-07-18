package com.meession.etm.module.crm.controller.admin.exporttask.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CrmExportDownloadTokenRespVO {
    private String token;
    private LocalDateTime expiresAt;
}
