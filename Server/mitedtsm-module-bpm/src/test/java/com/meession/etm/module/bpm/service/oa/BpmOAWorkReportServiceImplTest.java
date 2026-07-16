package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportUpdateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkReportDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOAWorkReportMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static com.meession.etm.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

@Import(BpmOAWorkReportServiceImpl.class)
public class BpmOAWorkReportServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BpmOAWorkReportServiceImpl workReportService;

    @Resource
    private BpmOAWorkReportMapper workReportMapper;

    @Test
    public void testCreateWorkReport() {
        BpmOAWorkReportCreateReqVO createReqVO = new BpmOAWorkReportCreateReqVO();
        createReqVO.setType("daily");
        createReqVO.setReportDate(LocalDate.now());
        createReqVO.setContent("Completed customer visit");

        Long id = workReportService.createWorkReport(1L, createReqVO);
        assertNotNull(id);

        BpmOAWorkReportDO dbReport = workReportMapper.selectById(id);
        assertNotNull(dbReport);
        assertEquals("daily", dbReport.getType());
        assertEquals(BpmTaskStatusEnum.RUNNING.getStatus(), dbReport.getStatus());
    }

    @Test
    public void testUpdateWorkReport() {
        BpmOAWorkReportDO dbReport = randomPojo(BpmOAWorkReportDO.class, o -> {
            o.setType("daily");
            o.setContent("Original content");
        });
        workReportMapper.insert(dbReport);

        BpmOAWorkReportUpdateReqVO updateReqVO = new BpmOAWorkReportUpdateReqVO();
        updateReqVO.setId(dbReport.getId());
        updateReqVO.setContent("Updated content");

        workReportService.updateWorkReport(updateReqVO);

        BpmOAWorkReportDO updatedReport = workReportMapper.selectById(dbReport.getId());
        assertEquals("Updated content", updatedReport.getContent());
    }

    @Test
    public void testDeleteWorkReport() {
        BpmOAWorkReportDO dbReport = randomPojo(BpmOAWorkReportDO.class);
        workReportMapper.insert(dbReport);

        workReportService.deleteWorkReport(dbReport.getId());

        BpmOAWorkReportDO deletedReport = workReportMapper.selectById(dbReport.getId());
        assertNull(deletedReport);
    }

    @Test
    public void testGetWorkReport() {
        BpmOAWorkReportDO dbReport = randomPojo(BpmOAWorkReportDO.class, o -> {
            o.setType("daily");
        });
        workReportMapper.insert(dbReport);

        BpmOAWorkReportDO report = workReportService.getWorkReport(dbReport.getId());
        assertNotNull(report);
        assertEquals(dbReport.getType(), report.getType());
    }

    @Test
    public void testGetWorkReportPage() {
        BpmOAWorkReportDO dbReport = randomPojo(BpmOAWorkReportDO.class, o -> {
            o.setType("daily");
        });
        workReportMapper.insert(dbReport);

        BpmOAWorkReportPageReqVO reqVO = new BpmOAWorkReportPageReqVO();
        PageResult<BpmOAWorkReportDO> pageResult = workReportService.getWorkReportPage(null, reqVO);
        assertNotNull(pageResult);
        assertTrue(pageResult.getTotal() >= 1);
    }

}