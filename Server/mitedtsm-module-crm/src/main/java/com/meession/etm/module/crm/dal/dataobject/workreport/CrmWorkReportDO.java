package com.meession.etm.module.crm.dal.dataobject.workreport;

import com.baomidou.mybatisplus.annotation.*;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.framework.mybatis.core.type.LongListTypeHandler;
import com.meession.etm.framework.mybatis.core.type.StringListTypeHandler;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@TableName(value = "crm_work_report", autoResultMap = true)
@KeySequence("crm_work_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmWorkReportDO extends BaseDO {
    @TableId private Long id;
    private Long authorUserId;
    private Integer reportType;
    private LocalDate reportDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String title;
    private String completedContent;
    private String pendingContent;
    private String nextPlan;
    private String issues;
    @TableField(typeHandler = LongListTypeHandler.class) private List<Long> receiverUserIds;
    @TableField(typeHandler = StringListTypeHandler.class) private List<String> attachmentUrls;
    private Integer status;
    private LocalDateTime submitTime;
}
