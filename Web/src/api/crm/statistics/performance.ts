import request from '@/config/axios'

export interface StatisticsPerformanceRespVO {
  time: string
  currentMonthCount: number | string
  lastMonthCount: number | string
  lastYearCount: number | string
  monthOnMonthRate: number | string | null
  yearOnYearRate: number | string | null
}

export interface StatisticsTargetCompletionRespVO {
  time: string
  targetValue: number | string
  actualValue: number | string
  completionRate?: number | string
}

export interface StatisticsTargetCompletionSummaryRespVO {
  targetType: number
  annualTarget: number | string
  annualActual: number | string
  annualCompletionRate?: number | string
  monthlyList: StatisticsTargetCompletionRespVO[]
}

export interface PerformanceTargetRespVO {
  scopeType: number
  scopeId: number
  targetYear: number
  targetType: number
  monthlyTargets: string[]
  quarterlyTargets: string[]
  annualTarget: string
}

export interface PerformanceTargetSaveReqVO {
  scopeType: number
  scopeId: number
  targetYear: number
  targetType: number
  monthlyTargets: string[]
}

// 排行 API
export const StatisticsPerformanceApi = {
  // 员工获得合同金额统计
  getContractPricePerformance: (params: any) => {
    return request.get({
      url: '/crm/statistics-performance/get-contract-price-performance',
      params
    })
  },
  // 员工获得回款统计
  getReceivablePricePerformance: (params: any) => {
    return request.get({
      url: '/crm/statistics-performance/get-receivable-price-performance',
      params
    })
  },
  //员工获得签约合同数量统计
  getContractCountPerformance: (params: any) => {
    return request.get({
      url: '/crm/statistics-performance/get-contract-count-performance',
      params
    })
  },
  // 月度业绩目标、实际值和完成率
  getTargetCompletion: (params: any) => {
    return request.get({
      url: '/crm/statistics-performance/get-target-completion',
      params
    })
  }
}

export const PerformanceTargetApi = {
  getList: (params: { scopeType: number; scopeId: number; targetYear: number }) =>
    request.get({ url: '/crm/performance-target/list', params }),
  save: (data: PerformanceTargetSaveReqVO) =>
    request.put({ url: '/crm/performance-target/save', data }),
  delete: (params: {
    scopeType: number
    scopeId: number
    targetYear: number
    targetType: number
  }) => request.delete({ url: '/crm/performance-target/delete', params })
}
