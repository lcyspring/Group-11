import request from '@/config/axios'

export interface ContractAttachmentVO {
  id: number
  contractId: number
  amendmentId?: number
  contractVersion: number
  category: number
  fileName: string
  contentType?: string
  fileSize?: number
  sha256?: string
  immutable: boolean
  uploaderUserId: number
  createTime: number
}

export interface ContractSigningVO {
  id: number
  contractId: number
  contractVersion: number
  status: number
  method: number
  signedTime: number
  signedAttachmentId: number
  handlerUserId: number
  providerCode: string
  providerRequestId: string
  externalSigningId?: string
  voidReason?: string
  voidTime?: number
}

export interface ContractChangeRecordVO {
  id: number
  sequenceNo: number
  contractVersion: number
  actionType: number
  operatorUserId?: number
  reason?: string
  actionTime: number
}

export interface ContractLifecycleVO {
  signing?: ContractSigningVO
  attachments: ContractAttachmentVO[]
  changeRecords: ContractChangeRecordVO[]
  supportedSignMethods: number[]
}

export interface ContractAttachmentCreateReqVO {
  contractId: number
  amendmentId?: number
  category: number
  fileName: string
  fileUrl: string
}

export interface ContractSignReqVO {
  contractId: number
  method: number
  signedTime: number
  signedAttachmentId: number
  handlerUserId: number
}

export const getContractLifecycle = (contractId: number) =>
  request.get({ url: `/crm/contract-lifecycle/get?contractId=${contractId}` })

export const createContractAttachment = (data: ContractAttachmentCreateReqVO) =>
  request.post({ url: '/crm/contract-lifecycle/attachment', data })

export const uploadContractAttachment = (contractId: number, file: File, onUploadProgress?: Function) =>
  request.upload({
    url: '/crm/contract-lifecycle/attachment/upload',
    data: { contractId, file },
    onUploadProgress
  })

export const downloadContractAttachment = (contractId: number, attachmentId: number) =>
  request.download({
    url: '/crm/contract-lifecycle/attachment/download',
    params: { contractId, attachmentId }
  })

export const deleteContractAttachment = (contractId: number, attachmentId: number) =>
  request.delete({
    url: `/crm/contract-lifecycle/attachment?contractId=${contractId}&attachmentId=${attachmentId}`
  })

export const signContract = (data: ContractSignReqVO) =>
  request.put({ url: '/crm/contract-lifecycle/sign', data })

export const voidContractSign = (contractId: number, reason: string) =>
  request.put({ url: '/crm/contract-lifecycle/sign-void', data: { contractId, reason } })
