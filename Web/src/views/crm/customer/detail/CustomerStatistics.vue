<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never" :title="t('statistics')">
          <Echart :height="300" :options="statusChartOption" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" :title="t('levelDistribution')">
          <Echart :height="300" :options="levelChartOption" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" class="mt-16px">
      <el-col :span="12">
        <el-card shadow="never" :title="t('sourceDistribution')">
          <Echart :height="300" :options="sourceChartOption" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" :title="t('industryDistribution')">
          <Echart :height="300" :options="industryChartOption" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue'
import type { EChartsOption } from 'echarts'
import { StatisticsPortraitApi } from '@/api/crm/statistics/portrait'

const props = defineProps<{
  customerId: number
}>()

const { t } = useI18n('crm.customer')

const statusList = ref<any[]>([])
const levelList = ref<any[]>([])
const sourceList = ref<any[]>([])
const industryList = ref<any[]>([])

const getChartOption = (list: any[], labelField: string, valueField: string, title: string): EChartsOption => {
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

const statusChartOption = computed(() => getChartOption(statusList.value, 'status', 'customerCount', t('status')))
const levelChartOption = computed(() => getChartOption(levelList.value, 'level', 'customerCount', t('level')))
const sourceChartOption = computed(() => getChartOption(sourceList.value, 'source', 'customerCount', t('source')))
const industryChartOption = computed(() => getChartOption(industryList.value, 'industryId', 'customerCount', t('industryId')))

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
}

onMounted(() => {
  loadData()
})

watch(() => props.customerId, () => {
  loadData()
})
</script>