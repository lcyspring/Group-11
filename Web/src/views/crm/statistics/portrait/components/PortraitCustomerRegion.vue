<template>
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="500" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column align="center" :label="t('portrait.index')" type="index" width="80" />
      <el-table-column align="center" :label="t(`portrait.${regionType}Name`)" min-width="200">
        <template #default="scope">
          <el-link
            v-if="scope.row.areaId !== null"
            :underline="false"
            type="primary"
            @click="openRegion(scope.row.areaId, scope.row.areaName)"
          >
            {{ scope.row.areaName }}
          </el-link>
          <span v-else>{{ scope.row.areaName }}</span>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('portrait.customerCount')"
        min-width="160"
        prop="customerCount"
      />
      <el-table-column
        align="center"
        :label="t('portrait.dealCustomerCount')"
        min-width="160"
        prop="dealCount"
      />
      <el-table-column
        align="center"
        :label="t('portrait.customerPortion')"
        min-width="160"
        prop="areaPortion"
      />
      <el-table-column
        align="center"
        :label="t('portrait.dealPortion')"
        min-width="160"
        prop="dealPortion"
      />
    </el-table>
  </el-card>
  <PortraitCustomerRegionDetail ref="detailRef" :query-params="queryParams" />
</template>

<script lang="ts" setup>
import {
  CrmStatisticCustomerAreaRespVO,
  StatisticsPortraitApi
} from '@/api/crm/statistics/portrait'
import { EChartsOption } from 'echarts'
import { erpCalculatePercentage, getSumValue } from '@/utils'
import PortraitCustomerRegionDetail from './PortraitCustomerRegionDetail.vue'

defineOptions({ name: 'PortraitCustomerRegion' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{
  queryParams: any
  regionType: 'city' | 'country'
}>()
const loading = ref(false)
const list = ref<CrmStatisticCustomerAreaRespVO[]>([])
const detailRef = ref<InstanceType<typeof PortraitCustomerRegionDetail>>()

const openRegion = (areaId: number, areaName: string) => {
  detailRef.value?.open(areaId, areaName, props.regionType === 'city' ? 3 : 1)
}

const echartsOption = reactive<EChartsOption>({
  grid: { left: 30, right: 40, bottom: 20, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { top: 0 },
  toolbox: { feature: { saveAsImage: { show: true } } },
  xAxis: { type: 'value', min: 0 },
  yAxis: { type: 'category', inverse: true, data: [] },
  series: [
    { name: t('portrait.allCustomer'), type: 'bar', data: [] },
    { name: t('portrait.dealCustomer'), type: 'bar', data: [] }
  ]
}) as EChartsOption

const loadData = async () => {
  loading.value = true
  try {
    const rows =
      props.regionType === 'city'
        ? await StatisticsPortraitApi.getCustomerCity(props.queryParams)
        : await StatisticsPortraitApi.getCustomerCountry(props.queryParams)
    const customerTotal = getSumValue(
      rows.map((item: CrmStatisticCustomerAreaRespVO) => item.customerCount)
    )
    const dealTotal = getSumValue(
      rows.map((item: CrmStatisticCustomerAreaRespVO) => item.dealCount)
    )
    rows.forEach((item: CrmStatisticCustomerAreaRespVO) => {
      item.areaPortion = erpCalculatePercentage(item.customerCount, customerTotal)
      item.dealPortion = erpCalculatePercentage(item.dealCount, dealTotal)
    })
    list.value = rows
    if (echartsOption.yAxis && echartsOption.yAxis['data']) {
      echartsOption.yAxis['data'] = rows.map(
        (item: CrmStatisticCustomerAreaRespVO) => item.areaName
      )
    }
    if (echartsOption.series?.[0]?.['data']) {
      echartsOption.series[0]['data'] = rows.map(
        (item: CrmStatisticCustomerAreaRespVO) => item.customerCount
      )
    }
    if (echartsOption.series?.[1]?.['data']) {
      echartsOption.series[1]['data'] = rows.map(
        (item: CrmStatisticCustomerAreaRespVO) => item.dealCount
      )
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ loadData })
onMounted(loadData)
</script>
