package com.meession.etm.module.bpm.service.definition;

import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BpmProcessDefinitionTenantTest {

    @InjectMocks
    private BpmProcessDefinitionServiceImpl definitionService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private ProcessDefinitionQuery definitionQuery;
    @Mock
    private DeploymentQuery deploymentQuery;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(23L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void getByIdAlwaysFiltersCurrentTenant() {
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionId("definition-1")).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionTenantId("23")).thenReturn(definitionQuery);

        definitionService.getProcessDefinition("definition-1");

        verify(definitionQuery).processDefinitionTenantId("23");
        verify(definitionQuery).singleResult();
    }

    @Test
    void getDeploymentByIdAlwaysFiltersCurrentTenant() {
        when(repositoryService.createDeploymentQuery()).thenReturn(deploymentQuery);
        when(deploymentQuery.deploymentId("deployment-1")).thenReturn(deploymentQuery);
        when(deploymentQuery.deploymentTenantId("23")).thenReturn(deploymentQuery);

        definitionService.getDeployment("deployment-1");

        verify(deploymentQuery).deploymentTenantId("23");
        verify(deploymentQuery).singleResult();
    }
}
