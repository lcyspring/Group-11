import request from '@/config/axios'

export interface QuotePolicyVO {
  version: string
  baseCurrency: string
  defaultCurrency: string
  exchangeRatesToBase: Record<string, number>
  allowedTaxRates: number[]
  defaultTaxRate: number
  amountScale: number
  maxVersionsPerBusiness: number
}

export interface QuoteItemVO {
  id: number
  productId: number
  productNameSnapshot: string
  productNoSnapshot: string
  productUnitSnapshot?: number
  productCategoryIdSnapshot?: number
  productVersionSnapshot: number
  listPrice: number
  businessPrice: number
  count: number
  taxRatePercent: number
  lineSubtotal: number
  lineDiscountAmount: number
  netAmount: number
  taxAmount: number
  grossAmount: number
}

export interface QuoteActionVO {
  id: number
  actionType: number
  fromStatus?: number
  toStatus: number
  operatorUserId?: number
  remark: string
  createTime: Date
}

export interface QuoteVO {
  id: number
  businessId: number
  versionNo: number
  status: number
  sourceQuoteId?: number
  currencyCode: string
  baseCurrencyCode: string
  exchangeRateToBase: number
  discountPercent: number
  subtotal: number
  discountAmount: number
  netAmount: number
  taxAmount: number
  grossAmount: number
  baseGrossAmount: number
  lockedBy?: number
  lockedAt?: Date
  createTime: Date
  items: QuoteItemVO[]
  actions: QuoteActionVO[]
}

export const getQuotePolicy = () => request.get<QuotePolicyVO>({ url: '/crm/business-quote/policy' })
export const getCurrentQuote = (businessId: number) =>
  request.get<QuoteVO>({ url: '/crm/business-quote/current', params: { businessId } })
export const getQuoteVersions = (businessId: number) =>
  request.get<QuoteVO[]>({ url: '/crm/business-quote/versions', params: { businessId } })
export const lockQuote = (businessId: number, remark: string) =>
  request.put<QuoteVO>({ url: '/crm/business-quote/lock', data: { businessId, remark } })
export const reopenQuote = (businessId: number, remark: string) =>
  request.put<QuoteVO>({ url: '/crm/business-quote/reopen', data: { businessId, remark } })
