import request from '@/config/axios'

export interface ReceivableRefundVO {
  id?: number
  no?: string
  receivableId: number
  receivableNo?: string
  receivablePrice?: number
  customerId?: number
  customerName?: string
  contractId?: number
  contractNo?: string
  contractName?: string
  ownerUserId?: number
  ownerUserName?: string
  type: number
  /** Epoch milliseconds, matching the Server-wide LocalDateTime JSON contract. */
  refundTime: number
  amount: number
  reason: string
  remark?: string
  processInstanceId?: string
  auditStatus?: number
  creatorName?: string
  createTime?: number
  updateTime?: number
}

export interface ReceivableRefundSaveReqVO {
  id?: number
  receivableId: number
  type: number
  refundTime: number
  amount: number
  reason: string
  remark?: string
}

export interface RefundSourceSummaryVO {
  receivableId: number
  receivableNo: string
  receivableAmount: number
  reservedRefundAmount: number
  remainingRefundableAmount: number
}

export interface RefundActionVO {
  id: number
  actionType: number
  fromStatus?: number
  toStatus?: number
  operatorUserId?: number
  operatorUserName?: string
  actionTime: number
  processInstanceId?: string
  remark?: string
}

export const getRefundPage = (params: any) =>
  request.get({ url: '/crm/receivable-refund/page', params })
export const getRefund = (id: number) =>
  request.get<ReceivableRefundVO>({ url: '/crm/receivable-refund/get', params: { id } })
export const createRefund = (data: ReceivableRefundSaveReqVO) =>
  request.post<number>({ url: '/crm/receivable-refund/create', data })
export const updateRefund = (data: ReceivableRefundSaveReqVO) =>
  request.put({ url: '/crm/receivable-refund/update', data })
export const deleteRefund = (id: number) =>
  request.delete({ url: '/crm/receivable-refund/delete', params: { id } })
export const submitRefund = (id: number) =>
  request.put({ url: '/crm/receivable-refund/submit', params: { id } })
export const getSourceSummary = (receivableId: number, excludeRefundId?: number) =>
  request.get<RefundSourceSummaryVO>({
    url: '/crm/receivable-refund/source-summary',
    params: { receivableId, excludeRefundId }
  })
export const getActionRecords = (refundId: number) =>
  request.get<RefundActionVO[]>({ url: '/crm/receivable-refund/action-records', params: { refundId } })
