import request from '@/config/axios'

export interface WorkReportVO {
  id?: number
  authorUserId?: number
  authorUserName?: string
  reportType: number
  reportDate: string
  periodStart?: string
  periodEnd?: string
  title: string
  completedContent: string
  pendingContent?: string
  nextPlan: string
  issues?: string
  receiverUserIds: number[]
  receiverUserNames?: string[]
  attachmentUrls?: string[]
  status?: number
  submitTime?: string
  createTime?: string
}

export const getWorkReportPage = (params: any) => request.get({ url: '/crm/work-report/page', params })
export const getWorkReport = (id: number): Promise<WorkReportVO> => request.get({ url: '/crm/work-report/get', params: { id } })
export const createWorkReport = (data: WorkReportVO) => request.post({ url: '/crm/work-report/create', data })
export const updateWorkReport = (data: WorkReportVO) => request.put({ url: '/crm/work-report/update', data })
export const submitWorkReport = (id: number) => request.put({ url: '/crm/work-report/submit', params: { id } })
export const deleteWorkReport = (id: number) => request.delete({ url: '/crm/work-report/delete', params: { id } })
