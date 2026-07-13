<!-- 员工业绩统计 -->
<template>
  <!-- Echarts图表 -->
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="500" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <!-- 统计列表 -->
  <el-card shadow="never" class="mt-16px">
    <el-table v-loading="loading" :data="tableData" :table-layout="'auto'">
      <el-table-column
        v-for="item in columnsData"
        :key="item.prop"
        :label="item.label"
        :prop="item.prop"
        align="center"
      >
        <template #default="scope">
          {{ scope.row[item.prop] }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts">
import { EChartsOption } from 'echarts'
import {
  StatisticsPerformanceApi,
  StatisticsPerformanceRespVO
} from '@/api/crm/statistics/performance'
import { calculateGrowthRate, formatGrowthRate } from '../growthRate'

defineOptions({ name: 'ReceivablePricePerformance' })

const { t } = useI18n('crm.statistics') // 国际化
const props = defineProps<{ queryParams: any }>() // 搜索参数

const loading = ref(false) // 加载中
const list = ref<StatisticsPerformanceRespVO[]>([]) // 列表的数据
/** 柱状图配置：纵向 */
const echartsOption = reactive<EChartsOption>({
  grid: {
    left: 20,
    right: 20,
    bottom: 20,
    containLabel: true
  },
  legend: {},
  series: [
    {
      name: t('performance.currentReceivablePrice'),
      type: 'line',
      data: []
    },
    {
      name: t('performance.lastMonthReceivablePrice'),
      type: 'line',
      data: []
    },
    {
      name: t('performance.lastYearReceivablePrice'),
      type: 'line',
      data: []
    },
    {
      name: t('performance.chainRate'),
      type: 'line',
      yAxisIndex: 1,
      data: []
    },
    {
      name: t('performance.yearOnYearRate'),
      type: 'line',
      yAxisIndex: 1,
      data: []
    }
  ],
  toolbox: {
    feature: {
      dataZoom: {
        xAxisIndex: false // 数据区域缩放：Y 轴不缩放
      },
      brush: {
        type: ['lineX', 'clear'] // 区域缩放按钮、还原按钮
      },
      saveAsImage: { show: true, name: t('performance.receivablePrice') } // 保存为图片
    }
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  yAxis: [
    {
      type: 'value',
      name: t('performance.priceUnit'),
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#BDBDBD',
        formatter: '{value}'
      },
      /** 坐标轴轴线相关设置 */
      axisLine: {
        lineStyle: {
          color: '#BDBDBD'
        }
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#e6e6e6'
        }
      }
    },
    {
      type: 'value',
      name: '',
      axisTick: {
        lineStyle: {
          width: 0
        }
      },
      axisLabel: {
        color: '#BDBDBD',
        formatter: '{value}%'
      },
      /** 坐标轴轴线相关设置 */
      axisLine: {
        lineStyle: {
          color: '#BDBDBD'
        }
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#e6e6e6'
        }
      }
    }
  ],
  xAxis: {
    type: 'category',
    name: t('performance.date'),
    data: []
  }
}) as EChartsOption

/** 获取统计数据 */
const fetchAndFill = async () => {
  const performanceList = await StatisticsPerformanceApi.getReceivablePricePerformance(
    props.queryParams
  )

  // 2.1 更新 Echarts 数据
  if (echartsOption.xAxis && echartsOption.xAxis['data']) {
    echartsOption.xAxis['data'] = performanceList.map((s: StatisticsPerformanceRespVO) => s.time)
  }
  if (echartsOption.series && echartsOption.series[0] && echartsOption.series[0]['data']) {
    echartsOption.series[0]['data'] = performanceList.map(
      (s: StatisticsPerformanceRespVO) => s.currentMonthCount
    )
  }
  if (echartsOption.series && echartsOption.series[1] && echartsOption.series[1]['data']) {
    echartsOption.series[1]['data'] = performanceList.map(
      (s: StatisticsPerformanceRespVO) => s.lastMonthCount
    )
    echartsOption.series[3]['data'] = performanceList.map((s: StatisticsPerformanceRespVO) =>
      calculateGrowthRate(s.currentMonthCount, s.lastMonthCount)
    )
  }
  if (echartsOption.series && echartsOption.series[2] && echartsOption.series[2]['data']) {
    echartsOption.series[2]['data'] = performanceList.map(
      (s: StatisticsPerformanceRespVO) => s.lastYearCount
    )
    echartsOption.series[4]['data'] = performanceList.map((s: StatisticsPerformanceRespVO) =>
      calculateGrowthRate(s.currentMonthCount, s.lastYearCount)
    )
  }

  // 2.2 更新列表数据
  list.value = performanceList
  convertListData()
}

/** 获取统计数据 */
const loadData = async () => {
  loading.value = true
  try {
    await fetchAndFill()
  } finally {
    loading.value = false
  }
}

// 初始化数据
interface StatisticsTableRow {
  title: string
  [key: string]: string | number
}
const columnsData = reactive<Array<{ label: string; prop: string }>>([])
const tableData = reactive<StatisticsTableRow[]>([
  { title: t('performance.currentReceivablePrice') },
  { title: t('performance.lastMonthReceivablePrice') },
  { title: t('performance.lastYearReceivablePrice') },
  { title: t('performance.chainRate') },
  { title: t('performance.yearOnYearRate') }
])

// 定义 convertListData 方法
const convertListData = () => {
  const columnObj = { label: t('performance.date'), prop: 'title' }
  columnsData.splice(0, columnsData.length) //清空数组
  columnsData.push(columnObj)

  list.value.forEach((item, index) => {
    const columnObj = { label: item.time, prop: 'prop' + index }
    columnsData.push(columnObj)
    tableData[0]['prop' + index] = item.currentMonthCount
    tableData[1]['prop' + index] = item.lastMonthCount
    tableData[2]['prop' + index] = item.lastYearCount
    tableData[3]['prop' + index] = formatGrowthRate(item.currentMonthCount, item.lastMonthCount)
    tableData[4]['prop' + index] = formatGrowthRate(item.currentMonthCount, item.lastYearCount)
  })
}

defineExpose({ loadData })

/** 初始化 */
onMounted(async () => {
  await loadData()
})
</script>
