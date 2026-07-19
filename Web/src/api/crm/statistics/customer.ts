import request from '@/config/axios'

export interface CrmStatisticsCustomerSummaryByDateRespVO {
  time: string
  customerCreateCount: number
  customerDealCount: number
  customerDealRate: number | string
}

export interface CrmStatisticsCustomerSummaryByUserRespVO {
  ownerUserName: string
  customerCreateCount: number
  customerDealCount: number
  customerDealRate: number | string
  contractPrice: number
  receivablePrice: number
  unreceivablePrice: number
  receivableRate: number | string
}

export interface CrmStatisticsFollowUpSummaryByDateRespVO {
  time: string
  followUpRecordCount: number
  followUpCustomerCount: number
}

export interface CrmStatisticsFollowUpSummaryByUserRespVO {
  ownerUserName: string
  followUpRecordCount: number
  followUpCustomerCount: number
}

export interface CrmStatisticsFollowUpSummaryByTypeRespVO {
  followUpType: string
  followUpRecordCount: number
}

export interface CrmStatisticsCustomerContractSummaryRespVO {
  customerName: string
  contractName: string
  totalPrice: number
  receivablePrice: number
  industryId: number
  source: number
  ownerUserId: number
  ownerUserName: string
  creator: string
  creatorUserName: string
  createTime: Date
  orderDate: Date
}

export interface CrmStatisticsCustomerDealTopRespVO {
  customerId: number
  customerName: string
  contractCount: number
  contractAmount: number | string
}

export interface CrmStatisticsPoolSummaryByDateRespVO {
  time: string
  customerPutCount: number
  customerTakeCount: number
}

export interface CrmStatisticsPoolSummaryByUserRespVO {
  ownerUserName: string
  customerPutCount: number
  customerTakeCount: number
}

export interface CrmStatisticsCustomerDealCycleByDateRespVO {
  time: string
  customerDealCycle: number
  negativeSampleCount: number
}

export interface CrmStatisticsCustomerDealCycleByUserRespVO {
  ownerUserName: string
  customerDealCycle: number
  customerDealCount: number
  negativeSampleCount: number
}

export interface CrmStatisticsCustomerDealCycleByAreaRespVO {
  areaName: string
  customerDealCycle: number
  customerDealCount: number
  negativeSampleCount: number
}

export interface CrmStatisticsCustomerDealCycleByProductRespVO {
  productName: string
  customerDealCycle: number
  customerDealCount: number
  negativeSampleCount: number
}

// 客户分析 API
export const StatisticsCustomerApi = {
  // 1.1 客户总量分析(按日期)
  getCustomerSummaryByDate: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-summary-by-date',
      params
    })
  },
  // 1.2 客户总量分析(按用户)
  getCustomerSummaryByUser: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-summary-by-user',
      params
    })
  },
  // 2.1 客户跟进次数分析(按日期)
  getFollowUpSummaryByDate: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-follow-up-summary-by-date',
      params
    })
  },
  // 2.2 客户跟进次数分析(按用户)
  getFollowUpSummaryByUser: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-follow-up-summary-by-user',
      params
    })
  },
  // 3.1 获取客户跟进方式统计数
  getFollowUpSummaryByType: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-follow-up-summary-by-type',
      params
    })
  },
  // 4.1 合同摘要信息(客户转化率页面)
  getContractSummary: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-contract-summary',
      params
    })
  },
  // 4.2 客户成交金额 TOP10
  getCustomerDealTop10: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-deal-top10',
      params
    })
  },
  // 5.1 获取客户公海分析(按日期)
  getPoolSummaryByDate: (param: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-pool-summary-by-date',
      params: param
    })
  },
  // 5.2 获取客户公海分析(按用户)
  getPoolSummaryByUser: (param: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-pool-summary-by-user',
      params: param
    })
  },
  // 6.1 获取客户成交周期(按日期)
  getCustomerDealCycleByDate: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-deal-cycle-by-date',
      params
    })
  },
  // 6.2 获取客户成交周期(按用户)
  getCustomerDealCycleByUser: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-deal-cycle-by-user',
      params
    })
  },
  // 6.2 获取客户成交周期(按用户)
  getCustomerDealCycleByArea: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-deal-cycle-by-area',
      params
    })
  },
  // 6.2 获取客户成交周期(按用户)
  getCustomerDealCycleByProduct: (params: any) => {
    return request.get({
      url: '/crm/statistics-customer/get-customer-deal-cycle-by-product',
      params
    })
  }
}
