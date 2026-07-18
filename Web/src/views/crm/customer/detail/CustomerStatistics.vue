<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never" :title="t('businessStatusDistribution')">
          <Echart :height="300" :options="businessStatusChartOption" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" :title="t('businessAmountDistribution')">
          <Echart :height="300" :options="businessAmountChartOption" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" class="mt-16px">
      <el-col :span="12">
        <el-card shadow="never" :title="t('customerLevelDistribution')">
          <Echart :height="300" :options="levelChartOption" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" :title="t('customerSourceDistribution')">
          <Echart :height="300" :options="sourceChartOption" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" class="mt-16px">
      <el-col :span="12">
        <el-card shadow="never" :title="t('customerIndustryDistribution')">
          <Echart :height="300" :options="industryChartOption" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" :title="t('customerStatusDistribution')">
          <Echart :height="300" :options="statusChartOption" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue'
import type { EChartsOption } from 'echarts'
import { StatisticsPortraitApi } from '@/api/crm/statistics/portrait'
import * as BusinessApi from '@/api/crm/business'

const props = defineProps<{
  customerId: number
}>()

const { t } = useI18n('crm.customer')

const statusList = ref<any[]>([])
const levelList = ref<any[]>([])
const sourceList = ref<any[]>([])
const industryList = ref<any[]>([])
const businessStatusList = ref<any[]>([])
const businessAmountList = ref<any[]>([])

const getPieChartOption = (list: any[], labelField: string, valueField: string, title: string): EChartsOption => {
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: list.map((item, index) => ({
          value: item[valueField],
          name: item[labelField],
          itemStyle: { color: colors[index % colors.length] }
        }))
      }
    ]
  }
}

const getBarChartOption = (list: any[], labelField: string, valueField: string, title: string): EChartsOption => {
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: list.map(item => item[labelField])
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: title,
        type: 'bar',
        data: list.map((item, index) => ({
          value: item[valueField],
          itemStyle: { color: colors[index % colors.length], borderRadius: [4, 4, 0, 0] }
        }))
      }
    ]
  }
}

const statusChartOption = computed(() => getPieChartOption(statusList.value, 'status', 'customerCount', t('status')))
const levelChartOption = computed(() => getPieChartOption(levelList.value, 'level', 'customerCount', t('level')))
const sourceChartOption = computed(() => getPieChartOption(sourceList.value, 'source', 'customerCount', t('source')))
const industryChartOption = computed(() => getPieChartOption(industryList.value, 'industryId', 'customerCount', t('industryId')))
const businessStatusChartOption = computed(() => getPieChartOption(businessStatusList.value, 'statusName', 'count', t('businessStatus')))
const businessAmountChartOption = computed(() => getBarChartOption(businessAmountList.value, 'statusTypeName', 'amount', t('businessAmount')))

const loadBusinessStatistics = async () => {
  try {
    const data = await BusinessApi.getBusinessPageByCustomer({
      customerId: props.customerId,
      pageNo: 1,
      pageSize: 1000
    })
    
    const statusMap: Record<string, number> = {}
    const statusTypeMap: Record<string, number> = {}
    
    data.list.forEach((item: any) => {
      const statusName = item.statusName || '未知状态'
      const statusTypeName = item.statusTypeName || '未知类型'
      statusMap[statusName] = (statusMap[statusName] || 0) + 1
      statusTypeMap[statusTypeName] = (statusTypeMap[statusTypeName] || 0) + (item.totalPrice || 0)
    })
    
    businessStatusList.value = Object.entries(statusMap).map(([statusName, count]) => ({ statusName, count }))
    businessAmountList.value = Object.entries(statusTypeMap).map(([statusTypeName, amount]) => ({ statusTypeName, amount }))
  } catch (e) {
    console.error('加载商机统计数据失败', e)
    businessStatusList.value = []
    businessAmountList.value = []
  }
}

const loadData = async () => {
  try {
    const [statusData, levelData, sourceData, industryData] = await Promise.all([
      StatisticsPortraitApi.getCustomerStatus({}),
      StatisticsPortraitApi.getCustomerLevel({}),
      StatisticsPortraitApi.getCustomerSource({}),
      StatisticsPortraitApi.getCustomerIndustry({})
    ])
    statusList.value = statusData
    levelList.value = levelData
    sourceList.value = sourceData
    industryList.value = industryData
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
  await loadBusinessStatistics()
}

onMounted(() => {
  loadData()
})

watch(() => props.customerId, () => {
  loadData()
})
</script>