package com.meession.etm.module.bpm.dal.dataobject.oa;
import com.baomidou.mybatisplus.annotation.*; import com.meession.etm.framework.mybatis.core.dataobject.BaseDO; import lombok.*;
@TableName("bpm_oa_document") @KeySequence("bpm_oa_document_seq") @Data @EqualsAndHashCode(callSuper=true) @NoArgsConstructor @AllArgsConstructor
public class BpmOADocumentDO extends BaseDO { @TableId private Long id; private Long ownerUserId; private Long parentId; private String name; private String description; private Integer visibility; private Integer status; private Integer currentVersion; }
