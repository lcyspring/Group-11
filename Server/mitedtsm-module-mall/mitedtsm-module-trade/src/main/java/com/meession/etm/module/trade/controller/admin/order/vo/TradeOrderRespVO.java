package com.meession.etm.module.trade.controller.admin.order.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TradeOrderRespVO {

    private Long id;

    private String no;

    private Integer type;

    private String typeName;

    private Integer terminal;

    private Long userId;

    private String userIp;

    private String userRemark;

    private Integer status;

    private String statusName;

    private Integer productCount;

    private Integer cancelType;

    private String remark;

    private Boolean commentStatus;

    private Long brokerageUserId;

    private Boolean payStatus;

    private LocalDateTime payTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private Integer totalPrice;

    private Integer orderPrice;

    private Integer discountPrice;

    private Integer deliveryPrice;

    private Integer adjustPrice;

    private Integer payPrice;

    private Integer deliveryType;

    private Long payOrderId;

    private String payChannelCode;

    private Long deliveryTemplateId;

    private Long logisticsId;

    private String logisticsNo;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private String receiverName;

    private String receiverMobile;

    private Integer receiverAreaId;

    private Integer receiverPostCode;

    private String receiverDetailAddress;

    private Long pickUpStoreId;

    private String pickUpVerifyCode;

    private Integer refundStatus;

    private Integer refundPrice;

    private Integer afterSaleStatus;

    private Long couponId;

    private Integer couponPrice;

    private Integer usePoint;

    private Integer pointPrice;

    private Integer givePoint;

    private Integer refundPoint;

    private Integer vipPrice;

    private Long seckillActivityId;

    private Long bargainActivityId;

    private Long bargainRecordId;

    private Long combinationActivityId;

    private Long combinationHeadId;

    private Long combinationRecordId;

    private List<TradeOrderItemRespVO> items;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}