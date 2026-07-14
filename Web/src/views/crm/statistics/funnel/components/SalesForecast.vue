<template>
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="420" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="summaryList" :table-layout="'auto'">
      <el-table-column align="center" :label="t('customer.index')" type="index" width="80" />
      <el-table-column align="center" :label="t('customer.date')" prop="time" min-width="160" />
      <el-table-column
        align="right"
        :label="t('funnel.forecastBusinessCount')"
        prop="businessCount"
        min-width="160"
      />
      <el-table-column
        align="right"
        :formatter="erpPriceTableColumnFormatter"
        :label="t('funnel.expectedAmount')"
        prop="expectedAmount"
        min-width="180"
      />
      <el-table-column
        align="right"
        :formatter="erpPriceTableColumnFormatter"
        :label="t('funnel.weightedAmount')"
        prop="weightedAmount"
        min-width="180"
      />
    </el-table>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="businessList" :table-layout="'auto'">
      <el-table-column align="center" :label="t('customer.index')" type="index" width="80" />
      <el-table-column align="center" :label="t('funnel.businessName')" min-width="180">
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openBusinessDetail(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('funnel.customerName')" min-width="160">
        <template #default="scope">
          <el-link
            :underline="false"
            type="primary"
            @click="openCustomerDetail(scope.row.customerId)"
          >
            {{ scope.row.customerName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        align="right"
        :formatter="erpPriceTableColumnFormatter"
        :label="t('funnel.totalPrice')"
        prop="totalPrice"
        min-width="160"
      />
      <el-table-column
        align="center"
        :formatter="dateFormatter"
        :label="t('funnel.dealTime')"
        prop="dealTime"
        min-width="180"
      />
      <el-table-column align="center" :label="t('funnel.statusName')" prop="statusName" min-width="140" />
      <el-table-column align="right" :label="t('winRate')" min-width="120">
        <template #default="scope">{{ scope.row.statusPercent }}%</template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('funnel.ownerUserName')"
        prop="ownerUserName"
        min-width="140"
      />
    </el-table>
    <Pagination
      v-model:limit="pageParams.pageSize"
      v-model:page="pageParams.pageNo"
      :total="total"
      @pagination="loadBusinessPage"
    />
  </el-card>
</template>

<script lang="ts" setup>
import {
  CrmStatisticsBusinessForecastByDateRespVO,
  StatisticFunnelApi
} from '@/api/crm/statistics/funnel'
import type { BusinessVO } from '@/api/crm/business'
import { EChartsOption } from 'echarts'
import { erpPriceTableColumnFormatter } from '@/utils'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SalesForecast' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const summaryList = ref<CrmStatisticsBusinessForecastByDateRespVO[]>([])
const businessList = ref<BusinessVO[]>([])
const total = ref(0)
const pageParams = reactive({ pageNo: 1, pageSize: 10 })

const echartsOption = reactive<EChartsOption>({
  grid: { left: 30, right: 30, bottom: 20, containLabel: true },
  legend: {},
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  toolbox: { feature: { saveAsImage: { show: true, name: t('funnel.salesForecast') } } },
  xAxis: { type: 'category', name: t('customer.date'), data: [] },
  yAxis: { type: 'value', min: 0 },
  series: [
    { name: t('funnel.expectedAmount'), type: 'bar', data: [] },
    { name: t('funnel.weightedAmount'), type: 'bar', data: [] }
  ]
}) as EChartsOption

const fillChart = (rows: CrmStatisticsBusinessForecastByDateRespVO[]) => {
  if (echartsOption.xAxis && echartsOption.xAxis['data']) {
    echartsOption.xAxis['data'] = rows.map((item) => item.time)
  }
  if (echartsOption.series?.[0]?.['data']) {
    echartsOption.series[0]['data'] = rows.map((item) => item.expectedAmount)
  }
  if (echartsOption.series?.[1]?.['data']) {
    echartsOption.series[1]['data'] = rows.map((item) => item.weightedAmount)
  }
}

const loadBusinessPage = async () => {
  const data = await StatisticFunnelApi.getBusinessForecastPage({
    ...props.queryParams,
    pageNo: pageParams.pageNo,
    pageSize: pageParams.pageSize
  })
  businessList.value = data.list
  total.value = data.total
}

const loadData = async () => {
  loading.value = true
  pageParams.pageNo = 1
  try {
    const [rows] = await Promise.all([
      StatisticFunnelApi.getBusinessForecastByDate(props.queryParams),
      loadBusinessPage()
    ])
    summaryList.value = rows
    fillChart(rows)
  } finally {
    loading.value = false
  }
}

const { push } = useRouter()
const openBusinessDetail = (id: number) => push({ name: 'CrmBusinessDetail', params: { id } })
const openCustomerDetail = (id: number) => push({ name: 'CrmCustomerDetail', params: { id } })

defineExpose({ loadData })
onMounted(loadData)
</script>
