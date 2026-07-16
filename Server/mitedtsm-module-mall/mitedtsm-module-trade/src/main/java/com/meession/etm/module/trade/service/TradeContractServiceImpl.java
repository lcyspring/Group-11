package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractPageReqVO;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.contract.TradeContractMapper;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.TradeContractStatusEnum;
import com.meession.etm.module.trade.service.order.TradeOrderNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

@Service
public class TradeContractServiceImpl implements TradeContractService {

    @Resource
    private TradeContractMapper tradeContractMapper;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeOrderNoGenerator tradeOrderNoGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(TradeContractSaveReqVO createReqVO) {
        TradeContractDO contract = BeanUtils.toBean(createReqVO, TradeContractDO.class);
        contract.setNo("CT" + tradeOrderNoGenerator.generateOrderNo());

        if (contract.getStatus() == null) {
            contract.setStatus(TradeContractStatusEnum.DRAFT.getStatus());
        }

        if (contract.getOrderId() != null) {
            TradeOrderDO order = tradeOrderMapper.selectById(contract.getOrderId());
            if (order != null) {
                contract.setOrderNo(order.getNo());
            }
        }

        tradeContractMapper.insert(contract);
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(TradeContractSaveReqVO updateReqVO) {
        validateContractExists(updateReqVO.getId());

        TradeContractDO updateObj = BeanUtils.toBean(updateReqVO, TradeContractDO.class);
        tradeContractMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        validateContractExists(id);
        tradeContractMapper.deleteById(id);
    }

    @Override
    public PageResult<TradeContractDO> getContractPage(TradeContractPageReqVO reqVO) {
        return tradeContractMapper.selectPage(reqVO);
    }

    @Override
    public TradeContractDO getContract(Long id) {
        return tradeContractMapper.selectById(id);
    }

    @Override
    public List<TradeContractDO> getContractsByOrderId(Long orderId) {
        return tradeContractMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<TradeContractDO> getContractsByCustomerId(Long customerId) {
        return tradeContractMapper.selectListByCustomerId(customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractStatus(Long id, Integer status) {
        validateContractExists(id);
        TradeContractDO updateObj = new TradeContractDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        tradeContractMapper.updateById(updateObj);
    }

    private void validateContractExists(Long id) {
        if (id == null) {
            return;
        }
        TradeContractDO contract = tradeContractMapper.selectById(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
    }

}