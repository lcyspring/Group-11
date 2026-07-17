import request from '@/config/axios'

export interface WorkOrderStatisticsVO {
  totalCount: number
  todayNewCount: number
  todayCompletedCount: number
  statusDistribution: Record<string, number>
  priorityDistribution: Record<string, number>
  typeDistribution: Record<string, number>
}

export interface WorkOrderEfficiencyVO {
  avgProcessingHours: number
  medianProcessingHours: number
  minProcessingHours: number
  maxProcessingHours: number
  completedCount: number
  onTimeCount: number
  delayedCount: number
  onTimeRate: number
  avgProcessingHoursByHandler: Record<string, number>
  avgProcessingHoursByPriority: Record<string, number>
}

export interface WorkOrderTrendVO {
  dailyTrends: TrendItem[]
}

export interface TrendItem {
  date: string
  newCount: number
  processingCount: number
  completedCount: number
  returnedCount: number
}

// 获取工单统计
export const getWorkOrderStatistics = () => {
  return request.get({ url: '/workorder/work-order/statistics' })
}

// 获取工单效率分析
export const getWorkOrderEfficiencyAnalysis = () => {
  return request.get({ url: '/workorder/work-order/efficiency-analysis' })
}

// 获取工单趋势分析
export const getWorkOrderTrendAnalysis = (params?: { startTime?: string; endTime?: string }) => {
  return request.get({ url: '/workorder/work-order/trend-analysis', params })
}
