package com.meession.etm.module.bpm.dal.dataobject.oa;
import com.baomidou.mybatisplus.annotation.*; import com.meession.etm.framework.mybatis.core.dataobject.BaseDO; import lombok.*;
@TableName("bpm_oa_document_version") @KeySequence("bpm_oa_document_version_seq") @Data @EqualsAndHashCode(callSuper=true) @NoArgsConstructor @AllArgsConstructor
public class BpmOADocumentVersionDO extends BaseDO { @TableId private Long id; private Long documentId; private Integer version; private Long fileId; private String changeSummary; private Long createdBy; }
