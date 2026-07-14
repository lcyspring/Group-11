package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABorrowDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOABorrowMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertPojoEquals;
import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.framework.test.core.util.RandomUtils.randomLongId;
import static com.meession.etm.framework.test.core.util.RandomUtils.randomPojo;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_BORROW_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link BpmOABorrowServiceImpl} 的单元测试类
 *
 * @author 李春雨
 */
@Import(BpmOABorrowServiceImpl.class)
public class BpmOABorrowServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BpmOABorrowServiceImpl borrowService;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private BpmOABorrowMapper borrowMapper;

    @Test
    public void testCreateBorrow_success() {
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("process-123");

        BpmOABorrowCreateReqVO createReqVO = randomPojo(BpmOABorrowCreateReqVO.class, o -> {
            o.setAmount(new BigDecimal("5000.00"));
            o.setReason("项目备用金");
            o.setBankAccount("622202********1234");
            o.setBankName("工商银行");
            o.setExpectRepayDate(LocalDateTime.now().plusDays(30));
        });

        Long borrowId = borrowService.createBorrow(1L, createReqVO);
        assertNotNull(borrowId);

        BpmOABorrowDO borrow = borrowMapper.selectById(borrowId);
        assertEquals(createReqVO.getAmount(), borrow.getAmount());
        assertEquals(createReqVO.getReason(), borrow.getReason());
        assertEquals(createReqVO.getBankAccount(), borrow.getBankAccount());
        assertEquals(createReqVO.getBankName(), borrow.getBankName());
        assertNotNull(borrow.getExpectRepayDate());
        assertEquals("process-123", borrow.getProcessInstanceId());
    }

    @Test
    public void testUpdateBorrowStatus_success() {
        BpmOABorrowDO dbBorrow = randomPojo(BpmOABorrowDO.class, o -> {
            o.setAmount(new BigDecimal("5000.00"));
        });
        borrowMapper.insert(dbBorrow);

        borrowService.updateBorrowStatus(dbBorrow.getId(), 2);

        BpmOABorrowDO borrow = borrowMapper.selectById(dbBorrow.getId());
        assertEquals(Integer.valueOf(2), borrow.getStatus());
    }

    @Test
    public void testUpdateBorrowStatus_notExists() {
        assertServiceException(() -> borrowService.updateBorrowStatus(randomLongId(), 2), OA_BORROW_NOT_EXISTS);
    }

    @Test
    public void testGetBorrow_success() {
        BpmOABorrowDO dbBorrow = randomPojo(BpmOABorrowDO.class, o -> {
            o.setAmount(new BigDecimal("5000.00"));
        });
        borrowMapper.insert(dbBorrow);

        BpmOABorrowDO borrow = borrowService.getBorrow(dbBorrow.getId());
        assertNotNull(borrow);
        assertEquals(dbBorrow.getId(), borrow.getId());
    }

    @Test
    public void testGetBorrow_notExists() {
        assertNull(borrowService.getBorrow(randomLongId()));
    }

    @Test
    public void testGetBorrowPage() {
        BpmOABorrowDO dbBorrow = randomPojo(BpmOABorrowDO.class, o -> {
            o.setReason("Project reserve");
            o.setAmount(new BigDecimal("5000.00"));
        });
        borrowMapper.insert(dbBorrow);

        BpmOABorrowPageReqVO reqVO = new BpmOABorrowPageReqVO();

        PageResult<BpmOABorrowDO> pageResult = borrowService.getBorrowPage(null, reqVO);
        assertTrue(pageResult.getTotal() >= 1);
        assertTrue(pageResult.getList().size() >= 1);
    }

}