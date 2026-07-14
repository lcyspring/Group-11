<template>
  <el-card shadow="never">
    <el-form inline>
      <el-form-item :label="t('performance.targetType')">
        <el-select v-model="targetType" class="!w-220px" @change="loadData">
          <el-option
            v-for="option in targetTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <el-row :gutter="16">
      <el-col :span="8">
        <div class="text-14px text-gray-500">{{ t('performance.annualTarget') }}</div>
        <div class="mt-8px text-24px font-600">{{ formatValue(summary.annualTarget) }}</div>
      </el-col>
      <el-col :span="8">
        <div class="text-14px text-gray-500">{{ t('performance.annualActual') }}</div>
        <div class="mt-8px text-24px font-600">{{ formatValue(summary.annualActual) }}</div>
      </el-col>
      <el-col :span="8">
        <div class="text-14px text-gray-500">{{ t('performance.annualCompletionRate') }}</div>
        <div class="mt-8px text-24px font-600">
          {{ summary.annualCompletionRate == null ? '--' : `${summary.annualCompletionRate}%` }}
        </div>
      </el-col>
    </el-row>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="460" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column align="center" :label="t('performance.date')" prop="time" />
      <el-table-column align="right" :label="t('performance.targetValue')">
        <template #default="scope">{{ formatValue(scope.row.targetValue) }}</template>
      </el-table-column>
      <el-table-column align="right" :label="t('performance.actualValue')">
        <template #default="scope">{{ formatValue(scope.row.actualValue) }}</template>
      </el-table-column>
      <el-table-column align="center" :label="t('performance.completionRate')">
        <template #default="scope">
          {{ scope.row.completionRate == null ? '--' : `${scope.row.completionRate}%` }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script lang="ts" setup>
import { EChartsOption } from 'echarts'
import {
  StatisticsPerformanceApi,
  StatisticsTargetCompletionRespVO,
  StatisticsTargetCompletionSummaryRespVO
} from '@/api/crm/statistics/performance'

defineOptions({ name: 'TargetCompletionPerformance' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const targetType = ref(1)
const list = ref<StatisticsTargetCompletionRespVO[]>([])
const summary = ref<StatisticsTargetCompletionSummaryRespVO>({
  targetType: 1,
  annualTarget: '0',
  annualActual: '0',
  monthlyList: []
})

const targetTypeOptions = computed(() => [
  { value: 1, label: t('performance.targetContractPrice') },
  { value: 2, label: t('performance.targetReceivablePrice') },
  { value: 3, label: t('performance.targetFollowUpCount') },
  { value: 4, label: t('performance.targetCustomerCount') },
  { value: 5, label: t('performance.targetBusinessCount') }
])

const echartsOption = reactive<EChartsOption>({
  legend: {},
  tooltip: { trigger: 'axis' },
  grid: { left: 20, right: 30, bottom: 20, containLabel: true },
  xAxis: { type: 'category', data: [] },
  yAxis: [
    { type: 'value', name: t('performance.targetActualAxis') },
    { type: 'value', name: t('performance.completionRate'), axisLabel: { formatter: '{value}%' } }
  ],
  series: [
    { name: t('performance.targetValue'), type: 'bar', data: [] },
    { name: t('performance.actualValue'), type: 'bar', data: [] },
    {
      name: t('performance.completionRate'),
      type: 'line',
      yAxisIndex: 1,
      connectNulls: false,
      data: []
    }
  ]
}) as EChartsOption

const isAmountTarget = computed(() => targetType.value === 1 || targetType.value === 2)
const formatValue = (value: number | string) => {
  const [integerPart, decimalPart = ''] = String(value ?? '0').split('.')
  const groupedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  if (!isAmountTarget.value) return groupedInteger
  return `${groupedInteger}.${decimalPart.padEnd(2, '0').slice(0, 2)}`
}

const loadData = async () => {
  loading.value = true
  try {
    const data = (await StatisticsPerformanceApi.getTargetCompletion({
      ...props.queryParams,
      scopeType: props.queryParams.userId ? 3 : 2,
      targetType: targetType.value
    })) as StatisticsTargetCompletionSummaryRespVO
    summary.value = data
    list.value = data.monthlyList
    if (echartsOption.xAxis) echartsOption.xAxis['data'] = data.monthlyList.map((row) => row.time)
    if (echartsOption.series) {
      echartsOption.series[0]['data'] = data.monthlyList.map((row) => row.targetValue)
      echartsOption.series[1]['data'] = data.monthlyList.map((row) => row.actualValue)
      echartsOption.series[2]['data'] = data.monthlyList.map((row) => row.completionRate ?? null)
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ loadData })
onMounted(loadData)
</script>
