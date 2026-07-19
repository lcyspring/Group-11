<!-- 客户总量统计 -->
<template>
  <!-- ECharts 图表 -->
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="500" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <!-- 统计列表 -->
  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column
        align="center"
        fixed="left"
        :label="t('customer.index')"
        type="index"
        width="80"
      />
      <el-table-column
        align="center"
        fixed="left"
        :label="t('funnel.businessName')"
        prop="name"
        min-width="160"
      >
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openDetail(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        fixed="left"
        :label="t('funnel.customerName')"
        prop="customerName"
        min-width="120"
      >
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
        :formatter="erpPriceTableColumnFormatter"
        align="center"
        :label="t('funnel.totalPrice')"
        prop="totalPrice"
        min-width="140"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('funnel.dealTime')"
        prop="dealTime"
        min-width="180"
      />
      <el-table-column align="center" :label="t('funnel.remark')" prop="remark" min-width="200" />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('funnel.contactNextTime')"
        prop="contactNextTime"
        min-width="180"
      />
      <el-table-column
        align="center"
        :label="t('funnel.ownerUserName')"
        prop="ownerUserName"
        min-width="100"
      />
      <el-table-column
        align="center"
        :label="t('funnel.ownerUserDeptName')"
        prop="ownerUserDeptName"
        min-width="100"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('funnel.contactLastTime')"
        prop="contactLastTime"
        min-width="180"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('funnel.updateTime')"
        prop="updateTime"
        min-width="180"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('funnel.createTime')"
        prop="createTime"
        min-width="180"
      />
      <el-table-column
        align="center"
        :label="t('funnel.creatorName')"
        prop="creatorName"
        min-width="100"
      />
      <el-table-column
        align="center"
        fixed="right"
        :label="t('funnel.statusTypeName')"
        prop="statusTypeName"
        min-width="140"
      />
      <el-table-column
        align="center"
        fixed="right"
        :label="t('funnel.statusName')"
        prop="statusName"
        min-width="120"
      />
    </el-table>
    <!-- 分页 -->
    <Pagination
      v-model:limit="queryParams0.pageSize"
      v-model:page="queryParams0.pageNo"
      :total="total"
      @pagination="getList"
    />
  </el-card>
</template>
<script lang="ts" setup>
import {
  CrmStatisticsBusinessInversionRateSummaryByDateRespVO,
  StatisticFunnelApi
} from '@/api/crm/statistics/funnel'
import type { BusinessVO } from '@/api/crm/business'
import { EChartsOption } from 'echarts'
import { erpPriceTableColumnFormatter } from '@/utils'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'BusinessInversionRateSummary' })

const { t } = useI18n('crm.statistics') // 国际化
const props = defineProps<{ queryParams: any }>() // 搜索参数
type BusinessStatisticsRow = BusinessVO & { ownerUserDeptName?: string }
const queryParams0 = reactive({
  pageNo: 1,
  pageSize: 10
})
const loading = ref(false) // 加载中
const list = ref<BusinessStatisticsRow[]>([]) // 商机明细
const total = ref(0)
/** 柱状图配置：纵向 */
const echartsOption = reactive<EChartsOption>({
  color: ['#6ca2ff', '#6ac9d7', '#ff7474'],
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      // 坐标轴指示器，坐标轴触发有效
      type: 'shadow' // 默认为直线，可选为 'line' | 'shadow'
    }
  },
  legend: {
    data: [
      t('funnel.winConversionRate'),
      t('funnel.totalBusinessCount'),
      t('funnel.winBusinessCount')
    ],
    bottom: '0px',
    itemWidth: 14
  },
  grid: {
    top: '40px',
    left: '40px',
    right: '40px',
    bottom: '40px',
    containLabel: true,
    borderColor: '#fff'
  },
  xAxis: [
    {
      type: 'category',
      data: [],
      axisTick: {
        alignWithLabel: true,
        lineStyle: { width: 0 }
      },
      axisLabel: {
        color: '#BDBDBD'
      },
      /** 坐标轴轴线相关设置 */
      axisLine: {
        lineStyle: { color: '#BDBDBD' }
      },
      splitLine: {
        show: false
      }
    }
  ],
  yAxis: [
    {
      type: 'value',
      name: t('funnel.winConversionRate'),
      axisTick: {
        lineStyle: { width: 0 }
      },
      axisLabel: {
        color: '#BDBDBD',
        formatter: '{value}%'
      },
      /** 坐标轴轴线相关设置 */
      axisLine: {
        lineStyle: { color: '#BDBDBD' }
      },
      splitLine: {
        show: false
      }
    },
    {
      type: 'value',
      name: t('funnel.businessCount'),
      axisTick: {
        lineStyle: { width: 0 }
      },
      axisLabel: {
        color: '#BDBDBD',
        formatter: `{value}${t('funnel.businessUnit')}`
      },
      /** 坐标轴轴线相关设置 */
      axisLine: {
        lineStyle: { color: '#BDBDBD' }
      },
      splitLine: {
        show: false
      }
    }
  ],
  series: [
    {
      name: t('funnel.winConversionRate'),
      type: 'line',
      yAxisIndex: 0,
      data: []
    },
    {
      name: t('funnel.totalBusinessCount'),
      type: 'bar',
      yAxisIndex: 1,
      barWidth: 15,
      data: []
    },
    {
      name: t('funnel.winBusinessCount'),
      type: 'bar',
      yAxisIndex: 1,
      barWidth: 15,
      data: []
    }
  ]
}) as EChartsOption

/** 获取数据并填充图表 */
const fetchAndFill = async () => {
  // 1. 加载统计数据
  const businessSummaryByDate = await StatisticFunnelApi.getBusinessInversionRateSummaryByDate(
    props.queryParams
  )
  // 2.1 更新 Echarts 数据
  if (echartsOption.xAxis && echartsOption.xAxis[0] && echartsOption.xAxis[0]['data']) {
    echartsOption.xAxis[0]['data'] = businessSummaryByDate.map(
      (s: CrmStatisticsBusinessInversionRateSummaryByDateRespVO) => s.time
    )
  }
  if (echartsOption.series && echartsOption.series[0] && echartsOption.series[0]['data']) {
    echartsOption.series[0]['data'] = businessSummaryByDate.map(
      (s: CrmStatisticsBusinessInversionRateSummaryByDateRespVO) => s.businessWinRate
    )
  }
  if (echartsOption.series && echartsOption.series[1] && echartsOption.series[1]['data']) {
    echartsOption.series[1]['data'] = businessSummaryByDate.map(
      (s: CrmStatisticsBusinessInversionRateSummaryByDateRespVO) => s.businessCount
    )
  }
  if (echartsOption.series && echartsOption.series[2] && echartsOption.series[2]['data']) {
    echartsOption.series[2]['data'] = businessSummaryByDate.map(
      (s: CrmStatisticsBusinessInversionRateSummaryByDateRespVO) => s.businessWinCount
    )
  }

  // 2.2 更新列表数据
  await getList()
}
/** 获取商机列表 */
const getList = async () => {
  const data = await StatisticFunnelApi.getBusinessPageByDate({
    ...props.queryParams,
    pageNo: queryParams0.pageNo,
    pageSize: queryParams0.pageSize
  })
  list.value = data.list
  total.value = data.total
}
/** 打开客户详情 */
const { push } = useRouter()
const openDetail = (id: number) => {
  push({ name: 'CrmBusinessDetail', params: { id } })
}

/** 打开客户详情 */
const openCustomerDetail = (id: number) => {
  push({ name: 'CrmCustomerDetail', params: { id } })
}

/** 获取统计数据 */
const loadData = async () => {
  loading.value = true
  queryParams0.pageNo = 1
  try {
    await fetchAndFill()
  } finally {
    loading.value = false
  }
}

defineExpose({ loadData })

/** 初始化 */
onMounted(() => {
  loadData()
})
</script>
