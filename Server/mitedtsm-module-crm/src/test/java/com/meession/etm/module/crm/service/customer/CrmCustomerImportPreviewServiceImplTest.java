package com.meession.etm.module.crm.service.customer;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewRespVO.Row;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportExcelVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerImportPreviewDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerImportPreviewMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.framework.customer.CrmCustomerImportProperties;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_PREVIEW_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmCustomerImportPreviewServiceImplTest {

    @Mock private CrmCustomerImportPreviewMapper previewMapper;
    @Mock private CrmCustomerMapper customerMapper;
    @Mock private CrmCustomerService customerService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private Validator validator;

    private CrmCustomerImportPreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CrmCustomerImportPreviewServiceImpl();
        CrmCustomerImportProperties properties = new CrmCustomerImportProperties();
        properties.setMaxRows(20);
        properties.setPreviewTtlMinutes(30);
        ReflectionTestUtils.setField(service, "previewMapper", previewMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerService", customerService);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "validator", validator);
        ReflectionTestUtils.setField(service, "properties", properties);
    }

    @Test
    void customHeadersAreMappedAndPreviewDoesNotWriteCustomer() throws Exception {
        MockMultipartFile file = workbook(List.of("公司", "联系电话"), List.of("Acme", "13800000000"));
        CrmCustomerImportPreviewReqVO request = new CrmCustomerImportPreviewReqVO();
        request.setFile(file);
        request.setUpdateSupport(false);
        request.setOwnerUserId(7L);
        request.setFieldMapping("{\"公司\":\"name\",\"联系电话\":\"mobile\"}");
        when(customerMapper.selectListByNamesOrMobiles(any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            CrmCustomerImportPreviewDO row = invocation.getArgument(0);
            row.setId(99L);
            return 1;
        }).when(previewMapper).insert(any(CrmCustomerImportPreviewDO.class));

        var result = service.createPreview(request, 7L);

        assertEquals(99L, result.getId());
        assertEquals(Map.of("公司", "name", "联系电话", "mobile"), result.getFieldMapping());
        assertEquals(1, result.getCreateCount());
        assertEquals("Acme", result.getRows().get(0).getCustomer().getName());
        verify(customerMapper, never()).insert(any(com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO.class));
    }

    @Test
    void previewReadsOnlyTheFirstBusinessSheet() throws Exception {
        MockMultipartFile file = workbookWithReferenceSheet();
        CrmCustomerImportPreviewReqVO request = new CrmCustomerImportPreviewReqVO();
        request.setFile(file);
        request.setUpdateSupport(false);
        request.setOwnerUserId(7L);
        when(customerMapper.selectListByNamesOrMobiles(any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            CrmCustomerImportPreviewDO row = invocation.getArgument(0);
            row.setId(100L);
            return 1;
        }).when(previewMapper).insert(any(CrmCustomerImportPreviewDO.class));

        var result = service.createPreview(request, 7L);

        assertEquals(1, result.getTotalCount());
        assertEquals("Acme", result.getRows().get(0).getCustomer().getName());
    }

    @Test
    void duplicateMobileInFileIsRejectedWithoutPerRowDatabaseQueries() throws Exception {
        MockMultipartFile file = workbookRows(List.of("客户名称", "手机"),
                List.of(List.of("Acme", "13800000000"), List.of("Beta", "13800000000")));
        CrmCustomerImportPreviewReqVO request = new CrmCustomerImportPreviewReqVO();
        request.setFile(file);
        request.setUpdateSupport(false);
        when(customerMapper.selectListByNamesOrMobiles(any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            CrmCustomerImportPreviewDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(previewMapper).insert(any(CrmCustomerImportPreviewDO.class));

        var result = service.createPreview(request, 7L);

        assertEquals(1, result.getCreateCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(List.of("文件内客户手机号重复"), result.getRows().get(1).getErrors());
        verify(customerMapper).selectListByNamesOrMobiles(any(), any());
    }

    @Test
    void confirmationPersistsResultAndCanBeRetriedIdempotently() {
        Row row = Row.builder().rowNumber(2).action(CrmCustomerImportPreviewServiceImpl.ACTION_CREATE)
                .errors(List.of()).customer(CrmCustomerImportExcelVO.builder().name("Acme").build()).build();
        CrmCustomerImportPreviewDO pending = preview(10, JsonUtils.toJsonString(List.of(row)));
        when(previewMapper.selectByIdForUpdate(11L)).thenReturn(pending);
        CrmCustomerImportRespVO expected = CrmCustomerImportRespVO.builder()
                .createCustomerNames(List.of("Acme")).updateCustomerNames(List.of())
                .failureCustomerNames(Map.of()).build();
        when(customerService.importCustomerList(any(), any())).thenReturn(expected);

        assertEquals(expected, service.confirmPreview(11L, 7L));
        assertEquals(CrmCustomerImportPreviewServiceImpl.STATUS_CONFIRMED, pending.getStatus());
        verify(previewMapper).updateById(pending);

        when(previewMapper.selectByIdForUpdate(11L)).thenReturn(pending);
        assertEquals(expected, service.confirmPreview(11L, 7L));
        verify(customerService).importCustomerList(any(), any());
    }

    @Test
    void previewCannotBeReadByAnotherUser() {
        CrmCustomerImportPreviewDO preview = preview(10, "[]");
        when(previewMapper.selectById(11L)).thenReturn(preview);

        assertServiceException(() -> service.getPreview(11L, 8L), CUSTOMER_IMPORT_PREVIEW_NOT_EXISTS);
    }

    private CrmCustomerImportPreviewDO preview(int status, String rows) {
        CrmCustomerImportPreviewDO preview = new CrmCustomerImportPreviewDO().setId(11L).setStatus(status)
                .setFileName("customers.xlsx").setFieldMapping("{\"客户名称\":\"name\"}")
                .setUpdateSupport(false).setTotalCount(1).setCreateCount(1).setUpdateCount(0)
                .setFailureCount(0).setRowsSnapshot(rows).setExpiresAt(LocalDateTime.now().plusMinutes(10));
        preview.setCreator("7");
        return preview;
    }

    private MockMultipartFile workbook(List<String> headers, List<String> values) throws Exception {
        return workbookRows(headers, List.of(values));
    }

    private MockMultipartFile workbookRows(List<String> headers, List<List<String>> values) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<List<String>> head = headers.stream().map(List::of).toList();
        FastExcelFactory.write(output).head(head).sheet("客户").doWrite(values);
        return new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    }

    private MockMultipartFile workbookWithReferenceSheet() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ExcelWriter writer = FastExcelFactory.write(output).build()) {
            WriteSheet customerSheet = FastExcelFactory.writerSheet(0, "客户")
                    .head(List.of(List.of("客户名称"), List.of("手机"))).build();
            writer.write(List.of(List.of("Acme", "13800000000")), customerSheet);
            WriteSheet referenceSheet = FastExcelFactory.writerSheet(1, "字典")
                    .head(List.of(List.of("标签"))).build();
            writer.write(java.util.stream.IntStream.range(0, 25)
                    .mapToObj(index -> List.of("字典值" + index)).toList(), referenceSheet);
        }
        return new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    }
}
