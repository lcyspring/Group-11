/**
 * 增长率由服务端使用 BigDecimal 统一计算；前端只负责空值展示，避免重复口径和浮点误差。
 */
export const formatGrowthRate = (
  rate: number | string | null | undefined
): number | string => rate ?? '--'
