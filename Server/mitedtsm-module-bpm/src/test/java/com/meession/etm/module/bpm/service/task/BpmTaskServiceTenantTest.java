package com.meession.etm.module.bpm.service.task;

import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BpmTaskServiceTenantTest {

    @InjectMocks
    private BpmTaskServiceImpl taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private HistoricTaskInstanceQuery taskQuery;
    @Mock
    private TaskService flowableTaskService;
    @Mock
    private TaskQuery runtimeTaskQuery;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(23L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void donePageAlwaysFiltersCurrentTenant() {
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(taskQuery);
        when(taskQuery.finished()).thenReturn(taskQuery);
        when(taskQuery.taskAssignee("9")).thenReturn(taskQuery);
        when(taskQuery.taskTenantId("23")).thenReturn(taskQuery);
        when(taskQuery.includeTaskLocalVariables()).thenReturn(taskQuery);
        when(taskQuery.orderByHistoricTaskInstanceEndTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.count()).thenReturn(0L);

        taskService.getTaskDonePage(9L, new BpmTaskPageReqVO());

        verify(taskQuery).taskTenantId("23");
        verify(taskQuery, never()).taskWithoutTenantId();
    }

    @Test
    void runtimeTaskByIdAlwaysFiltersCurrentTenant() {
        when(flowableTaskService.createTaskQuery()).thenReturn(runtimeTaskQuery);
        when(runtimeTaskQuery.taskId("task-1")).thenReturn(runtimeTaskQuery);
        when(runtimeTaskQuery.taskTenantId("23")).thenReturn(runtimeTaskQuery);
        when(runtimeTaskQuery.includeTaskLocalVariables()).thenReturn(runtimeTaskQuery);

        taskService.getTask("task-1");

        verify(runtimeTaskQuery).taskTenantId("23");
        verify(runtimeTaskQuery).singleResult();
    }

    @Test
    void historicTaskByIdAlwaysFiltersCurrentTenant() {
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
        when(taskQuery.taskTenantId("23")).thenReturn(taskQuery);
        when(taskQuery.includeTaskLocalVariables()).thenReturn(taskQuery);

        taskService.getHistoricTask("task-1");

        verify(taskQuery).taskTenantId("23");
        verify(taskQuery).singleResult();
    }
}
