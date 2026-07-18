import type { EChartsOption } from 'echarts'
import type { WorkOrderStatisticsTrend } from '@/api/crm/statistics/workorder'

export interface WorkOrderTrendLabels {
  created: string
  completed: string
}

/**
 * 每次返回新的 options 引用，确保 shallowRef 和 Echart 组件都能观察到统计结果变化。
 */
export const buildWorkOrderTrendOptions = (
  rows: WorkOrderStatisticsTrend[],
  labels: WorkOrderTrendLabels
): EChartsOption => {
  const times = rows.map((row) => row.time)
  const createdCounts = rows.map((row) => row.createdCount)
  const completedCounts = rows.map((row) => row.completedCount)
  return {
    tooltip: { trigger: 'axis' },
    legend: {},
    grid: { left: 30, right: 30, bottom: 20, containLabel: true },
    xAxis: { type: 'category', data: times },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: labels.created, type: 'line', data: createdCounts, smooth: true },
      { name: labels.completed, type: 'line', data: completedCounts, smooth: true }
    ]
  }
}
