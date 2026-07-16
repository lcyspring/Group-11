import request from '@/config/axios'

export interface MarketingCampaignVO {
  id?: number; code: string; name: string; status?: number; ownerUserId: number
  startTime: Date | string; endTime: Date | string; budgetAmount?: number
  targetLeadCount?: number; targetCustomerCount?: number; description?: string; summary?: string
  relations?: Array<{ bizType: number; bizId: number }>
}
export interface MarketingBroadcastVO {
  id?: number; campaignId?: number; name: string; channel: number
  smsTemplateCode?: string; mailTemplateCode?: string; templateParams?: string
  scheduledAt?: Date | string; customerIds?: number[]; contactIds?: number[]
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
export const getCampaignPage = (params: any) => request.get({ url: '/crm/marketing/campaign/page', params })
export const getCampaign = (id: number) => request.get({ url: '/crm/marketing/campaign/get', params: { id } })
export const saveCampaign = (data: MarketingCampaignVO) => request.post({ url: '/crm/marketing/campaign/save', data })
export const startCampaign = (id: number) => request.put({ url: '/crm/marketing/campaign/start', params: { id } })
export const lockCampaign = (id: number) => request.put({ url: '/crm/marketing/campaign/lock', params: { id } })
export const completeCampaign = (data: any) => request.put({ url: '/crm/marketing/campaign/complete', data })
export const terminateCampaign = (data: any) => request.put({ url: '/crm/marketing/campaign/terminate', data })
export const getCompetitorPage = (params: any) => request.get({ url: '/crm/marketing/competitor/page', params })
export const saveCompetitor = (data: MarketingCompetitorVO) =>
  request.post({ url: '/crm/marketing/competitor/save', data })
export const deleteCompetitor = (id: number) => request.delete({ url: '/crm/marketing/competitor/delete', params: { id } })
export const saveConsent = (data: any) => request.post({ url: '/crm/marketing/outreach/consent/save', data })
export const saveBroadcast = (data: MarketingBroadcastVO) => request.post({ url: '/crm/marketing/outreach/broadcast/save', data })
export const getBroadcastPage = (params: any) => request.get({ url: '/crm/marketing/outreach/broadcast/page', params })
export const getRecipientPage = (params: any) => request.get({ url: '/crm/marketing/outreach/broadcast/recipients', params })
export const submitBroadcastReview = (id: number) => request.put({ url: '/crm/marketing/outreach/broadcast/submit-review', params: { id } })
export const approveBroadcast = (data: any) => request.put({ url: '/crm/marketing/outreach/broadcast/approve', data })
export const rejectBroadcast = (data: any) => request.put({ url: '/crm/marketing/outreach/broadcast/reject', data })
export const sendBroadcast = (id: number) => request.put({ url: '/crm/marketing/outreach/broadcast/send', params: { id } })
export const retryBroadcast = (id: number) => request.put({ url: '/crm/marketing/outreach/broadcast/retry', params: { id } })
export const saveCarePlan = (data: any) => request.post({ url: '/crm/marketing/care/plan/save', data })
export const getCarePlanPage = (params: any) => request.get({ url: '/crm/marketing/care/plan/page', params })
export const getCareRecordPage = (params: any) => request.get({ url: '/crm/marketing/care/record/page', params })
