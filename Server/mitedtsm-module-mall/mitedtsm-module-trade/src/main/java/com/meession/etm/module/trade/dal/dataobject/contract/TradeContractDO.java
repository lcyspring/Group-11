package com.meession.etm.module.trade.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("trade_contract")
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeContractDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String no;

    private String name;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private BigDecimal amount;

    private String contractType;

    private Integer status;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String signer;

    private String signerPhone;

    private LocalDateTime signDate;

    private String attachmentUrls;

    private String remark;

}