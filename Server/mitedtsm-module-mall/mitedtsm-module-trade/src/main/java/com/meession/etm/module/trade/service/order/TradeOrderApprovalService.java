package com.meession.etm.module.trade.service.order;

/**
 * 订单审批服务接口
 */
public interface TradeOrderApprovalService {

    void submitForApproval(Long userId, Long orderId);

    void cancelApproval(Long orderId);

    void updateApprovalStatus(String businessKey, Integer approvalStatus, String comment);

}