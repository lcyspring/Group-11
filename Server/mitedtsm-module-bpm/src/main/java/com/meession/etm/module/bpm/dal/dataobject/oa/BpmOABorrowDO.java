package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("bpm_oa_borrow")
@KeySequence("bpm_oa_borrow_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmOABorrowDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    private String bankAccount;

    private String bankName;

    private LocalDateTime expectRepayDate;

    private Integer status;

    private String processInstanceId;

}