import request from '@/config/axios'

export interface CustomerVisitVO {
  id?: number
  applicantUserId?: number
  customerId: number
  customerName?: string
  contactId?: number
  contactName?: string
  plannedStartTime: string | number
  plannedEndTime: string | number
  location: string
  purpose: string
  participantUserIds?: number[]
  attachmentUrls?: string[]
  auditStatus?: number
  processInstanceId?: string
  resultStatus?: number
  actualStartTime?: string | number
  actualEndTime?: string | number
  resultContent?: string
  nextContactTime?: string | number
  resultAttachmentUrls?: string[]
  followUpRecordId?: number
  startUserSelectAssignees?: Record<string, number[]>
}

export interface CustomerVisitResultVO {
  id: number
  actualStartTime: string | number
  actualEndTime: string | number
  resultContent: string
  nextContactTime: string | number
  resultAttachmentUrls?: string[]
}

export const createCustomerVisit = (data: CustomerVisitVO) =>
  request.post<number>({ url: '/crm/customer-visit/create', data })
export const getCustomerVisit = (id: number) =>
  request.get<CustomerVisitVO>({ url: '/crm/customer-visit/get', params: { id } })
export const getCustomerVisitPage = (params: PageParam) =>
  request.get({ url: '/crm/customer-visit/page', params })
export const recordCustomerVisitResult = (data: CustomerVisitResultVO) =>
  request.post<number>({ url: '/crm/customer-visit/result', data })
