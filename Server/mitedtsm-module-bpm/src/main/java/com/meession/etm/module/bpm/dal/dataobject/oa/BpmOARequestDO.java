package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_oa_request")
@KeySequence("bpm_oa_request_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmOARequestDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String type;

    private String attachments;

    private Integer status;

    private String processInstanceId;

}