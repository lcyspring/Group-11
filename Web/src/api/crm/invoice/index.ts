import request from '@/config/axios'

export interface InvoiceDraftFields {
  handlerUserId: number
  type: number
  amount: number
  title: string
  taxNo?: string
  registeredAddress?: string
  registeredPhone?: string
  bankName?: string
  bankAccount?: string
  email?: string
  content: string
  remark?: string
}

export interface InvoiceVO extends InvoiceDraftFields {
  id?: number
  no?: string
  contractId: number
  contractNo?: string
  contractName?: string
  customerId?: number
  customerName?: string
  ownerUserId?: number
  ownerUserName?: string
  handlerUserName?: string
  direction?: number
  originalInvoiceId?: number
  originalInvoiceNo?: string
  status?: number
  redAmount?: number
  invoiceNo?: string
  /** Epoch milliseconds. The Server-wide LocalDateTime JSON contract does not accept formatted date strings. */
  invoiceDate?: number
  externalProvider?: string
  externalRequestId?: string
  externalInvoiceId?: string
  issueRemark?: string
  createTime?: number
  updateTime?: number
  actionRecords?: InvoiceActionRecordVO[]
}

export interface InvoiceCreateReqVO extends InvoiceDraftFields { contractId: number }
export interface InvoiceUpdateReqVO extends InvoiceDraftFields { id: number }

export interface InvoiceActionRecordVO {
  id: number
  actionType: number
  fromStatus?: number
  toStatus?: number
  operatorUserId: number
  operatorUserName?: string
  actionTime: number
  providerRequestId?: string
  remark?: string
}

export interface InvoiceSummaryVO {
  contractAmount: number
  blueAmount: number
  redAmount: number
  netAmount: number
  availableAmount: number
}

export interface InvoiceIssueReqVO {
  id: number
  invoiceNo: string
  invoiceDate: number
  handlerUserId: number
  remark?: string
}

export interface InvoiceRedFlushReqVO {
  originalInvoiceId: number
  amount: number
  invoiceNo: string
  invoiceDate: number
  handlerUserId: number
  reason: string
}

export const getInvoicePage = async (params: any) =>
  await request.get({ url: '/crm/invoice/page', params })
export const getInvoice = async (id: number) =>
  await request.get({ url: `/crm/invoice/get?id=${id}` })
export const createInvoice = async (data: InvoiceCreateReqVO) =>
  await request.post({ url: '/crm/invoice/create', data })
export const updateInvoice = async (data: InvoiceUpdateReqVO) =>
  await request.put({ url: '/crm/invoice/update', data })
export const deleteInvoice = async (id: number) =>
  await request.delete({ url: `/crm/invoice/delete?id=${id}` })
export const issueInvoice = async (data: InvoiceIssueReqVO) =>
  await request.put({ url: '/crm/invoice/issue', data })
export const redFlushInvoice = async (data: InvoiceRedFlushReqVO) =>
  await request.post({ url: '/crm/invoice/red-flush', data })
export const voidInvoice = async (id: number, reason: string) =>
  await request.put({ url: '/crm/invoice/void', data: { id, reason } })
export const getContractSummary = async (contractId: number) =>
  await request.get({ url: `/crm/invoice/contract-summary?contractId=${contractId}` })
export const exportInvoice = async (params: any) =>
  await request.download({ url: '/crm/invoice/export-excel', params })
