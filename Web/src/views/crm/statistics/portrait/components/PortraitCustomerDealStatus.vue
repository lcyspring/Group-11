<!-- 客户成交状态分析 -->
<template>
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="500" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column align="center" :label="t('portrait.index')" type="index" width="80" />
      <el-table-column align="center" :label="t('portrait.dealStatusName')" min-width="200">
        <template #default="scope">
          {{ statusLabel(scope.row.dealStatus) }}
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('portrait.customerCount')"
        min-width="200"
        prop="customerCount"
      />
      <el-table-column
        align="center"
        :label="t('portrait.customerPortion')"
        min-width="200"
        prop="customerPortion"
      />
    </el-table>
  </el-card>
</template>

<script lang="ts" setup>
import {
  CrmStatisticCustomerDealStatusRespVO,
  StatisticsPortraitApi
} from '@/api/crm/statistics/portrait'
import { EChartsOption } from 'echarts'
import { erpCalculatePercentage, getSumValue } from '@/utils'

defineOptions({ name: 'PortraitCustomerDealStatus' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const list = ref<CrmStatisticCustomerDealStatusRespVO[]>([])

const statusLabel = (dealStatus: boolean) =>
  dealStatus ? t('portrait.dealStatusDeal') : t('portrait.dealStatusUndeal')

const echartsOption = reactive<EChartsOption>({
  title: {
    text: t('portrait.dealStatus'),
    left: 'center'
  },
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  toolbox: {
    feature: { saveAsImage: { show: true, name: t('portrait.dealStatus') } }
  },
  series: [
    {
      name: t('portrait.dealStatus'),
      type: 'pie',
      radius: ['40%', '70%'],
      data: []
    }
  ]
}) as EChartsOption

const loadData = async () => {
  loading.value = true
  try {
    const rows = await StatisticsPortraitApi.getCustomerDealStatus(props.queryParams)
    const total = getSumValue(rows.map((item: CrmStatisticCustomerDealStatusRespVO) => item.customerCount))
    rows.forEach((item: CrmStatisticCustomerDealStatusRespVO) => {
      item.customerPortion = erpCalculatePercentage(item.customerCount, total)
    })
    if (echartsOption.series?.[0]?.['data']) {
      echartsOption.series[0]['data'] = rows.map((item: CrmStatisticCustomerDealStatusRespVO) => ({
        name: statusLabel(item.dealStatus),
        value: item.customerCount
      }))
    }
    list.value = rows
  } finally {
    loading.value = false
  }
}

defineExpose({ loadData })

onMounted(loadData)
</script>
