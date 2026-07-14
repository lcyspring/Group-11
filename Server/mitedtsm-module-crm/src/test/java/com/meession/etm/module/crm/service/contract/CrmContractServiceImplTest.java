package com.meession.etm.module.crm.service.contract;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.crm.controller.admin.contract.vo.contract.CrmContractSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.crm.service.product.CrmProductService;
import com.meession.etm.module.crm.service.receivable.CrmReceivableService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTRACT_CREATE_FAIL_BUSINESS_NOT_WON;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTRACT_CREATE_BUSINESS_REQUIRES_CONVERSION;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTRACT_DELETE_FAIL_NOT_NEW_DRAFT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTRACT_PRODUCT_ROW_NOT_BELONGS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTRACT_SIGN_CONTACT_CUSTOMER_MISMATCH;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CONTRACT_UPDATE_FAIL_NOT_EDITABLE;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.ACTION_CANCEL;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.ACTION_CREATE;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.ACTION_SUBMIT;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.ACTION_UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmContractServiceImplTest {

    @Mock
    private CrmContractMapper contractMapper;
    @Mock
    private CrmContractProductMapper contractProductMapper;
    @Mock
    private CrmNoRedisDAO noRedisDAO;
    @Mock
    private CrmPermissionService crmPermissionService;
    @Mock
    private CrmProductService productService;
    @Mock
    private CrmCustomerService customerService;
    @Mock
    private CrmBusinessService businessService;
    @Mock
    private CrmContactService contactService;
    @Mock
    private CrmReceivableService receivableService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private CrmContractLifecycleService contractLifecycleService;

    @InjectMocks
    private CrmContractServiceImpl service;

    @Test
    void createContractRejectsBusinessThatIsNotWon() {
        when(businessService.validateBusiness(10L)).thenReturn(business(null));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createContractFromBusiness(request(), 1L));

        assertEquals(CONTRACT_CREATE_FAIL_BUSINESS_NOT_WON.getCode(), exception.getCode());
        verify(contractMapper, never()).insert(any(CrmContractDO.class));
    }

    @Test
    void createContractRejectsBusinessOnGenericEntry() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createContract(request(), 1L));

        assertEquals(CONTRACT_CREATE_BUSINESS_REQUIRES_CONVERSION.getCode(), exception.getCode());
        verify(businessService, never()).validateBusiness(any());
    }

    @Test
    void createContractReturnsExistingBusinessContractIdempotently() {
        when(businessService.validateBusiness(10L)).thenReturn(business(CrmBusinessEndStatusEnum.WIN.getStatus()));
        when(contractMapper.selectFirstByBusinessId(10L)).thenReturn(new CrmContractDO().setId(88L).setName("已有合同"));

        assertEquals(88L, service.createContractFromBusiness(request(), 1L));

        verify(noRedisDAO, never()).generate(any());
        verify(contractMapper, never()).insert(any(CrmContractDO.class));
    }

    @Test
    void createContractInheritsBusinessOwnershipAndSetsConversionSource() {
        when(businessService.validateBusiness(10L)).thenReturn(business(CrmBusinessEndStatusEnum.WIN.getStatus()));
        when(noRedisDAO.generate(CrmNoRedisDAO.CONTRACT_NO_PREFIX)).thenReturn("HT20260714000001");
        when(productService.validProductList(Collections.singleton(4L)))
                .thenReturn(Collections.singletonList(product(4L, "商机成交产品", "OPP-SKU", 1, "88.00")));
        doAnswer(invocation -> {
            ((CrmContractDO) invocation.getArgument(0)).setId(99L);
            return 1;
        }).when(contractMapper).insert(any(CrmContractDO.class));
        CrmContractSaveReqVO conversionRequest = request().setProducts(
                Collections.singletonList(productRequest(444L, 4L, "999.00", "80.00", 2)));

        assertEquals(99L, service.createContractFromBusiness(conversionRequest, 1L));

        ArgumentCaptor<CrmContractDO> contractCaptor = ArgumentCaptor.forClass(CrmContractDO.class);
        verify(contractMapper).insert(contractCaptor.capture());
        CrmContractDO contract = contractCaptor.getValue();
        assertEquals(10L, contract.getBusinessId());
        assertEquals(10L, contract.getSourceBusinessId());
        assertEquals(20L, contract.getCustomerId());
        assertEquals(30L, contract.getOwnerUserId());

        ArgumentCaptor<CrmPermissionCreateReqBO> permissionCaptor =
                ArgumentCaptor.forClass(CrmPermissionCreateReqBO.class);
        verify(crmPermissionService).createPermission(permissionCaptor.capture());
        assertEquals(30L, permissionCaptor.getValue().getUserId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<CrmContractProductDO>> productCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(contractProductMapper).insertBatch(productCaptor.capture());
        CrmContractProductDO savedProduct = productCaptor.getValue().iterator().next();
        assertNull(savedProduct.getId()); // 商机产品行 444 不能复用为合同产品行编号
        assertEquals("商机成交产品", savedProduct.getProductNameSnapshot());
        assertEquals(new BigDecimal("88.00"), savedProduct.getProductPrice());
        verify(contractLifecycleService).recordChange(99L, ACTION_CREATE, 1, 30L, "创建合同");
    }

    @Test
    void createContractReturnsConcurrentWinner() {
        when(businessService.validateBusiness(10L)).thenReturn(business(CrmBusinessEndStatusEnum.WIN.getStatus()));
        when(noRedisDAO.generate(CrmNoRedisDAO.CONTRACT_NO_PREFIX)).thenReturn("HT20260714000002");
        doThrow(new DuplicateKeyException("duplicate conversion"))
                .when(contractMapper).insert(any(CrmContractDO.class));
        when(contractMapper.selectBySourceBusinessIdForUpdate(10L))
                .thenReturn(new CrmContractDO().setId(100L).setName("并发创建的合同"));

        assertEquals(100L, service.createContractFromBusiness(request(), 1L));

        verify(crmPermissionService, never()).createPermission(any());
    }

    @Test
    void createContractSnapshotsCatalogAndIgnoresClientRowIdAndStandardPrice() {
        when(noRedisDAO.generate(CrmNoRedisDAO.CONTRACT_NO_PREFIX)).thenReturn("HT20260714000003");
        when(productService.validProductList(Collections.singleton(4L)))
                .thenReturn(Collections.singletonList(product(4L, "成交产品", "SKU-OLD", 2, "88.00")));
        doAnswer(invocation -> {
            ((CrmContractDO) invocation.getArgument(0)).setId(101L);
            return 1;
        }).when(contractMapper).insert(any(CrmContractDO.class));
        CrmContractSaveReqVO createRequest = request().setBusinessId(null)
                .setProducts(Collections.singletonList(productRequest(777L, 4L, "999.00", "80.00", 2)));

        assertEquals(101L, service.createContract(createRequest, 1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<CrmContractProductDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(contractProductMapper).insertBatch(captor.capture());
        CrmContractProductDO saved = captor.getValue().iterator().next();
        assertNull(saved.getId());
        assertEquals("成交产品", saved.getProductNameSnapshot());
        assertEquals("SKU-OLD", saved.getProductNoSnapshot());
        assertEquals(2, saved.getProductUnitSnapshot());
        assertEquals(new BigDecimal("88.00"), saved.getProductPrice());
        assertEquals(new BigDecimal("160.00"), saved.getTotalPrice());
    }

    @Test
    void updateContractPreservesExistingSnapshotWhenCatalogChanges() {
        when(contractMapper.selectByIdForUpdate(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.DRAFT, null));
        CrmContractProductDO oldProduct = contractProduct(55L, 7L, 4L,
                "历史名称", "SKU-HISTORY", 1, "88.00");
        when(contractProductMapper.selectListByContractId(7L))
                .thenReturn(Collections.singletonList(oldProduct));
        CrmContractSaveReqVO updateRequest = updateRequest(7L).setBusinessId(null)
                .setProducts(Collections.singletonList(productRequest(55L, 4L, "999.00", "70.00", 3)));

        service.updateContract(updateRequest);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<CrmContractProductDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(contractProductMapper).updateBatch(captor.capture());
        CrmContractProductDO saved = captor.getValue().iterator().next();
        assertEquals("历史名称", saved.getProductNameSnapshot());
        assertEquals("SKU-HISTORY", saved.getProductNoSnapshot());
        assertEquals(1, saved.getProductUnitSnapshot());
        assertEquals(new BigDecimal("88.00"), saved.getProductPrice());
        assertEquals(new BigDecimal("210.00"), saved.getTotalPrice());
        verify(productService, never()).validProductList(any());
        verify(contractLifecycleService).recordChange(7L, ACTION_UPDATE, 1, null, "修改合同");
    }

    @Test
    void updateContractRejectsProductRowFromAnotherContract() {
        when(contractMapper.selectByIdForUpdate(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.DRAFT, null));
        when(contractProductMapper.selectListByContractId(7L)).thenReturn(Collections.emptyList());
        CrmContractSaveReqVO updateRequest = updateRequest(7L).setBusinessId(null)
                .setProducts(Collections.singletonList(productRequest(999L, 4L, "88.00", "70.00", 1)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateContract(updateRequest));

        assertEquals(CONTRACT_PRODUCT_ROW_NOT_BELONGS.getCode(), exception.getCode());
        verify(contractMapper, never()).updateById(any(CrmContractDO.class));
    }

    @Test
    void conversionEntryRequiresBusinessWritePermission() throws NoSuchMethodException {
        CrmPermission permission = CrmContractServiceImpl.class
                .getMethod("createContractFromBusiness", CrmContractSaveReqVO.class, Long.class)
                .getAnnotation(CrmPermission.class);

        assertEquals(CrmBizTypeEnum.CRM_BUSINESS, permission.bizType()[0]);
        assertEquals("#createReqVO.businessId", permission.bizId());
        assertEquals(CrmPermissionLevelEnum.WRITE, permission.level());
    }

    @Test
    void updateContractRejectsProcessingState() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract(7L, CrmAuditStatusEnum.PROCESS, "process-1"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateContract(updateRequest(7L)));

        assertEquals(CONTRACT_UPDATE_FAIL_NOT_EDITABLE.getCode(), exception.getCode());
        verify(contractMapper, never()).updateById(any(CrmContractDO.class));
    }

    @Test
    void createContractRejectsSignContactFromAnotherCustomer() {
        CrmContractSaveReqVO createRequest = request().setBusinessId(null).setCustomerId(20L)
                .setSignContactId(8L);
        when(contactService.getContact(8L)).thenReturn(new CrmContactDO().setId(8L).setCustomerId(21L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createContract(createRequest, 1L));

        assertEquals(CONTRACT_SIGN_CONTACT_CUSTOMER_MISMATCH.getCode(), exception.getCode());
        verify(contractMapper, never()).insert(any(CrmContractDO.class));
    }

    @Test
    void updateRejectedContractCreatesRevisionDraftAndPreservesConversionSource() {
        CrmContractDO oldContract = contract(7L, CrmAuditStatusEnum.REJECT, "process-1")
                .setSourceBusinessId(10L)
                .setBusinessId(10L)
                .setCustomerId(20L)
                .setOwnerUserId(30L);
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(oldContract);
        when(contractProductMapper.selectListByContractId(7L)).thenReturn(Collections.emptyList());
        CrmContractSaveReqVO request = updateRequest(7L).setBusinessId(999L).setCustomerId(999L);

        service.updateContract(request);

        ArgumentCaptor<CrmContractDO> captor = ArgumentCaptor.forClass(CrmContractDO.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), captor.getValue().getAuditStatus());
        assertEquals(10L, captor.getValue().getBusinessId());
        assertEquals(20L, captor.getValue().getCustomerId());
        assertNull(captor.getValue().getOwnerUserId());
    }

    @Test
    void updateCanceledContractCreatesRevisionDraft() {
        when(contractMapper.selectByIdForUpdate(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.CANCEL, "process-1"));
        when(contractProductMapper.selectListByContractId(7L)).thenReturn(Collections.emptyList());

        service.updateContract(updateRequest(7L));

        ArgumentCaptor<CrmContractDO> captor = ArgumentCaptor.forClass(CrmContractDO.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), captor.getValue().getAuditStatus());
    }

    @Test
    void submitRevisedDraftStartsNewProcessAndRequiresWritePermission() throws NoSuchMethodException {
        when(contractMapper.selectByIdForUpdate(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.DRAFT, "process-1"));
        when(bpmProcessInstanceApi.createProcessInstance(any(), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-2");

        service.submitContract(7L, 1L);

        ArgumentCaptor<CrmContractDO> captor = ArgumentCaptor.forClass(CrmContractDO.class);
        verify(contractMapper).updateById(captor.capture());
        assertEquals("process-2", captor.getValue().getProcessInstanceId());
        assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(), captor.getValue().getAuditStatus());
        verify(contractLifecycleService).recordChange(7L, ACTION_SUBMIT, 0, 1L, "提交合同审批");
        CrmPermission permission = CrmContractServiceImpl.class
                .getMethod("submitContract", Long.class, Long.class)
                .getAnnotation(CrmPermission.class);
        assertEquals(CrmBizTypeEnum.CRM_CONTRACT, permission.bizType()[0]);
        assertEquals("#id", permission.bizId());
        assertEquals(CrmPermissionLevelEnum.WRITE, permission.level());
    }

    @Test
    void updateContractAuditStatusMapsCancelToCrmStatus() {
        when(contractMapper.selectById(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.PROCESS, "process-2"));
        when(contractMapper.updateAuditStatusIfProcessing(7L, "process-2", CrmAuditStatusEnum.CANCEL.getStatus()))
                .thenReturn(1);

        service.updateContractAuditStatus(7L, "process-2", BpmProcessInstanceStatusEnum.CANCEL.getStatus());

        verify(contractMapper).updateAuditStatusIfProcessing(7L, "process-2", CrmAuditStatusEnum.CANCEL.getStatus());
        verify(contractLifecycleService).recordChange(7L, ACTION_CANCEL, 0, null, "合同审批状态变更为 40");
    }

    @Test
    void updateContractAuditStatusIsIdempotentForDuplicateEvent() {
        when(contractMapper.selectById(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.REJECT, "process-2"));

        service.updateContractAuditStatus(7L, "process-2", BpmProcessInstanceStatusEnum.REJECT.getStatus());

        verify(contractMapper, never()).updateAuditStatusIfProcessing(any(), any(), any());
    }

    @Test
    void updateContractAuditStatusIgnoresStaleProcess() {
        when(contractMapper.selectById(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.PROCESS, "process-2"));

        service.updateContractAuditStatus(7L, "process-1", BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        verify(contractMapper, never()).updateAuditStatusIfProcessing(any(), any(), any());
    }

    @Test
    void deleteContractRejectsSubmittedHistory() {
        when(contractMapper.selectByIdForUpdate(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.REJECT, "process-1"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteContract(7L));

        assertEquals(CONTRACT_DELETE_FAIL_NOT_NEW_DRAFT.getCode(), exception.getCode());
        verify(contractMapper, never()).deleteById(7L);
    }

    @Test
    void deleteContractAllowsNewDraftWithoutProcessHistory() {
        when(contractMapper.selectByIdForUpdate(7L))
                .thenReturn(contract(7L, CrmAuditStatusEnum.DRAFT, null));

        service.deleteContract(7L);

        verify(contractMapper).deleteById(7L);
        verify(crmPermissionService).deletePermission(CrmBizTypeEnum.CRM_CONTRACT.getType(), 7L);
    }

    private static CrmContractSaveReqVO request() {
        return new CrmContractSaveReqVO()
                .setName("商机合同")
                .setCustomerId(999L)
                .setBusinessId(10L)
                .setOwnerUserId(999L)
                .setOrderDate(LocalDateTime.of(2026, 7, 14, 0, 0))
                .setDiscountPercent(BigDecimal.ZERO)
                .setProducts(Collections.emptyList());
    }

    private static CrmContractSaveReqVO updateRequest(Long id) {
        return request().setId(id);
    }

    private static CrmContractDO contract(Long id, CrmAuditStatusEnum auditStatus, String processInstanceId) {
        return new CrmContractDO()
                .setId(id)
                .setName("测试合同")
                .setAuditStatus(auditStatus.getStatus())
                .setProcessInstanceId(processInstanceId);
    }

    private static CrmBusinessDO business(Integer endStatus) {
        return new CrmBusinessDO()
                .setId(10L)
                .setName("重点商机")
                .setCustomerId(20L)
                .setOwnerUserId(30L)
                .setEndStatus(endStatus);
    }

    private static CrmContractSaveReqVO.Product productRequest(Long id, Long productId,
                                                                String productPrice, String contractPrice,
                                                                Integer count) {
        return new CrmContractSaveReqVO.Product()
                .setId(id)
                .setProductId(productId)
                .setProductPrice(new BigDecimal(productPrice))
                .setContractPrice(new BigDecimal(contractPrice))
                .setCount(count);
    }

    private static CrmProductDO product(Long id, String name, String no, Integer unit, String price) {
        return new CrmProductDO().setId(id).setName(name).setNo(no).setUnit(unit)
                .setPrice(new BigDecimal(price));
    }

    private static CrmContractProductDO contractProduct(Long id, Long contractId, Long productId,
                                                         String name, String no, Integer unit,
                                                         String productPrice) {
        return new CrmContractProductDO().setId(id).setContractId(contractId).setProductId(productId)
                .setProductNameSnapshot(name).setProductNoSnapshot(no).setProductUnitSnapshot(unit)
                .setProductPrice(new BigDecimal(productPrice));
    }

}
