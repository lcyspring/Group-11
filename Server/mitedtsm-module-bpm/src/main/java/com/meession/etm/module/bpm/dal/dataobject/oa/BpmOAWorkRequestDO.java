package com.meession.etm.module.bpm.dal.dataobject.oa;
import com.baomidou.mybatisplus.annotation.*; import com.meession.etm.framework.mybatis.core.dataobject.BaseDO; import lombok.*; import java.time.LocalDateTime;
@TableName("bpm_oa_work_request") @KeySequence("bpm_oa_work_request_seq") @Data @EqualsAndHashCode(callSuper=true) @NoArgsConstructor @AllArgsConstructor
public class BpmOAWorkRequestDO extends BaseDO { @TableId private Long id; private Long userId; private String title; private String content; private Integer urgency; private Integer status; private String processInstanceId; private LocalDateTime approvedTime; }
