package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOACustomerVisitCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOACustomerVisitPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOACustomerVisitDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOACustomerVisitMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static com.meession.etm.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import(BpmOACustomerVisitServiceImpl.class)
public class BpmOACustomerVisitServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BpmOACustomerVisitServiceImpl customerVisitService;

    @Resource
    private BpmOACustomerVisitMapper customerVisitMapper;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    @Test
    public void testCreateCustomerVisit() {
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("process-123");

        BpmOACustomerVisitCreateReqVO createReqVO = new BpmOACustomerVisitCreateReqVO();
        createReqVO.setCustomerId(1L);
        createReqVO.setCustomerName("Beijing");
        createReqVO.setVisitTime(LocalDateTime.now());
        createReqVO.setPurpose("Customer visit");

        Long id = customerVisitService.createCustomerVisit(1L, createReqVO);
        assertNotNull(id);

        BpmOACustomerVisitDO dbVisit = customerVisitMapper.selectById(id);
        assertNotNull(dbVisit);
        assertEquals("Beijing", dbVisit.getCustomerName());
        assertEquals(BpmTaskStatusEnum.RUNNING.getStatus(), dbVisit.getStatus());
    }

    @Test
    public void testGetCustomerVisit() {
        BpmOACustomerVisitDO dbVisit = randomPojo(BpmOACustomerVisitDO.class, o -> {
            o.setCustomerName("Beijing");
        });
        customerVisitMapper.insert(dbVisit);

        BpmOACustomerVisitDO visit = customerVisitService.getCustomerVisit(dbVisit.getId());
        assertNotNull(visit);
        assertEquals(dbVisit.getCustomerName(), visit.getCustomerName());
    }

    @Test
    public void testGetCustomerVisitPage() {
        BpmOACustomerVisitDO dbVisit = randomPojo(BpmOACustomerVisitDO.class, o -> {
            o.setCustomerName("Beijing");
        });
        customerVisitMapper.insert(dbVisit);

        BpmOACustomerVisitPageReqVO reqVO = new BpmOACustomerVisitPageReqVO();
        PageResult<BpmOACustomerVisitDO> pageResult = customerVisitService.getCustomerVisitPage(null, reqVO);
        assertNotNull(pageResult);
        assertTrue(pageResult.getTotal() >= 1);
    }

    @Test
    public void testUpdateCustomerVisitStatus() {
        BpmOACustomerVisitDO dbVisit = randomPojo(BpmOACustomerVisitDO.class);
        customerVisitMapper.insert(dbVisit);

        customerVisitService.updateCustomerVisitStatus(dbVisit.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        BpmOACustomerVisitDO updatedVisit = customerVisitMapper.selectById(dbVisit.getId());
        assertEquals(BpmTaskStatusEnum.APPROVE.getStatus(), updatedVisit.getStatus());
    }

}