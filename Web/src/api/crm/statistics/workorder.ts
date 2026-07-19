import request from '@/config/axios'

export interface WorkOrderStatisticsSummary {
  totalCount: number
  pendingCount: number
  processingCount: number
  completedCount: number
  returnedCount: number
  completionRate: string
}

export interface WorkOrderStatisticsTrend {
  time: string
  createdCount: number
  completedCount: number
}

export interface WorkOrderStatisticsStatus { status: number; count: number }
export interface WorkOrderStatisticsType { type: number; count: number }
export interface WorkOrderStatisticsHandler { handlerUserId: number; handlerUserName?: string; count: number }

export const WorkOrderStatisticsApi = {
  getSummary: (params: any) => request.get({ url: '/crm/statistics-work-order/summary', params }),
  getTrend: (params: any) => request.get({ url: '/crm/statistics-work-order/trend', params }),
  getByStatus: (params: any) => request.get({ url: '/crm/statistics-work-order/by-status', params }),
  getByType: (params: any) => request.get({ url: '/crm/statistics-work-order/by-type', params }),
  getByHandler: (params: any) => request.get({ url: '/crm/statistics-work-order/by-handler', params })
}
