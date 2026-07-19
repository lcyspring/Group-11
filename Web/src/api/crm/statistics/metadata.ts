import request from '@/config/axios'

export type CrmStatisticsScope =
  | 'customer'
  | 'funnel'
  | 'performance'
  | 'portrait'
  | 'rank'
  | 'workorder'

export interface CrmStatisticsMetricMetadata {
  code: string
  name: string
  sourceTables: string[]
  sourceFields: string[]
  businessTime: string
  formula: string
  filters: string[]
  permission: string
}

export interface CrmStatisticsMetadataCatalog {
  generatedAt: string
  scope: CrmStatisticsScope
  refreshMode: string
  permissionMode: string
  historyRecalculation: string
  reconciliation: string
  metrics: CrmStatisticsMetricMetadata[]
}

export const getStatisticsMetadataCatalog = (scope: CrmStatisticsScope) =>
  request.get<CrmStatisticsMetadataCatalog>({
    url: '/crm/statistics-metadata/catalog',
    params: { scope }
  })
