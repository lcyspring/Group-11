import request from '@/config/axios'

export interface MarketingCampaignVO {
  id?: number
  code: string
  name: string
  status?: number
  ownerUserId: number
  startTime: Date | string
  endTime: Date | string
  budgetAmount?: number
  targetLeadCount?: number
  targetCustomerCount?: number
  description?: string
  summary?: string
  ownerUserName?: string
  actualCostAmount?: number
  createTime?: Date | string
  updateTime?: Date | string
  relations?: Array<{ bizType: number; bizId: number }>
}
export interface MarketingBroadcastVO {
  id?: number
  campaignId?: number
  name: string
  channel: number
  smsTemplateCode?: string
  mailTemplateCode?: string
  templateParams?: string
  scheduledAt?: Date | string
  customerIds?: number[]
  contactIds?: number[]
  links?: MarketingLinkVO[]
  status?: number
  totalCount?: number
  validCount?: number
  suppressedCount?: number
  sentCount?: number
  failedCount?: number
  reviewerUserId?: number
  reviewedAt?: Date | string
  reviewComment?: string
  sentAt?: Date | string
  creatorUserId?: number
  createTime?: Date | string
  updateTime?: Date | string
}
export interface MarketingLinkVO {
  code: string
  name: string
  targetUrl: string
}
export interface MarketingRecipientVO {
  id: number
  broadcastId: number
  customerId: number
  contactId?: number
  channel: number
  mobile?: string
  email?: string
  status: number
  suppressedReason?: string
  providerLogId?: number
  failureReason?: string
  attemptCount?: number
  sentAt?: Date | string
  lastAttemptAt?: Date | string
  deliveryStatus?: number
  deliveredAt?: Date | string
  openedAt?: Date | string
}
export interface MarketingDeliverySummaryVO {
  broadcastId: number
  smsSentCount: number
  smsDeliveredCount: number
  smsFailedCount: number
  smsDeliveryRate: number
  emailSentCount: number
  emailAcceptedCount: number
  emailFailedCount: number
  emailOpenedCount: number
  emailOpenRate: number
  providerPendingCount: number
  unknownCount: number
  trackedRecipientCount: number
  uniqueClickCount: number
  totalClickCount: number
  uniqueClickRate: number
  links: Array<MarketingLinkVO & {
    linkId: number
    trackedRecipientCount: number
    uniqueClickCount: number
    totalClickCount: number
    uniqueClickRate: number
  }>
}
export interface MarketingTargetOptionsVO {
  customers: Array<{ id: number; name: string; mobile?: string; email?: string }>
  contacts: Array<{
    id: number
    customerId: number
    name: string
    mobile?: string
    email?: string
  }>
}
export interface MarketingCompetitorVO {
  id?: number
  name: string
  website?: string
  strengths?: string
  weaknesses?: string
  strategy?: string
  ownerUserId?: number
  ownerUserName?: string
  status: number
  remark?: string
  createTime?: Date | string
  updateTime?: Date | string
}
export interface CustomerCarePlanVO {
  id?: number
  code: string
  name: string
  ruleType: number
  eventMonthDay?: string
  followUpDays?: number
  channel: number
  smsTemplateCode?: string
  mailTemplateCode?: string
  enabled: boolean
  targetScope?: string
  createTime?: Date | string
  updateTime?: Date | string
}
export interface CustomerCareRecordVO {
  id: number
  planId: number
  planName?: string
  customerId: number
  customerName?: string
  contactId?: number
  contactName?: string
  eventDate: string
  channel: number
  status: number
  failureReason?: string
  providerLogId?: number
  sentAt?: Date | string
  createTime?: Date | string
}
export interface CustomerBirthdayVO {
  targetType: number
  customerId: number
  customerName?: string
  contactId?: number
  contactName?: string
  birthday: string
  nextBirthday: string
  daysUntil: number
  mobile?: string
  email?: string
}
export const getCampaignPage = (params: any) =>
  request.get({ url: '/crm/marketing/campaign/page', params })
export const getCampaign = (id: number) =>
  request.get({ url: '/crm/marketing/campaign/get', params: { id } })
export const saveCampaign = (data: MarketingCampaignVO) =>
  request.post({ url: '/crm/marketing/campaign/save', data })
export const deleteCampaign = (id: number) =>
  request.delete({ url: '/crm/marketing/campaign/delete', params: { id } })
export const startCampaign = (id: number) =>
  request.put({ url: '/crm/marketing/campaign/start', params: { id } })
export const lockCampaign = (id: number) =>
  request.put({ url: '/crm/marketing/campaign/lock', params: { id } })
export const completeCampaign = (data: any) =>
  request.put({ url: '/crm/marketing/campaign/complete', data })
export const terminateCampaign = (data: any) =>
  request.put({ url: '/crm/marketing/campaign/terminate', data })
export const getCompetitorPage = (params: any) =>
  request.get({ url: '/crm/marketing/competitor/page', params })
export const saveCompetitor = (data: MarketingCompetitorVO) =>
  request.post({ url: '/crm/marketing/competitor/save', data })
export const deleteCompetitor = (id: number) =>
  request.delete({ url: '/crm/marketing/competitor/delete', params: { id } })
export const saveConsent = (data: any) =>
  request.post({ url: '/crm/marketing/outreach/consent/save', data })
export const refreshBroadcastRecipients = (id: number) =>
  request.put({ url: '/crm/marketing/outreach/broadcast/refresh-recipients', params: { id } })
export const saveBroadcast = (data: MarketingBroadcastVO) =>
  request.post({ url: '/crm/marketing/outreach/broadcast/save', data })
export const getBroadcast = (id: number) =>
  request.get<MarketingBroadcastVO>({
    url: '/crm/marketing/outreach/broadcast/get',
    params: { id }
  })
export const deleteBroadcast = (id: number) =>
  request.delete({ url: '/crm/marketing/outreach/broadcast/delete', params: { id } })
export const getBroadcastTargetOptions = () =>
  request.get<MarketingTargetOptionsVO>({
    url: '/crm/marketing/outreach/broadcast/target-options'
  })
export const getBroadcastPage = (params: any) =>
  request.get({ url: '/crm/marketing/outreach/broadcast/page', params })
export const getRecipientPage = (params: any) =>
  request.get({ url: '/crm/marketing/outreach/broadcast/recipients', params })
export const getBroadcastDeliverySummary = (id: number) =>
  request.get<MarketingDeliverySummaryVO>({
    url: '/crm/marketing/outreach/broadcast/delivery-summary', params: { id }
  })
export const syncBroadcastDeliveryResults = (id: number) =>
  request.put<number>({ url: '/crm/marketing/outreach/broadcast/sync-results', params: { id } })
export const submitBroadcastReview = (id: number) =>
  request.put({ url: '/crm/marketing/outreach/broadcast/submit-review', params: { id } })
export const approveBroadcast = (data: any) =>
  request.put({ url: '/crm/marketing/outreach/broadcast/approve', data })
export const rejectBroadcast = (data: any) =>
  request.put({ url: '/crm/marketing/outreach/broadcast/reject', data })
export const sendBroadcast = (id: number) =>
  request.put({ url: '/crm/marketing/outreach/broadcast/send', params: { id } })
export const retryBroadcast = (id: number) =>
  request.put({ url: '/crm/marketing/outreach/broadcast/retry', params: { id } })
export const saveCarePlan = (data: CustomerCarePlanVO) =>
  request.post({ url: '/crm/marketing/care/plan/save', data })
export const getCarePlan = (id: number) =>
  request.get<CustomerCarePlanVO>({ url: '/crm/marketing/care/plan/get', params: { id } })
export const updateCarePlanStatus = (id: number, enabled: boolean) =>
  request.put({ url: '/crm/marketing/care/plan/status', data: { id, enabled } })
export const deleteCarePlan = (id: number) =>
  request.delete({ url: '/crm/marketing/care/plan/delete', params: { id } })
export const getCarePlanPage = (params: any) =>
  request.get({ url: '/crm/marketing/care/plan/page', params })
export const getCareRecordPage = (params: any) =>
  request.get({ url: '/crm/marketing/care/record/page', params })
export const getCustomerBirthdayPage = (params: any) =>
  request.get({ url: '/crm/marketing/care/birthday/page', params })
