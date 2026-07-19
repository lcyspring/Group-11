package com.meession.etm.module.crm.service.contract;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractAttachmentCreateReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignVoidReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAmendmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAttachmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractChangeRecordDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAttachmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAmendmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractChangeRecordMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractSigningMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.contract.CrmContractSignProvider;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.framework.security.CrmSecurityProperties;
import com.meession.etm.module.infra.api.file.FileApi;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmContractLifecycleServiceImpl implements CrmContractLifecycleService {

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractProductMapper productMapper;
    @Resource
    private CrmContractAttachmentMapper attachmentMapper;
    @Resource
    private CrmContractAmendmentMapper amendmentMapper;
    @Resource
    private CrmContractSigningMapper signingMapper;
    @Resource
    private CrmContractChangeRecordMapper changeMapper;
    @Resource
    private CrmContractSignProvider signProvider;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private FileApi fileApi;
    @Resource
    private CrmSecurityProperties securityProperties;

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public String uploadAttachmentFile(Long contractId, byte[] content, String fileName, String contentType) {
        requireContract(contractId);
        return fileApi.createFile(content, fileName, getContractProtectedDirectory(contractId), contentType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#req.contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createAttachment(CrmContractAttachmentCreateReqVO req, Long userId) {
        requireContractForUpdate(req.getContractId());
        FileRespDTO file = getProtectedManagedFile(req.getFileUrl(), req.getContractId());
        Integer contractVersion = currentVersion(req.getContractId());
        if (ObjUtil.equal(req.getCategory(), CrmContractLifecycleEnums.ATTACHMENT_AMENDMENT)) {
            CrmContractAmendmentDO amendment = req.getAmendmentId() == null
                    ? null : amendmentMapper.selectByIdForUpdate(req.getAmendmentId());
            if (amendment == null || ObjUtil.notEqual(amendment.getContractId(), req.getContractId())
                    || !List.of(CrmAuditStatusEnum.DRAFT.getStatus(), CrmAuditStatusEnum.REJECT.getStatus(),
                    CrmAuditStatusEnum.CANCEL.getStatus()).contains(amendment.getAuditStatus())) {
                throw exception(CONTRACT_AMENDMENT_ATTACHMENT_INVALID);
            }
            contractVersion = amendment.getTargetVersion();
        } else if (req.getAmendmentId() != null) {
            throw exception(CONTRACT_AMENDMENT_ATTACHMENT_INVALID);
        }
        CrmContractAttachmentDO item = new CrmContractAttachmentDO().setContractId(req.getContractId())
                .setAmendmentId(req.getAmendmentId()).setContractVersion(contractVersion).setCategory(req.getCategory())
                .setFileName(req.getFileName()).setFileUrl(file.getUrl()).setContentType(file.getType())
                .setFileSize(file.getSize()).setSha256(req.getSha256() == null ? null
                        : req.getSha256().toLowerCase(Locale.ROOT))
                .setImmutable(false).setUploaderUserId(userId);
        attachmentMapper.insert(item);
        return item.getId();
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public AttachmentDownload getAttachmentDownload(Long contractId, Long attachmentId) {
        requireContract(contractId);
        CrmContractAttachmentDO attachment = requireAttachment(contractId, attachmentId);
        FileRespDTO file = fileApi.getFileByUrl(attachment.getFileUrl());
        return new AttachmentDownload(attachment.getFileName(),
                StrUtil.blankToDefault(attachment.getContentType(), file.getType()),
                fileApi.getFileContent(file.getConfigId(), file.getPath()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public void deleteAttachment(Long contractId, Long attachmentId) {
        requireContractForUpdate(contractId);
        CrmContractAttachmentDO item = requireAttachment(contractId, attachmentId);
        if (Boolean.TRUE.equals(item.getImmutable())) {
            throw exception(CONTRACT_ATTACHMENT_IMMUTABLE);
        }
        if (item.getAmendmentId() != null) {
            CrmContractAmendmentDO amendment = amendmentMapper.selectByIdForUpdate(item.getAmendmentId());
            if (amendment == null || ObjUtil.notEqual(amendment.getContractId(), contractId)
                    || ObjUtil.equal(amendment.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())
                    || ObjUtil.equal(amendment.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
                throw exception(CONTRACT_AMENDMENT_ATTACHMENT_INVALID);
            }
        }
        attachmentMapper.deleteById(attachmentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#req.contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long sign(CrmContractSignReqVO req, Long userId) {
        CrmContractDO contract = requireContractForUpdate(req.getContractId());
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
            throw exception(CONTRACT_SIGN_REQUIRES_APPROVED);
        }
        CrmContractSigningDO existing = signingMapper.selectByContractId(req.getContractId());
        if (existing != null) {
            if (ObjUtil.equal(existing.getStatus(), CrmContractLifecycleEnums.SIGNED)
                    && ObjUtil.equal(existing.getMethod(), req.getMethod())
                    && ObjUtil.equal(existing.getSignedTime(), req.getSignedTime())
                    && ObjUtil.equal(existing.getSignedAttachmentId(), req.getSignedAttachmentId())
                    && ObjUtil.equal(existing.getHandlerUserId(), req.getHandlerUserId())) {
                return existing.getId();
            }
            throw exception(CONTRACT_SIGN_ALREADY_EXISTS);
        }
        if (!signProvider.getSupportedMethods().contains(req.getMethod())) {
            throw exception(CONTRACT_SIGN_METHOD_UNSUPPORTED);
        }
        CrmContractAttachmentDO attachment = attachmentMapper.selectById(req.getSignedAttachmentId());
        if (attachment == null) {
            throw exception(CONTRACT_ATTACHMENT_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(attachment.getContractId(), contract.getId())) {
            throw exception(CONTRACT_ATTACHMENT_NOT_BELONGS);
        }
        if (ObjUtil.notEqual(attachment.getCategory(), CrmContractLifecycleEnums.ATTACHMENT_SIGNED_COPY)) {
            throw exception(CONTRACT_ATTACHMENT_SIGNED_COPY_REQUIRED);
        }
        adminUserApi.validateUser(req.getHandlerUserId());
        int version = currentVersion(contract.getId());
        String requestId = "contract:sign:" + contract.getId() + ":v" + version;
        CrmContractSignProvider.Result result = signProvider.sign(contract, requestId);
        validateProvider(result, requestId);
        CrmContractSigningDO signing = new CrmContractSigningDO().setContractId(contract.getId())
                .setContractVersion(version).setStatus(CrmContractLifecycleEnums.SIGNED).setMethod(req.getMethod())
                .setSignedTime(req.getSignedTime()).setSignedAttachmentId(attachment.getId())
                .setHandlerUserId(req.getHandlerUserId()).setProviderCode(result.providerCode())
                .setProviderRequestId(result.requestId()).setExternalSigningId(result.externalSigningId());
        signingMapper.insert(signing);
        attachmentMapper.updateById(new CrmContractAttachmentDO().setId(attachment.getId()).setImmutable(true));
        recordChange(contract.getId(), CrmContractLifecycleEnums.ACTION_SIGN, version, userId, "合同已签署");
        return signing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#req.contractId",
            level = CrmPermissionLevelEnum.OWNER)
    public void voidSign(CrmContractSignVoidReqVO req, Long userId) {
        CrmContractDO contract = requireContractForUpdate(req.getContractId());
        CrmContractSigningDO signing = signingMapper.selectByContractId(contract.getId());
        if (signing == null) {
            throw exception(CONTRACT_SIGN_NOT_EXISTS);
        }
        if (ObjUtil.equal(signing.getStatus(), CrmContractLifecycleEnums.SIGN_VOIDED)
                && StrUtil.equals(signing.getVoidReason(), req.getReason())) {
            return;
        }
        if (ObjUtil.notEqual(signing.getStatus(), CrmContractLifecycleEnums.SIGNED)) {
            throw exception(CONTRACT_SIGN_STATUS_INVALID);
        }
        String requestId = "contract:sign:void:" + contract.getId() + ":v" + signing.getContractVersion();
        validateProvider(signProvider.voidSign(contract, requestId), requestId);
        signingMapper.updateById(new CrmContractSigningDO().setId(signing.getId())
                .setStatus(CrmContractLifecycleEnums.SIGN_VOIDED).setVoidReason(req.getReason()).setVoidTime(LocalDateTime.now()));
        recordChange(contract.getId(), CrmContractLifecycleEnums.ACTION_VOID_SIGN,
                signing.getContractVersion(), userId, req.getReason());
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public CrmContractSigningDO getSigning(Long contractId) {
        requireContract(contractId);
        return signingMapper.selectByContractId(contractId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmContractAttachmentDO> getAttachments(Long contractId) {
        requireContract(contractId);
        return attachmentMapper.selectListByContractId(contractId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#contractId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmContractChangeRecordDO> getChangeRecords(Long contractId) {
        requireContract(contractId);
        return changeMapper.selectListByContractId(contractId);
    }

    @Override
    public List<Integer> getSupportedSignMethods() {
        return signProvider.getSupportedMethods().stream().sorted().toList();
    }

    @Override
    public int getCurrentVersion(Long contractId) {
        return currentVersion(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordChange(Long contractId, Integer actionType, Integer contractVersion, Long userId, String reason) {
        CrmContractDO contract = requireContractForUpdate(contractId);
        CrmContractChangeRecordDO latest = changeMapper.selectLatest(contractId);
        int sequence = latest == null ? 1 : latest.getSequenceNo() + 1;
        changeMapper.insert(new CrmContractChangeRecordDO().setContractId(contractId).setSequenceNo(sequence)
                .setContractVersion(contractVersion).setActionType(actionType).setOperatorUserId(userId).setReason(reason)
                .setContractSnapshot(JsonUtils.toJsonString(contract))
                .setProductSnapshot(JsonUtils.toJsonString(productMapper.selectListByContractId(contractId)))
                .setActionTime(LocalDateTime.now()));
    }

    private int currentVersion(Long contractId) {
        CrmContractChangeRecordDO latest = changeMapper.selectLatestEffective(contractId);
        return latest == null ? 1 : latest.getContractVersion();
    }

    private CrmContractDO requireContract(Long id) {
        CrmContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private CrmContractDO requireContractForUpdate(Long id) {
        CrmContractDO contract = contractMapper.selectByIdForUpdate(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private CrmContractAttachmentDO requireAttachment(Long contractId, Long attachmentId) {
        CrmContractAttachmentDO item = attachmentMapper.selectById(attachmentId);
        if (item == null) {
            throw exception(CONTRACT_ATTACHMENT_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(item.getContractId(), contractId)) {
            throw exception(CONTRACT_ATTACHMENT_NOT_BELONGS);
        }
        return item;
    }

    private FileRespDTO getProtectedManagedFile(String fileUrl, Long contractId) {
        FileRespDTO file;
        try {
            file = fileApi.getFileByUrl(fileUrl);
        } catch (RuntimeException ex) {
            throw exception(CONTRACT_ATTACHMENT_FILE_NOT_MANAGED);
        }
        String protectedDirectory = getContractProtectedDirectory(contractId);
        if (!StrUtil.startWith(file.getPath(), protectedDirectory + "/")) {
            throw exception(CONTRACT_ATTACHMENT_FILE_NOT_PROTECTED);
        }
        return file;
    }

    private String getContractProtectedDirectory(Long contractId) {
        return StrUtil.removeSuffix(securityProperties.getProtectedFileDirectory(), "/") + "/" + contractId;
    }

    private void validateProvider(CrmContractSignProvider.Result result, String requestId) {
        if (result == null || StrUtil.isBlank(result.providerCode())
                || !StrUtil.equals(result.requestId(), requestId)) {
            throw exception(CONTRACT_SIGN_PROVIDER_INVALID);
        }
    }
}
