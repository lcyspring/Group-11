package com.meession.etm.module.bpm.service.definition;

import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BpmModelTenantTest {

    @InjectMocks
    private BpmModelServiceImpl modelService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private ModelQuery modelQuery;
    @Mock
    private Model model;
    @Mock
    private BpmProcessDefinitionService processDefinitionService;

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
        when(repositoryService.createModelQuery()).thenReturn(modelQuery);
        when(modelQuery.modelId("model-1")).thenReturn(modelQuery);
        when(modelQuery.modelTenantId("23")).thenReturn(modelQuery);

        modelService.getModel("model-1");

        verify(modelQuery).modelTenantId("23");
        verify(modelQuery).singleResult();
    }

    @Test
    void getBpmnXmlValidatesModelTenantBeforeReadingSource() {
        byte[] expected = "<bpmn/>".getBytes();
        stubCurrentTenantModel("model-1");
        when(model.getId()).thenReturn("model-1");
        when(repositoryService.getModelEditorSource("model-1")).thenReturn(expected);

        byte[] actual = modelService.getModelBpmnXML("model-1");

        assertArrayEquals(expected, actual);
        verify(modelQuery).modelTenantId("23");
    }

    @Test
    void updateBpmnXmlValidatesModelTenantBeforeWritingSource() {
        stubCurrentTenantModel("model-1");

        modelService.updateModelBpmnXml("model-1", "<bpmn/>");

        verify(modelQuery).modelTenantId("23");
        verify(repositoryService).addModelEditorSource("model-1", "<bpmn/>".getBytes());
    }

    @Test
    void getBpmnModelDelegatesToTenantSafeDefinitionService() {
        BpmnModel expected = new BpmnModel();
        when(processDefinitionService.getProcessDefinitionBpmnModel("definition-1")).thenReturn(expected);

        BpmnModel actual = modelService.getBpmnModelByDefinitionId("definition-1");

        assertSame(expected, actual);
        verifyNoInteractions(repositoryService);
    }

    private void stubCurrentTenantModel(String id) {
        when(repositoryService.createModelQuery()).thenReturn(modelQuery);
        when(modelQuery.modelId(id)).thenReturn(modelQuery);
        when(modelQuery.modelTenantId("23")).thenReturn(modelQuery);
        when(modelQuery.singleResult()).thenReturn(model);
    }
}
