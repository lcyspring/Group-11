<template>
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="500" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column align="center" :label="t('customer.index')" type="index" width="80" />
      <el-table-column align="center" :label="t('funnel.stage')" min-width="180">
        <template #default="scope">
          <dict-tag
            v-if="scope.row.endStatus"
            :type="DICT_TYPE.CRM_BUSINESS_END_STATUS_TYPE"
            :value="scope.row.endStatus"
          />
          <span v-else>{{ scope.row.statusName }}</span>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('funnel.businessCount')"
        min-width="140"
        prop="businessCount"
      />
      <el-table-column
        align="center"
        :label="t('funnel.businessTotalPrice')"
        min-width="180"
        prop="totalPrice"
      />
      <el-table-column align="center" :label="t('funnel.stageConversionRate')" min-width="160">
        <template #default="scope">{{ scope.row.conversionRate }}%</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script lang="ts" setup>
import {
  CrmStatisticsBusinessStageSummaryRespVO,
  StatisticFunnelApi
} from '@/api/crm/statistics/funnel'
import { DICT_TYPE } from '@/utils/dict'
import { EChartsOption } from 'echarts'

defineOptions({ name: 'FunnelBusiness' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const list = ref<CrmStatisticsBusinessStageSummaryRespVO[]>([])

const echartsOption = reactive<EChartsOption>({
  title: { text: t('funnel.funnel') },
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      const row = params.data.raw as CrmStatisticsBusinessStageSummaryRespVO
      return `${params.name}<br/>${t('funnel.businessTotalPrice')}: ${row.totalPrice}<br/>${t(
        'funnel.stageConversionRate'
      )}: ${row.conversionRate}%`
    }
  },
  toolbox: {
    feature: {
      dataView: { readOnly: true },
      restore: {},
      saveAsImage: {}
    }
  },
  series: [
    {
      name: t('funnel.funnel'),
      type: 'funnel',
      left: '10%',
      top: 60,
      bottom: 40,
      width: '80%',
      min: 0,
      minSize: '0%',
      maxSize: '100%',
      sort: 'none',
      gap: 2,
      label: { show: true, position: 'inside' },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis: { label: { fontSize: 18 } },
      data: []
    }
  ]
}) as EChartsOption

const stageLabel = (row: CrmStatisticsBusinessStageSummaryRespVO) =>
  row.endStatus ? t('funnel.win') : row.statusName || '-'

const clearData = () => {
  list.value = []
  if (echartsOption.series?.[0]) {
    echartsOption.series[0]['data'] = []
  }
}

const loadData = async () => {
  if (!props.queryParams.statusTypeId) {
    clearData()
    return
  }
  loading.value = true
  try {
    const data = (await StatisticFunnelApi.getBusinessStageSummary(
      props.queryParams
    )) as CrmStatisticsBusinessStageSummaryRespVO[]
    list.value = data
    if (echartsOption.series?.[0]) {
      echartsOption.series[0]['data'] = data.map((row) => ({
        value: row.businessCount,
        name: `${stageLabel(row)}-${row.businessCount}${t('funnel.unit')}`,
        raw: row
      }))
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ loadData })
onMounted(loadData)
</script>
