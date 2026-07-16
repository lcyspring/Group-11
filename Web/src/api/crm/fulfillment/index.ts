import request from '@/config/axios'

export interface ErpMappingSaveReqVO {
  crmId: number
  erpId: number
  remark?: string
}

export interface CustomerMappingVO {
  id: number
  crmCustomerId: number
  crmCustomerName?: string
  erpCustomerId: number
  erpCustomerName?: string
  remark?: string
  createTime: Date
  updateTime: Date
}

export interface ProductMappingVO {
  id: number
  crmProductId: number
  crmProductName?: string
  crmProductNo?: string
  erpProductId: number
  erpProductName?: string
  erpProductBarCode?: string
  remark?: string
  createTime: Date
  updateTime: Date
}

export interface ContractFulfillmentVO {
  enabled: boolean
  policyVersion: string
  currencyMode: string
  erpCurrencyCode: string
  allowedSourceCurrencies: string[]
  eligible: boolean
  sourceInvalidated: boolean
  blockers: string[]
  contractId: number
  contractVersion?: number
  crmCustomerId: number
  crmCustomerName?: string
  erpCustomerId?: number
  erpCustomerName?: string
  productMappings: Array<{
    crmProductId: number
    crmProductName?: string
    crmProductNo?: string
    erpProductId?: number
    erpProductName?: string
    mapped: boolean
  }>
  record?: {
    id: number
    status: number
    requestId: string
    requestHash: string
    attemptCount: number
    erpOrderId?: number
    erpOrderNo?: string
    erpOrderStatus?: number
    sourceCurrencyCode: string
    erpCurrencyCode: string
    exchangeRate: number
    sourceGrossAmount: number
    erpTotalAmount?: number
    totalCount?: number
    outCount?: number
    returnCount?: number
    lastErrorCode?: string
    lastErrorMessage?: string
    lastAttemptTime: Date
    completedTime?: Date
    lastSyncTime?: Date
  }
}

export const getCustomerMappingPage = (params: any) =>
  request.get({ url: '/crm/erp-mapping/customer/page', params })
export const getProductMappingPage = (params: any) =>
  request.get({ url: '/crm/erp-mapping/product/page', params })
export const saveCustomerMapping = (data: ErpMappingSaveReqVO) =>
  request.post({ url: '/crm/erp-mapping/customer/save', data })
export const saveProductMapping = (data: ErpMappingSaveReqVO) =>
  request.post({ url: '/crm/erp-mapping/product/save', data })
export const deleteCustomerMapping = (id: number) =>
  request.delete({ url: '/crm/erp-mapping/customer/delete', params: { id } })
export const deleteProductMapping = (id: number) =>
  request.delete({ url: '/crm/erp-mapping/product/delete', params: { id } })

export const getContractFulfillment = (contractId: number) =>
  request.get<ContractFulfillmentVO>({ url: '/crm/contract-fulfillment/get', params: { contractId } })
export const createOrRetryContractFulfillment = (contractId: number) =>
  request.post<ContractFulfillmentVO>({
    url: '/crm/contract-fulfillment/create-or-retry',
    params: { contractId }
  })
export const refreshContractFulfillment = (contractId: number) =>
  request.post<ContractFulfillmentVO>({
    url: '/crm/contract-fulfillment/refresh',
    params: { contractId }
  })
