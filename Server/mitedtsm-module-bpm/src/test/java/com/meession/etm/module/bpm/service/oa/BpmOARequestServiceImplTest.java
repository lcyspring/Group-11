package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOARequestDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOARequestMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.meession.etm.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import(BpmOARequestServiceImpl.class)
public class BpmOARequestServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BpmOARequestServiceImpl requestService;

    @Resource
    private BpmOARequestMapper requestMapper;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    @Test
    public void testCreateRequest() {
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("process-123");

        BpmOARequestCreateReqVO createReqVO = new BpmOARequestCreateReqVO();
        createReqVO.setTitle("Purchase request");
        createReqVO.setContent("Need to purchase new equipment");

        Long id = requestService.createRequest(1L, createReqVO);
        assertNotNull(id);

        BpmOARequestDO dbRequest = requestMapper.selectById(id);
        assertNotNull(dbRequest);
        assertEquals("Purchase request", dbRequest.getTitle());
        assertEquals(BpmTaskStatusEnum.RUNNING.getStatus(), dbRequest.getStatus());
    }

    @Test
    public void testGetRequest() {
        BpmOARequestDO dbRequest = randomPojo(BpmOARequestDO.class, o -> {
            o.setTitle("Purchase request");
        });
        requestMapper.insert(dbRequest);

        BpmOARequestDO request = requestService.getRequest(dbRequest.getId());
        assertNotNull(request);
        assertEquals(dbRequest.getTitle(), request.getTitle());
    }

    @Test
    public void testGetRequestPage() {
        BpmOARequestDO dbRequest = randomPojo(BpmOARequestDO.class, o -> {
            o.setTitle("Purchase request");
        });
        requestMapper.insert(dbRequest);

        BpmOARequestPageReqVO reqVO = new BpmOARequestPageReqVO();
        PageResult<BpmOARequestDO> pageResult = requestService.getRequestPage(null, reqVO);
        assertNotNull(pageResult);
        assertTrue(pageResult.getTotal() >= 1);
    }

    @Test
    public void testUpdateRequestStatus() {
        BpmOARequestDO dbRequest = randomPojo(BpmOARequestDO.class);
        requestMapper.insert(dbRequest);

        requestService.updateRequestStatus(dbRequest.getId(), BpmTaskStatusEnum.APPROVE.getStatus());

        BpmOARequestDO updatedRequest = requestMapper.selectById(dbRequest.getId());
        assertEquals(BpmTaskStatusEnum.APPROVE.getStatus(), updatedRequest.getStatus());
    }

}