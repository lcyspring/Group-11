package com.meession.etm.module.crm.dal.dataobject.exporttask;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_export_task")
@KeySequence("crm_export_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmExportTaskDO extends BaseDO {
    @TableId private Long id;
    private String objectType;
    private Long creatorUserId;
    private String filterSnapshot;
    private String objectIdsSnapshot;
    private Integer status;
    private Integer totalCount;
    private String fileUrl;
    private String fileName;
    private String contentType;
    private String failureReason;
    private String downloadTokenHash;
    private LocalDateTime downloadTokenExpiresAt;
    private LocalDateTime downloadedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime expiresAt;
}
