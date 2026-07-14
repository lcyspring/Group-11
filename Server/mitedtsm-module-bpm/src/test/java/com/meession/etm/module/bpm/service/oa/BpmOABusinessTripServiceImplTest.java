package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABusinessTripDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOABusinessTripMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertPojoEquals;
import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.framework.test.core.util.RandomUtils.randomLongId;
import static com.meession.etm.framework.test.core.util.RandomUtils.randomPojo;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_BUSINESS_TRIP_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link BpmOABusinessTripServiceImpl} 的单元测试类
 *
 * @author 李春雨
 */
@Import(BpmOABusinessTripServiceImpl.class)
public class BpmOABusinessTripServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BpmOABusinessTripServiceImpl businessTripService;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private BpmOABusinessTripMapper businessTripMapper;

    @Test
    public void testCreateBusinessTrip_success() {
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("process-123");

        BpmOABusinessTripCreateReqVO createReqVO = randomPojo(BpmOABusinessTripCreateReqVO.class, o -> {
            o.setDestination("北京");
            o.setReason("客户拜访");
            o.setStartTime(LocalDateTime.now().minusDays(1));
            o.setEndTime(LocalDateTime.now().plusDays(2));
            o.setBudget(new BigDecimal("1000.00"));
        });

        Long businessTripId = businessTripService.createBusinessTrip(1L, createReqVO);
        assertNotNull(businessTripId);

        BpmOABusinessTripDO businessTrip = businessTripMapper.selectById(businessTripId);
        assertEquals(createReqVO.getDestination(), businessTrip.getDestination());
        assertEquals(createReqVO.getReason(), businessTrip.getReason());
        assertNotNull(businessTrip.getStartTime());
        assertNotNull(businessTrip.getEndTime());
        assertEquals(3L, businessTrip.getDays());
        assertEquals(createReqVO.getBudget(), businessTrip.getBudget());
        assertEquals("process-123", businessTrip.getProcessInstanceId());
    }

    @Test
    public void testUpdateBusinessTripStatus_success() {
        BpmOABusinessTripDO dbBusinessTrip = randomPojo(BpmOABusinessTripDO.class, o -> {
            o.setBudget(new BigDecimal("1000.00"));
        });
        businessTripMapper.insert(dbBusinessTrip);

        businessTripService.updateBusinessTripStatus(dbBusinessTrip.getId(), 2);

        BpmOABusinessTripDO businessTrip = businessTripMapper.selectById(dbBusinessTrip.getId());
        assertEquals(Integer.valueOf(2), businessTrip.getStatus());
    }

    @Test
    public void testUpdateBusinessTripStatus_notExists() {
        assertServiceException(() -> businessTripService.updateBusinessTripStatus(randomLongId(), 2), OA_BUSINESS_TRIP_NOT_EXISTS);
    }

    @Test
    public void testGetBusinessTrip_success() {
        BpmOABusinessTripDO dbBusinessTrip = randomPojo(BpmOABusinessTripDO.class, o -> {
            o.setBudget(new BigDecimal("1000.00"));
        });
        businessTripMapper.insert(dbBusinessTrip);

        BpmOABusinessTripDO businessTrip = businessTripService.getBusinessTrip(dbBusinessTrip.getId());
        assertNotNull(businessTrip);
        assertEquals(dbBusinessTrip.getId(), businessTrip.getId());
    }

    @Test
    public void testGetBusinessTrip_notExists() {
        assertNull(businessTripService.getBusinessTrip(randomLongId()));
    }

    @Test
    public void testGetBusinessTripPage() {
        BpmOABusinessTripDO dbBusinessTrip = randomPojo(BpmOABusinessTripDO.class, o -> {
            o.setDestination("Beijing");
            o.setReason("Customer visit");
            o.setBudget(new BigDecimal("1000.00"));
        });
        businessTripMapper.insert(dbBusinessTrip);

        BpmOABusinessTripPageReqVO reqVO = new BpmOABusinessTripPageReqVO();

        PageResult<BpmOABusinessTripDO> pageResult = businessTripService.getBusinessTripPage(null, reqVO);
        assertTrue(pageResult.getTotal() >= 1);
        assertTrue(pageResult.getList().size() >= 1);
    }

}