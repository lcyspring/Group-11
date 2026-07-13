/**
 * 计算同比/环比增长率。
 *
 * 基期为 0 或任一输入不是有限数值时，增长率没有可比较意义，返回 null；
 * 其余情况返回保留两位小数的数值，供 ECharts 和表格共同使用。
 */
export const calculateGrowthRate = (
  currentValue: number | string,
  previousValue: number | string
): number | null => {
  const current = Number(currentValue)
  const previous = Number(previousValue)
  if (!Number.isFinite(current) || !Number.isFinite(previous) || previous === 0) {
    return null
  }
  return Number((((current - previous) / previous) * 100).toFixed(2))
}

/** 表格中的空增长率统一显示为短横线。 */
export const formatGrowthRate = (
  currentValue: number | string,
  previousValue: number | string
): number | string => calculateGrowthRate(currentValue, previousValue) ?? '--'
