package com.meession.etm.module.crm.service.contract;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractAttachmentCreateReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignVoidReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAttachmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractChangeRecordDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAttachmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractChangeRecordMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractSigningMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums;
import com.meession.etm.module.crm.framework.contract.CrmContractSignProvider;
import com.meession.etm.module.crm.framework.security.CrmSecurityProperties;
import com.meession.etm.module.infra.api.file.FileApi;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmContractLifecycleServiceImplTest {

    @Mock
    private CrmContractMapper contractMapper;
    @Mock
    private CrmContractProductMapper productMapper;
    @Mock
    private CrmContractAttachmentMapper attachmentMapper;
    @Mock
    private CrmContractSigningMapper signingMapper;
    @Mock
    private CrmContractChangeRecordMapper changeMapper;
    @Mock
    private CrmContractSignProvider provider;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private FileApi fileApi;
    @Mock
    private CrmSecurityProperties securityProperties;
    @InjectMocks
    private CrmContractLifecycleServiceImpl service;

    @Test
    void createAttachmentNormalizesShaAndUsesCurrentVersion() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(changeMapper.selectLatest(7L)).thenReturn(new CrmContractChangeRecordDO().setContractVersion(3));
        when(securityProperties.getProtectedFileDirectory()).thenReturn("crm-protected/contract");
        when(fileApi.getFileByUrl("/files/a")).thenReturn(managedFile());
        doAnswer(invocation -> {
            ((CrmContractAttachmentDO) invocation.getArgument(0)).setId(9L);
            return 1;
        })
                .when(attachmentMapper).insert(any(CrmContractAttachmentDO.class));
        CrmContractAttachmentCreateReqVO req = new CrmContractAttachmentCreateReqVO().setContractId(7L)
                .setCategory(1).setFileName("A.PDF").setFileUrl("/files/a")
                .setSha256("ABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCD");

        assertEquals(9L, service.createAttachment(req, 1L));
        ArgumentCaptor<CrmContractAttachmentDO> captor = ArgumentCaptor.forClass(CrmContractAttachmentDO.class);
        verify(attachmentMapper).insert(captor.capture());
        assertEquals(3, captor.getValue().getContractVersion());
        assertEquals(req.getSha256().toLowerCase(), captor.getValue().getSha256());
        assertEquals("application/pdf", captor.getValue().getContentType());
        assertEquals(12L, captor.getValue().getFileSize());
    }

    @Test
    void createAttachmentRejectsManagedFileFromAnotherContractDirectory() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(securityProperties.getProtectedFileDirectory()).thenReturn("crm-protected/contract");
        when(fileApi.getFileByUrl("/files/public.pdf"))
                .thenReturn(managedFile().setPath("crm-protected/contract/8/public.pdf"));
        CrmContractAttachmentCreateReqVO req = new CrmContractAttachmentCreateReqVO().setContractId(7L)
                .setCategory(1).setFileName("public.pdf").setFileUrl("/files/public.pdf");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createAttachment(req, 1L));

        assertEquals(CONTRACT_ATTACHMENT_FILE_NOT_PROTECTED.getCode(), ex.getCode());
        verify(attachmentMapper, never()).insert(any(CrmContractAttachmentDO.class));
    }

    @Test
    void attachmentDownloadRequiresContractBindingAndReadsManagedFile() {
        when(contractMapper.selectById(7L)).thenReturn(contract());
        when(attachmentMapper.selectById(9L)).thenReturn(new CrmContractAttachmentDO().setId(9L)
                .setContractId(7L).setFileName("合同.pdf").setFileUrl("/files/a"));
        when(fileApi.getFileByUrl("/files/a")).thenReturn(managedFile());
        when(fileApi.getFileContent(4L, "crm-protected/contract/7/20260715/a.pdf"))
                .thenReturn(new byte[]{1, 2, 3});

        CrmContractLifecycleService.AttachmentDownload result = service.getAttachmentDownload(7L, 9L);

        assertEquals("合同.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        assertArrayEquals(new byte[]{1, 2, 3}, result.content());
    }

    @Test
    void attachmentDownloadRejectsAttachmentFromAnotherContract() {
        when(contractMapper.selectById(7L)).thenReturn(contract());
        when(attachmentMapper.selectById(9L)).thenReturn(new CrmContractAttachmentDO().setId(9L)
                .setContractId(8L).setFileUrl("/files/a"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getAttachmentDownload(7L, 9L));

        assertEquals(CONTRACT_ATTACHMENT_NOT_BELONGS.getCode(), ex.getCode());
        verifyNoInteractions(fileApi);
    }

    @Test
    void attachmentUploadUsesConfiguredProtectedDirectory() {
        when(contractMapper.selectById(7L)).thenReturn(contract());
        when(securityProperties.getProtectedFileDirectory()).thenReturn("crm-protected/contract");
        when(fileApi.createFile(any(), any(), any(), any())).thenReturn("/files/a");

        assertEquals("/files/a", service.uploadAttachmentFile(
                7L, new byte[]{1}, "合同.pdf", "application/pdf"));
        verify(fileApi).createFile(new byte[]{1}, "合同.pdf", "crm-protected/contract/7", "application/pdf");
    }

    @Test
    void deleteAttachmentRejectsImmutableSignedCopy() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(attachmentMapper.selectById(9L)).thenReturn(new CrmContractAttachmentDO()
                .setId(9L).setContractId(7L).setImmutable(true));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.deleteAttachment(7L, 9L));
        assertEquals(CONTRACT_ATTACHMENT_IMMUTABLE.getCode(), ex.getCode());
        verify(attachmentMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void signRequiresApprovedContract() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract().setAuditStatus(0));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.sign(signRequest(), 1L));
        assertEquals(CONTRACT_SIGN_REQUIRES_APPROVED.getCode(), ex.getCode());
    }

    @Test
    void signRejectsMethodUnsupportedByProvider() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(provider.getSupportedMethods()).thenReturn(Set.of(CrmContractLifecycleEnums.SIGN_OFFLINE));
        CrmContractSignReqVO request = signRequest().setMethod(CrmContractLifecycleEnums.SIGN_ELECTRONIC);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.sign(request, 1L));

        assertEquals(CONTRACT_SIGN_METHOD_UNSUPPORTED.getCode(), ex.getCode());
        verify(provider, never()).sign(any(), any());
    }

    @Test
    void signLocksCopyPersistsProviderAndTrajectory() {
        CrmContractDO contract = contract();
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract);
        when(provider.getSupportedMethods()).thenReturn(Set.of(CrmContractLifecycleEnums.SIGN_OFFLINE));
        when(attachmentMapper.selectById(9L)).thenReturn(new CrmContractAttachmentDO().setId(9L)
                .setContractId(7L).setCategory(CrmContractLifecycleEnums.ATTACHMENT_SIGNED_COPY));
        when(productMapper.selectListByContractId(7L)).thenReturn(Collections.emptyList());
        when(provider.sign(contract, "contract:sign:7:v1"))
                .thenReturn(new CrmContractSignProvider.Result("local-record", "contract:sign:7:v1", null));
        doAnswer(invocation -> {
            ((CrmContractSigningDO) invocation.getArgument(0)).setId(11L);
            return 1;
        })
                .when(signingMapper).insert(any(CrmContractSigningDO.class));

        assertEquals(11L, service.sign(signRequest(), 1L));
        verify(signingMapper).insert(ArgumentMatchers.<CrmContractSigningDO>argThat(
                signing -> signing.getStatus() == 10 && "local-record".equals(signing.getProviderCode())));
        verify(attachmentMapper).updateById(ArgumentMatchers.<CrmContractAttachmentDO>argThat(
                attachment -> Boolean.TRUE.equals(attachment.getImmutable())));
        verify(changeMapper).insert(ArgumentMatchers.<CrmContractChangeRecordDO>argThat(
                record -> record.getActionType() == CrmContractLifecycleEnums.ACTION_SIGN));
    }

    @Test
    void signRetryIsIdempotentOnlyForSameCommand() {
        CrmContractSignReqVO req = signRequest();
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(new CrmContractSigningDO().setId(11L).setStatus(10)
                .setMethod(req.getMethod()).setSignedTime(req.getSignedTime()).setSignedAttachmentId(9L).setHandlerUserId(1L));
        assertEquals(11L, service.sign(req, 1L));
        req.setHandlerUserId(2L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.sign(req, 1L));
        assertEquals(CONTRACT_SIGN_ALREADY_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void voidRetryWithSameReasonIsIdempotent() {
        CrmContractSigningDO signing = new CrmContractSigningDO()
                .setId(11L)
                .setContractId(7L)
                .setStatus(CrmContractLifecycleEnums.SIGN_VOIDED)
                .setVoidReason("重复请求");
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(signing);

        service.voidSign(new CrmContractSignVoidReqVO().setContractId(7L).setReason("重复请求"), 1L);

        verify(provider, never()).voidSign(any(), any());
        verify(signingMapper, never()).updateById(any(CrmContractSigningDO.class));
        verify(changeMapper, never()).insert(any(CrmContractChangeRecordDO.class));
    }

    private static CrmContractDO contract() {
        return new CrmContractDO().setId(7L).setName("合同")
                .setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus());
    }

    private static CrmContractSignReqVO signRequest() {
        return new CrmContractSignReqVO().setContractId(7L)
                .setMethod(1).setSignedTime(LocalDateTime.of(2026, 7, 14, 10, 0))
                .setSignedAttachmentId(9L).setHandlerUserId(1L);
    }

    private static FileRespDTO managedFile() {
        return new FileRespDTO().setId(5L).setConfigId(4L).setName("a.pdf")
                .setPath("crm-protected/contract/7/20260715/a.pdf").setUrl("/files/a")
                .setType("application/pdf").setSize(12L);
    }
}
