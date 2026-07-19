package com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CrmContractLifecycleRespVO {
    private Signing signing;
    private List<Attachment> attachments;
    private List<ChangeRecord> changeRecords;
    private List<Integer> supportedSignMethods;

    @Data
    public static class Signing {
        private Long id;
        private Long contractId;
        private Integer contractVersion;
        private Integer status;
        private Integer method;
        private LocalDateTime signedTime;
        private Long signedAttachmentId;
        private Long handlerUserId;
        private String providerCode;
        private String providerRequestId;
        private String externalSigningId;
        private String voidReason;
        private LocalDateTime voidTime;
    }

    @Data
    public static class Attachment {
        private Long id;
        private Long contractId;
        private Long amendmentId;
        private Integer contractVersion;
        private Integer category;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String sha256;
        private Boolean immutable;
        private Long uploaderUserId;
        private LocalDateTime createTime;
    }

    @Data
    public static class ChangeRecord {
        private Long id;
        private Integer sequenceNo;
        private Integer contractVersion;
        private Integer actionType;
        private Long operatorUserId;
        private String reason;
        private LocalDateTime actionTime;
    }
}
