package com.meession.etm.module.trade.service.contract;

import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;

import java.util.List;

/**
 * 订单合同集成服务接口
 */
public interface TradeOrderContractService {

    Long createContractForOrder(Long orderId, String contractName, String attachmentUrls);

    void bindContractToOrder(Long orderId, Long contractId);

    TradeContractDO getContractByOrderId(Long orderId);

    List<TradeContractDO> getContractsByOrderIds(List<Long> orderIds);

    void updateContractStatus(Long contractId, Integer status);

    void signContract(Long contractId, String signedBy);

}