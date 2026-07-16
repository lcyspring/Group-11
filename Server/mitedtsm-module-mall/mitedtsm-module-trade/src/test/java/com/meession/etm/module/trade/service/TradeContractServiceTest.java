package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractPageReqVO;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import com.meession.etm.module.trade.dal.mysql.contract.TradeContractMapper;
import com.meession.etm.module.trade.enums.ErrorCodeConstants;
import com.meession.etm.module.trade.enums.TradeContractStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeContractServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeContractService tradeContractService;

    @Resource
    private TradeContractMapper tradeContractMapper;

    @Test
    void testCreateContract_success() {
        TradeContractSaveReqVO reqVO = new TradeContractSaveReqVO();
        reqVO.setName("测试合同");
        reqVO.setAmount(new BigDecimal("10000.00"));

        Long id = tradeContractService.createContract(reqVO);

        assertNotNull(id);
        TradeContractDO contract = tradeContractMapper.selectById(id);
        assertEquals("测试合同", contract.getName());
        assertEquals(TradeContractStatusEnum.DRAFT.getStatus(), contract.getStatus());
    }

    @Test
    void testUpdateContract_success() {
        TradeContractSaveReqVO createReqVO = new TradeContractSaveReqVO();
        createReqVO.setName("测试合同");
        createReqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeContractService.createContract(createReqVO);

        TradeContractSaveReqVO updateReqVO = new TradeContractSaveReqVO();
        updateReqVO.setId(id);
        updateReqVO.setName("修改后的合同");
        updateReqVO.setAmount(new BigDecimal("20000.00"));

        tradeContractService.updateContract(updateReqVO);

        TradeContractDO contract = tradeContractMapper.selectById(id);
        assertEquals("修改后的合同", contract.getName());
    }

    @Test
    void testDeleteContract_success() {
        TradeContractSaveReqVO reqVO = new TradeContractSaveReqVO();
        reqVO.setName("测试合同");
        reqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeContractService.createContract(reqVO);

        tradeContractService.deleteContract(id);

        assertNull(tradeContractMapper.selectById(id));
    }

    @Test
    void testDeleteContract_notExists() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> tradeContractService.deleteContract(99999L));
        assertEquals(ErrorCodeConstants.CONTRACT_NOT_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void testGetContractPage_success() {
        TradeContractPageReqVO reqVO = new TradeContractPageReqVO();

        PageResult<TradeContractDO> pageResult = tradeContractService.getContractPage(reqVO);

        assertNotNull(pageResult);
    }

    @Test
    void testGetContract_success() {
        TradeContractSaveReqVO reqVO = new TradeContractSaveReqVO();
        reqVO.setName("测试合同");
        reqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeContractService.createContract(reqVO);

        TradeContractDO contract = tradeContractService.getContract(id);

        assertNotNull(contract);
        assertEquals("测试合同", contract.getName());
    }

    @Test
    void testGetContractsByOrderId_success() {
        List<TradeContractDO> contracts = tradeContractService.getContractsByOrderId(1L);
        assertNotNull(contracts);
    }

    @Test
    void testUpdateContractStatus_success() {
        TradeContractSaveReqVO reqVO = new TradeContractSaveReqVO();
        reqVO.setName("测试合同");
        reqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeContractService.createContract(reqVO);

        tradeContractService.updateContractStatus(id, TradeContractStatusEnum.SIGNED.getStatus());

        TradeContractDO contract = tradeContractMapper.selectById(id);
        assertEquals(TradeContractStatusEnum.SIGNED.getStatus(), contract.getStatus());
    }

}