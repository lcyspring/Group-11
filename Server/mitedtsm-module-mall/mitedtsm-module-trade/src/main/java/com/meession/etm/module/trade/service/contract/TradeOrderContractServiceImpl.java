package com.meession.etm.module.trade.service.contract;

import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.contract.TradeContractMapper;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.ErrorCodeConstants;
import com.meession.etm.module.trade.enums.TradeContractStatusEnum;
import com.meession.etm.module.trade.enums.order.TradeOrderStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
@Service
@Validated
public class TradeOrderContractServiceImpl implements TradeOrderContractService {

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeContractMapper tradeContractMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContractForOrder(Long orderId, String contractName, String attachmentUrls) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        if (!TradeOrderStatusEnum.PAID.getStatus().equals(order.getStatus())) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        TradeContractDO contract = new TradeContractDO();
        contract.setNo("C" + System.currentTimeMillis());
        contract.setName(contractName);
        contract.setOrderId(orderId);
        contract.setCustomerId(order.getUserId());
        contract.setAmount(BigDecimal.valueOf(order.getPayPrice()));
        contract.setStatus(TradeContractStatusEnum.DRAFT.getStatus());
        contract.setAttachmentUrls(attachmentUrls);
        contract.setSignDate(null);
        contract.setStartDate(null);
        contract.setEndDate(null);
        tradeContractMapper.insert(contract);

        log.info("为订单创建合同成功: orderId={}, contractId={}", orderId, contract.getId());
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindContractToOrder(Long orderId, Long contractId) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        TradeContractDO contract = tradeContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(ErrorCodeConstants.CONTRACT_NOT_EXISTS);
        }

        contract.setOrderId(orderId);
        tradeContractMapper.updateById(contract);

        log.info("合同绑定订单成功: orderId={}, contractId={}", orderId, contractId);
    }

    @Override
    public TradeContractDO getContractByOrderId(Long orderId) {
        List<TradeContractDO> contracts = tradeContractMapper.selectListByOrderId(orderId);
        return contracts.isEmpty() ? null : contracts.get(0);
    }

    @Override
    public List<TradeContractDO> getContractsByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<TradeContractDO> result = new ArrayList<>();
        for (Long orderId : orderIds) {
            List<TradeContractDO> contracts = tradeContractMapper.selectListByOrderId(orderId);
            result.addAll(contracts);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractStatus(Long contractId, Integer status) {
        TradeContractDO contract = tradeContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(ErrorCodeConstants.CONTRACT_NOT_EXISTS);
        }

        contract.setStatus(status);
        tradeContractMapper.updateById(contract);

        log.info("合同状态更新成功: contractId={}, status={}", contractId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signContract(Long contractId, String signedBy) {
        TradeContractDO contract = tradeContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(ErrorCodeConstants.CONTRACT_NOT_EXISTS);
        }

        contract.setStatus(TradeContractStatusEnum.SIGNED.getStatus());
        contract.setSignDate(LocalDateTime.now());
        contract.setStartDate(LocalDateTime.now());
        tradeContractMapper.updateById(contract);

        log.info("合同签署成功: contractId={}, signedBy={}", contractId, signedBy);
    }

}