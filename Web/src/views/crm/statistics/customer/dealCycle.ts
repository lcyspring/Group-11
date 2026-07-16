export interface ProductDealCycle {
  productName?: string | null
  customerDealCycle: number
  customerDealCount: number
  negativeSampleCount: number
}

/** 保留后端成交周期，仅为空产品名提供展示兜底。 */
export const normalizeProductDealCycles = <T extends ProductDealCycle>(
  items: T[],
  unknownProductName: string
): Array<T & { productName: string }> =>
  items.map((item) => ({
    ...item,
    productName: item.productName ?? unknownProductName
  }))
