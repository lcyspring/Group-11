package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractPageReqVO;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;

import java.util.List;

public interface TradeContractService {

    Long createContract(TradeContractSaveReqVO createReqVO);

    void updateContract(TradeContractSaveReqVO updateReqVO);

    void deleteContract(Long id);

    PageResult<TradeContractDO> getContractPage(TradeContractPageReqVO reqVO);

    TradeContractDO getContract(Long id);

    List<TradeContractDO> getContractsByOrderId(Long orderId);

    List<TradeContractDO> getContractsByCustomerId(Long customerId);

    void updateContractStatus(Long id, Integer status);

}