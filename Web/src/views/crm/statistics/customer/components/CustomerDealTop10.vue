<template>
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="480" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column align="center" :label="t('customer.rank')" type="index" width="100" />
      <el-table-column align="center" :label="t('customer.customerName')" min-width="220">
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openCustomer(scope.row.customerId)">
            {{ scope.row.customerName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        align="right"
        :label="t('customer.contractCount')"
        prop="contractCount"
        min-width="180"
      />
      <el-table-column
        align="right"
        :formatter="erpPriceTableColumnFormatter"
        :label="t('customer.dealAmount')"
        prop="contractAmount"
        min-width="200"
      />
    </el-table>
  </el-card>
</template>

<script lang="ts" setup>
import {
  CrmStatisticsCustomerDealTopRespVO,
  StatisticsCustomerApi
} from '@/api/crm/statistics/customer'
import { EChartsOption } from 'echarts'
import { erpPriceTableColumnFormatter } from '@/utils'

defineOptions({ name: 'CustomerDealTop10' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const list = ref<CrmStatisticsCustomerDealTopRespVO[]>([])

const echartsOption = reactive<EChartsOption>({
  grid: { left: 30, right: 40, bottom: 20, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  toolbox: { feature: { saveAsImage: { show: true, name: t('customer.dealTop10') } } },
  xAxis: { type: 'value', min: 0, name: t('customer.dealAmount') },
  yAxis: { type: 'category', inverse: true, data: [] },
  series: [{ name: t('customer.dealAmount'), type: 'bar', data: [] }]
}) as EChartsOption

const loadData = async () => {
  loading.value = true
  try {
    const rows = await StatisticsCustomerApi.getCustomerDealTop10(props.queryParams)
    list.value = rows
    if (echartsOption.yAxis && echartsOption.yAxis['data']) {
      echartsOption.yAxis['data'] = rows.map((item: CrmStatisticsCustomerDealTopRespVO) =>
        item.customerName
      )
    }
    if (echartsOption.series?.[0]?.['data']) {
      echartsOption.series[0]['data'] = rows.map(
        (item: CrmStatisticsCustomerDealTopRespVO) => item.contractAmount
      )
    }
  } finally {
    loading.value = false
  }
}

const { push } = useRouter()
const openCustomer = (id: number) => push({ name: 'CrmCustomerDetail', params: { id } })

defineExpose({ loadData })
onMounted(loadData)
</script>
