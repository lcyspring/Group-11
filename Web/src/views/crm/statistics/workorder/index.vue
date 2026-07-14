<template>
  <ContentWrap>
    <el-form :model="queryParams" inline label-width="auto" class="-mb-15px">
      <el-form-item :label="t('timeRange')">
        <el-date-picker
          v-model="queryParams.times" type="daterange" value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          :start-placeholder="t('common.startTime')" :end-placeholder="t('common.endTime')" />
      </el-form-item>
      <el-form-item :label="t('timeInterval')">
        <el-select v-model="queryParams.interval" class="!w-180px">
          <el-option v-for="item in getIntDictOptions(DICT_TYPE.DATE_INTERVAL)" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="loadData"><Icon icon="ep:search" class="mr-5px" />{{ t('search') }}</el-button></el-form-item>
    </el-form>
  </ContentWrap>

  <StatisticsLineagePanel
    ref="statisticsLineageRef"
    scope="workorder"
    :on-refresh="loadData"
  />

  <el-row :gutter="16" v-loading="loading">
    <el-col v-for="card in summaryCards" :key="card.label" :xs="12" :sm="8" :lg="4">
      <el-card shadow="never" class="mb-16px"><el-statistic :title="card.label" :value="card.value" /></el-card>
    </el-col>
  </el-row>

  <el-row :gutter="16">
    <el-col :span="24"><el-card shadow="never" v-loading="loading"><template #header>{{ t('workOrder.trend') }}</template><Echart :height="360" :options="trendOptions" /></el-card></el-col>
  </el-row>
  <el-row :gutter="16" class="mt-16px">
    <el-col :xs="24" :lg="8"><el-card shadow="never"><template #header>{{ t('workOrder.byStatus') }}</template><el-table :data="statusRows"><el-table-column :label="t('workOrder.dimension')"><template #default="{ row }">{{ statusLabel(row.status) }}</template></el-table-column><el-table-column :label="t('workOrder.count')" prop="count" align="right" /></el-table></el-card></el-col>
    <el-col :xs="24" :lg="8"><el-card shadow="never"><template #header>{{ t('workOrder.byType') }}</template><el-table :data="typeRows"><el-table-column :label="t('workOrder.dimension')"><template #default="{ row }">{{ typeLabel(row.type) }}</template></el-table-column><el-table-column :label="t('workOrder.count')" prop="count" align="right" /></el-table></el-card></el-col>
    <el-col :xs="24" :lg="8"><el-card shadow="never"><template #header>{{ t('workOrder.byHandler') }}</template><el-table :data="handlerRows"><el-table-column :label="t('workOrder.handler')" prop="handlerUserName" /><el-table-column :label="t('workOrder.count')" prop="count" align="right" /></el-table></el-card></el-col>
  </el-row>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { beginOfDay, endOfDay, formatDate } from '@/utils/formatTime'
import { WorkOrderStatisticsApi, WorkOrderStatisticsHandler, WorkOrderStatisticsStatus, WorkOrderStatisticsSummary, WorkOrderStatisticsTrend, WorkOrderStatisticsType } from '@/api/crm/statistics/workorder'
import type { EChartsOption } from 'echarts'
import StatisticsLineagePanel from '../components/StatisticsLineagePanel.vue'

defineOptions({ name: 'CrmStatisticsWorkOrder' })
const { t } = useI18n('crm.statistics')
const loading = ref(false)
const now = Date.now()
const queryParams = reactive({ interval: 1, times: [formatDate(beginOfDay(new Date(now - 30 * 86400000))), formatDate(endOfDay(new Date(now)))] })
const summary = ref<WorkOrderStatisticsSummary>({ totalCount: 0, pendingCount: 0, processingCount: 0, completedCount: 0, returnedCount: 0, completionRate: '0%' })
const statusRows = ref<WorkOrderStatisticsStatus[]>([])
const typeRows = ref<WorkOrderStatisticsType[]>([])
const handlerRows = ref<WorkOrderStatisticsHandler[]>([])
const statisticsLineageRef = ref<InstanceType<typeof StatisticsLineagePanel>>()

const summaryCards = computed(() => [
  { label: t('workOrder.total'), value: summary.value.totalCount },
  { label: t('workOrder.pending'), value: summary.value.pendingCount },
  { label: t('workOrder.processing'), value: summary.value.processingCount },
  { label: t('workOrder.completed'), value: summary.value.completedCount },
  { label: t('workOrder.returned'), value: summary.value.returnedCount },
  { label: t('workOrder.completionRate'), value: summary.value.completionRate }
])

const trendOptions = reactive<EChartsOption>({
  tooltip: { trigger: 'axis' }, legend: {}, grid: { left: 30, right: 30, bottom: 20, containLabel: true },
  xAxis: { type: 'category', data: [] }, yAxis: { type: 'value', minInterval: 1 },
  series: [{ name: t('workOrder.created'), type: 'line', data: [], smooth: true }, { name: t('workOrder.completed'), type: 'line', data: [], smooth: true }]
})

const loadData = async () => {
  loading.value = true
  try {
    const [summaryData, trend, statuses, types, handlers] = await Promise.all([
      WorkOrderStatisticsApi.getSummary(queryParams), WorkOrderStatisticsApi.getTrend(queryParams),
      WorkOrderStatisticsApi.getByStatus(queryParams), WorkOrderStatisticsApi.getByType(queryParams), WorkOrderStatisticsApi.getByHandler(queryParams)
    ])
    summary.value = summaryData
    statusRows.value = statuses
    typeRows.value = types
    handlerRows.value = handlers
    const rows = trend as WorkOrderStatisticsTrend[]
    ;(trendOptions.xAxis as any).data = rows.map(row => row.time)
    ;(trendOptions.series as any[])[0].data = rows.map(row => row.createdCount)
    ;(trendOptions.series as any[])[1].data = rows.map(row => row.completedCount)
    statisticsLineageRef.value?.markRefreshed()
  } finally { loading.value = false }
}
const statusLabel = (value: number) => ({ 10: t('workOrder.pending'), 20: t('workOrder.processing'), 30: t('workOrder.completed'), 40: t('workOrder.returned') } as Record<number, string>)[value] || value
const typeLabel = (value: number) => ({ 1: t('workOrder.issue'), 2: t('workOrder.demand'), 3: t('workOrder.complaint'), 4: t('workOrder.consultation') } as Record<number, string>)[value] || value
onMounted(loadData)
</script>
