package com.meession.etm.module.bpm.dal.dataobject.oa;
import com.baomidou.mybatisplus.annotation.*; import com.meession.etm.framework.mybatis.core.dataobject.BaseDO; import lombok.*;
@TableName("bpm_oa_document_member") @KeySequence("bpm_oa_document_member_seq") @Data @EqualsAndHashCode(callSuper=true) @NoArgsConstructor @AllArgsConstructor
public class BpmOADocumentMemberDO extends BaseDO { @TableId private Long id; private Long documentId; private Long userId; private Integer permission; }
